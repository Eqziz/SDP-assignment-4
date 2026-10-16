package com.flightreservation.coordinator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public class BookingTransaction {

    private final String bookingReference;
    private final String flightNumber;
    private final String passengerId;
    private final String passengerName;
    private final String seatNumber;
    private final Instant createdAt;

    private String seatReservationId;
    private String ticketNumber;
    private String confirmationCode;
    private BookingStage stage;
    private String failureReason;
    private Instant updatedAt;

    @JsonCreator
    public BookingTransaction(
            @JsonProperty("bookingReference") String bookingReference,
            @JsonProperty("flightNumber") String flightNumber,
            @JsonProperty("passengerId") String passengerId,
            @JsonProperty("passengerName") String passengerName,
            @JsonProperty("seatNumber") String seatNumber,
            @JsonProperty("seatReservationId") String seatReservationId,
            @JsonProperty("ticketNumber") String ticketNumber,
            @JsonProperty("confirmationCode") String confirmationCode,
            @JsonProperty("stage") BookingStage stage,
            @JsonProperty("failureReason") String failureReason,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt) {
        this.bookingReference = Objects.requireNonNull(bookingReference, "bookingReference cannot be null");
        this.flightNumber = Objects.requireNonNull(flightNumber, "flightNumber cannot be null");
        this.passengerId = Objects.requireNonNull(passengerId, "passengerId cannot be null");
        this.passengerName = Objects.requireNonNull(passengerName, "passengerName cannot be null");
        this.seatNumber = Objects.requireNonNull(seatNumber, "seatNumber cannot be null");
        this.seatReservationId = seatReservationId;
        this.ticketNumber = ticketNumber;
        this.confirmationCode = confirmationCode;
        this.stage = stage != null ? stage : BookingStage.INITIALIZED;
        this.failureReason = failureReason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static BookingTransaction create(
            String bookingReference,
            String flightNumber,
            String passengerId,
            String passengerName,
            String seatNumber) {
        Instant now = Instant.now();
        return new BookingTransaction(
                bookingReference,
                flightNumber,
                passengerId,
                passengerName,
                seatNumber,
                null,
                null,
                null,
                BookingStage.INITIALIZED,
                null,
                now,
                now);
    }

    public synchronized void advanceStage(BookingStage nextStage) {
        this.stage = Objects.requireNonNull(nextStage, "nextStage cannot be null");
        this.updatedAt = Instant.now();
    }

    public synchronized void recordFailure(BookingStage failureStage, String reason) {
        this.stage = Objects.requireNonNull(failureStage, "failureStage cannot be null");
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public synchronized String getSeatReservationId() {
        return seatReservationId;
    }

    public synchronized void setSeatReservationId(String seatReservationId) {
        this.seatReservationId = seatReservationId;
        this.updatedAt = Instant.now();
    }

    public synchronized String getTicketNumber() {
        return ticketNumber;
    }

    public synchronized void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
        this.updatedAt = Instant.now();
    }

    public synchronized String getConfirmationCode() {
        return confirmationCode;
    }

    public synchronized void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
        this.updatedAt = Instant.now();
    }

    public synchronized BookingStage getStage() {
        return stage;
    }

    public synchronized String getFailureReason() {
        return failureReason;
    }

    public synchronized void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingTransaction that)) return false;
        return Objects.equals(bookingReference, that.bookingReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingReference);
    }

    @Override
    public String toString() {
        return "BookingTransaction{" +
                "bookingReference='" + bookingReference + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", passengerName='" + passengerName + '\'' +
                ", seatNumber='" + seatNumber + '\'' +
                ", stage=" + stage +
                ", failureReason='" + failureReason + '\'' +
                '}';
    }
}
