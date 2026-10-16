package com.flightreservation.subsystem.loyalty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public class LoyaltyAccountRecord {

    private final String confirmationCode;
    private final String bookingReference;
    private final String memberId;
    private final String membershipTier;
    private final int pointsAccrued;
    private final String mealPreference;
    private final double totalPaid;
    private final Instant confirmedAt;
    private LoyaltyStatus status;

    @JsonCreator
    public LoyaltyAccountRecord(
            @JsonProperty("confirmationCode") String confirmationCode,
            @JsonProperty("bookingReference") String bookingReference,
            @JsonProperty("memberId") String memberId,
            @JsonProperty("membershipTier") String membershipTier,
            @JsonProperty("pointsAccrued") int pointsAccrued,
            @JsonProperty("mealPreference") String mealPreference,
            @JsonProperty("totalPaid") double totalPaid,
            @JsonProperty("confirmedAt") Instant confirmedAt,
            @JsonProperty("status") LoyaltyStatus status) {
        this.confirmationCode = Objects.requireNonNull(confirmationCode, "confirmationCode cannot be null");
        this.bookingReference = Objects.requireNonNull(bookingReference, "bookingReference cannot be null");
        this.memberId = Objects.requireNonNull(memberId, "memberId cannot be null");
        this.membershipTier = Objects.requireNonNull(membershipTier, "membershipTier cannot be null");
        this.pointsAccrued = pointsAccrued;
        this.mealPreference = mealPreference != null ? mealPreference : "STANDARD";
        this.totalPaid = totalPaid;
        this.confirmedAt = confirmedAt != null ? confirmedAt : Instant.now();
        this.status = status != null ? status : LoyaltyStatus.ACCRUED;
    }

    public static LoyaltyAccountRecord accrue(
            String confirmationCode,
            String bookingReference,
            String memberId,
            String membershipTier,
            int pointsAccrued,
            String mealPreference,
            double totalPaid) {
        return new LoyaltyAccountRecord(
                confirmationCode,
                bookingReference,
                memberId,
                membershipTier,
                pointsAccrued,
                mealPreference,
                totalPaid,
                Instant.now(),
                LoyaltyStatus.ACCRUED);
    }

    public synchronized void transitionTo(LoyaltyStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus cannot be null");
        if (this.status == newStatus) {
            return;
        }

        boolean legal = switch (this.status) {
            case ACCRUED -> newStatus == LoyaltyStatus.SETTLED || newStatus == LoyaltyStatus.REVOKED;
            case SETTLED -> newStatus == LoyaltyStatus.REVOKED;
            case REVOKED -> false;
        };

        if (!legal) {
            throw new IllegalStateException("Illegal loyalty state transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMembershipTier() {
        return membershipTier;
    }

    public int getPointsAccrued() {
        return pointsAccrued;
    }

    public String getMealPreference() {
        return mealPreference;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public LoyaltyStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoyaltyAccountRecord that)) return false;
        return Objects.equals(confirmationCode, that.confirmationCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(confirmationCode);
    }

    @Override
    public String toString() {
        return "LoyaltyAccountRecord{" +
                "confirmationCode='" + confirmationCode + '\'' +
                ", bookingReference='" + bookingReference + '\'' +
                ", memberId='" + memberId + '\'' +
                ", membershipTier='" + membershipTier + '\'' +
                ", pointsAccrued=" + pointsAccrued +
                ", mealPreference='" + mealPreference + '\'' +
                ", totalPaid=" + totalPaid +
                ", status=" + status +
                '}';
    }
}
