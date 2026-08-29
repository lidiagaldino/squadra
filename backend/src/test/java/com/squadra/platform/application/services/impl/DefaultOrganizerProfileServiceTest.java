package com.squadra.platform.application.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.squadra.platform.application.services.interfaces.OrganizerProfileService;
import com.squadra.platform.domain.entities.OrganizerProfile;
import com.squadra.platform.domain.repositories.OrganizerProfileRepository;
import io.smallrye.mutiny.Uni;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultOrganizerProfileServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");

    @Test
    void createsProfileForAuthenticatedUserIdOnly() {
        var repository = new InMemoryProfiles();
        var service = new DefaultOrganizerProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        UUID authenticatedUserId = UUID.randomUUID();

        OrganizerProfile profile = service.saveMine(authenticatedUserId, validCommand("Liga Miga"))
                .await().indefinitely();

        assertEquals(authenticatedUserId, profile.userId());
        assertEquals("Liga Miga", profile.organizationName());
        assertEquals(profile, repository.profilesByUserId.get(authenticatedUserId));
    }

    @Test
    void editsExistingProfileWithoutCreatingAnotherRow() {
        var repository = new InMemoryProfiles();
        var service = new DefaultOrganizerProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        UUID userId = UUID.randomUUID();
        OrganizerProfile first = service.saveMine(userId, validCommand("Liga Miga")).await().indefinitely();

        OrganizerProfile edited = service.saveMine(userId, new OrganizerProfileService.SaveOrganizerProfileCommand(
                "Copa Minas", "Bia Costa", "info@copaminas.test", null, "Belo Horizonte", "MG",
                "http://copaminas.test", null)).await().indefinitely();

        assertEquals(first.id(), edited.id());
        assertEquals("Copa Minas", edited.organizationName());
        assertEquals("Belo Horizonte", edited.city());
        assertEquals(1, repository.profilesByUserId.size());
    }

    @Test
    void repeatedSameSubmissionKeepsSingleProfileRow() {
        var repository = new InMemoryProfiles();
        var service = new DefaultOrganizerProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        UUID userId = UUID.randomUUID();

        OrganizerProfile first = service.saveMine(userId, validCommand("Liga Miga")).await().indefinitely();
        OrganizerProfile second = service.saveMine(userId, validCommand("Liga Miga")).await().indefinitely();

        assertEquals(first.id(), second.id());
        assertEquals(1, repository.profilesByUserId.size());
    }

    @Test
    void findsMissingProfileAsEmptyOptional() {
        var service = new DefaultOrganizerProfileService(new InMemoryProfiles(), Clock.fixed(NOW, ZoneOffset.UTC));

        Optional<OrganizerProfile> profile = service.findMine(UUID.randomUUID()).await().indefinitely();

        assertTrue(profile.isEmpty());
    }

    @Test
    void rejectsInvalidProfileInput() {
        var service = new DefaultOrganizerProfileService(new InMemoryProfiles(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(IllegalArgumentException.class, () -> service.saveMine(UUID.randomUUID(),
                new OrganizerProfileService.SaveOrganizerProfileCommand("L", "Ana Silva", "contato@ligamiga.test",
                        null, "Sao Paulo", "SP", "https://ligamiga.test", null)).await().indefinitely());
    }

    private static OrganizerProfileService.SaveOrganizerProfileCommand validCommand(String organizationName) {
        return new OrganizerProfileService.SaveOrganizerProfileCommand(organizationName, "Ana Silva",
                "contato@ligamiga.test", "+55 11 99999-0000", "Sao Paulo", "SP", "https://ligamiga.test",
                "Organizes women's football tournaments.");
    }

    private static final class InMemoryProfiles implements OrganizerProfileRepository {
        final HashMap<UUID, OrganizerProfile> profilesByUserId = new HashMap<>();

        @Override
        public Uni<Optional<OrganizerProfile>> findByUserId(UUID userId) {
            return Uni.createFrom().item(Optional.ofNullable(profilesByUserId.get(userId)));
        }

        @Override
        public Uni<OrganizerProfile> upsert(OrganizerProfile profile) {
            OrganizerProfile previous = profilesByUserId.put(profile.userId(), profile);
            if (previous != null) {
                assertEquals(previous.id(), profile.id());
            } else {
                assertNotEquals(null, profile.id());
            }
            return Uni.createFrom().item(profile);
        }
    }
}
