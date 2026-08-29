package com.squadra.platform.domain.entities;

import com.squadra.platform.domain.enums.TeamAdminRole;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class TeamAdministrator {
    private final UUID teamId;
    private final UUID userId;
    private TeamAdminRole role;
    private final Instant createdAt;
    private Instant updatedAt;

    public TeamAdministrator(UUID teamId, UUID userId, TeamAdminRole role, Instant createdAt, Instant updatedAt) {
        this.teamId = Objects.requireNonNull(teamId, "teamId is required");
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        changeRole(role, Objects.requireNonNull(updatedAt, "updatedAt is required"));
    }

    public static TeamAdministrator create(UUID teamId, UUID userId, TeamAdminRole role, Instant now) {
        return new TeamAdministrator(teamId, userId, role, now, now);
    }

    public UUID teamId() { return teamId; }
    public UUID userId() { return userId; }
    public TeamAdminRole role() { return role; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public void changeRole(TeamAdminRole replacement, Instant now) {
        this.role = Objects.requireNonNull(replacement, "role is required");
        this.updatedAt = Objects.requireNonNull(now, "updatedAt is required");
    }
}
