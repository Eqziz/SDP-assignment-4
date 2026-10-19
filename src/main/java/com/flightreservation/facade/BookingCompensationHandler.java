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

import java.util.List;
import java.util.Objects;

public class BookingCompensationHandler {

    private final SeatReservationRepository seatRepository;
    private final TicketIssuanceRepository ticketRepository;
    private final BookingJournalRepository journalRepository;

    public BookingCompensationHandler(
            SeatReservationRepository seatRepository,
            TicketIssuanceRepository ticketRepository,
            BookingJournalRepository journalRepository) {
        this.seatRepository = Objects.requireNonNull(seatRepository, "seatRepository cannot be null");
        this.ticketRepository = Objects.requireNonNull(ticketRepository, "ticketRepository cannot be null");
        this.journalRepository = Objects.requireNonNull(journalRepository, "journalRepository cannot be null");
    }

    public BookingResult compensateStepBFailure(
            BookingTransaction transaction,
            SeatReservation seatReservation,
            String failureReason) {
        transaction.recordFailure(BookingStage.COMPENSATING_AFTER_B_FAILURE, failureReason);
        journalRepository.update(transaction);

        releaseSeat(seatReservation);

        transaction.advanceStage(BookingStage.COMPENSATED_A_ONLY);
        journalRepository.update(transaction);

        return BookingResult.compensated(
                transaction.getBookingReference(),
                BookingStage.COMPENSATED_A_ONLY,
                seatReservation,
                null,
                failureReason,
                List.of("Subsystem A (Seat HELD -> RELEASED)"));
    }

    public BookingResult compensateStepCFailure(
            BookingTransaction transaction,
            SeatReservation seatReservation,
            TicketIssuance ticketIssuance,
            String failureReason) {
        transaction.recordFailure(BookingStage.COMPENSATING_AFTER_C_FAILURE, failureReason);
        journalRepository.update(transaction);

        voidTicket(ticketIssuance);
        releaseSeat(seatReservation);

        transaction.advanceStage(BookingStage.COMPENSATED_FULL);
        journalRepository.update(transaction);

        return BookingResult.compensated(
                transaction.getBookingReference(),
                BookingStage.COMPENSATED_FULL,
                seatReservation,
                ticketIssuance,
                failureReason,
                List.of(
                        "Subsystem B (Ticket ISSUED -> VOIDED)",
                        "Subsystem A (Seat HELD -> RELEASED)"
                ));
    }

    public void releaseSeat(SeatReservation seatReservation) {
        if (seatReservation != null && seatReservation.getStatus() != SeatReservationStatus.RELEASED) {
            seatReservation.transitionTo(SeatReservationStatus.RELEASED);
            seatRepository.update(seatReservation);
        }
    }

    public void voidTicket(TicketIssuance ticketIssuance) {
        if (ticketIssuance != null && ticketIssuance.getStatus() != TicketStatus.VOIDED) {
            ticketIssuance.transitionTo(TicketStatus.VOIDED);
            ticketRepository.update(ticketIssuance);
        }
    }
}

