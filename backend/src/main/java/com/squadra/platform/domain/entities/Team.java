package com.squadra.platform.domain.entities;

import java.time.Instant;
import java.time.Year;
import java.util.Objects;
import java.util.UUID;

public final class Team {
    private final UUID id;
    private UUID ownerUserId;
    private final Instant createdAt;
    private String name;
    private String city;
    private String state;
    private Integer foundedYear;
    private String description;
    private Instant updatedAt;

    public Team(UUID id, UUID ownerUserId, String name, String city, String state, Integer foundedYear,
            String description, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(name, city, state, foundedYear, description, Objects.requireNonNull(updatedAt, "updatedAt is required"));
    }

    public static Team create(UUID ownerUserId, SaveFields fields, Instant now) {
        return new Team(UUID.randomUUID(), ownerUserId, fields.name(), fields.city(), fields.state(),
                fields.foundedYear(), fields.description(), now, now);
    }

    public void update(SaveFields fields, Instant now) {
        apply(fields.name(), fields.city(), fields.state(), fields.foundedYear(), fields.description(), now);
    }

    public void transferOwnership(UUID replacementOwnerUserId, Instant now) {
        this.ownerUserId = Objects.requireNonNull(replacementOwnerUserId, "ownerUserId is required");
        this.updatedAt = Objects.requireNonNull(now, "updatedAt is required");
    }

    public UUID id() { return id; }
    public UUID ownerUserId() { return ownerUserId; }
    public String name() { return name; }
    public String city() { return city; }
    public String state() { return state; }
    public Integer foundedYear() { return foundedYear; }
    public String description() { return description; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    private void apply(String name, String city, String state, Integer foundedYear, String description, Instant updatedAt) {
        this.name = requiredText(name, "name", 2, 100);
        this.city = optionalText(city, "city", 80);
        this.state = optionalText(state, "state", 80);
        this.foundedYear = foundedYear(foundedYear);
        this.description = optionalText(description, "description", 500);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static Integer foundedYear(Integer value) {
        if (value == null) return null;
        if (value < 1850 || value > Year.now().getValue()) {
            throw new IllegalArgumentException("foundedYear must be between 1850 and the current year");
        }
        return value;
    }

    private static String requiredText(String value, String field, int min, int max) {
        String normalized = optionalText(value, field, max);
        if (normalized == null || normalized.length() < min) {
            throw new IllegalArgumentException(field + " must have at least " + min + " characters");
        }
        return normalized;
    }

    private static String optionalText(String value, String field, int max) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > max) {
            throw new IllegalArgumentException(field + " must have at most " + max + " characters");
        }
        return normalized;
    }

    public record SaveFields(String name, String city, String state, Integer foundedYear, String description) { }
}
