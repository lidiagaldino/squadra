package com.squadra.platform.domain.entities;

import com.squadra.platform.domain.enums.TeamAthleteMembershipStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class TeamAthleteMembership {
    private final UUID teamId;
    private final UUID athleteUserId;
    private TeamAthleteMembershipStatus status;
    private final Instant invitedAt;
    private Instant updatedAt;

    public TeamAthleteMembership(UUID teamId, UUID athleteUserId, TeamAthleteMembershipStatus status,
            Instant invitedAt, Instant updatedAt) {
        this.teamId = Objects.requireNonNull(teamId, "teamId is required");
        this.athleteUserId = Objects.requireNonNull(athleteUserId, "athleteUserId is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.invitedAt = Objects.requireNonNull(invitedAt, "invitedAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static TeamAthleteMembership invited(UUID teamId, UUID athleteUserId, Instant now) {
        return new TeamAthleteMembership(teamId, athleteUserId, TeamAthleteMembershipStatus.INVITED, now, now);
    }

    public static TeamAthleteMembership active(UUID teamId, UUID athleteUserId, Instant now) {
        return new TeamAthleteMembership(teamId, athleteUserId, TeamAthleteMembershipStatus.ACTIVE, now, now);
    }

    public UUID teamId() { return teamId; }
    public UUID athleteUserId() { return athleteUserId; }
    public TeamAthleteMembershipStatus status() { return status; }
    public Instant invitedAt() { return invitedAt; }
    public Instant updatedAt() { return updatedAt; }

    public void activate(Instant now) {
        if (status == TeamAthleteMembershipStatus.ACTIVE) {
            throw new IllegalStateException("Athlete membership is already active");
        }
        status = TeamAthleteMembershipStatus.ACTIVE;
        updatedAt = Objects.requireNonNull(now, "updatedAt is required");
    }
}
