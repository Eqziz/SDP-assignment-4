package com.flightreservation.subsystem.loyalty;

import java.util.Locale;

public class LoyaltyPointsCalculator {

    public int calculatePoints(double totalPaid, String tier) {
        if (totalPaid < 0) {
            throw new IllegalArgumentException("Total amount paid cannot be negative: " + totalPaid);
        }

        String normalizedTier = tier != null ? tier.trim().toUpperCase(Locale.ROOT) : "STANDARD";
        double multiplier = switch (normalizedTier) {
            case "PLATINUM" -> 2.0;
            case "GOLD" -> 1.5;
            case "SILVER" -> 1.25;
            default -> 1.0;
        };

        return (int) Math.round(totalPaid * multiplier);
    }
}
