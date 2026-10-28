package com.squadra.platform.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.squadra.platform.domain.enums.RegistrationStatus;
import com.squadra.platform.domain.exceptions.TeamRegistrationConflictException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TeamRegistrationTest {
    private static final Instant NOW = Instant.parse("2026-08-29T12:00:00Z");

    @Test
    void createsDraftRegistrationForTeamTournamentAndCategory() {
        UUID teamId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        TeamRegistration registration = TeamRegistration.create(teamId, tournamentId, categoryId, NOW);

        assertNotNull(registration.id());
        assertEquals(teamId, registration.teamId());
        assertEquals(tournamentId, registration.tournamentId());
        assertEquals(categoryId, registration.categoryId());
        assertEquals(RegistrationStatus.DRAFT, registration.status());
        assertEquals(NOW, registration.createdAt());
        assertEquals(NOW, registration.updatedAt());
    }

    @Test
    void requiresIdentifiersAndTimestamp() {
        assertThrows(NullPointerException.class, () -> TeamRegistration.create(null, UUID.randomUUID(), UUID.randomUUID(), NOW));
        assertThrows(NullPointerException.class, () -> TeamRegistration.create(UUID.randomUUID(), null, UUID.randomUUID(), NOW));
        assertThrows(NullPointerException.class, () -> TeamRegistration.create(UUID.randomUUID(), UUID.randomUUID(), null, NOW));
        assertThrows(NullPointerException.class, () -> TeamRegistration.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null));
        assertThrows(NullPointerException.class, () -> new TeamRegistration(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), RegistrationStatus.DRAFT, NOW, NOW));
        assertThrows(NullPointerException.class, () -> new TeamRegistration(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), null, NOW, NOW));
    }

    @Test
    void cancelsDraftRegistrationAndUpdatesTimestamp() {
        TeamRegistration registration = TeamRegistration.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), NOW);

        registration.cancel(NOW.plusSeconds(60));

        assertEquals(RegistrationStatus.CANCELLED, registration.status());
        assertEquals(NOW, registration.createdAt());
        assertEquals(NOW.plusSeconds(60), registration.updatedAt());
    }

    @Test
    void rejectsCancellingNonDraftRegistration() {
        TeamRegistration registration = new TeamRegistration(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), RegistrationStatus.APPROVED, NOW, NOW);

        assertThrows(TeamRegistrationConflictException.class, () -> registration.cancel(NOW.plusSeconds(60)));
        assertEquals(RegistrationStatus.APPROVED, registration.status());
        assertEquals(NOW, registration.updatedAt());
    }

    @Test
    void supportsDocumentedRegistrationStatuses() {
        Set<String> statuses = Arrays.stream(RegistrationStatus.values()).map(Enum::name).collect(Collectors.toSet());

        assertEquals(Set.of("DRAFT", "WAITING_ATHLETES", "WAITING_PAYMENT", "UNDER_REVIEW", "APPROVED",
                "REJECTED", "CANCELLED"), statuses);
    }
}
