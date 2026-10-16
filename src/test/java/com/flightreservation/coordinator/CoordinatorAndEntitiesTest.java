package com.flightreservation.coordinator;

import com.flightreservation.exception.ExcessBaggageException;
import com.flightreservation.exception.FlightReservationException;
import com.flightreservation.exception.InvalidBookingException;
import com.flightreservation.exception.LoyaltyProcessingException;
import com.flightreservation.exception.SeatUnavailableException;
import com.flightreservation.facade.BookingRequest;
import com.flightreservation.facade.BookingResult;
import com.flightreservation.facade.ReservationInspection;
import com.flightreservation.subsystem.loyalty.LoyaltyAccountRecord;
import com.flightreservation.subsystem.loyalty.LoyaltyRecordRepository;
import com.flightreservation.subsystem.loyalty.LoyaltyStatus;
import com.flightreservation.subsystem.seat.SeatReservation;
import com.flightreservation.subsystem.seat.SeatReservationRepository;
import com.flightreservation.subsystem.seat.SeatReservationStatus;
import com.flightreservation.subsystem.ticket.TicketIssuance;
import com.flightreservation.subsystem.ticket.TicketIssuanceRepository;
import com.flightreservation.subsystem.ticket.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoordinatorAndEntitiesTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCoverExceptionsAndGetters() {
        FlightReservationException baseEx = new FlightReservationException("Error", new RuntimeException("cause"));
        assertThat(baseEx.getMessage()).isEqualTo("Error");
        assertThat(baseEx.getCause()).isNotNull();

        ExcessBaggageException baggageEx = new ExcessBaggageException(35.5, 32.0);
        assertThat(baggageEx.getRequestedWeight()).isEqualTo(35.5);
        assertThat(baggageEx.getMaxAllowedWeight()).isEqualTo(32.0);

        LoyaltyProcessingException loyaltyEx = new LoyaltyProcessingException("Tier blocked", "MEM-123");
        assertThat(loyaltyEx.getMemberId()).isEqualTo("MEM-123");

        InvalidBookingException invalidBookingEx = new InvalidBookingException("Invalid input");
        assertThat(invalidBookingEx.getMessage()).isEqualTo("Invalid input");

        SeatUnavailableException seatEx = new SeatUnavailableException("Seat unavailable");
        assertThat(seatEx.getMessage()).isEqualTo("SeatUnavailableException".length() > 0 ? "Seat unavailable" : "");
    }

    @Test
    void shouldCoverBookingTransactionMethods() {
        Instant now = Instant.now();
        BookingTransaction tx = new BookingTransaction(
                "BKG-TEST", "AA100", "PAX-1", "John", "10A",
                "RES-1", "TKT-1", "CONF-1",
                BookingStage.INITIALIZED, null, now, now);

        assertThat(tx.getBookingReference()).isEqualTo("BKG-TEST");
        assertThat(tx.getFlightNumber()).isEqualTo("AA100");
        assertThat(tx.getPassengerId()).isEqualTo("PAX-1");
        assertThat(tx.getPassengerName()).isEqualTo("John");
        assertThat(tx.getSeatNumber()).isEqualTo("10A");
        assertThat(tx.getSeatReservationId()).isEqualTo("RES-1");
        assertThat(tx.getTicketNumber()).isEqualTo("TKT-1");
        assertThat(tx.getConfirmationCode()).isEqualTo("CONF-1");
        assertThat(tx.getStage()).isEqualTo(BookingStage.INITIALIZED);
        assertThat(tx.getCreatedAt()).isEqualTo(now);
        assertThat(tx.getUpdatedAt()).isEqualTo(now);
        assertThat(tx.toString()).contains("BKG-TEST");

        BookingTransaction txSame = BookingTransaction.create("BKG-TEST", "AA200", "PAX-2", "Jane", "12B");
        assertThat(tx).isEqualTo(txSame);
        assertThat(tx.hashCode()).isEqualTo(txSame.hashCode());

        BookingTransaction txOther = BookingTransaction.create("BKG-OTHER", "AA300", "PAX-3", "Bob", "14C");
        assertThat(tx).isNotEqualTo(txOther);
        assertThat(tx.equals(new Object())).isFalse();

        tx.setFailureReason("Failed B");
        assertThat(tx.getFailureReason()).isEqualTo("Failed B");

        tx.recordFailure(BookingStage.COMPENSATED_A_ONLY, "Unwound");
        assertThat(tx.getStage()).isEqualTo(BookingStage.COMPENSATED_A_ONLY);
        assertThat(tx.getFailureReason()).isEqualTo("Unwound");
    }

    @Test
    void shouldCoverBookingJournalRepositoryOperations() {
        Path journalPath = tempDir.resolve("journal.json");
        BookingJournalRepository repository = new BookingJournalRepository(journalPath);

        BookingTransaction tx = BookingTransaction.create("BKG-JOURNAL", "AA111", "PAX-5", "Alice", "15D");
        repository.save(tx);

        assertThat(repository.findByReference("BKG-JOURNAL")).isPresent();
        assertThat(repository.findByReference("UNKNOWN")).isEmpty();

        tx.advanceStage(BookingStage.STEP_A_SEAT_HELD);
        repository.update(tx);

        List<BookingTransaction> unresolved = repository.findUnresolvedTransactions();
        assertThat(unresolved).hasSize(1);

        BookingTransaction nonExistent = BookingTransaction.create("NON-EXISTENT", "AA", "P", "N", "1A");
        assertThatThrownBy(() -> repository.update(nonExistent)).isInstanceOf(IllegalArgumentException.class);

        repository.clear();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void shouldCoverEntityEqualsHashCodeAndToString() {
        SeatReservation seat1 = SeatReservation.create("RES-EQ", "AA100", "PAX-1", "1A", "FIRST", 790.0);
        SeatReservation seat2 = SeatReservation.create("RES-EQ", "AA200", "PAX-2", "2B", "ECONOMY", 225.0);
        SeatReservation seat3 = SeatReservation.create("RES-DIFF", "AA100", "PAX-1", "1A", "FIRST", 790.0);

        assertThat(seat1).isEqualTo(seat2);
        assertThat(seat1.hashCode()).isEqualTo(seat2.hashCode());
        assertThat(seat1).isNotEqualTo(seat3);
        assertThat(seat1.equals(null)).isFalse();
        assertThat(seat1.toString()).contains("RES-EQ");
        assertThat(seat1.getFlightNumber()).isEqualTo("AA100");
        assertThat(seat1.getPassengerId()).isEqualTo("PAX-1");
        assertThat(seat1.getSeatClass()).isEqualTo("FIRST");
        assertThat(seat1.getFare()).isEqualTo(790.0);
        assertThat(seat1.getCreatedAt()).isNotNull();

        TicketIssuance ticket1 = TicketIssuance.issue("TKT-EQ", "BKG-1", "John", "AA100", 15.0, 0.0);
        TicketIssuance ticket2 = TicketIssuance.issue("TKT-EQ", "BKG-2", "Jane", "AA200", 25.0, 75.0);
        TicketIssuance ticket3 = TicketIssuance.issue("TKT-DIFF", "BKG-1", "John", "AA100", 15.0, 0.0);

        assertThat(ticket1).isEqualTo(ticket2);
        assertThat(ticket1.hashCode()).isEqualTo(ticket2.hashCode());
        assertThat(ticket1).isNotEqualTo(ticket3);
        assertThat(ticket1.equals(null)).isFalse();
        assertThat(ticket1.toString()).contains("TKT-EQ");
        assertThat(ticket1.getFlightNumber()).isEqualTo("AA100");
        assertThat(ticket1.getIssuedAt()).isNotNull();

        LoyaltyAccountRecord loyalty1 = LoyaltyAccountRecord.accrue("CONF-EQ", "BKG-1", "MEM-1", "GOLD", 300, "VEGAN", 200.0);
        LoyaltyAccountRecord loyalty2 = LoyaltyAccountRecord.accrue("CONF-EQ", "BKG-2", "MEM-2", "SILVER", 250, "HALAL", 200.0);
        LoyaltyAccountRecord loyalty3 = LoyaltyAccountRecord.accrue("CONF-DIFF", "BKG-1", "MEM-1", "GOLD", 300, "VEGAN", 200.0);

        assertThat(loyalty1).isEqualTo(loyalty2);
        assertThat(loyalty1.hashCode()).isEqualTo(loyalty2.hashCode());
        assertThat(loyalty1).isNotEqualTo(loyalty3);
        assertThat(loyalty1.equals(null)).isFalse();
        assertThat(loyalty1.toString()).contains("CONF-EQ");
        assertThat(loyalty1.getTotalPaid()).isEqualTo(200.0);
        assertThat(loyalty1.getConfirmedAt()).isNotNull();
    }

    @Test
    void shouldCoverDtoAndInspection() {
        BookingResult preCommitFail = BookingResult.failedPreCommit("BKG-FAIL", "Invalid seat");
        assertThat(preCommitFail.isSuccess()).isFalse();
        assertThat(preCommitFail.getFinalStage()).isEqualTo(BookingStage.FAILED_INITIALIZATION);
        assertThat(preCommitFail.getFailureReason()).isEqualTo("Invalid seat");
        assertThat(preCommitFail.getCompensatedSteps()).isEmpty();

        BookingTransaction tx = BookingTransaction.create("BKG-INSP", "AA100", "PAX-1", "John", "10A");
        ReservationInspection inspection = new ReservationInspection(
                "BKG-INSP", tx, null, null, null, false, "Inspection summary");
        assertThat(inspection.getBookingReference()).isEqualTo("BKG-INSP");
        assertThat(inspection.getTransaction()).isEqualTo(tx);
        assertThat(inspection.getSeatReservation()).isNull();
        assertThat(inspection.getTicketIssuance()).isNull();
        assertThat(inspection.getLoyaltyRecord()).isNull();
        assertThat(inspection.isConsistent()).isFalse();
        assertThat(inspection.getSummary()).isEqualTo("Inspection summary");
    }

    @Test
    void shouldHandleRepositoryUpdateNonExistentExceptions() {
        Path seatPath = tempDir.resolve("seats_sub.json");
        SeatReservationRepository seatRepo = new SeatReservationRepository(seatPath);
        SeatReservation nonExistentSeat = SeatReservation.create("RES-NONE", "AA1", "P1", "1A", "ECONOMY", 200.0);
        assertThatThrownBy(() -> seatRepo.update(nonExistentSeat)).isInstanceOf(IllegalArgumentException.class);

        Path ticketPath = tempDir.resolve("tickets_sub.json");
        TicketIssuanceRepository ticketRepo = new TicketIssuanceRepository(ticketPath);
        TicketIssuance nonExistentTicket = TicketIssuance.issue("TKT-NONE", "BKG-NONE", "John", "AA1", 10.0, 0.0);
        assertThatThrownBy(() -> ticketRepo.update(nonExistentTicket)).isInstanceOf(IllegalArgumentException.class);

        Path loyaltyPath = tempDir.resolve("loyalty_sub.json");
        LoyaltyRecordRepository loyaltyRepo = new LoyaltyRecordRepository(loyaltyPath);
        LoyaltyAccountRecord nonExistentLoyalty = LoyaltyAccountRecord.accrue("CONF-NONE", "BKG-NONE", "M1", "GOLD", 100, "STANDARD", 100.0);
        assertThatThrownBy(() -> loyaltyRepo.update(nonExistentLoyalty)).isInstanceOf(IllegalArgumentException.class);
    }
}
