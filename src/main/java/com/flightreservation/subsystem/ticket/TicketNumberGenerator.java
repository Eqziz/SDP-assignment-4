package com.flightreservation.subsystem.ticket;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class TicketNumberGenerator {

    private static final Pattern TICKET_PATTERN = Pattern.compile("^ETKT-2026-\\d{6}$");
    private final AtomicLong sequence = new AtomicLong(100000);

    public String generateTicketNumber() {
        long current = sequence.incrementAndGet();
        return "ETKT-2026-" + current;
    }

    public boolean isValidTicketNumber(String ticketNumber) {
        return ticketNumber != null && TICKET_PATTERN.matcher(ticketNumber.trim()).matches();
    }
}
