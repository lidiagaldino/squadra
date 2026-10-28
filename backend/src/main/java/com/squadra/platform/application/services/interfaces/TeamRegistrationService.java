package com.squadra.platform.application.services.interfaces;

import com.squadra.platform.domain.repositories.TeamRegistrationRepository;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;

public interface TeamRegistrationService {
    Uni<TeamRegistrationRepository.RegistrationDetails> register(UUID userId, UUID tournamentId, UUID categoryId,
            RegisterTeamCommand command);
    Uni<List<TeamRegistrationRepository.RegistrationDetails>> listCategory(UUID userId, UUID tournamentId, UUID categoryId);
    Uni<List<TeamRegistrationRepository.RegistrationDetails>> listTeam(UUID userId, UUID teamId);
    Uni<TeamRegistrationRepository.RegistrationDetails> cancelTeamRegistration(UUID userId, UUID teamId, UUID registrationId);

    record RegisterTeamCommand(UUID teamId) { }
}
