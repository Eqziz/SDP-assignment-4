package com.flightreservation.subsystem.seat;

import com.flightreservation.exception.SeatUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeatSubsystemTest {

    @TempDir
    Path tempDir;

    private SeatReservationRepository repository;
    private FareCalculator fareCalculator;
    private SeatMapValidator validator;

    @BeforeEach
    void setUp() {
        Path storePath = tempDir.resolve("seats.json");
        repository = new SeatReservationRepository(storePath);
        fareCalculator = new FareCalculator();
        validator = new SeatMapValidator();
    }

    @Test
    void shouldCalculateFareCorrectlyForDifferentClassesAndSeats() {
        double economyMiddle = fareCalculator.calculateFare("ECONOMY", "15B");
        assertThat(economyMiddle).isEqualTo(225.0);

        double economyWindow = fareCalculator.calculateFare("ECONOMY", "15A");
        assertThat(economyWindow).isEqualTo(240.0);

        double economyExitRowAisle = fareCalculator.calculateFare("ECONOMY", "10C");
        assertThat(economyExitRowAisle).isEqualTo(270.0);

        double businessWindow = fareCalculator.calculateFare("BUSINESS", "2F");
        assertThat(businessWindow).isEqualTo(480.0);

        double firstClass = fareCalculator.calculateFare("FIRST", "1A");
        assertThat(firstClass).isEqualTo(790.0);
    }

    @Test
    void shouldThrowWhenSeatClassIsInvalid() {
        assertThatThrownBy(() -> fareCalculator.calculateFare("VIP_SUITE", "1A"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldValidateSeatFormatCorrectly() {
        validator.validateSeatFormat("12A");
        validator.validateSeatFormat("1F");
        validator.validateSeatFormat("35D");

        assertThatThrownBy(() -> validator.validateSeatFormat("0A"))
                .isInstanceOf(SeatUnavailableException.class);
        assertThatThrownBy(() -> validator.validateSeatFormat("36A"))
                .isInstanceOf(SeatUnavailableException.class);
        assertThatThrownBy(() -> validator.validateSeatFormat("12G"))
                .isInstanceOf(SeatUnavailableException.class);
        assertThatThrownBy(() -> validator.validateSeatFormat("INVALID"))
                .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    void shouldPreventReservingOccupiedSeat() {
        SeatReservation reservation = SeatReservation.create("RES-1", "AA100", "PAX-1", "12B", "ECONOMY", 225.0);
        repository.save(reservation);

        validator.ensureSeatAvailable("AA100", "12C", repository);

        assertThatThrownBy(() -> validator.ensureSeatAvailable("AA100", "12B", repository))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("already occupied");
    }

    @Test
    void shouldEnforceLegalStateTransitionsOnEntity() {
        SeatReservation reservation = SeatReservation.create("RES-2", "AA100", "PAX-2", "14A", "ECONOMY", 240.0);
        assertThat(reservation.getStatus()).isEqualTo(SeatReservationStatus.HELD);

        reservation.transitionTo(SeatReservationStatus.HELD);
        assertThat(reservation.getStatus()).isEqualTo(SeatReservationStatus.HELD);

        reservation.transitionTo(SeatReservationStatus.CONFIRMED);
        assertThat(reservation.getStatus()).isEqualTo(SeatReservationStatus.CONFIRMED);

        reservation.transitionTo(SeatReservationStatus.RELEASED);
        assertThat(reservation.getStatus()).isEqualTo(SeatReservationStatus.RELEASED);

        assertThatThrownBy(() -> reservation.transitionTo(SeatReservationStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldPersistAndReloadFromDisk() {
        SeatReservation reservation = SeatReservation.create("RES-3", "AA200", "PAX-3", "18C", "BUSINESS", 475.0);
        repository.save(reservation);

        SeatReservationRepository reopened = new SeatReservationRepository(tempDir.resolve("seats.json"));
        Optional<SeatReservation> found = reopened.findById("RES-3");

        assertThat(found).isPresent();
        assertThat(found.get().getSeatNumber()).isEqualTo("18C");
        assertThat(found.get().getStatus()).isEqualTo(SeatReservationStatus.HELD);

        reservation.transitionTo(SeatReservationStatus.RELEASED);
        repository.update(reservation);

        reopened.reload();
        assertThat(reopened.findById("RES-3").get().getStatus()).isEqualTo(SeatReservationStatus.RELEASED);
    }

    @Test
    void shouldSupportDirectStandaloneSubsystemUsage() {
        SeatReservation reservation = SeatReservation.create("RES-DIRECT", "AA500", "PAX-99", "1A", "FIRST", 790.0);
        repository.save(reservation);
        List<SeatReservation> flightList = repository.findByFlight("AA500");
        assertThat(flightList).hasSize(1);
        assertThat(flightList.getFirst().getReservationId()).isEqualTo("RES-DIRECT");
    }
}
