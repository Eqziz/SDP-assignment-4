package com.flightreservation.facade;

import com.flightreservation.coordinator.BookingJournalRepository;
import com.flightreservation.coordinator.BookingStage;
import com.flightreservation.coordinator.BookingTransaction;
import com.flightreservation.subsystem.loyalty.LoyaltyAccountRecord;
import com.flightreservation.subsystem.loyalty.LoyaltyRecordRepository;
import com.flightreservation.subsystem.loyalty.LoyaltyStatus;
import com.flightreservation.subsystem.seat.SeatReservation;
import com.flightreservation.subsystem.seat.SeatReservationRepository;
import com.flightreservation.subsystem.seat.SeatReservationStatus;
import com.flightreservation.subsystem.ticket.TicketIssuance;
import com.flightreservation.subsystem.ticket.TicketIssuanceRepository;
import com.flightreservation.subsystem.ticket.TicketStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FlightOperationsFacade {

    private final SeatReservationRepository seatRepository;
    private final TicketIssuanceRepository ticketRepository;
    private final LoyaltyRecordRepository loyaltyRepository;
    private final BookingJournalRepository journalRepository;
    private final BookingCompensationHandler compensationHandler;

    public FlightOperationsFacade(
            SeatReservationRepository seatRepository,
            TicketIssuanceRepository ticketRepository,
            LoyaltyRecordRepository loyaltyRepository,
            BookingJournalRepository journalRepository) {
        this(
                seatRepository,
                ticketRepository,
                loyaltyRepository,
                journalRepository,
                new BookingCompensationHandler(seatRepository, ticketRepository, journalRepository));
    }

    public FlightOperationsFacade(
            SeatReservationRepository seatRepository,
            TicketIssuanceRepository ticketRepository,
            LoyaltyRecordRepository loyaltyRepository,
            BookingJournalRepository journalRepository,
            BookingCompensationHandler compensationHandler) {
        this.seatRepository = Objects.requireNonNull(seatRepository, "seatRepository cannot be null");
        this.ticketRepository = Objects.requireNonNull(ticketRepository, "ticketRepository cannot be null");
        this.loyaltyRepository = Objects.requireNonNull(loyaltyRepository, "loyaltyRepository cannot be null");
        this.journalRepository = Objects.requireNonNull(journalRepository, "journalRepository cannot be null");
        this.compensationHandler = Objects.requireNonNull(compensationHandler, "compensationHandler cannot be null");
    }

    public ReservationInspection inspectBooking(String bookingReference) {
        Objects.requireNonNull(bookingReference, "bookingReference cannot be null");

        BookingTransaction transaction = journalRepository.findByReference(bookingReference)
                .orElse(null);

        SeatReservation seatReservation = null;
        if (transaction != null && transaction.getSeatReservationId() != null) {
            seatReservation = seatRepository.findById(transaction.getSeatReservationId()).orElse(null);
        }

        TicketIssuance ticketIssuance = ticketRepository.findByBookingReference(bookingReference).orElse(null);
        LoyaltyAccountRecord loyaltyRecord = loyaltyRepository.findByBookingReference(bookingReference).orElse(null);

        boolean consistent = evaluateConsistency(transaction, seatReservation, ticketIssuance, loyaltyRecord);
        String summary = buildSummary(transaction, seatReservation, ticketIssuance, loyaltyRecord, consistent);

        return new ReservationInspection(
                bookingReference,
                transaction,
                seatReservation,
                ticketIssuance,
                loyaltyRecord,
                consistent,
                summary);
    }

    private boolean evaluateConsistency(
            BookingTransaction tx,
            SeatReservation seat,
            TicketIssuance ticket,
            LoyaltyAccountRecord loyalty) {
        if (tx == null) {
            return false;
        }

        return switch (tx.getStage()) {
            case COMPLETED ->
                    seat != null && seat.getStatus() == SeatReservationStatus.CONFIRMED &&
                    ticket != null && ticket.getStatus() == TicketStatus.COMPLETED &&
                    loyalty != null && loyalty.getStatus() == LoyaltyStatus.SETTLED;
            case COMPENSATED_A_ONLY ->
                    seat != null && seat.getStatus() == SeatReservationStatus.RELEASED &&
                    ticket == null &&
                    loyalty == null;
            case COMPENSATED_FULL ->
                    seat != null && seat.getStatus() == SeatReservationStatus.RELEASED &&
                    ticket != null && ticket.getStatus() == TicketStatus.VOIDED &&
                    (loyalty == null || loyalty.getStatus() == LoyaltyStatus.REVOKED);
            case STEP_A_SEAT_HELD, STEP_B_TICKET_ISSUED, COMPENSATING_AFTER_B_FAILURE, COMPENSATING_AFTER_C_FAILURE ->
                    false;
            case INITIALIZED, FAILED_INITIALIZATION ->
                    seat == null && ticket == null && loyalty == null;
        };
    }

    private String buildSummary(
            BookingTransaction tx,
            SeatReservation seat,
            TicketIssuance ticket,
            LoyaltyAccountRecord loyalty,
            boolean consistent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Journal Stage: ").append(tx != null ? tx.getStage() : "NOT_FOUND").append(" | ");
        sb.append("Seat: ").append(seat != null ? seat.getStatus() : "NONE").append(" | ");
        sb.append("Ticket: ").append(ticket != null ? ticket.getStatus() : "NONE").append(" | ");
        sb.append("Loyalty: ").append(loyalty != null ? loyalty.getStatus() : "NONE").append(" | ");
        sb.append("Consistent: ").append(consistent);
        return sb.toString();
    }

    public synchronized int reconcilePendingTransactions() {
        seatRepository.reload();
        ticketRepository.reload();
        loyaltyRepository.reload();
        journalRepository.reload();

        List<BookingTransaction> unresolved = journalRepository.findUnresolvedTransactions();
        int reconciledCount = 0;

        for (BookingTransaction tx : unresolved) {
            BookingStage stage = tx.getStage();
            if (stage == BookingStage.STEP_A_SEAT_HELD || stage == BookingStage.COMPENSATING_AFTER_B_FAILURE) {
                if (tx.getSeatReservationId() != null) {
                    seatRepository.findById(tx.getSeatReservationId())
                            .ifPresent(compensationHandler::releaseSeat);
                }
                tx.advanceStage(BookingStage.COMPENSATED_A_ONLY);
                journalRepository.update(tx);
                reconciledCount++;
            } else if (stage == BookingStage.STEP_B_TICKET_ISSUED || stage == BookingStage.COMPENSATING_AFTER_C_FAILURE) {
                ticketRepository.findByBookingReference(tx.getBookingReference())
                        .ifPresent(compensationHandler::voidTicket);

                if (tx.getSeatReservationId() != null) {
                    seatRepository.findById(tx.getSeatReservationId())
                            .ifPresent(compensationHandler::releaseSeat);
                }
                tx.advanceStage(BookingStage.COMPENSATED_FULL);
                journalRepository.update(tx);
                reconciledCount++;
            }
        }
        return reconciledCount;
    }

    public synchronized boolean manualRollback(String bookingReference) {
        Objects.requireNonNull(bookingReference, "bookingReference cannot be null");

        Optional<BookingTransaction> txOpt = journalRepository.findByReference(bookingReference);
        if (txOpt.isEmpty()) {
            return false;
        }

        BookingTransaction tx = txOpt.get();

        Optional<LoyaltyAccountRecord> loyaltyOpt = loyaltyRepository.findByBookingReference(bookingReference);
        if (loyaltyOpt.isPresent()) {
            LoyaltyAccountRecord loyalty = loyaltyOpt.get();
            if (loyalty.getStatus() != LoyaltyStatus.REVOKED) {
                loyalty.transitionTo(LoyaltyStatus.REVOKED);
                loyaltyRepository.update(loyalty);
            }
        }

        ticketRepository.findByBookingReference(bookingReference)
                .ifPresent(compensationHandler::voidTicket);

        if (tx.getSeatReservationId() != null) {
            seatRepository.findById(tx.getSeatReservationId())
                    .ifPresent(compensationHandler::releaseSeat);
        }

        tx.recordFailure(BookingStage.COMPENSATED_FULL, "Manual administrative rollback invoked");
        journalRepository.update(tx);
        return true;
    }

    public List<BookingTransaction> listAllTransactions() {
        return journalRepository.findAll();
    }

    public synchronized void resetAllStores() {
        seatRepository.clear();
        ticketRepository.clear();
        loyaltyRepository.clear();
        journalRepository.clear();
    }
}
