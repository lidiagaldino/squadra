package com.squadra.platform.application.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.squadra.platform.application.services.interfaces.TournamentService;
import com.squadra.platform.domain.entities.Tournament;
import com.squadra.platform.domain.enums.TournamentStatus;
import com.squadra.platform.domain.exceptions.TournamentConflictException;
import com.squadra.platform.domain.exceptions.TournamentNotFoundException;
import com.squadra.platform.domain.repositories.TournamentRepository;
import io.smallrye.mutiny.Uni;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultTournamentServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-29T12:00:00Z");

    @Test
    void createsDraftTournamentForAuthenticatedOrganizer() {
        UUID ownerId = UUID.randomUUID();
        var repository = new InMemoryTournamentRepository();
        var service = service(repository);

        var details = service.create(ownerId, command("Copa Squadra")).await().indefinitely();

        assertEquals(ownerId, details.tournament().ownerOrganizerUserId());
        assertEquals(TournamentStatus.DRAFT, details.tournament().status());
        assertEquals(0, details.categoryCount());
    }

    @Test
    void listsAndFindsOnlyOwnedTournaments() {
        UUID ownerId = UUID.randomUUID();
        UUID otherOwnerId = UUID.randomUUID();
        var repository = new InMemoryTournamentRepository();
        var service = service(repository);
        var owned = service.create(ownerId, command("Copa Owner")).await().indefinitely();
        service.create(otherOwnerId, command("Copa Other")).await().indefinitely();

        var summaries = service.listMine(ownerId).await().indefinitely();

        assertEquals(1, summaries.size());
        assertEquals(owned.tournament().id(), summaries.getFirst().tournament().id());
        assertThrows(TournamentNotFoundException.class,
                () -> service.getMine(otherOwnerId, owned.tournament().id()).await().indefinitely());
    }

    @Test
    void editsOwnedTournamentAndRejectsCancelledEdit() {
        UUID ownerId = UUID.randomUUID();
        var repository = new InMemoryTournamentRepository();
        var service = service(repository);
        UUID tournamentId = service.create(ownerId, command("Copa Squadra")).await().indefinitely().tournament().id();

        var updated = service.update(ownerId, tournamentId, command("Copa Atualizada")).await().indefinitely();
        service.cancel(ownerId, tournamentId).await().indefinitely();

        assertEquals("Copa Atualizada", updated.tournament().name());
        assertThrows(TournamentConflictException.class,
                () -> service.update(ownerId, tournamentId, command("Copa Bloqueada")).await().indefinitely());
    }

    @Test
    void appliesLifecycleTransitionsAndConflicts() {
        UUID ownerId = UUID.randomUUID();
        var repository = new InMemoryTournamentRepository();
        var service = service(repository);
        UUID tournamentId = service.create(ownerId, command("Copa Squadra")).await().indefinitely().tournament().id();

        assertThrows(TournamentConflictException.class,
                () -> service.close(ownerId, tournamentId).await().indefinitely());
        assertEquals(TournamentStatus.OPEN, service.publish(ownerId, tournamentId).await().indefinitely().tournament().status());
        assertThrows(TournamentConflictException.class,
                () -> service.publish(ownerId, tournamentId).await().indefinitely());
        assertEquals(TournamentStatus.CLOSED, service.close(ownerId, tournamentId).await().indefinitely().tournament().status());
        assertEquals(TournamentStatus.OPEN, service.reopen(ownerId, tournamentId).await().indefinitely().tournament().status());
        assertEquals(TournamentStatus.CANCELLED, service.cancel(ownerId, tournamentId).await().indefinitely().tournament().status());
    }

    @Test
    void validatesCommandsBeforePersistence() {
        var service = service(new InMemoryTournamentRepository());

        assertThrows(IllegalArgumentException.class,
                () -> service.create(UUID.randomUUID(), command("Co")).await().indefinitely());
    }

    private static DefaultTournamentService service(InMemoryTournamentRepository repository) {
        return new DefaultTournamentService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static TournamentService.SaveTournamentCommand command(String name) {
        return new TournamentService.SaveTournamentCommand(name, "Women's football tournament.", "Sao Paulo", "SP",
                "Arena Central", LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-12"),
                LocalDate.parse("2026-10-01"), "Standard rules.");
    }

    private static final class InMemoryTournamentRepository implements TournamentRepository {
        private final LinkedHashMap<UUID, Tournament> tournaments = new LinkedHashMap<>();

        @Override
        public Uni<TournamentDetails> create(Tournament tournament) {
            tournaments.put(tournament.id(), tournament);
            return Uni.createFrom().item(new TournamentDetails(tournament, 0));
        }

        @Override
        public Uni<List<TournamentSummary>> listForOwner(UUID ownerOrganizerUserId) {
            List<TournamentSummary> summaries = new ArrayList<>();
            for (Tournament tournament : tournaments.values()) {
                if (tournament.ownerOrganizerUserId().equals(ownerOrganizerUserId)) {
                    summaries.add(new TournamentSummary(tournament, 0));
                }
            }
            return Uni.createFrom().item(summaries);
        }

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
}
