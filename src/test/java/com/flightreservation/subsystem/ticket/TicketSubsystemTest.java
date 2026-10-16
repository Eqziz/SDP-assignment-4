package com.flightreservation.subsystem.ticket;

import com.flightreservation.exception.ExcessBaggageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketSubsystemTest {

    @TempDir
    Path tempDir;

    private TicketIssuanceRepository repository;
    private BaggageRulesEngine baggageEngine;
    private TicketNumberGenerator generator;

    @BeforeEach
    void setUp() {
        Path storePath = tempDir.resolve("tickets.json");
        repository = new TicketIssuanceRepository(storePath);
        baggageEngine = new BaggageRulesEngine();
        generator = new TicketNumberGenerator();
    }

    @Test
    void shouldCalculateExcessBaggageFeesCorrectly() {
        double feeForZeroKg = baggageEngine.validateAndCalculateFee(0.0);
        assertThat(feeForZeroKg).isEqualTo(0.0);

        double feeForWithinLimit = baggageEngine.validateAndCalculateFee(18.5);
        assertThat(feeForWithinLimit).isEqualTo(0.0);

        double feeForExactAllowance = baggageEngine.validateAndCalculateFee(20.0);
        assertThat(feeForExactAllowance).isEqualTo(0.0);

        double feeForExcess = baggageEngine.validateAndCalculateFee(25.0);
        assertThat(feeForExcess).isEqualTo(75.0);
    }

    @Test
    void shouldRejectBaggageExceedingMaxLimit() {
        assertThatThrownBy(() -> baggageEngine.validateAndCalculateFee(32.5))
                .isInstanceOf(ExcessBaggageException.class)
                .hasMessageContaining("exceeds maximum allowed limit");
    }

    @Test
    void shouldRejectNegativeBaggageWeight() {
        assertThatThrownBy(() -> baggageEngine.validateAndCalculateFee(-1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldGenerateValidTicketNumbers() {
        String ticketNumber1 = generator.generateTicketNumber();
        String ticketNumber2 = generator.generateTicketNumber();

        assertThat(generator.isValidTicketNumber(ticketNumber1)).isTrue();
        assertThat(generator.isValidTicketNumber(ticketNumber2)).isTrue();
        assertThat(ticketNumber1).isNotEqualTo(ticketNumber2);
        assertThat(generator.isValidTicketNumber("INVALID")).isFalse();
    }

    @Test
    void shouldEnforceLegalStateTransitionsOnTicket() {
        TicketIssuance ticket = TicketIssuance.issue("ETKT-1", "BKG-1", "John Doe", "AA100", 15.0, 0.0);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ISSUED);

        ticket.transitionTo(TicketStatus.ISSUED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ISSUED);

        ticket.transitionTo(TicketStatus.COMPLETED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.COMPLETED);

        ticket.transitionTo(TicketStatus.VOIDED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.VOIDED);

        assertThatThrownBy(() -> ticket.transitionTo(TicketStatus.ISSUED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldPersistAndReloadTicketsFromDisk() {
        TicketIssuance ticket = TicketIssuance.issue("ETKT-2026-100001", "BKG-2", "Jane Roe", "AA200", 24.0, 60.0);
        repository.save(ticket);

        TicketIssuanceRepository reopened = new TicketIssuanceRepository(tempDir.resolve("tickets.json"));
        Optional<TicketIssuance> found = reopened.findById("ETKT-2026-100001");

        assertThat(found).isPresent();
        assertThat(found.get().getPassengerName()).isEqualTo("Jane Roe");
        assertThat(found.get().getExcessBaggageFee()).isEqualTo(60.0);
        assertThat(found.get().getStatus()).isEqualTo(TicketStatus.ISSUED);

        ticket.transitionTo(TicketStatus.VOIDED);
        repository.update(ticket);

        reopened.reload();
        assertThat(reopened.findById("ETKT-2026-100001").get().getStatus()).isEqualTo(TicketStatus.VOIDED);
    }

    @Test
    void shouldFindByBookingReference() {
        TicketIssuance ticket = TicketIssuance.issue("ETKT-2026-999999", "BKG-SPECIAL", "Bob", "AA300", 10.0, 0.0);
        repository.save(ticket);

        Optional<TicketIssuance> retrieved = repository.findByBookingReference("BKG-SPECIAL");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTicketNumber()).isEqualTo("ETKT-2026-999999");
    }
}
