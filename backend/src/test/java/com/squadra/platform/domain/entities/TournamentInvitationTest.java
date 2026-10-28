package com.squadra.platform.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.squadra.platform.domain.enums.TournamentInvitationStatus;
import com.squadra.platform.domain.exceptions.TournamentInvitationConflictException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TournamentInvitationTest {
    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");

    @Test
    void createsPendingInvitationWithRequiredContextAndTimestamps() {
        UUID tournamentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID inviterId = UUID.randomUUID();

        TournamentInvitation invitation = TournamentInvitation.create(tournamentId, categoryId, teamId, inviterId, NOW);

        assertEquals(tournamentId, invitation.tournamentId());
        assertEquals(categoryId, invitation.categoryId());
        assertEquals(teamId, invitation.teamId());
        assertEquals(inviterId, invitation.inviterUserId());
        assertEquals(TournamentInvitationStatus.PENDING, invitation.status());
        assertEquals(NOW, invitation.createdAt());
        assertEquals(NOW, invitation.updatedAt());
        assertNull(invitation.decidedAt());
    }

    @Test
    void acceptRejectAndCancelRecordTerminalDecisionAndTimestamp() {
        TournamentInvitation accepted = invitation();
        accepted.accept(NOW.plusSeconds(5));
        assertEquals(TournamentInvitationStatus.ACCEPTED, accepted.status());
        assertEquals(NOW.plusSeconds(5), accepted.decidedAt());
        assertEquals(NOW.plusSeconds(5), accepted.updatedAt());

        TournamentInvitation rejected = invitation();
        rejected.reject(NOW.plusSeconds(6));
        assertEquals(TournamentInvitationStatus.REJECTED, rejected.status());
        assertEquals(NOW.plusSeconds(6), rejected.decidedAt());

        TournamentInvitation cancelled = invitation();
        cancelled.cancel(NOW.plusSeconds(7));
        assertEquals(TournamentInvitationStatus.CANCELLED, cancelled.status());
        assertEquals(NOW.plusSeconds(7), cancelled.decidedAt());
    }

    @Test
    void rejectsDecisionRetries() {
        TournamentInvitation invitation = invitation();
        invitation.reject(NOW.plusSeconds(1));

        assertThrows(TournamentInvitationConflictException.class, () -> invitation.accept(NOW.plusSeconds(2)));
        assertThrows(TournamentInvitationConflictException.class, () -> invitation.reject(NOW.plusSeconds(2)));
        assertThrows(TournamentInvitationConflictException.class, () -> invitation.cancel(NOW.plusSeconds(2)));
        assertEquals(TournamentInvitationStatus.REJECTED, invitation.status());
        assertEquals(NOW.plusSeconds(1), invitation.updatedAt());
    }

    private static TournamentInvitation invitation() {
        return TournamentInvitation.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), NOW);
    }
}
