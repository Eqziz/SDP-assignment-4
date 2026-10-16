package com.flightreservation.facade;

import com.flightreservation.coordinator.BookingJournalRepository;
import com.flightreservation.coordinator.BookingStage;
import com.flightreservation.coordinator.BookingTransaction;
import com.flightreservation.exception.InvalidBookingException;
import com.flightreservation.subsystem.loyalty.LoyaltyAccountRecord;
import com.flightreservation.subsystem.loyalty.LoyaltyPointsCalculator;
import com.flightreservation.subsystem.loyalty.LoyaltyRecordRepository;
import com.flightreservation.subsystem.loyalty.LoyaltyStatus;
import com.flightreservation.subsystem.loyalty.MealPreferenceValidator;
import com.flightreservation.subsystem.seat.FareCalculator;
import com.flightreservation.subsystem.seat.SeatMapValidator;
import com.flightreservation.subsystem.seat.SeatReservation;
import com.flightreservation.subsystem.seat.SeatReservationRepository;
import com.flightreservation.subsystem.seat.SeatReservationStatus;
import com.flightreservation.subsystem.ticket.BaggageRulesEngine;
import com.flightreservation.subsystem.ticket.TicketIssuance;
import com.flightreservation.subsystem.ticket.TicketIssuanceRepository;
import com.flightreservation.subsystem.ticket.TicketNumberGenerator;
import com.flightreservation.subsystem.ticket.TicketStatus;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FlightBookingFacade {

    private final SeatReservationRepository seatRepository;
    private final FareCalculator fareCalculator;
    private final SeatMapValidator seatMapValidator;

    private final TicketIssuanceRepository ticketRepository;
    private final BaggageRulesEngine baggageRulesEngine;
    private final TicketNumberGenerator ticketGenerator;

    private final LoyaltyRecordRepository loyaltyRepository;
    private final LoyaltyPointsCalculator pointsCalculator;
    private final MealPreferenceValidator mealValidator;

    private final BookingJournalRepository journalRepository;

    public FlightBookingFacade(
            SeatReservationRepository seatRepository,
            FareCalculator fareCalculator,
            SeatMapValidator seatMapValidator,
            TicketIssuanceRepository ticketRepository,
            BaggageRulesEngine baggageRulesEngine,
            TicketNumberGenerator ticketGenerator,
            LoyaltyRecordRepository loyaltyRepository,
            LoyaltyPointsCalculator pointsCalculator,
            MealPreferenceValidator mealValidator,
            BookingJournalRepository journalRepository) {
        this.seatRepository = Objects.requireNonNull(seatRepository, "seatRepository cannot be null");
        this.fareCalculator = Objects.requireNonNull(fareCalculator, "fareCalculator cannot be null");
        this.seatMapValidator = Objects.requireNonNull(seatMapValidator, "seatMapValidator cannot be null");
        this.ticketRepository = Objects.requireNonNull(ticketRepository, "ticketRepository cannot be null");
        this.baggageRulesEngine = Objects.requireNonNull(baggageRulesEngine, "baggageRulesEngine cannot be null");
        this.ticketGenerator = Objects.requireNonNull(ticketGenerator, "ticketGenerator cannot be null");
        this.loyaltyRepository = Objects.requireNonNull(loyaltyRepository, "loyaltyRepository cannot be null");
        this.pointsCalculator = Objects.requireNonNull(pointsCalculator, "pointsCalculator cannot be null");
        this.mealValidator = Objects.requireNonNull(mealValidator, "mealValidator cannot be null");
        this.journalRepository = Objects.requireNonNull(journalRepository, "journalRepository cannot be null");
    }

    public BookingResult bookFlight(BookingRequest request) {
        validateRequest(request);

        seatMapValidator.ensureSeatAvailable(
                request.getFlightNumber(),
                request.getSeatNumber(),
                seatRepository);

        String bookingReference = "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BookingTransaction transaction = BookingTransaction.create(
                bookingReference,
                request.getFlightNumber(),
                request.getPassengerId(),
                request.getPassengerName(),
                request.getSeatNumber());
        journalRepository.save(transaction);

        double fare = fareCalculator.calculateFare(request.getSeatClass(), request.getSeatNumber());
        String reservationId = "RES-" + bookingReference;
        SeatReservation seatReservation = SeatReservation.create(
                reservationId,
                request.getFlightNumber(),
                request.getPassengerId(),
                request.getSeatNumber(),
                request.getSeatClass(),
                fare);

        seatRepository.save(seatReservation);
        transaction.setSeatReservationId(reservationId);
        transaction.advanceStage(BookingStage.STEP_A_SEAT_HELD);
        journalRepository.update(transaction);

        TicketIssuance ticketIssuance;
        try {
            double excessBaggageFee = baggageRulesEngine.validateAndCalculateFee(request.getBaggageWeightKg());
            String ticketNumber = ticketGenerator.generateTicketNumber();
            ticketIssuance = TicketIssuance.issue(
                    ticketNumber,
                    bookingReference,
                    request.getPassengerName(),
                    request.getFlightNumber(),
                    request.getBaggageWeightKg(),
                    excessBaggageFee);

            ticketRepository.save(ticketIssuance);
            transaction.setTicketNumber(ticketNumber);
            transaction.advanceStage(BookingStage.STEP_B_TICKET_ISSUED);
            journalRepository.update(transaction);
        } catch (Exception ex) {
            transaction.recordFailure(BookingStage.COMPENSATING_AFTER_B_FAILURE, ex.getMessage());
            journalRepository.update(transaction);

            seatReservation.transitionTo(SeatReservationStatus.RELEASED);
            seatRepository.update(seatReservation);

            transaction.advanceStage(BookingStage.COMPENSATED_A_ONLY);
            journalRepository.update(transaction);

            return BookingResult.compensated(
                    bookingReference,
                    BookingStage.COMPENSATED_A_ONLY,
                    seatReservation,
                    null,
                    ex.getMessage(),
                    List.of("Subsystem A (Seat HELD -> RELEASED)"));
        }

        LoyaltyAccountRecord loyaltyRecord;
        try {
            String meal = mealValidator.validate(request.getMealPreference(), request.getMemberId());
            double totalPaid = fare + ticketIssuance.getExcessBaggageFee();
            int points = pointsCalculator.calculatePoints(totalPaid, request.getMembershipTier());
            String confirmationCode = "CONF-" + bookingReference;

            loyaltyRecord = LoyaltyAccountRecord.accrue(
                    confirmationCode,
                    bookingReference,
                    request.getMemberId(),
                    request.getMembershipTier(),
                    points,
                    meal,
                    totalPaid);
            loyaltyRepository.save(loyaltyRecord);

            seatReservation.transitionTo(SeatReservationStatus.CONFIRMED);
            seatRepository.update(seatReservation);

            ticketIssuance.transitionTo(TicketStatus.COMPLETED);
            ticketRepository.update(ticketIssuance);

            loyaltyRecord.transitionTo(LoyaltyStatus.SETTLED);
            loyaltyRepository.update(loyaltyRecord);

            transaction.setConfirmationCode(confirmationCode);
            transaction.advanceStage(BookingStage.COMPLETED);
            journalRepository.update(transaction);

            return BookingResult.success(bookingReference, seatReservation, ticketIssuance, loyaltyRecord);
        } catch (Exception ex) {
            transaction.recordFailure(BookingStage.COMPENSATING_AFTER_C_FAILURE, ex.getMessage());
            journalRepository.update(transaction);

            ticketIssuance.transitionTo(TicketStatus.VOIDED);
            ticketRepository.update(ticketIssuance);

            seatReservation.transitionTo(SeatReservationStatus.RELEASED);
            seatRepository.update(seatReservation);

            transaction.advanceStage(BookingStage.COMPENSATED_FULL);
            journalRepository.update(transaction);

            return BookingResult.compensated(
                    bookingReference,
                    BookingStage.COMPENSATED_FULL,
                    seatReservation,
                    ticketIssuance,
                    ex.getMessage(),
                    List.of(
                            "Subsystem B (Ticket ISSUED -> VOIDED)",
                            "Subsystem A (Seat HELD -> RELEASED)"
                    ));
        }
    }

    private void validateRequest(BookingRequest request) {
        if (request == null) {
            throw new InvalidBookingException("Booking request cannot be null");
        }
        if (isBlank(request.getFlightNumber())) {
            throw new InvalidBookingException("Flight number is required");
        }
        if (isBlank(request.getPassengerId())) {
            throw new InvalidBookingException("Passenger ID is required");
        }
        if (isBlank(request.getPassengerName())) {
            throw new InvalidBookingException("Passenger name is required");
        }
        if (isBlank(request.getSeatNumber())) {
            throw new InvalidBookingException("Seat number is required");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
