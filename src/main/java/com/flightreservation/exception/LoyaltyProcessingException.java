package com.flightreservation.exception;

public class LoyaltyProcessingException extends FlightReservationException {

    private final String memberId;

    public LoyaltyProcessingException(String message, String memberId) {
        super(message);
        this.memberId = memberId;
    }

    public String getMemberId() {
        return memberId;
    }
}
