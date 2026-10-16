package com.flightreservation.facade;

import com.flightreservation.coordinator.BookingTransaction;
import com.flightreservation.subsystem.loyalty.LoyaltyAccountRecord;
import com.flightreservation.subsystem.seat.SeatReservation;
import com.flightreservation.subsystem.ticket.TicketIssuance;

public class ReservationInspection {

    private final String bookingReference;
    private final BookingTransaction transaction;
    private final SeatReservation seatReservation;
    private final TicketIssuance ticketIssuance;
    private final LoyaltyAccountRecord loyaltyRecord;
    private final boolean consistent;
    private final String summary;

    public ReservationInspection(
            String bookingReference,
            BookingTransaction transaction,
            SeatReservation seatReservation,
            TicketIssuance ticketIssuance,
            LoyaltyAccountRecord loyaltyRecord,
            boolean consistent,
            String summary) {
        this.bookingReference = bookingReference;
        this.transaction = transaction;
        this.seatReservation = seatReservation;
        this.ticketIssuance = ticketIssuance;
        this.loyaltyRecord = loyaltyRecord;
        this.consistent = consistent;
        this.summary = summary;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public BookingTransaction getTransaction() {
        return transaction;
    }

    public SeatReservation getSeatReservation() {
        return seatReservation;
    }

    public TicketIssuance getTicketIssuance() {
        return ticketIssuance;
    }

    public LoyaltyAccountRecord getLoyaltyRecord() {
        return loyaltyRecord;
    }

    public boolean isConsistent() {
        return consistent;
    }

    public String getSummary() {
        return summary;
    }
}
