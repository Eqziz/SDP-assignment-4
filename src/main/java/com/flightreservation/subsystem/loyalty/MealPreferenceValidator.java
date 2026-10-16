package com.flightreservation.subsystem.loyalty;

import com.flightreservation.exception.LoyaltyProcessingException;

import java.util.Locale;
import java.util.Set;

public class MealPreferenceValidator {

    private static final Set<String> ALLOWED_PREFERENCES = Set.of(
            "STANDARD",
            "VEGAN",
            "HALAL",
            "KOSHER",
            "GLUTEN_FREE",
            "NONE"
    );

    public String validate(String mealPreference, String memberId) {
        if (memberId != null) {
            String upperMember = memberId.trim().toUpperCase(Locale.ROOT);
            if (upperMember.startsWith("SUSPENDED") || upperMember.startsWith("RESTRICTED")) {
                throw new LoyaltyProcessingException("Loyalty account is restricted or suspended: " + memberId, memberId);
            }
        }

        if (mealPreference == null || mealPreference.trim().isEmpty()) {
            return "STANDARD";
        }

        String normalized = mealPreference.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PREFERENCES.contains(normalized)) {
            throw new LoyaltyProcessingException("Meal preference rejected by catering policy: " + mealPreference, memberId);
        }

        return normalized;
    }
}
