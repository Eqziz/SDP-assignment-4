package com.flightreservation.subsystem.ticket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public class TicketIssuance {

    private final String ticketNumber;
    private final String bookingReference;
    private final String passengerName;
    private final String flightNumber;
    private final double baggageWeightKg;
    private final double excessBaggageFee;
    private final Instant issuedAt;
    private TicketStatus status;

    @JsonCreator
    public TicketIssuance(
            @JsonProperty("ticketNumber") String ticketNumber,
            @JsonProperty("bookingReference") String bookingReference,
            @JsonProperty("passengerName") String passengerName,
            @JsonProperty("flightNumber") String flightNumber,
            @JsonProperty("baggageWeightKg") double baggageWeightKg,
            @JsonProperty("excessBaggageFee") double excessBaggageFee,
            @JsonProperty("issuedAt") Instant issuedAt,
            @JsonProperty("status") TicketStatus status) {
        this.ticketNumber = Objects.requireNonNull(ticketNumber, "ticketNumber cannot be null");
        this.bookingReference = Objects.requireNonNull(bookingReference, "bookingReference cannot be null");
        this.passengerName = Objects.requireNonNull(passengerName, "passengerName cannot be null");
        this.flightNumber = Objects.requireNonNull(flightNumber, "flightNumber cannot be null");
        this.baggageWeightKg = baggageWeightKg;
        this.excessBaggageFee = excessBaggageFee;
        this.issuedAt = issuedAt != null ? issuedAt : Instant.now();
        this.status = status != null ? status : TicketStatus.ISSUED;
    }

    public static TicketIssuance issue(
            String ticketNumber,
            String bookingReference,
            String passengerName,
            String flightNumber,
            double baggageWeightKg,
            double excessBaggageFee) {
        return new TicketIssuance(
                ticketNumber,
                bookingReference,
                passengerName,
                flightNumber,
                baggageWeightKg,
                excessBaggageFee,
                Instant.now(),
                TicketStatus.ISSUED);
    }

    public synchronized void transitionTo(TicketStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus cannot be null");
        if (this.status == newStatus) {
            return;
        }

        boolean legal = switch (this.status) {
            case ISSUED -> newStatus == TicketStatus.COMPLETED || newStatus == TicketStatus.VOIDED;
            case COMPLETED -> newStatus == TicketStatus.VOIDED;
            case VOIDED -> false;
        };

        if (!legal) {
            throw new IllegalStateException("Illegal ticket state transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public double getBaggageWeightKg() {
        return baggageWeightKg;
    }

    public double getExcessBaggageFee() {
        return excessBaggageFee;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public TicketStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TicketIssuance that)) return false;
        return Objects.equals(ticketNumber, that.ticketNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketNumber);
    }

    @Override
    public String toString() {
        return "TicketIssuance{" +
                "ticketNumber='" + ticketNumber + '\'' +
                ", bookingReference='" + bookingReference + '\'' +
                ", passengerName='" + passengerName + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", baggageWeightKg=" + baggageWeightKg +
                ", excessBaggageFee=" + excessBaggageFee +
                ", status=" + status +
                '}';
    }
}
