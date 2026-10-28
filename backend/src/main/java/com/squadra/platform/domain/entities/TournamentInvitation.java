package com.squadra.platform.domain.entities;

import com.squadra.platform.domain.enums.TournamentInvitationStatus;
import com.squadra.platform.domain.exceptions.TournamentInvitationConflictException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class TournamentInvitation {
    private final UUID id;
    private final UUID tournamentId;
    private final UUID categoryId;
    private final UUID teamId;
    private final UUID inviterUserId;
    private final Instant createdAt;
    private TournamentInvitationStatus status;
    private Instant updatedAt;
    private Instant decidedAt;

    public TournamentInvitation(UUID id, UUID tournamentId, UUID categoryId, UUID teamId, UUID inviterUserId,
            TournamentInvitationStatus status, Instant createdAt, Instant updatedAt, Instant decidedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.tournamentId = Objects.requireNonNull(tournamentId, "tournamentId is required");
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId is required");
        this.teamId = Objects.requireNonNull(teamId, "teamId is required");
        this.inviterUserId = Objects.requireNonNull(inviterUserId, "inviterUserId is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.decidedAt = decidedAt;
    }

    public static TournamentInvitation create(UUID tournamentId, UUID categoryId, UUID teamId, UUID inviterUserId, Instant now) {
        return new TournamentInvitation(UUID.randomUUID(), tournamentId, categoryId, teamId, inviterUserId,
                TournamentInvitationStatus.PENDING, now, now, null);
    }

    public void accept(Instant now) { decide(TournamentInvitationStatus.ACCEPTED, now); }
    public void reject(Instant now) { decide(TournamentInvitationStatus.REJECTED, now); }
    public void cancel(Instant now) { decide(TournamentInvitationStatus.CANCELLED, now); }

    private void decide(TournamentInvitationStatus decision, Instant now) {
        if (status != TournamentInvitationStatus.PENDING) {
            throw new TournamentInvitationConflictException("Only pending invitations can be decided");
        }
        status = decision;
        updatedAt = Objects.requireNonNull(now, "now is required");
        decidedAt = now;
    }

    public UUID id() { return id; }
    public UUID tournamentId() { return tournamentId; }
    public UUID categoryId() { return categoryId; }
    public UUID teamId() { return teamId; }
    public UUID inviterUserId() { return inviterUserId; }
    public TournamentInvitationStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant decidedAt() { return decidedAt; }
}
