package com.squadra.platform.application.services.impl;

import com.squadra.platform.application.services.interfaces.TournamentService;
import com.squadra.platform.domain.entities.Tournament;
import com.squadra.platform.domain.exceptions.TournamentNotFoundException;
import com.squadra.platform.domain.repositories.TournamentRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public final class DefaultTournamentService implements TournamentService {
    private static final Logger LOG = Logger.getLogger(DefaultTournamentService.class);

    private final TournamentRepository tournaments;
    private final Clock clock;

    @Inject
    public DefaultTournamentService(TournamentRepository tournaments) {
        this(tournaments, Clock.systemUTC());
    }

    DefaultTournamentService(TournamentRepository tournaments, Clock clock) {
        this.tournaments = Objects.requireNonNull(tournaments, "tournaments is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public Uni<TournamentRepository.TournamentDetails> create(UUID userId, SaveTournamentCommand command) {
        UUID ownerId = Objects.requireNonNull(userId, "userId is required");
        Tournament tournament = Tournament.create(ownerId, fields(command), clock.instant());
        return tournaments.create(tournament).invoke(details -> log("create", ownerId, details.tournament().id()));
    }

    @Override
    public Uni<List<TournamentRepository.TournamentSummary>> listMine(UUID userId) {
        return tournaments.listForOwner(Objects.requireNonNull(userId, "userId is required"));
    }

    @Override
    public Uni<TournamentRepository.TournamentDetails> getMine(UUID userId, UUID tournamentId) {
        return owned(tournamentId, userId);
    }

    @Override
    public Uni<TournamentRepository.TournamentDetails> update(UUID userId, UUID tournamentId, SaveTournamentCommand command) {
        return owned(tournamentId, userId).chain(details -> {
            details.tournament().update(fields(command), clock.instant());
            return tournaments.save(details.tournament()).invoke(saved -> log("edit", userId, saved.tournament().id()));
        });
    }

    @Override
    public Uni<TournamentRepository.TournamentDetails> publish(UUID userId, UUID tournamentId) {
        return transition(userId, tournamentId, "publish", tournament -> tournament.publish(clock.instant()));
    }

    @Override
    public Uni<TournamentRepository.TournamentDetails> close(UUID userId, UUID tournamentId) {
        return transition(userId, tournamentId, "close", tournament -> tournament.close(clock.instant()));
    }

    @Override
    public Uni<TournamentRepository.TournamentDetails> reopen(UUID userId, UUID tournamentId) {
        return transition(userId, tournamentId, "reopen", tournament -> tournament.reopen(clock.instant()));
    }

    @Override
    public Uni<TournamentRepository.TournamentDetails> cancel(UUID userId, UUID tournamentId) {
        return transition(userId, tournamentId, "cancel", tournament -> tournament.cancel(clock.instant()));
    }

    private Uni<TournamentRepository.TournamentDetails> transition(UUID userId, UUID tournamentId, String action,
            java.util.function.Consumer<Tournament> transition) {
        return owned(tournamentId, userId).chain(details -> {
            transition.accept(details.tournament());
            return tournaments.save(details.tournament()).invoke(saved -> log(action, userId, saved.tournament().id()));
        });
    }

    private Uni<TournamentRepository.TournamentDetails> owned(UUID tournamentId, UUID userId) {
        return tournaments.findOwnedByOrganizer(Objects.requireNonNull(tournamentId, "tournamentId is required"),
                Objects.requireNonNull(userId, "userId is required"))
                .chain(details -> details.map(Uni.createFrom()::item).orElseGet(() -> Uni.createFrom().failure(new TournamentNotFoundException())));
    }

    private static Tournament.SaveFields fields(SaveTournamentCommand command) {
        Objects.requireNonNull(command, "command is required");
        return new Tournament.SaveFields(command.name(), command.description(), command.city(), command.state(),
                command.venueName(), command.startDate(), command.endDate(), command.registrationDeadline(), command.rules(),
                command.visibility());
    }

    private static void log(String action, UUID ownerId, UUID tournamentId) {
        LOG.infof("tournament.%s owner=%s tournament=%s", action, ownerId, tournamentId);
    }
}
