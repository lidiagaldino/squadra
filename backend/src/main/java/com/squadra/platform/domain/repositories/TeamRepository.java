package com.squadra.platform.domain.repositories;

import com.squadra.platform.domain.entities.Team;
import com.squadra.platform.domain.entities.TeamAdministrator;
import com.squadra.platform.domain.entities.TeamAthleteMembership;
import com.squadra.platform.domain.enums.TeamAdminRole;
import com.squadra.platform.domain.enums.TeamAthleteMembershipStatus;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository {
    Uni<TeamDetails> createWithOwner(Team team, TeamAdministrator owner);
    Uni<List<TeamSummary>> listForUser(UUID userId);
    Uni<Optional<TeamDetails>> findVisibleToUser(UUID teamId, UUID userId);
    Uni<TeamDetails> saveTeam(Team team, UUID viewerUserId);
    Uni<TeamDetails> upsertAdmin(UUID teamId, UUID userId, TeamAdminRole role, UUID viewerUserId);
    Uni<TeamDetails> removeAdmin(UUID teamId, UUID userId, UUID viewerUserId);
    Uni<TeamDetails> transferOwnership(UUID teamId, UUID fromUserId, UUID toUserId);
    Uni<TeamDetails> upsertAthleteMembership(UUID teamId, UUID athleteUserId, TeamAthleteMembershipStatus status, UUID viewerUserId);
    Uni<TeamDetails> activateInvitation(UUID teamId, UUID athleteUserId);
    Uni<TeamDetails> removeAthleteMembership(UUID teamId, UUID athleteUserId, UUID viewerUserId);
    Uni<Void> deleteAthleteMembership(UUID teamId, UUID athleteUserId);

    record TeamSummary(Team team, TeamAdminRole administrationRole,
            TeamAthleteMembershipStatus athleteMembershipStatus) { }

    record TeamDetails(Team team, TeamAdminRole administrationRole,
            TeamAthleteMembershipStatus athleteMembershipStatus, List<AdminView> administrators,
            List<AthleteView> athletes) { }

    record AdminView(UUID userId, String email, TeamAdminRole role) { }
    record AthleteView(UUID userId, String email, TeamAthleteMembershipStatus status) { }
}
