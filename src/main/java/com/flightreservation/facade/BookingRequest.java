package com.flightreservation.facade;

public class BookingRequest {

    private final String flightNumber;
    private final String passengerId;
    private final String passengerName;
    private final String seatNumber;
    private final String seatClass;
    private final double baggageWeightKg;
    private final String memberId;
    private final String membershipTier;
    private final String mealPreference;

    private BookingRequest(Builder builder) {
        this.flightNumber = builder.flightNumber;
        this.passengerId = builder.passengerId;
        this.passengerName = builder.passengerName;
        this.seatNumber = builder.seatNumber;
        this.seatClass = builder.seatClass;
        this.baggageWeightKg = builder.baggageWeightKg;
        this.memberId = builder.memberId;
        this.membershipTier = builder.membershipTier;
        this.mealPreference = builder.mealPreference;
    }

    public static Builder builder() {
        return new Builder();
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

    public String getSeatClass() {
        return seatClass;
    }

    public double getBaggageWeightKg() {
        return baggageWeightKg;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMembershipTier() {
        return membershipTier;
    }

    public String getMealPreference() {
        return mealPreference;
    }

    public static class Builder {
        private String flightNumber = "AA101";
        private String passengerId = "PAX-001";
        private String passengerName = "John Doe";
        private String seatNumber = "12A";
        private String seatClass = "ECONOMY";
        private double baggageWeightKg = 15.0;
        private String memberId = "MEM-001";
        private String membershipTier = "STANDARD";
        private String mealPreference = "STANDARD";

        public Builder flightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
            return this;
        }

        public Builder passengerId(String passengerId) {
            this.passengerId = passengerId;
            return this;
        }

        public Builder passengerName(String passengerName) {
            this.passengerName = passengerName;
            return this;
        }

        public Builder seatNumber(String seatNumber) {
            this.seatNumber = seatNumber;
            return this;
        }

        public Builder seatClass(String seatClass) {
            this.seatClass = seatClass;
            return this;
        }

        public Builder baggageWeightKg(double baggageWeightKg) {
            this.baggageWeightKg = baggageWeightKg;
            return this;
        }

        public Builder memberId(String memberId) {
            this.memberId = memberId;
            return this;
        }

        public Builder membershipTier(String membershipTier) {
            this.membershipTier = membershipTier;
            return this;
        }

        public Builder mealPreference(String mealPreference) {
            this.mealPreference = mealPreference;
            return this;
        }

        public BookingRequest build() {
            return new BookingRequest(this);
        }
    }
}
