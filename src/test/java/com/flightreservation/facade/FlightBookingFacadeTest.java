package com.flightreservation.facade;

import com.flightreservation.coordinator.BookingJournalRepository;
import com.flightreservation.coordinator.BookingStage;
import com.flightreservation.coordinator.BookingTransaction;
import com.flightreservation.dsl.FlightBookingDsl;
import com.flightreservation.exception.InvalidBookingException;
import com.flightreservation.exception.SeatUnavailableException;
import com.flightreservation.subsystem.loyalty.LoyaltyPointsCalculator;
import com.flightreservation.subsystem.loyalty.LoyaltyRecordRepository;
import com.flightreservation.subsystem.loyalty.LoyaltyStatus;
import com.flightreservation.subsystem.loyalty.MealPreferenceValidator;
import com.flightreservation.subsystem.seat.FareCalculator;
import com.flightreservation.subsystem.seat.SeatMapValidator;
import com.flightreservation.subsystem.seat.SeatReservation;
import com.flightreservation.subsystem.seat.SeatReservationRepository;
import com.flightreservation.subsystem.seat.SeatReservationStatus;
import com.flightreservation.subsystem.ticket.BaggageRulesEngine;
import com.flightreservation.subsystem.ticket.TicketIssuance;
import com.flightreservation.subsystem.ticket.TicketIssuanceRepository;
import com.flightreservation.subsystem.ticket.TicketNumberGenerator;
import com.flightreservation.subsystem.ticket.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlightBookingFacadeTest {

    @TempDir
    Path tempDir;

    private SeatReservationRepository seatRepository;
    private TicketIssuanceRepository ticketRepository;
    private LoyaltyRecordRepository loyaltyRepository;
    private BookingJournalRepository journalRepository;

    private FlightBookingFacade bookingFacade;
    private FlightOperationsFacade operationsFacade;

    @BeforeEach
    void setUp() {
        Path seatPath = tempDir.resolve("seats.json");
        Path ticketPath = tempDir.resolve("tickets.json");
        Path loyaltyPath = tempDir.resolve("loyalty.json");
        Path journalPath = tempDir.resolve("journal.json");

        seatRepository = new SeatReservationRepository(seatPath);
        FareCalculator fareCalculator = new FareCalculator();
        SeatMapValidator seatMapValidator = new SeatMapValidator();

        ticketRepository = new TicketIssuanceRepository(ticketPath);
        BaggageRulesEngine baggageRulesEngine = new BaggageRulesEngine();
        TicketNumberGenerator ticketGenerator = new TicketNumberGenerator();

        loyaltyRepository = new LoyaltyRecordRepository(loyaltyPath);
        LoyaltyPointsCalculator pointsCalculator = new LoyaltyPointsCalculator();
        MealPreferenceValidator mealValidator = new MealPreferenceValidator();

        journalRepository = new BookingJournalRepository(journalPath);

        bookingFacade = new FlightBookingFacade(
                seatRepository,
                fareCalculator,
                seatMapValidator,
                ticketRepository,
                baggageRulesEngine,
                ticketGenerator,
                loyaltyRepository,
                pointsCalculator,
                mealValidator,
                journalRepository);

        operationsFacade = new FlightOperationsFacade(
                seatRepository,
                ticketRepository,
                loyaltyRepository,
                journalRepository);
    }

    @Test
    void shouldCompleteFullThreeStepChainSuccessfully() {
        FlightBookingDsl.given(bookingFacade, operationsFacade)
                .forPassenger("PAX-001", "Emma Watson")
                .flying("AA101")
                .requestingSeat("12A", "ECONOMY")
                .withBaggage(18.0)
                .withLoyalty("MEM-888", "GOLD", "VEGAN")
                .whenBooking()
                .thenExpectSuccess()
                .andVerifyPersistedStates(
                        SeatReservationStatus.CONFIRMED,
                        TicketStatus.COMPLETED,
                        LoyaltyStatus.SETTLED);
    }

    @Test
    void shouldCompensateSubsystemAAloneWhenStepBFailsAfterACommitted() {
        FlightBookingDsl.given(bookingFacade, operationsFacade)
                .forPassenger("PAX-002", "Heavy Luggage Passenger")
                .flying("AA102")
                .requestingSeat("14B", "ECONOMY")
                .withBaggage(35.0)
                .withLoyalty("MEM-777", "STANDARD", "STANDARD")
                .whenBooking()
                .thenExpectFailureAtStepB()
                .andVerifyCompensatedAOnly();

        assertThat(ticketRepository.findAll()).isEmpty();
        assertThat(loyaltyRepository.findAll()).isEmpty();
        assertThat(seatRepository.findAll()).hasSize(1);
        assertThat(seatRepository.findAll().getFirst().getStatus()).isEqualTo(SeatReservationStatus.RELEASED);
    }

    @Test
    void shouldCompensateSubsystemBThenSubsystemAWhenStepCFailsAfterAAndBCommitted() {
        FlightBookingDsl.given(bookingFacade, operationsFacade)
                .forPassenger("PAX-003", "Restricted Member Passenger")
                .flying("AA103")
                .requestingSeat("16C", "BUSINESS")
                .withBaggage(15.0)
                .withLoyalty("SUSPENDED_USER_99", "PLATINUM", "STANDARD")
                .whenBooking()
                .thenExpectFailureAtStepC()
                .andVerifyCompensatedFull();

        assertThat(loyaltyRepository.findAll()).isEmpty();
        assertThat(ticketRepository.findAll()).hasSize(1);
        assertThat(ticketRepository.findAll().getFirst().getStatus()).isEqualTo(TicketStatus.VOIDED);
        assertThat(seatRepository.findAll()).hasSize(1);
        assertThat(seatRepository.findAll().getFirst().getStatus()).isEqualTo(SeatReservationStatus.RELEASED);
    }

    @Test
    void shouldFailPreCommitWhenSeatIsInvalidOrUnavailableWithoutWritingToStorage() {
        FlightBookingDsl.given(bookingFacade, operationsFacade)
                .forPassenger("PAX-004", "Invalid Seat Guy")
                .flying("AA104")
                .requestingSeat("99Z", "ECONOMY")
                .withBaggage(10.0)
                .withLoyalty("MEM-101", "STANDARD", "STANDARD")
                .whenBookingExpectingException(SeatUnavailableException.class);

        assertThat(seatRepository.findAll()).isEmpty();
        assertThat(ticketRepository.findAll()).isEmpty();
        assertThat(loyaltyRepository.findAll()).isEmpty();
        assertThat(journalRepository.findAll()).isEmpty();
    }

    @Test
    void shouldFailPreCommitWhenRequestDataIsBlank() {
        BookingRequest nullPassenger = BookingRequest.builder()
                .passengerName("")
                .build();
        assertThatThrownBy(() -> bookingFacade.bookFlight(nullPassenger))
                .isInstanceOf(InvalidBookingException.class);

        assertThatThrownBy(() -> bookingFacade.bookFlight(null))
                .isInstanceOf(InvalidBookingException.class);
    }

    @Test
    void shouldSupportManualAdministrativeRollback() {
        BookingResult result = bookingFacade.bookFlight(BookingRequest.builder()
                .flightNumber("AA303")
                .passengerId("PAX-303")
                .passengerName("Manual Rollback Test")
                .seatNumber("3A")
                .seatClass("FIRST")
                .baggageWeightKg(20.0)
                .memberId("MEM-303")
                .membershipTier("PLATINUM")
                .mealPreference("KOSHER")
                .build());

        assertThat(result.isSuccess()).isTrue();
        String ref = result.getBookingReference();

        boolean rolledBack = operationsFacade.manualRollback(ref);
        assertThat(rolledBack).isTrue();

        ReservationInspection inspection = operationsFacade.inspectBooking(ref);
        assertThat(inspection.isConsistent()).isTrue();
        assertThat(inspection.getSeatReservation().getStatus()).isEqualTo(SeatReservationStatus.RELEASED);
        assertThat(inspection.getTicketIssuance().getStatus()).isEqualTo(TicketStatus.VOIDED);
        assertThat(inspection.getLoyaltyRecord().getStatus()).isEqualTo(LoyaltyStatus.REVOKED);
        assertThat(inspection.getTransaction().getStage()).isEqualTo(BookingStage.COMPENSATED_FULL);

        boolean nonExistent = operationsFacade.manualRollback("NON-EXISTENT-REF");
        assertThat(nonExistent).isFalse();
    }

    @Test
    void shouldSimulateRestartAndReconcileInterruptedChainFromDisk() {
        String bookingRef = "BKG-CRASH-TEST";
        BookingTransaction interruptedTx = BookingTransaction.create(
                bookingRef, "AA555", "PAX-CRASH", "Crash Test Dummy", "8D");
        interruptedTx.setSeatReservationId("RES-CRASH");
        interruptedTx.advanceStage(BookingStage.STEP_A_SEAT_HELD);
        journalRepository.save(interruptedTx);

        SeatReservation seat = SeatReservation.create(
                "RES-CRASH", "AA555", "PAX-CRASH", "8D", "ECONOMY", 235.0);
        seatRepository.save(seat);

        TicketIssuance ticket = TicketIssuance.issue(
                "ETKT-2026-888888", bookingRef, "Crash Test Dummy", "AA555", 10.0, 0.0);
        ticketRepository.save(ticket);
        interruptedTx.setTicketNumber(ticket.getTicketNumber());
        interruptedTx.advanceStage(BookingStage.STEP_B_TICKET_ISSUED);
        journalRepository.update(interruptedTx);

        Path seatPath = tempDir.resolve("seats.json");
        Path ticketPath = tempDir.resolve("tickets.json");
        Path loyaltyPath = tempDir.resolve("loyalty.json");
        Path journalPath = tempDir.resolve("journal.json");

        SeatReservationRepository reopenedSeatRepo = new SeatReservationRepository(seatPath);
        TicketIssuanceRepository reopenedTicketRepo = new TicketIssuanceRepository(ticketPath);
        LoyaltyRecordRepository reopenedLoyaltyRepo = new LoyaltyRecordRepository(loyaltyPath);
        BookingJournalRepository reopenedJournalRepo = new BookingJournalRepository(journalPath);

        FlightOperationsFacade recoveryOperationsFacade = new FlightOperationsFacade(
                reopenedSeatRepo,
                reopenedTicketRepo,
                reopenedLoyaltyRepo,
                reopenedJournalRepo);

        int reconciled = recoveryOperationsFacade.reconcilePendingTransactions();
        assertThat(reconciled).isEqualTo(1);

        ReservationInspection inspection = recoveryOperationsFacade.inspectBooking(bookingRef);
        assertThat(inspection.isConsistent()).isTrue();
        assertThat(inspection.getTransaction().getStage()).isEqualTo(BookingStage.COMPENSATED_FULL);
        assertThat(inspection.getSeatReservation().getStatus()).isEqualTo(SeatReservationStatus.RELEASED);
        assertThat(inspection.getTicketIssuance().getStatus()).isEqualTo(TicketStatus.VOIDED);
        assertThat(inspection.getLoyaltyRecord()).isNull();
    }

    @Test
    void shouldReconcileStageASingleFailureOnRestart() {
        String bookingRef = "BKG-CRASH-STEP-A";
        BookingTransaction interruptedTx = BookingTransaction.create(
                bookingRef, "AA777", "PAX-777", "Step A Crasher", "7C");
        interruptedTx.setSeatReservationId("RES-777");
        interruptedTx.advanceStage(BookingStage.STEP_A_SEAT_HELD);
        journalRepository.save(interruptedTx);

        SeatReservation seat = SeatReservation.create(
                "RES-777", "AA777", "PAX-777", "7C", "ECONOMY", 235.0);
        seatRepository.save(seat);

        int reconciled = operationsFacade.reconcilePendingTransactions();
        assertThat(reconciled).isEqualTo(1);

        ReservationInspection inspection = operationsFacade.inspectBooking(bookingRef);
        assertThat(inspection.isConsistent()).isTrue();
        assertThat(inspection.getTransaction().getStage()).isEqualTo(BookingStage.COMPENSATED_A_ONLY);
        assertThat(inspection.getSeatReservation().getStatus()).isEqualTo(SeatReservationStatus.RELEASED);
    }

    @Test
    void shouldInspectAndListAllTransactionsAndResetStores() {
        bookingFacade.bookFlight(BookingRequest.builder()
                .flightNumber("AA999")
                .passengerId("PAX-999")
                .passengerName("Listing Test")
                .seatNumber("5B")
                .seatClass("ECONOMY")
                .build());

        List<BookingTransaction> list = operationsFacade.listAllTransactions();
        assertThat(list).hasSize(1);

        operationsFacade.resetAllStores();
        assertThat(operationsFacade.listAllTransactions()).isEmpty();
        assertThat(seatRepository.findAll()).isEmpty();
        assertThat(ticketRepository.findAll()).isEmpty();
        assertThat(loyaltyRepository.findAll()).isEmpty();
    }
}
