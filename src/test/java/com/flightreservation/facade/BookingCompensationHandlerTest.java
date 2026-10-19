package com.flightreservation.facade;

import com.flightreservation.coordinator.BookingJournalRepository;
import com.flightreservation.coordinator.BookingStage;
import com.flightreservation.coordinator.BookingTransaction;
import com.flightreservation.subsystem.seat.SeatReservation;
import com.flightreservation.subsystem.seat.SeatReservationRepository;
import com.flightreservation.subsystem.seat.SeatReservationStatus;
import com.flightreservation.subsystem.ticket.TicketIssuance;
import com.flightreservation.subsystem.ticket.TicketIssuanceRepository;
import com.flightreservation.subsystem.ticket.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BookingCompensationHandlerTest {

    @TempDir
    Path tempDir;

    private SeatReservationRepository seatRepository;
    private TicketIssuanceRepository ticketRepository;
    private BookingJournalRepository journalRepository;
    private BookingCompensationHandler compensationHandler;

    @BeforeEach
    void setUp() {
        seatRepository = new SeatReservationRepository(tempDir.resolve("seats.json"));
        ticketRepository = new TicketIssuanceRepository(tempDir.resolve("tickets.json"));
        journalRepository = new BookingJournalRepository(tempDir.resolve("journal.json"));
        compensationHandler = new BookingCompensationHandler(seatRepository, ticketRepository, journalRepository);
    }

    @Test
    void shouldCompensateStepBFailureProperly() {
        BookingTransaction tx = BookingTransaction.create("BKG-COMP-B", "AA100", "PAX-1", "Bob", "12A");
        journalRepository.save(tx);

        SeatReservation seat = SeatReservation.create("RES-B", "AA100", "PAX-1", "12A", "ECONOMY", 200.0);
        seatRepository.save(seat);

        BookingResult result = compensationHandler.compensateStepBFailure(tx, seat, "Excess baggage rejected");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFinalStage()).isEqualTo(BookingStage.COMPENSATED_A_ONLY);
        assertThat(result.getFailureReason()).isEqualTo("Excess baggage rejected");
        assertThat(seat.getStatus()).isEqualTo(SeatReservationStatus.RELEASED);
        assertThat(tx.getStage()).isEqualTo(BookingStage.COMPENSATED_A_ONLY);
    }

    @Test
    void shouldCompensateStepCFailureProperly() {
        BookingTransaction tx = BookingTransaction.create("BKG-COMP-C", "AA200", "PAX-2", "Charlie", "14B");
        journalRepository.save(tx);

        SeatReservation seat = SeatReservation.create("RES-C", "AA200", "PAX-2", "14B", "BUSINESS", 440.0);
        seatRepository.save(seat);

        TicketIssuance ticket = TicketIssuance.issue("TKT-C", "BKG-COMP-C", "Charlie", "AA200", 15.0, 0.0);
        ticketRepository.save(ticket);

        BookingResult result = compensationHandler.compensateStepCFailure(tx, seat, ticket, "Account suspended");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFinalStage()).isEqualTo(BookingStage.COMPENSATED_FULL);
        assertThat(result.getFailureReason()).isEqualTo("Account suspended");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.VOIDED);
        assertThat(seat.getStatus()).isEqualTo(SeatReservationStatus.RELEASED);
        assertThat(tx.getStage()).isEqualTo(BookingStage.COMPENSATED_FULL);
    }

    @Test
    void shouldHandleNullOrAlreadyVoidedEntitiesGracefully() {
        compensationHandler.releaseSeat(null);
        compensationHandler.voidTicket(null);

        SeatReservation seat = SeatReservation.create("RES-REL", "AA300", "PAX-3", "16C", "ECONOMY", 200.0);
        seat.transitionTo(SeatReservationStatus.RELEASED);
        seatRepository.save(seat);
        compensationHandler.releaseSeat(seat);
        assertThat(seat.getStatus()).isEqualTo(SeatReservationStatus.RELEASED);

        TicketIssuance ticket = TicketIssuance.issue("TKT-VOID", "BKG-V", "David", "AA300", 10.0, 0.0);
        ticket.transitionTo(TicketStatus.VOIDED);
        ticketRepository.save(ticket);
        compensationHandler.voidTicket(ticket);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.VOIDED);
    }
}

