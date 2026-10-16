package com.flightreservation.subsystem.seat;

import java.util.Locale;
import java.util.Objects;

public class FareCalculator {

    public double calculateFare(String seatClass, String seatNumber) {
        Objects.requireNonNull(seatClass, "seatClass cannot be null");
        Objects.requireNonNull(seatNumber, "seatNumber cannot be null");

        String normalizedClass = seatClass.trim().toUpperCase(Locale.ROOT);
        double baseRate = switch (normalizedClass) {
            case "ECONOMY" -> 200.00;
            case "BUSINESS" -> 440.00;
            case "FIRST" -> 750.00;
            default -> throw new IllegalArgumentException("Unknown seat class: " + seatClass);
        };

        double locationSurcharge = 0.0;
        int row = extractRowNumber(seatNumber);
        if (row >= 10 && row <= 12) {
            locationSurcharge += 35.00;
        }

        char seatLetter = Character.toUpperCase(seatNumber.charAt(seatNumber.length() - 1));
        if (seatLetter == 'A' || seatLetter == 'F') {
            locationSurcharge += 15.00;
        } else if (seatLetter == 'C' || seatLetter == 'D') {
            locationSurcharge += 10.00;
        }

        double airportTaxes = 25.00;
        return baseRate + locationSurcharge + airportTaxes;
    }

    private int extractRowNumber(String seatNumber) {
        String trimmed = seatNumber.trim();
        String rowPart = trimmed.substring(0, trimmed.length() - 1);
        try {
            return Integer.parseInt(rowPart);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
