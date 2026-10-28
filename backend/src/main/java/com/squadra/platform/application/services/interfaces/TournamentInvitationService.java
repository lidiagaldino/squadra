package com.squadra.platform.application.services.interfaces;

import com.squadra.platform.domain.repositories.TeamRepository;
import com.squadra.platform.domain.repositories.TournamentInvitationRepository;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;

public interface TournamentInvitationService {
    Uni<List<TeamRepository.TeamDirectorySummary>> searchTeams(int page, int size, String query);
    Uni<TournamentInvitationRepository.InvitationDetails> invite(UUID organizerId, UUID tournamentId, UUID categoryId, UUID teamId);
    Uni<List<TournamentInvitationRepository.InvitationDetails>> listCategory(UUID organizerId, UUID tournamentId, UUID categoryId);
    Uni<TournamentInvitationRepository.InvitationDetails> cancel(UUID organizerId, UUID tournamentId, UUID categoryId, UUID invitationId);
    Uni<List<TournamentInvitationRepository.InvitationDetails>> listTeam(UUID userId, UUID teamId);
    Uni<TournamentInvitationRepository.InvitationDetails> accept(UUID userId, UUID teamId, UUID invitationId);
    Uni<TournamentInvitationRepository.InvitationDetails> reject(UUID userId, UUID teamId, UUID invitationId);
}
