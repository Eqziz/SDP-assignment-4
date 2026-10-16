package com.flightreservation.subsystem.ticket;

import com.flightreservation.exception.ExcessBaggageException;

public class BaggageRulesEngine {

    public static final double MAX_ALLOWED_WEIGHT_KG = 32.00;
    public static final double FREE_ALLOWANCE_WEIGHT_KG = 20.00;
    public static final double RATE_PER_EXCESS_KG = 15.00;

    public double validateAndCalculateFee(double baggageWeightKg) {
        if (baggageWeightKg < 0) {
            throw new IllegalArgumentException("Baggage weight cannot be negative: " + baggageWeightKg);
        }

        if (baggageWeightKg > MAX_ALLOWED_WEIGHT_KG) {
            throw new ExcessBaggageException(baggageWeightKg, MAX_ALLOWED_WEIGHT_KG);
        }

        if (baggageWeightKg <= FREE_ALLOWANCE_WEIGHT_KG) {
            return 0.00;
        }

        double excessWeight = baggageWeightKg - FREE_ALLOWANCE_WEIGHT_KG;
        return excessWeight * RATE_PER_EXCESS_KG;
    }
}
