package com.flightreservation.facade;

import com.flightreservation.coordinator.BookingStage;
import com.flightreservation.subsystem.loyalty.LoyaltyAccountRecord;
import com.flightreservation.subsystem.seat.SeatReservation;
import com.flightreservation.subsystem.ticket.TicketIssuance;

import java.util.Collections;
import java.util.List;

public class BookingResult {

    private final boolean success;
    private final String bookingReference;
    private final BookingStage finalStage;
    private final SeatReservation seatReservation;
    private final TicketIssuance ticketIssuance;
    private final LoyaltyAccountRecord loyaltyRecord;
    private final String failureReason;
    private final List<String> compensatedSteps;

    public BookingResult(
            boolean success,
            String bookingReference,
            BookingStage finalStage,
            SeatReservation seatReservation,
            TicketIssuance ticketIssuance,
            LoyaltyAccountRecord loyaltyRecord,
            String failureReason,
            List<String> compensatedSteps) {
        this.success = success;
        this.bookingReference = bookingReference;
        this.finalStage = finalStage;
        this.seatReservation = seatReservation;
        this.ticketIssuance = ticketIssuance;
        this.loyaltyRecord = loyaltyRecord;
        this.failureReason = failureReason;
        this.compensatedSteps = compensatedSteps != null ? List.copyOf(compensatedSteps) : Collections.emptyList();
    }

    public static BookingResult success(
            String bookingReference,
            SeatReservation seatReservation,
            TicketIssuance ticketIssuance,
            LoyaltyAccountRecord loyaltyRecord) {
        return new BookingResult(
                true,
                bookingReference,
                BookingStage.COMPLETED,
                seatReservation,
                ticketIssuance,
                loyaltyRecord,
                null,
                Collections.emptyList());
    }

    public static BookingResult compensated(
            String bookingReference,
            BookingStage stage,
            SeatReservation seatReservation,
            TicketIssuance ticketIssuance,
            String failureReason,
            List<String> compensatedSteps) {
        return new BookingResult(
                false,
                bookingReference,
                stage,
                seatReservation,
                ticketIssuance,
                null,
                failureReason,
                compensatedSteps);
    }

    public static BookingResult failedPreCommit(
            String bookingReference,
            String failureReason) {
        return new BookingResult(
                false,
                bookingReference,
                BookingStage.FAILED_INITIALIZATION,
                null,
                null,
                null,
                failureReason,
                Collections.emptyList());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public BookingStage getFinalStage() {
        return finalStage;
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

    public String getFailureReason() {
        return failureReason;
    }

    public List<String> getCompensatedSteps() {
        return compensatedSteps;
    }
}
