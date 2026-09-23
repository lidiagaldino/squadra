package com.squadra.platform.domain.entities;

import com.squadra.platform.domain.enums.TournamentStatus;
import com.squadra.platform.domain.exceptions.TournamentConflictException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Tournament {
    private final UUID id;
    private final UUID ownerOrganizerUserId;
    private final Instant createdAt;
    private TournamentStatus status;
    private String name;
    private String description;
    private String city;
    private String state;
    private String venueName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registrationDeadline;
    private String rules;
    private Instant publishedAt;
    private Instant closedAt;
    private Instant cancelledAt;
    private Instant updatedAt;

    public Tournament(UUID id, UUID ownerOrganizerUserId, TournamentStatus status, SaveFields fields,
            Instant publishedAt, Instant closedAt, Instant cancelledAt, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.ownerOrganizerUserId = Objects.requireNonNull(ownerOrganizerUserId, "ownerOrganizerUserId is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.publishedAt = publishedAt;
        this.closedAt = closedAt;
        this.cancelledAt = cancelledAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(fields, Objects.requireNonNull(updatedAt, "updatedAt is required"));
    }

    public static Tournament create(UUID ownerOrganizerUserId, SaveFields fields, Instant now) {
        return new Tournament(UUID.randomUUID(), ownerOrganizerUserId, TournamentStatus.DRAFT, fields, null, null, null,
                now, now);
    }

    public void update(SaveFields fields, Instant now) {
        if (status == TournamentStatus.CANCELLED) {
            throw new TournamentConflictException("Cancelled tournaments cannot be edited");
        }
        apply(fields, now);
    }

    public void publish(Instant now) {
        requireStatus(TournamentStatus.DRAFT, "Only draft tournaments can be published");
        status = TournamentStatus.OPEN;
        publishedAt = Objects.requireNonNull(now, "now is required");
        updatedAt = now;
    }

    public void close(Instant now) {
        requireStatus(TournamentStatus.OPEN, "Only open tournaments can be closed");
        status = TournamentStatus.CLOSED;
        closedAt = Objects.requireNonNull(now, "now is required");
        updatedAt = now;
    }

    public void reopen(Instant now) {
        requireStatus(TournamentStatus.CLOSED, "Only closed tournaments can be reopened");
        status = TournamentStatus.OPEN;
        updatedAt = Objects.requireNonNull(now, "now is required");
    }

    public void cancel(Instant now) {
        if (status == TournamentStatus.CANCELLED) {
            throw new TournamentConflictException("Tournament is already cancelled");
        }
        status = TournamentStatus.CANCELLED;
        cancelledAt = Objects.requireNonNull(now, "now is required");
        updatedAt = now;
    }

    public UUID id() { return id; }
    public UUID ownerOrganizerUserId() { return ownerOrganizerUserId; }
    public TournamentStatus status() { return status; }
    public String name() { return name; }
    public String description() { return description; }
    public String city() { return city; }
    public String state() { return state; }
    public String venueName() { return venueName; }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }
    public LocalDate registrationDeadline() { return registrationDeadline; }
    public String rules() { return rules; }
    public Instant publishedAt() { return publishedAt; }
    public Instant closedAt() { return closedAt; }
    public Instant cancelledAt() { return cancelledAt; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    private void apply(SaveFields fields, Instant updatedAt) {
        Objects.requireNonNull(fields, "fields are required");
        LocalDate normalizedStartDate = Objects.requireNonNull(fields.startDate(), "startDate is required");
        LocalDate normalizedEndDate = Objects.requireNonNull(fields.endDate(), "endDate is required");
        LocalDate normalizedRegistrationDeadline = Objects.requireNonNull(fields.registrationDeadline(),
                "registrationDeadline is required");
        if (normalizedEndDate.isBefore(normalizedStartDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
        if (normalizedRegistrationDeadline.isAfter(normalizedStartDate)) {
            throw new IllegalArgumentException("registrationDeadline must be on or before startDate");
        }

        this.name = requiredText(fields.name(), "name", 3, 120);
        this.description = optionalText(fields.description(), "description", 1000);
        this.city = requiredText(fields.city(), "city", 2, 80);
        this.state = requiredText(fields.state(), "state", 2, 80);
        this.venueName = requiredText(fields.venueName(), "venueName", 2, 120);
        this.startDate = normalizedStartDate;
        this.endDate = normalizedEndDate;
        this.registrationDeadline = normalizedRegistrationDeadline;
        this.rules = optionalText(fields.rules(), "rules", 5000);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private void requireStatus(TournamentStatus expected, String message) {
        if (status != expected) {
            throw new TournamentConflictException(message);
        }
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

    public record SaveFields(String name, String description, String city, String state, String venueName,
            LocalDate startDate, LocalDate endDate, LocalDate registrationDeadline, String rules) { }
}
