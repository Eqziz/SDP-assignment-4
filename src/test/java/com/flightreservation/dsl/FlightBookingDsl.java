package com.flightreservation.dsl;

import com.flightreservation.coordinator.BookingStage;
import com.flightreservation.coordinator.BookingTransaction;
import com.flightreservation.facade.BookingRequest;
import com.flightreservation.facade.BookingResult;
import com.flightreservation.facade.FlightBookingFacade;
import com.flightreservation.facade.FlightOperationsFacade;
import com.flightreservation.facade.ReservationInspection;
import com.flightreservation.subsystem.loyalty.LoyaltyAccountRecord;
import com.flightreservation.subsystem.loyalty.LoyaltyStatus;
import com.flightreservation.subsystem.seat.SeatReservation;
import com.flightreservation.subsystem.seat.SeatReservationStatus;
import com.flightreservation.subsystem.ticket.TicketIssuance;
import com.flightreservation.subsystem.ticket.TicketStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FlightBookingDsl {

    private final FlightBookingFacade bookingFacade;
    private final FlightOperationsFacade operationsFacade;

    private String passengerId = "PAX-DEFAULT";
    private String passengerName = "Default Passenger";
    private String flightNumber = "AA100";
    private String seatNumber = "12A";
    private String seatClass = "ECONOMY";
    private double baggageWeightKg = 15.0;
    private String memberId = "MEM-100";
    private String membershipTier = "STANDARD";
    private String mealPreference = "STANDARD";

    private BookingResult executionResult;

    private FlightBookingDsl(FlightBookingFacade bookingFacade, FlightOperationsFacade operationsFacade) {
        this.bookingFacade = bookingFacade;
        this.operationsFacade = operationsFacade;
    }

    public static FlightBookingDsl given(FlightBookingFacade bookingFacade, FlightOperationsFacade operationsFacade) {
        return new FlightBookingDsl(bookingFacade, operationsFacade);
    }

    public FlightBookingDsl forPassenger(String passengerId, String passengerName) {
        this.passengerId = passengerId;
        this.passengerName = passengerName;
        return this;
    }

    public FlightBookingDsl flying(String flightNumber) {
        this.flightNumber = flightNumber;
        return this;
    }

    public FlightBookingDsl requestingSeat(String seatNumber, String seatClass) {
        this.seatNumber = seatNumber;
        this.seatClass = seatClass;
        return this;
    }

    public FlightBookingDsl withBaggage(double baggageWeightKg) {
        this.baggageWeightKg = baggageWeightKg;
        return this;
    }

    public FlightBookingDsl withLoyalty(String memberId, String membershipTier, String mealPreference) {
        this.memberId = memberId;
        this.membershipTier = membershipTier;
        this.mealPreference = mealPreference;
        return this;
    }

    public FlightBookingDsl whenBooking() {
        BookingRequest request = BookingRequest.builder()
                .flightNumber(flightNumber)
                .passengerId(passengerId)
                .passengerName(passengerName)
                .seatNumber(seatNumber)
                .seatClass(seatClass)
                .baggageWeightKg(baggageWeightKg)
                .memberId(memberId)
                .membershipTier(membershipTier)
                .mealPreference(mealPreference)
                .build();
        this.executionResult = bookingFacade.bookFlight(request);
        return this;
    }

    public void whenBookingExpectingException(Class<? extends Throwable> exceptionClass) {
        BookingRequest request = BookingRequest.builder()
                .flightNumber(flightNumber)
                .passengerId(passengerId)
                .passengerName(passengerName)
                .seatNumber(seatNumber)
                .seatClass(seatClass)
                .baggageWeightKg(baggageWeightKg)
                .memberId(memberId)
                .membershipTier(membershipTier)
                .mealPreference(mealPreference)
                .build();
        assertThatThrownBy(() -> bookingFacade.bookFlight(request)).isInstanceOf(exceptionClass);
    }

    public FlightBookingDsl thenExpectSuccess() {
        assertThat(executionResult).isNotNull();
        assertThat(executionResult.isSuccess()).isTrue();
        assertThat(executionResult.getFinalStage()).isEqualTo(BookingStage.COMPLETED);
        assertThat(executionResult.getSeatReservation()).isNotNull();
        assertThat(executionResult.getTicketIssuance()).isNotNull();
        assertThat(executionResult.getLoyaltyRecord()).isNotNull();
        return this;
    }

    public FlightBookingDsl thenExpectFailureAtStepB() {
        assertThat(executionResult).isNotNull();
        assertThat(executionResult.isSuccess()).isFalse();
        assertThat(executionResult.getFinalStage()).isEqualTo(BookingStage.COMPENSATED_A_ONLY);
        assertThat(executionResult.getTicketIssuance()).isNull();
        assertThat(executionResult.getLoyaltyRecord()).isNull();
        assertThat(executionResult.getCompensatedSteps()).contains("Subsystem A (Seat HELD -> RELEASED)");
        return this;
    }

    public FlightBookingDsl thenExpectFailureAtStepC() {
        assertThat(executionResult).isNotNull();
        assertThat(executionResult.isSuccess()).isFalse();
        assertThat(executionResult.getFinalStage()).isEqualTo(BookingStage.COMPENSATED_FULL);
        assertThat(executionResult.getTicketIssuance()).isNotNull();
        assertThat(executionResult.getLoyaltyRecord()).isNull();
        assertThat(executionResult.getCompensatedSteps()).contains(
                "Subsystem B (Ticket ISSUED -> VOIDED)",
                "Subsystem A (Seat HELD -> RELEASED)"
        );
        return this;
    }

    public FlightBookingDsl andVerifyPersistedStates(
            SeatReservationStatus expectedSeatStatus,
            TicketStatus expectedTicketStatus,
            LoyaltyStatus expectedLoyaltyStatus) {
        ReservationInspection inspection = operationsFacade.inspectBooking(executionResult.getBookingReference());
        assertThat(inspection.isConsistent()).isTrue();

        SeatReservation seat = inspection.getSeatReservation();
        assertThat(seat).isNotNull();
        assertThat(seat.getStatus()).isEqualTo(expectedSeatStatus);

        TicketIssuance ticket = inspection.getTicketIssuance();
        if (expectedTicketStatus == null) {
            assertThat(ticket).isNull();
        } else {
            assertThat(ticket).isNotNull();
            assertThat(ticket.getStatus()).isEqualTo(expectedTicketStatus);
        }

        LoyaltyAccountRecord loyalty = inspection.getLoyaltyRecord();
        if (expectedLoyaltyStatus == null) {
            assertThat(loyalty).isNull();
        } else {
            assertThat(loyalty).isNotNull();
            assertThat(loyalty.getStatus()).isEqualTo(expectedLoyaltyStatus);
        }

        return this;
    }

    public FlightBookingDsl andVerifyCompensatedAOnly() {
        ReservationInspection inspection = operationsFacade.inspectBooking(executionResult.getBookingReference());
        assertThat(inspection.isConsistent()).isTrue();
        assertThat(inspection.getTransaction().getStage()).isEqualTo(BookingStage.COMPENSATED_A_ONLY);
        assertThat(inspection.getSeatReservation().getStatus()).isEqualTo(SeatReservationStatus.RELEASED);
        assertThat(inspection.getTicketIssuance()).isNull();
        assertThat(inspection.getLoyaltyRecord()).isNull();
        return this;
    }

    public FlightBookingDsl andVerifyCompensatedFull() {
        ReservationInspection inspection = operationsFacade.inspectBooking(executionResult.getBookingReference());
        assertThat(inspection.isConsistent()).isTrue();
        assertThat(inspection.getTransaction().getStage()).isEqualTo(BookingStage.COMPENSATED_FULL);
        assertThat(inspection.getSeatReservation().getStatus()).isEqualTo(SeatReservationStatus.RELEASED);
        assertThat(inspection.getTicketIssuance().getStatus()).isEqualTo(TicketStatus.VOIDED);
        assertThat(inspection.getLoyaltyRecord()).isNull();
        return this;
    }

    public BookingResult getExecutionResult() {
        return executionResult;
    }
}
