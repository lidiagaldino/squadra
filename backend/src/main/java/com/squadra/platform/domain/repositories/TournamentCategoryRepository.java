package com.squadra.platform.domain.repositories;

import com.squadra.platform.domain.entities.TournamentCategory;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentCategoryRepository {
    Uni<TournamentCategory> create(TournamentCategory category);
    Uni<List<TournamentCategory>> listByTournament(UUID tournamentId);
    Uni<Optional<TournamentCategory>> findByTournament(UUID tournamentId, UUID categoryId);
    Uni<Boolean> nameExists(UUID tournamentId, String normalizedName, UUID excludingCategoryId);
    Uni<TournamentCategory> save(TournamentCategory category);
    Uni<Boolean> delete(UUID tournamentId, UUID categoryId);
}
