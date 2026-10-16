package com.flightreservation.coordinator;

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

public class BookingJournalRepository {

    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final Map<String, BookingTransaction> memoryStore = new LinkedHashMap<>();

    public BookingJournalRepository(Path filePath) {
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
            List<BookingTransaction> list = objectMapper.readValue(bytes, new TypeReference<List<BookingTransaction>>() {});
            memoryStore.clear();
            for (BookingTransaction item : list) {
                memoryStore.put(item.getBookingReference(), item);
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

    public synchronized void save(BookingTransaction tx) {
        Objects.requireNonNull(tx, "transaction cannot be null");
        memoryStore.put(tx.getBookingReference(), tx);
        flushToDisk();
    }

    public synchronized void update(BookingTransaction tx) {
        Objects.requireNonNull(tx, "transaction cannot be null");
        if (!memoryStore.containsKey(tx.getBookingReference())) {
            throw new IllegalArgumentException("Cannot update non-existent transaction: " + tx.getBookingReference());
        }
        memoryStore.put(tx.getBookingReference(), tx);
        flushToDisk();
    }

    public synchronized Optional<BookingTransaction> findByReference(String bookingReference) {
        return Optional.ofNullable(memoryStore.get(bookingReference));
    }

    public synchronized List<BookingTransaction> findUnresolvedTransactions() {
        return memoryStore.values().stream()
                .filter(tx -> tx.getStage() == BookingStage.STEP_A_SEAT_HELD
                        || tx.getStage() == BookingStage.STEP_B_TICKET_ISSUED
                        || tx.getStage() == BookingStage.COMPENSATING_AFTER_B_FAILURE
                        || tx.getStage() == BookingStage.COMPENSATING_AFTER_C_FAILURE)
                .toList();
    }

    public synchronized List<BookingTransaction> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(memoryStore.values()));
    }

    public synchronized void clear() {
        memoryStore.clear();
        flushToDisk();
    }
}
