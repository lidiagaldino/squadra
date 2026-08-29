package com.squadra.platform.application.services.interfaces;

import com.squadra.platform.domain.enums.TeamAdminRole;
import com.squadra.platform.domain.repositories.TeamRepository;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;

public interface TeamService {
    Uni<TeamRepository.TeamDetails> create(UUID userId, SaveTeamCommand command);
    Uni<List<TeamRepository.TeamSummary>> listMine(UUID userId);
    Uni<TeamRepository.TeamDetails> getMine(UUID userId, UUID teamId);
    Uni<TeamRepository.TeamDetails> update(UUID userId, UUID teamId, SaveTeamCommand command);
    Uni<TeamRepository.TeamDetails> joinAsAthlete(UUID userId, UUID teamId);
    Uni<TeamRepository.TeamDetails> assignAdmin(UUID userId, UUID teamId, AssignAdminCommand command);
    Uni<TeamRepository.TeamDetails> removeAdmin(UUID userId, UUID teamId, UUID targetUserId);
    Uni<TeamRepository.TeamDetails> transferOwnership(UUID userId, UUID teamId, UUID targetUserId);
    Uni<TeamRepository.TeamDetails> inviteAthlete(UUID userId, UUID teamId, InviteAthleteCommand command);
    Uni<TeamRepository.TeamDetails> acceptInvitation(UUID userId, UUID teamId);
    Uni<Void> rejectInvitation(UUID userId, UUID teamId);
    Uni<TeamRepository.TeamDetails> cancelInvitation(UUID userId, UUID teamId, UUID athleteUserId);
    Uni<TeamRepository.TeamDetails> removeActiveAthlete(UUID userId, UUID teamId, UUID athleteUserId);

    record SaveTeamCommand(String name, String city, String state, Integer foundedYear, String description) { }
    record AssignAdminCommand(String email, TeamAdminRole role) { }
    record InviteAthleteCommand(String email) { }
}
