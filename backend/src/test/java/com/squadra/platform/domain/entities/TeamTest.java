package com.squadra.platform.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.squadra.platform.domain.enums.TeamAdminRole;
import com.squadra.platform.domain.enums.TeamAthleteMembershipStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamTest {
    private static final Instant NOW = Instant.parse("2026-08-29T12:00:00Z");

    @Test
    void createsValidTeamAndNormalizesOptionalFields() {
        Team team = Team.create(UUID.randomUUID(), new Team.SaveFields(" Liga Miga ", "", null, 2020, " "), NOW);

        assertEquals("Liga Miga", team.name());
        assertEquals(null, team.city());
        assertEquals(null, team.state());
        assertEquals(null, team.description());
        assertEquals(2020, team.foundedYear());
    }

    @Test
    void rejectsInvalidTeamFields() {
        assertThrows(IllegalArgumentException.class, () -> Team.create(UUID.randomUUID(), new Team.SaveFields("L", null, null, null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Team.create(UUID.randomUUID(), new Team.SaveFields("T".repeat(101), null, null, null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Team.create(UUID.randomUUID(), new Team.SaveFields("Liga", "C".repeat(81), null, null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Team.create(UUID.randomUUID(), new Team.SaveFields("Liga", null, "S".repeat(81), null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Team.create(UUID.randomUUID(), new Team.SaveFields("Liga", null, null, 1849, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Team.create(UUID.randomUUID(), new Team.SaveFields("Liga", null, null, 3000, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Team.create(UUID.randomUUID(), new Team.SaveFields("Liga", null, null, null, "D".repeat(501)), NOW));
    }

    @Test
    void administrationRolesRejectMembershipAndTournamentRoles() {
        assertEquals(TeamAdminRole.TEAM_OWNER, TeamAdminRole.permanentValueOf("TEAM_OWNER"));
        assertEquals(TeamAdminRole.TEAM_ADMIN, TeamAdminRole.permanentValueOf("TEAM_ADMIN"));
        assertThrows(IllegalArgumentException.class, () -> TeamAdminRole.permanentValueOf("MEMBER"));
        assertThrows(IllegalArgumentException.class, () -> TeamAdminRole.permanentValueOf("INVITED"));
        assertThrows(IllegalArgumentException.class, () -> TeamAdminRole.permanentValueOf("CAPTAIN"));
        assertThrows(IllegalArgumentException.class, () -> TeamAdminRole.permanentValueOf("TEAM_MANAGER"));
    }

    @Test
    void athleteMembershipMovesFromInvitedToActive() {
        TeamAthleteMembership membership = TeamAthleteMembership.invited(UUID.randomUUID(), UUID.randomUUID(), NOW);

        membership.activate(NOW.plusSeconds(5));

        assertEquals(TeamAthleteMembershipStatus.ACTIVE, membership.status());
        assertEquals(NOW.plusSeconds(5), membership.updatedAt());
        assertThrows(IllegalStateException.class, () -> membership.activate(NOW.plusSeconds(10)));
    }
}
