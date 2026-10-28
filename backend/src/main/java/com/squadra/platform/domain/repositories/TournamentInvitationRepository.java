package com.squadra.platform.domain.repositories;

import com.squadra.platform.domain.entities.TournamentInvitation;
import com.squadra.platform.domain.entities.TeamRegistration;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentInvitationRepository {
    Uni<InvitationDetails> create(TournamentInvitation invitation);
    Uni<InvitationDetails> acceptWithRegistration(TournamentInvitation invitation, TeamRegistration registration, int maxTeams);
    Uni<List<InvitationDetails>> listByCategory(UUID tournamentId, UUID categoryId);
    Uni<List<InvitationDetails>> listByTeam(UUID teamId);
    Uni<Optional<InvitationDetails>> findById(UUID invitationId);
    Uni<InvitationDetails> save(TournamentInvitation invitation);
    Uni<Boolean> hasTeamAdminAccess(UUID tournamentId, UUID userId);

    record InvitationDetails(TournamentInvitation invitation, String teamName, String tournamentName, String categoryName) { }
}
