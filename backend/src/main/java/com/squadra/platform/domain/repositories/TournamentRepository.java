package com.squadra.platform.domain.repositories;

import com.squadra.platform.domain.entities.Tournament;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentRepository {
    Uni<TournamentDetails> create(Tournament tournament);
    Uni<List<TournamentSummary>> listForOwner(UUID ownerOrganizerUserId);
    Uni<Optional<TournamentDetails>> findOwnedByOrganizer(UUID tournamentId, UUID ownerOrganizerUserId);
    Uni<TournamentDetails> save(Tournament tournament);

    record TournamentSummary(Tournament tournament, int categoryCount) { }
    record TournamentDetails(Tournament tournament, int categoryCount) { }
}
