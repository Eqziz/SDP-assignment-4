package com.flightreservation.subsystem.seat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SeatReservationRepository {

    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final Map<String, SeatReservation> memoryStore = new LinkedHashMap<>();

    public SeatReservationRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
        ensureFileExists();
        reload();
    }

    private void ensureFileExists() {
        try {
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, "[]");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize storage file: " + filePath, e);
        }
    }

    public synchronized void reload() {
        ensureFileExists();
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length == 0) {
                memoryStore.clear();
                return;
            }
            List<SeatReservation> list = objectMapper.readValue(bytes, new TypeReference<List<SeatReservation>>() {});
            memoryStore.clear();
            for (SeatReservation item : list) {
                memoryStore.put(item.getReservationId(), item);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to reload data from file: " + filePath, e);
        }
    }

    private void flushToDisk() {
        try {
            objectMapper.writeValue(filePath.toFile(), new ArrayList<>(memoryStore.values()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist data to file: " + filePath, e);
        }
    }

    public synchronized void save(SeatReservation reservation) {
        Objects.requireNonNull(reservation, "reservation cannot be null");
        memoryStore.put(reservation.getReservationId(), reservation);
        flushToDisk();
    }

    public synchronized void update(SeatReservation reservation) {
        Objects.requireNonNull(reservation, "reservation cannot be null");
        if (!memoryStore.containsKey(reservation.getReservationId())) {
            throw new IllegalArgumentException("Cannot update non-existent reservation: " + reservation.getReservationId());
        }
        memoryStore.put(reservation.getReservationId(), reservation);
        flushToDisk();
    }

    public synchronized Optional<SeatReservation> findById(String reservationId) {
        return Optional.ofNullable(memoryStore.get(reservationId));
    }

    public synchronized Optional<SeatReservation> findActiveByFlightAndSeat(String flightNumber, String seatNumber) {
        return memoryStore.values().stream()
                .filter(r -> r.getFlightNumber().equalsIgnoreCase(flightNumber))
                .filter(r -> r.getSeatNumber().equalsIgnoreCase(seatNumber))
                .filter(r -> r.getStatus() == SeatReservationStatus.HELD || r.getStatus() == SeatReservationStatus.CONFIRMED)
                .findFirst();
    }

    public synchronized List<SeatReservation> findByFlight(String flightNumber) {
        return memoryStore.values().stream()
                .filter(r -> r.getFlightNumber().equalsIgnoreCase(flightNumber))
                .toList();
    }

    public synchronized List<SeatReservation> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(memoryStore.values()));
    }

    public synchronized void clear() {
        memoryStore.clear();
        flushToDisk();
    }
}
