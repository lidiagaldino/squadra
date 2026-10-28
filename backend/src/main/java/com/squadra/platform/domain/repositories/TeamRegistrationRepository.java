package com.squadra.platform.domain.repositories;

import com.squadra.platform.domain.entities.TeamRegistration;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRegistrationRepository {
    Uni<RegistrationDetails> create(TeamRegistration registration);
    Uni<RegistrationDetails> createWithinCapacity(TeamRegistration registration, int maxTeams);
    Uni<List<RegistrationDetails>> listByCategory(UUID tournamentId, UUID categoryId);
    Uni<List<RegistrationDetails>> listByTeam(UUID teamId);
    Uni<Optional<RegistrationDetails>> findByTeam(UUID teamId, UUID registrationId);
    Uni<Boolean> activeExists(UUID categoryId, UUID teamId);
    Uni<Integer> activeCountByCategory(UUID categoryId);
    Uni<RegistrationDetails> save(TeamRegistration registration);

    record RegistrationDetails(TeamRegistration registration, String teamName, String tournamentName,
            String categoryName) { }
}
