package com.flightreservation.exception;

public class SeatUnavailableException extends FlightReservationException {

    public SeatUnavailableException(String message) {
        super(message);
    }
}
