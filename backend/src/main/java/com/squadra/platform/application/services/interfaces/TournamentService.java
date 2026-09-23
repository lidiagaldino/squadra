package com.squadra.platform.application.services.interfaces;

import com.squadra.platform.domain.repositories.TournamentRepository;
import io.smallrye.mutiny.Uni;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TournamentService {
    Uni<TournamentRepository.TournamentDetails> create(UUID userId, SaveTournamentCommand command);
    Uni<List<TournamentRepository.TournamentSummary>> listMine(UUID userId);
    Uni<TournamentRepository.TournamentDetails> getMine(UUID userId, UUID tournamentId);
    Uni<TournamentRepository.TournamentDetails> update(UUID userId, UUID tournamentId, SaveTournamentCommand command);
    Uni<TournamentRepository.TournamentDetails> publish(UUID userId, UUID tournamentId);
    Uni<TournamentRepository.TournamentDetails> close(UUID userId, UUID tournamentId);
    Uni<TournamentRepository.TournamentDetails> reopen(UUID userId, UUID tournamentId);
    Uni<TournamentRepository.TournamentDetails> cancel(UUID userId, UUID tournamentId);

    record SaveTournamentCommand(String name, String description, String city, String state, String venueName,
            LocalDate startDate, LocalDate endDate, LocalDate registrationDeadline, String rules) { }
}
