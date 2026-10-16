package com.flightreservation.subsystem.seat;

import com.flightreservation.exception.SeatUnavailableException;

import java.util.Objects;
import java.util.regex.Pattern;

public class SeatMapValidator {

    private static final Pattern SEAT_PATTERN = Pattern.compile("^([1-9]|[1-2][0-9]|3[0-5])[A-F]$");

    public void validateSeatFormat(String seatNumber) {
        if (seatNumber == null || !SEAT_PATTERN.matcher(seatNumber.trim().toUpperCase()).matches()) {
            throw new SeatUnavailableException("Seat format invalid or out of aircraft configuration: " + seatNumber);
        }
    }

    public void ensureSeatAvailable(String flightNumber, String seatNumber, SeatReservationRepository repository) {
        Objects.requireNonNull(flightNumber, "flightNumber cannot be null");
        Objects.requireNonNull(seatNumber, "seatNumber cannot be null");
        Objects.requireNonNull(repository, "repository cannot be null");

        validateSeatFormat(seatNumber);

        boolean occupied = repository.findActiveByFlightAndSeat(flightNumber, seatNumber).isPresent();
        if (occupied) {
            throw new SeatUnavailableException("Seat " + seatNumber + " is already occupied on flight " + flightNumber);
        }
    }
}
