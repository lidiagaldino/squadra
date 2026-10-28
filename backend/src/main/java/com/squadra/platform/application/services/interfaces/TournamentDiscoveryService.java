package com.squadra.platform.application.services.interfaces;

import com.squadra.platform.domain.repositories.TournamentRepository;
import com.squadra.platform.domain.entities.TournamentCategory;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;

public interface TournamentDiscoveryService {
    Uni<List<TournamentRepository.TournamentSummary>> available(int page, int size);
    Uni<TournamentBrowseDetails> overview(UUID userId, UUID tournamentId);

    record TournamentBrowseDetails(TournamentRepository.TournamentDetails tournament, List<TournamentCategory> categories) { }
}
