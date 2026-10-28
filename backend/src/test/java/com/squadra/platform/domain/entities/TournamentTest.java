package com.squadra.platform.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.squadra.platform.domain.enums.TournamentStatus;
import com.squadra.platform.domain.enums.TournamentVisibility;
import com.squadra.platform.domain.exceptions.TournamentConflictException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TournamentTest {
    private static final Instant NOW = Instant.parse("2026-08-29T12:00:00Z");

    @Test
    void createsDraftTournamentForOwnerAndNormalizesFields() {
        UUID ownerId = UUID.randomUUID();

        Tournament tournament = Tournament.create(ownerId, validFields(), NOW);

        assertEquals(ownerId, tournament.ownerOrganizerUserId());
        assertEquals(TournamentStatus.DRAFT, tournament.status());
        assertEquals(TournamentVisibility.PUBLIC, tournament.visibility());
        assertEquals("Copa Squadra", tournament.name());
        assertEquals("Sao Paulo", tournament.city());
        assertEquals("SP", tournament.state());
        assertEquals("Arena Central", tournament.venueName());
        assertEquals(LocalDate.parse("2026-10-01"), tournament.registrationDeadline());
        assertNull(tournament.publishedAt());
        assertEquals(NOW, tournament.createdAt());
        assertEquals(NOW, tournament.updatedAt());
    }

    @Test
    void rejectsInvalidFieldsAndDateOrdering() {
        assertThrows(IllegalArgumentException.class, () -> Tournament.create(UUID.randomUUID(), fields("Co", "Sao Paulo", "SP", "Arena", "2026-10-10", "2026-10-12", "2026-10-01", null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Tournament.create(UUID.randomUUID(), fields("N".repeat(121), "Sao Paulo", "SP", "Arena", "2026-10-10", "2026-10-12", "2026-10-01", null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Tournament.create(UUID.randomUUID(), fields("Copa", "S", "SP", "Arena", "2026-10-10", "2026-10-12", "2026-10-01", null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Tournament.create(UUID.randomUUID(), fields("Copa", "Sao Paulo", "S", "Arena", "2026-10-10", "2026-10-12", "2026-10-01", null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Tournament.create(UUID.randomUUID(), fields("Copa", "Sao Paulo", "SP", "A", "2026-10-10", "2026-10-12", "2026-10-01", null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Tournament.create(UUID.randomUUID(), fields("Copa", "Sao Paulo", "SP", "Arena", "2026-10-12", "2026-10-10", "2026-10-01", null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Tournament.create(UUID.randomUUID(), fields("Copa", "Sao Paulo", "SP", "Arena", "2026-10-10", "2026-10-12", "2026-10-11", null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Tournament.create(UUID.randomUUID(), fields("Copa", "Sao Paulo", "SP", "Arena", "2026-10-10", "2026-10-12", "2026-10-01", "D".repeat(1001), null), NOW));
        assertThrows(IllegalArgumentException.class, () -> Tournament.create(UUID.randomUUID(), fields("Copa", "Sao Paulo", "SP", "Arena", "2026-10-10", "2026-10-12", "2026-10-01", null, "R".repeat(5001)), NOW));
    }

    @Test
    void allowsPublishCloseReopenAndCancelFromSpecifiedStatuses() {
        Tournament tournament = Tournament.create(UUID.randomUUID(), validFields(), NOW);

        tournament.publish(NOW.plusSeconds(1));
        assertEquals(TournamentStatus.OPEN, tournament.status());
        assertEquals(NOW.plusSeconds(1), tournament.publishedAt());

        tournament.close(NOW.plusSeconds(2));
        assertEquals(TournamentStatus.CLOSED, tournament.status());
        assertEquals(NOW.plusSeconds(2), tournament.closedAt());

        tournament.reopen(NOW.plusSeconds(3));
        assertEquals(TournamentStatus.OPEN, tournament.status());

        tournament.cancel(NOW.plusSeconds(4));
        assertEquals(TournamentStatus.CANCELLED, tournament.status());
        assertEquals(NOW.plusSeconds(4), tournament.cancelledAt());
    }

    @Test
    void rejectsInvalidLifecycleTransitions() {
        Tournament tournament = Tournament.create(UUID.randomUUID(), validFields(), NOW);

        assertThrows(TournamentConflictException.class, () -> tournament.close(NOW.plusSeconds(1)));
        assertThrows(TournamentConflictException.class, () -> tournament.reopen(NOW.plusSeconds(1)));

        tournament.publish(NOW.plusSeconds(1));
        assertThrows(TournamentConflictException.class, () -> tournament.publish(NOW.plusSeconds(2)));
        assertThrows(TournamentConflictException.class, () -> tournament.update(
                new Tournament.SaveFields("Copa Squadra", null, "Sao Paulo", "SP", "Arena Central",
                        LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-12"),
                        LocalDate.parse("2026-10-01"), null, TournamentVisibility.PRIVATE), NOW.plusSeconds(3)));
    }

    @Test
    void cancelledTournamentsRejectEditsAndRepeatedCancel() {
        Tournament tournament = Tournament.create(UUID.randomUUID(), validFields(), NOW);

        tournament.cancel(NOW.plusSeconds(1));

        assertThrows(TournamentConflictException.class, () -> tournament.update(validFields(), NOW.plusSeconds(2)));
        assertThrows(TournamentConflictException.class, () -> tournament.cancel(NOW.plusSeconds(2)));
    }

    private static Tournament.SaveFields validFields() {
        return fields(" Copa Squadra ", " Sao Paulo ", " SP ", " Arena Central ", "2026-10-10", "2026-10-12",
                "2026-10-01", " Opening edition ", " Standard rules ");
    }

    private static Tournament.SaveFields fields(String name, String city, String state, String venueName,
            String startDate, String endDate, String registrationDeadline, String description, String rules) {
        return new Tournament.SaveFields(name, description, city, state, venueName, LocalDate.parse(startDate),
                LocalDate.parse(endDate), LocalDate.parse(registrationDeadline), rules, null);
    }
}
