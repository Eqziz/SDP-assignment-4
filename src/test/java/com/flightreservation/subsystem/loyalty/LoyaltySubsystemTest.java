package com.flightreservation.subsystem.loyalty;

import com.flightreservation.exception.LoyaltyProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoyaltySubsystemTest {

    @TempDir
    Path tempDir;

    private LoyaltyRecordRepository repository;
    private LoyaltyPointsCalculator pointsCalculator;
    private MealPreferenceValidator mealValidator;

    @BeforeEach
    void setUp() {
        Path storePath = tempDir.resolve("loyalty.json");
        repository = new LoyaltyRecordRepository(storePath);
        pointsCalculator = new LoyaltyPointsCalculator();
        mealValidator = new MealPreferenceValidator();
    }

    @Test
    void shouldCalculatePointsByTierMultipliers() {
        assertThat(pointsCalculator.calculatePoints(200.0, "STANDARD")).isEqualTo(200);
        assertThat(pointsCalculator.calculatePoints(200.0, "SILVER")).isEqualTo(250);
        assertThat(pointsCalculator.calculatePoints(200.0, "GOLD")).isEqualTo(300);
        assertThat(pointsCalculator.calculatePoints(200.0, "PLATINUM")).isEqualTo(400);
        assertThat(pointsCalculator.calculatePoints(200.0, "UNKNOWN")).isEqualTo(200);
    }

    @Test
    void shouldRejectNegativeAmountInPointsCalculation() {
        assertThatThrownBy(() -> pointsCalculator.calculatePoints(-50.0, "GOLD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldValidateMealPreferencesCorrectly() {
        assertThat(mealValidator.validate("vegan", "MEM-1")).isEqualTo("VEGAN");
        assertThat(mealValidator.validate(null, "MEM-1")).isEqualTo("STANDARD");
        assertThat(mealValidator.validate("", "MEM-1")).isEqualTo("STANDARD");
        assertThat(mealValidator.validate("halal", "MEM-1")).isEqualTo("HALAL");
        assertThat(mealValidator.validate("kosher", "MEM-1")).isEqualTo("KOSHER");

        assertThatThrownBy(() -> mealValidator.validate("UNSUPPORTED_FAST_FOOD", "MEM-1"))
                .isInstanceOf(LoyaltyProcessingException.class)
                .hasMessageContaining("rejected by catering policy");
    }

    @Test
    void shouldRejectSuspendedOrRestrictedMembers() {
        assertThatThrownBy(() -> mealValidator.validate("STANDARD", "SUSPENDED_USER"))
                .isInstanceOf(LoyaltyProcessingException.class)
                .hasMessageContaining("restricted or suspended");

        assertThatThrownBy(() -> mealValidator.validate("STANDARD", "RESTRICTED_99"))
                .isInstanceOf(LoyaltyProcessingException.class)
                .hasMessageContaining("restricted or suspended");
    }

    @Test
    void shouldEnforceLegalStateTransitionsOnLoyaltyRecord() {
        LoyaltyAccountRecord record = LoyaltyAccountRecord.accrue(
                "CONF-1", "BKG-1", "MEM-10", "GOLD", 300, "VEGAN", 200.0);
        assertThat(record.getStatus()).isEqualTo(LoyaltyStatus.ACCRUED);

        record.transitionTo(LoyaltyStatus.ACCRUED);
        assertThat(record.getStatus()).isEqualTo(LoyaltyStatus.ACCRUED);

        record.transitionTo(LoyaltyStatus.SETTLED);
        assertThat(record.getStatus()).isEqualTo(LoyaltyStatus.SETTLED);

        record.transitionTo(LoyaltyStatus.REVOKED);
        assertThat(record.getStatus()).isEqualTo(LoyaltyStatus.REVOKED);

        assertThatThrownBy(() -> record.transitionTo(LoyaltyStatus.ACCRUED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldPersistAndReloadLoyaltyRecordsFromDisk() {
        LoyaltyAccountRecord record = LoyaltyAccountRecord.accrue(
                "CONF-2026", "BKG-99", "MEM-77", "PLATINUM", 500, "KOSHER", 250.0);
        repository.save(record);

        LoyaltyRecordRepository reopened = new LoyaltyRecordRepository(tempDir.resolve("loyalty.json"));
        Optional<LoyaltyAccountRecord> found = reopened.findById("CONF-2026");

        assertThat(found).isPresent();
        assertThat(found.get().getMemberId()).isEqualTo("MEM-77");
        assertThat(found.get().getPointsAccrued()).isEqualTo(500);
        assertThat(found.get().getStatus()).isEqualTo(LoyaltyStatus.ACCRUED);

        record.transitionTo(LoyaltyStatus.REVOKED);
        repository.update(record);

        reopened.reload();
        assertThat(reopened.findById("CONF-2026").get().getStatus()).isEqualTo(LoyaltyStatus.REVOKED);
    }

    @Test
    void shouldFindByBookingReference() {
        LoyaltyAccountRecord record = LoyaltyAccountRecord.accrue(
                "CONF-REF", "BKG-REF", "MEM-1", "STANDARD", 100, "STANDARD", 100.0);
        repository.save(record);

        Optional<LoyaltyAccountRecord> retrieved = repository.findByBookingReference("BKG-REF");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getConfirmationCode()).isEqualTo("CONF-REF");
    }
}
