package com.flightreservation.subsystem.loyalty;

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

public class LoyaltyRecordRepository {

    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final Map<String, LoyaltyAccountRecord> memoryStore = new LinkedHashMap<>();

    public LoyaltyRecordRepository(Path filePath) {
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
            List<LoyaltyAccountRecord> list = objectMapper.readValue(bytes, new TypeReference<List<LoyaltyAccountRecord>>() {});
            memoryStore.clear();
            for (LoyaltyAccountRecord item : list) {
                memoryStore.put(item.getConfirmationCode(), item);
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

    public synchronized void save(LoyaltyAccountRecord record) {
        Objects.requireNonNull(record, "record cannot be null");
        memoryStore.put(record.getConfirmationCode(), record);
        flushToDisk();
    }

    public synchronized void update(LoyaltyAccountRecord record) {
        Objects.requireNonNull(record, "record cannot be null");
        if (!memoryStore.containsKey(record.getConfirmationCode())) {
            throw new IllegalArgumentException("Cannot update non-existent loyalty record: " + record.getConfirmationCode());
        }
        memoryStore.put(record.getConfirmationCode(), record);
        flushToDisk();
    }

    public synchronized Optional<LoyaltyAccountRecord> findById(String confirmationCode) {
        return Optional.ofNullable(memoryStore.get(confirmationCode));
    }

    public synchronized Optional<LoyaltyAccountRecord> findByBookingReference(String bookingReference) {
        return memoryStore.values().stream()
                .filter(r -> r.getBookingReference().equalsIgnoreCase(bookingReference))
                .findFirst();
    }

    public synchronized List<LoyaltyAccountRecord> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(memoryStore.values()));
    }

    public synchronized void clear() {
        memoryStore.clear();
        flushToDisk();
    }
}
