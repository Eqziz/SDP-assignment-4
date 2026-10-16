package com.flightreservation.subsystem.seat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public class SeatReservation {

    private final String reservationId;
    private final String flightNumber;
    private final String passengerId;
    private final String seatNumber;
    private final String seatClass;
    private final double fare;
    private final Instant createdAt;
    private SeatReservationStatus status;

    @JsonCreator
    public SeatReservation(
            @JsonProperty("reservationId") String reservationId,
            @JsonProperty("flightNumber") String flightNumber,
            @JsonProperty("passengerId") String passengerId,
            @JsonProperty("seatNumber") String seatNumber,
            @JsonProperty("seatClass") String seatClass,
            @JsonProperty("fare") double fare,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("status") SeatReservationStatus status) {
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId cannot be null");
        this.flightNumber = Objects.requireNonNull(flightNumber, "flightNumber cannot be null");
        this.passengerId = Objects.requireNonNull(passengerId, "passengerId cannot be null");
        this.seatNumber = Objects.requireNonNull(seatNumber, "seatNumber cannot be null");
        this.seatClass = Objects.requireNonNull(seatClass, "seatClass cannot be null");
        this.fare = fare;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.status = status != null ? status : SeatReservationStatus.HELD;
    }

    public static SeatReservation create(
            String reservationId,
            String flightNumber,
            String passengerId,
            String seatNumber,
            String seatClass,
            double fare) {
        return new SeatReservation(
                reservationId,
                flightNumber,
                passengerId,
                seatNumber,
                seatClass,
                fare,
                Instant.now(),
                SeatReservationStatus.HELD);
    }

    public synchronized void transitionTo(SeatReservationStatus newStatus) {
        Objects.requireNonNull(newStatus, "New status cannot be null");
        if (this.status == newStatus) {
            return;
        }

        boolean legal = switch (this.status) {
            case HELD -> newStatus == SeatReservationStatus.CONFIRMED || newStatus == SeatReservationStatus.RELEASED;
            case CONFIRMED -> newStatus == SeatReservationStatus.RELEASED;
            case RELEASED -> false;
        };

        if (!legal) {
            throw new IllegalStateException("Illegal state transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getSeatClass() {
        return seatClass;
    }

    public double getFare() {
        return fare;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public SeatReservationStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeatReservation that)) return false;
        return Objects.equals(reservationId, that.reservationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId);
    }

    @Override
    public String toString() {
        return "SeatReservation{" +
                "reservationId='" + reservationId + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", passengerId='" + passengerId + '\'' +
                ", seatNumber='" + seatNumber + '\'' +
                ", seatClass='" + seatClass + '\'' +
                ", fare=" + fare +
                ", status=" + status +
                '}';
    }
}
