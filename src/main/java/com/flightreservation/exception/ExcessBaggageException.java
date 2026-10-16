package com.flightreservation.exception;

public class ExcessBaggageException extends FlightReservationException {

    private final double requestedWeight;
    private final double maxAllowedWeight;

    public ExcessBaggageException(double requestedWeight, double maxAllowedWeight) {
        super("Requested baggage weight " + requestedWeight + " kg exceeds maximum allowed limit of " + maxAllowedWeight + " kg.");
        this.requestedWeight = requestedWeight;
        this.maxAllowedWeight = maxAllowedWeight;
    }

    public double getRequestedWeight() {
        return requestedWeight;
    }

    public double getMaxAllowedWeight() {
        return maxAllowedWeight;
    }
}
