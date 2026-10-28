package com.squadra.platform.application.services.impl;

import com.squadra.platform.application.services.interfaces.TournamentCategoryService;
import com.squadra.platform.domain.entities.TournamentCategory;
import com.squadra.platform.domain.enums.TournamentStatus;
import com.squadra.platform.domain.exceptions.TournamentCategoryConflictException;
import com.squadra.platform.domain.exceptions.TournamentCategoryNotFoundException;
import com.squadra.platform.domain.exceptions.TournamentConflictException;
import com.squadra.platform.domain.exceptions.TournamentNotFoundException;
import com.squadra.platform.domain.repositories.TournamentCategoryRepository;
import com.squadra.platform.domain.repositories.TournamentRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public final class DefaultTournamentCategoryService implements TournamentCategoryService {
    private static final Logger LOG = Logger.getLogger(DefaultTournamentCategoryService.class);

    private final TournamentRepository tournaments;
    private final TournamentCategoryRepository categories;
    private final Clock clock;

    @Inject
    public DefaultTournamentCategoryService(TournamentRepository tournaments, TournamentCategoryRepository categories) {
        this(tournaments, categories, Clock.systemUTC());
    }

    DefaultTournamentCategoryService(TournamentRepository tournaments, TournamentCategoryRepository categories, Clock clock) {
        this.tournaments = Objects.requireNonNull(tournaments, "tournaments is required");
        this.categories = Objects.requireNonNull(categories, "categories is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public Uni<TournamentCategory> create(UUID userId, UUID tournamentId, SaveCategoryCommand command) {
        UUID ownerId = Objects.requireNonNull(userId, "userId is required");
        return mutableTournament(ownerId, tournamentId).chain(details -> {
            TournamentCategory category = TournamentCategory.create(details.tournament().id(), fields(command), clock.instant());
            return ensureUniqueName(category.tournamentId(), category.normalizedName(), null)
                    .chain(() -> categories.create(category))
                    .invoke(created -> log("create", ownerId, created.tournamentId(), created.id()));
        });
    }

    @Override
    public Uni<List<TournamentCategory>> list(UUID userId, UUID tournamentId) {
        return ownedTournament(userId, tournamentId).chain(details -> categories.listByTournament(details.tournament().id()));
    }

    @Override
    public Uni<TournamentCategory> get(UUID userId, UUID tournamentId, UUID categoryId) {
        return ownedTournament(userId, tournamentId).chain(details -> category(details.tournament().id(), categoryId));
    }

    @Override
    public Uni<TournamentCategory> update(UUID userId, UUID tournamentId, UUID categoryId, SaveCategoryCommand command) {
        UUID ownerId = Objects.requireNonNull(userId, "userId is required");
        return mutableTournament(ownerId, tournamentId).chain(details -> category(details.tournament().id(), categoryId)
                .chain(existing -> {
                    existing.update(fields(command), clock.instant());
                    return ensureUniqueName(existing.tournamentId(), existing.normalizedName(), existing.id())
                            .chain(() -> categories.save(existing))
                            .invoke(saved -> log("edit", ownerId, saved.tournamentId(), saved.id()));
                }));
    }

    @Override
    public Uni<Void> delete(UUID userId, UUID tournamentId, UUID categoryId) {
        UUID ownerId = Objects.requireNonNull(userId, "userId is required");
        return mutableTournament(ownerId, tournamentId).chain(details -> categories.delete(details.tournament().id(),
                Objects.requireNonNull(categoryId, "categoryId is required")))
                .chain(deleted -> {
                    if (!deleted) return Uni.createFrom().failure(new TournamentCategoryNotFoundException());
                    log("delete", ownerId, tournamentId, categoryId);
                    return Uni.createFrom().voidItem();
                });
    }

    private Uni<TournamentRepository.TournamentDetails> mutableTournament(UUID userId, UUID tournamentId) {
        return ownedTournament(userId, tournamentId).invoke(details -> {
            TournamentStatus status = details.tournament().status();
            if (status == TournamentStatus.CLOSED || status == TournamentStatus.CANCELLED) {
                throw new TournamentConflictException("Tournament categories cannot be changed in this status");
            }
        });
    }

    private Uni<TournamentRepository.TournamentDetails> ownedTournament(UUID userId, UUID tournamentId) {
        return tournaments.findOwnedByOrganizer(Objects.requireNonNull(tournamentId, "tournamentId is required"),
                Objects.requireNonNull(userId, "userId is required"))
                .chain(details -> details.map(Uni.createFrom()::item)
                        .orElseGet(() -> Uni.createFrom().failure(new TournamentNotFoundException())));
    }

    private Uni<TournamentCategory> category(UUID tournamentId, UUID categoryId) {
        return categories.findByTournament(tournamentId, Objects.requireNonNull(categoryId, "categoryId is required"))
                .chain(found -> found.map(Uni.createFrom()::item)
                        .orElseGet(() -> Uni.createFrom().failure(new TournamentCategoryNotFoundException())));
    }

    private Uni<Void> ensureUniqueName(UUID tournamentId, String normalizedName, UUID excludingCategoryId) {
        return categories.nameExists(tournamentId, normalizedName, excludingCategoryId).chain(exists -> {
            if (exists) return Uni.createFrom().failure(new TournamentCategoryConflictException("Category name already exists"));
            return Uni.createFrom().voidItem();
        });
    }

    private static TournamentCategory.SaveFields fields(SaveCategoryCommand command) {
        Objects.requireNonNull(command, "command is required");
        return new TournamentCategory.SaveFields(command.name(), command.modality(), command.minAthletes(),
                command.maxAthletes(), command.minTeams(), command.maxTeams(), command.paymentModel(),
                command.priceInCents(), command.competitionFormat(), command.swissRounds(),
                command.matchDurationMinutes(), command.intervalBetweenMatchesMinutes(), command.minimumRestMinutes(),
                command.schedulingStrategy());
    }

    private static void log(String action, UUID ownerId, UUID tournamentId, UUID categoryId) {
        LOG.infof("tournament.category.%s owner=%s tournament=%s category=%s", action, ownerId, tournamentId, categoryId);
    }
}
