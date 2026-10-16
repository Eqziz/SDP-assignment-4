package com.flightreservation.exception;

public class FlightReservationException extends RuntimeException {

    public FlightReservationException(String message) {
        super(message);
    }

    public FlightReservationException(String message, Throwable cause) {
        super(message, cause);
    }
}
