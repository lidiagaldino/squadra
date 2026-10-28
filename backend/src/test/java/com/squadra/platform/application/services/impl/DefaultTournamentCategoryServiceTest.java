package com.squadra.platform.application.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.squadra.platform.application.services.interfaces.TournamentCategoryService;
import com.squadra.platform.domain.entities.Tournament;
import com.squadra.platform.domain.entities.TournamentCategory;
import com.squadra.platform.domain.enums.CategoryPaymentModel;
import com.squadra.platform.domain.enums.CompetitionFormat;
import com.squadra.platform.domain.enums.SchedulingStrategy;
import com.squadra.platform.domain.enums.TournamentModality;
import com.squadra.platform.domain.exceptions.TournamentCategoryConflictException;
import com.squadra.platform.domain.exceptions.TournamentCategoryNotFoundException;
import com.squadra.platform.domain.exceptions.TournamentConflictException;
import com.squadra.platform.domain.exceptions.TournamentNotFoundException;
import com.squadra.platform.domain.repositories.TournamentCategoryRepository;
import com.squadra.platform.domain.repositories.TournamentRepository;
import io.smallrye.mutiny.Uni;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultTournamentCategoryServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-29T12:00:00Z");

    @Test
    void createsListsGetsUpdatesAndDeletesOwnedTournamentCategories() {
        UUID ownerId = UUID.randomUUID();
        var tournaments = new InMemoryTournamentRepository();
        var categories = new InMemoryTournamentCategoryRepository();
        Tournament tournament = tournaments.add(ownerId);
        var service = service(tournaments, categories);

        TournamentCategory created = service.create(ownerId, tournament.id(), command("Iniciante")).await().indefinitely();
        TournamentCategory updated = service.update(ownerId, tournament.id(), created.id(), command("Intermediario")).await().indefinitely();

        assertEquals(tournament.id(), created.tournamentId());
        assertEquals("Intermediario", updated.name());
        assertEquals(1, service.list(ownerId, tournament.id()).await().indefinitely().size());
        assertEquals(updated.id(), service.get(ownerId, tournament.id(), updated.id()).await().indefinitely().id());

        service.delete(ownerId, tournament.id(), updated.id()).await().indefinitely();
        assertEquals(0, service.list(ownerId, tournament.id()).await().indefinitely().size());
    }

    @Test
    void protectsOwnershipAndCategoryScope() {
        UUID ownerId = UUID.randomUUID();
        UUID otherOwnerId = UUID.randomUUID();
        var tournaments = new InMemoryTournamentRepository();
        var service = service(tournaments, new InMemoryTournamentCategoryRepository());
        Tournament tournament = tournaments.add(ownerId);

        assertThrows(TournamentNotFoundException.class,
                () -> service.create(otherOwnerId, tournament.id(), command("Iniciante")).await().indefinitely());
        assertThrows(TournamentCategoryNotFoundException.class,
                () -> service.get(ownerId, tournament.id(), UUID.randomUUID()).await().indefinitely());
    }

    @Test
    void blocksDuplicateNamesAndClosedTournamentMutations() {
        UUID ownerId = UUID.randomUUID();
        var tournaments = new InMemoryTournamentRepository();
        var categories = new InMemoryTournamentCategoryRepository();
        Tournament tournament = tournaments.add(ownerId);
        var service = service(tournaments, categories);

        service.create(ownerId, tournament.id(), command("Iniciante")).await().indefinitely();

        assertThrows(TournamentCategoryConflictException.class,
                () -> service.create(ownerId, tournament.id(), command(" iniciante ")).await().indefinitely());

        tournament.publish(NOW.plusSeconds(1));
        tournament.close(NOW.plusSeconds(2));

        assertThrows(TournamentConflictException.class,
                () -> service.create(ownerId, tournament.id(), command("Avancado")).await().indefinitely());
    }

    private static DefaultTournamentCategoryService service(InMemoryTournamentRepository tournaments,
            InMemoryTournamentCategoryRepository categories) {
        return new DefaultTournamentCategoryService(tournaments, categories, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static TournamentCategoryService.SaveCategoryCommand command(String name) {
        return new TournamentCategoryService.SaveCategoryCommand(name, TournamentModality.FUTSAL, 7, 12, 4, 16,
                CategoryPaymentModel.TEAM, 12500, CompetitionFormat.GROUP_STAGE, null, 40, 10, 60,
                SchedulingStrategy.BALANCED_REST);
    }

    private static final class InMemoryTournamentRepository implements TournamentRepository {
        private final LinkedHashMap<UUID, Tournament> tournaments = new LinkedHashMap<>();

        Tournament add(UUID ownerId) {
            Tournament tournament = Tournament.create(ownerId, new Tournament.SaveFields("Copa Squadra",
                    "Women's football tournament.", "Sao Paulo", "SP", "Arena Central",
                    LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-12"),
                    LocalDate.parse("2026-10-01"), "Standard rules.", null), NOW);
            tournaments.put(tournament.id(), tournament);
            return tournament;
        }

        @Override
        public Uni<TournamentDetails> create(Tournament tournament) {
            tournaments.put(tournament.id(), tournament);
            return Uni.createFrom().item(new TournamentDetails(tournament, 0));
        }

        @Override
        public Uni<List<TournamentSummary>> listForOwner(UUID ownerOrganizerUserId) {
            return Uni.createFrom().item(List.of());
        }

        @Override
        public Uni<Optional<TournamentDetails>> findById(UUID tournamentId) {
            Tournament tournament = tournaments.get(tournamentId);
            return Uni.createFrom().item(tournament == null ? Optional.empty() : Optional.of(new TournamentDetails(tournament, 0)));
        }

        @Override
        public Uni<List<TournamentSummary>> listAvailable(int limit, int offset) { return Uni.createFrom().item(List.of()); }

        @Override
        public Uni<Optional<TournamentDetails>> findPublicOpen(UUID tournamentId) { return findById(tournamentId); }

        @Override
        public Uni<Optional<TournamentDetails>> findOwnedByOrganizer(UUID tournamentId, UUID ownerOrganizerUserId) {
            Tournament tournament = tournaments.get(tournamentId);
            if (tournament == null || !tournament.ownerOrganizerUserId().equals(ownerOrganizerUserId)) {
                return Uni.createFrom().item(Optional.empty());
            }
            return Uni.createFrom().item(Optional.of(new TournamentDetails(tournament, 0)));
        }

        @Override
        public Uni<TournamentDetails> save(Tournament tournament) {
            tournaments.put(tournament.id(), tournament);
            return Uni.createFrom().item(new TournamentDetails(tournament, 0));
        }
    }

    private static final class InMemoryTournamentCategoryRepository implements TournamentCategoryRepository {
        private final LinkedHashMap<UUID, TournamentCategory> categories = new LinkedHashMap<>();

        @Override
        public Uni<TournamentCategory> create(TournamentCategory category) {
            categories.put(category.id(), category);
            return Uni.createFrom().item(category);
        }

        @Override
        public Uni<List<TournamentCategory>> listByTournament(UUID tournamentId) {
            List<TournamentCategory> list = categories.values().stream()
                    .filter(category -> category.tournamentId().equals(tournamentId))
                    .sorted(Comparator.comparing(TournamentCategory::createdAt))
                    .toList();
            return Uni.createFrom().item(new ArrayList<>(list));
        }

        @Override
        public Uni<Optional<TournamentCategory>> findByTournament(UUID tournamentId, UUID categoryId) {
            TournamentCategory category = categories.get(categoryId);
            if (category == null || !category.tournamentId().equals(tournamentId)) {
                return Uni.createFrom().item(Optional.empty());
            }
            return Uni.createFrom().item(Optional.of(category));
        }

        @Override
        public Uni<Boolean> nameExists(UUID tournamentId, String normalizedName, UUID excludingCategoryId) {
            boolean exists = categories.values().stream()
                    .anyMatch(category -> category.tournamentId().equals(tournamentId)
                            && category.normalizedName().equals(normalizedName)
                            && (excludingCategoryId == null || !category.id().equals(excludingCategoryId)));
            return Uni.createFrom().item(exists);
        }

        @Override
        public Uni<TournamentCategory> save(TournamentCategory category) {
            categories.put(category.id(), category);
            return Uni.createFrom().item(category);
        }

        @Override
        public Uni<Boolean> delete(UUID tournamentId, UUID categoryId) {
            TournamentCategory category = categories.get(categoryId);
            if (category == null || !category.tournamentId().equals(tournamentId)) {
                return Uni.createFrom().item(false);
            }
            categories.remove(categoryId);
            return Uni.createFrom().item(true);
        }
    }
}
