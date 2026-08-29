package com.squadra.platform.application.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.squadra.platform.application.services.interfaces.AthleteProfileService;
import com.squadra.platform.domain.entities.AthleteProfile;
import com.squadra.platform.domain.enums.DominantFoot;
import com.squadra.platform.domain.enums.FootballPosition;
import com.squadra.platform.domain.repositories.AthleteProfileRepository;
import io.smallrye.mutiny.Uni;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultAthleteProfileServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");

    @Test
    void createsProfileForAuthenticatedUserIdOnly() {
        var repository = new InMemoryProfiles();
        var service = new DefaultAthleteProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        UUID authenticatedUserId = UUID.randomUUID();

        AthleteProfile profile = service.saveMine(authenticatedUserId, validCommand("Marta")).await().indefinitely();

        assertEquals(authenticatedUserId, profile.userId());
        assertEquals("Marta", profile.displayName());
        assertEquals(profile, repository.profilesByUserId.get(authenticatedUserId));
    }

    @Test
    void editsExistingProfileWithoutCreatingAnotherRow() {
        var repository = new InMemoryProfiles();
        var service = new DefaultAthleteProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        UUID userId = UUID.randomUUID();
        AthleteProfile first = service.saveMine(userId, validCommand("Marta")).await().indefinitely();

        AthleteProfile edited = service.saveMine(userId, new AthleteProfileService.SaveAthleteProfileCommand(
                "Rainha", null, null, FootballPosition.MIDFIELDER, List.of(FootballPosition.FORWARD), DominantFoot.BOTH,
                10, "Salvador", "BA", "Meia atacante")).await().indefinitely();

        assertEquals(first.id(), edited.id());
        assertEquals("Rainha", edited.displayName());
        assertEquals(1, repository.profilesByUserId.size());
    }

    @Test
    void repeatedSameSubmissionKeepsSingleProfileRow() {
        var repository = new InMemoryProfiles();
        var service = new DefaultAthleteProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        UUID userId = UUID.randomUUID();

        AthleteProfile first = service.saveMine(userId, validCommand("Marta")).await().indefinitely();
        AthleteProfile second = service.saveMine(userId, validCommand("Marta")).await().indefinitely();

        assertEquals(first.id(), second.id());
        assertEquals(1, repository.profilesByUserId.size());
    }

    @Test
    void findsMissingProfileAsEmptyOptional() {
        var service = new DefaultAthleteProfileService(new InMemoryProfiles(), Clock.fixed(NOW, ZoneOffset.UTC));

        Optional<AthleteProfile> profile = service.findMine(UUID.randomUUID()).await().indefinitely();

        assertTrue(profile.isEmpty());
    }

    @Test
    void rejectsInvalidProfileInput() {
        var service = new DefaultAthleteProfileService(new InMemoryProfiles(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(IllegalArgumentException.class, () -> service.saveMine(UUID.randomUUID(),
                new AthleteProfileService.SaveAthleteProfileCommand("Marta", null, null, FootballPosition.FORWARD,
                        List.of(FootballPosition.FORWARD), null, null, null, null, null)).await().indefinitely());
    }

    private static AthleteProfileService.SaveAthleteProfileCommand validCommand(String displayName) {
        return new AthleteProfileService.SaveAthleteProfileCommand(displayName, "Marta Vieira", LocalDate.parse("1986-02-19"),
                FootballPosition.FORWARD, List.of(FootballPosition.MIDFIELDER), DominantFoot.LEFT, 10,
                "Dois Riachos", "AL", "Football story");
    }

    private static final class InMemoryProfiles implements AthleteProfileRepository {
        final HashMap<UUID, AthleteProfile> profilesByUserId = new HashMap<>();

        @Override
        public Uni<Optional<AthleteProfile>> findByUserId(UUID userId) {
            return Uni.createFrom().item(Optional.ofNullable(profilesByUserId.get(userId)));
        }

        @Override
        public Uni<AthleteProfile> upsert(AthleteProfile profile) {
            AthleteProfile previous = profilesByUserId.put(profile.userId(), profile);
            if (previous != null) {
                assertEquals(previous.id(), profile.id());
            } else {
                assertNotEquals(null, profile.id());
            }
            return Uni.createFrom().item(profile);
        }
    }
}
