package com.squadra.platform.domain.entities;

import com.squadra.platform.domain.enums.DominantFoot;
import com.squadra.platform.domain.enums.FootballPosition;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class AthleteProfile {
    private final UUID id;
    private final UUID userId;
    private final Instant createdAt;
    private String displayName;
    private String fullName;
    private LocalDate birthDate;
    private FootballPosition primaryPosition;
    private Set<FootballPosition> secondaryPositions;
    private DominantFoot dominantFoot;
    private Integer jerseyNumber;
    private String city;
    private String state;
    private String bio;
    private Instant updatedAt;

    public AthleteProfile(UUID id, UUID userId, String displayName, String fullName, LocalDate birthDate,
            FootballPosition primaryPosition, Set<FootballPosition> secondaryPositions, DominantFoot dominantFoot,
            Integer jerseyNumber, String city, String state, String bio, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(displayName, fullName, birthDate, primaryPosition, secondaryPositions, dominantFoot, jerseyNumber, city, state, bio,
                Objects.requireNonNull(updatedAt, "updatedAt is required"));
    }

    public static AthleteProfile create(UUID userId, SaveFields fields, Instant now) {
        return new AthleteProfile(UUID.randomUUID(), userId, fields.displayName(), fields.fullName(), fields.birthDate(),
                fields.primaryPosition(), fields.secondaryPositions(), fields.dominantFoot(), fields.jerseyNumber(),
                fields.city(), fields.state(), fields.bio(), now, now);
    }

    public void update(SaveFields fields, Instant now) {
        apply(fields.displayName(), fields.fullName(), fields.birthDate(), fields.primaryPosition(), fields.secondaryPositions(),
                fields.dominantFoot(), fields.jerseyNumber(), fields.city(), fields.state(), fields.bio(), now);
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String displayName() { return displayName; }
    public String fullName() { return fullName; }
    public LocalDate birthDate() { return birthDate; }
    public FootballPosition primaryPosition() { return primaryPosition; }
    public Set<FootballPosition> secondaryPositions() { return Set.copyOf(secondaryPositions); }
    public DominantFoot dominantFoot() { return dominantFoot; }
    public Integer jerseyNumber() { return jerseyNumber; }
    public String city() { return city; }
    public String state() { return state; }
    public String bio() { return bio; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    private void apply(String displayName, String fullName, LocalDate birthDate, FootballPosition primaryPosition,
            Set<FootballPosition> secondaryPositions, DominantFoot dominantFoot, Integer jerseyNumber, String city,
            String state, String bio, Instant updatedAt) {
        this.displayName = requiredText(displayName, "displayName", 2, 60);
        this.fullName = optionalText(fullName, "fullName", 120);
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("birthDate cannot be in the future");
        }
        this.birthDate = birthDate;
        this.primaryPosition = Objects.requireNonNull(primaryPosition, "primaryPosition is required");
        this.secondaryPositions = validateSecondaryPositions(secondaryPositions, primaryPosition);
        this.dominantFoot = dominantFoot;
        if (jerseyNumber != null && (jerseyNumber < 1 || jerseyNumber > 99)) {
            throw new IllegalArgumentException("jerseyNumber must be between 1 and 99");
        }
        this.jerseyNumber = jerseyNumber;
        this.city = optionalText(city, "city", 80);
        this.state = optionalText(state, "state", 80);
        this.bio = optionalText(bio, "bio", 280);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static Set<FootballPosition> validateSecondaryPositions(Set<FootballPosition> positions, FootballPosition primary) {
        if (positions == null || positions.isEmpty()) {
            return Set.of();
        }
        EnumSet<FootballPosition> normalized = EnumSet.copyOf(positions);
        if (normalized.contains(primary)) {
            throw new IllegalArgumentException("secondaryPositions cannot include primaryPosition");
        }
        return normalized;
    }

    private static String requiredText(String value, String field, int min, int max) {
        String normalized = optionalText(value, field, max);
        if (normalized == null || normalized.length() < min) {
            throw new IllegalArgumentException(field + " must have at least " + min + " characters");
        }
        return normalized;
    }

    private static String optionalText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > max) {
            throw new IllegalArgumentException(field + " must have at most " + max + " characters");
        }
        return normalized;
    }

    public record SaveFields(String displayName, String fullName, LocalDate birthDate, FootballPosition primaryPosition,
            Set<FootballPosition> secondaryPositions, DominantFoot dominantFoot, Integer jerseyNumber, String city,
            String state, String bio) { }
}
