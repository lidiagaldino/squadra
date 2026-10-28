package com.squadra.platform.domain.entities;

import com.squadra.platform.domain.enums.RegistrationStatus;
import com.squadra.platform.domain.exceptions.TeamRegistrationConflictException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class TeamRegistration {
    private final UUID id;
    private final UUID teamId;
    private final UUID tournamentId;
    private final UUID categoryId;
    private final Instant createdAt;
    private RegistrationStatus status;
    private Instant updatedAt;

    public TeamRegistration(UUID id, UUID teamId, UUID tournamentId, UUID categoryId, RegistrationStatus status,
            Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.teamId = Objects.requireNonNull(teamId, "teamId is required");
        this.tournamentId = Objects.requireNonNull(tournamentId, "tournamentId is required");
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static TeamRegistration create(UUID teamId, UUID tournamentId, UUID categoryId, Instant now) {
        return new TeamRegistration(UUID.randomUUID(), teamId, tournamentId, categoryId, RegistrationStatus.DRAFT,
                Objects.requireNonNull(now, "now is required"), now);
    }

    public void cancel(Instant now) {
        if (status != RegistrationStatus.DRAFT) {
            throw new TeamRegistrationConflictException("Only draft registrations can be cancelled");
        }
        status = RegistrationStatus.CANCELLED;
        updatedAt = Objects.requireNonNull(now, "now is required");
    }

    public UUID id() { return id; }
    public UUID teamId() { return teamId; }
    public UUID tournamentId() { return tournamentId; }
    public UUID categoryId() { return categoryId; }
    public RegistrationStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
