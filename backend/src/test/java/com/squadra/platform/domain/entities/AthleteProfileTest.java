package com.squadra.platform.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.squadra.platform.domain.enums.DominantFoot;
import com.squadra.platform.domain.enums.FootballPosition;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AthleteProfileTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");

    @Test
    void createsValidAthleteOwnedProfile() {
        AthleteProfile profile = AthleteProfile.create(UUID.randomUUID(), validFields(), NOW);

        assertEquals("Marta", profile.displayName());
        assertEquals(FootballPosition.FORWARD, profile.primaryPosition());
        assertTrue(profile.secondaryPositions().contains(FootballPosition.MIDFIELDER));
        assertEquals(DominantFoot.LEFT, profile.dominantFoot());
        assertEquals(NOW, profile.createdAt());
        assertEquals(NOW, profile.updatedAt());
    }

    @Test
    void rejectsInvalidFieldBounds() {
        assertThrows(IllegalArgumentException.class, () -> AthleteProfile.create(UUID.randomUUID(),
                new AthleteProfile.SaveFields("", null, null, FootballPosition.FORWARD, Set.of(), null, null, null, null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> AthleteProfile.create(UUID.randomUUID(),
                new AthleteProfile.SaveFields("a".repeat(61), null, null, FootballPosition.FORWARD, Set.of(), null, null, null, null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> AthleteProfile.create(UUID.randomUUID(),
                new AthleteProfile.SaveFields("Marta", "a".repeat(121), null, FootballPosition.FORWARD, Set.of(), null, null, null, null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> AthleteProfile.create(UUID.randomUUID(),
                new AthleteProfile.SaveFields("Marta", null, LocalDate.now().plusDays(1), FootballPosition.FORWARD, Set.of(), null, null, null, null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> AthleteProfile.create(UUID.randomUUID(),
                new AthleteProfile.SaveFields("Marta", null, null, FootballPosition.FORWARD, Set.of(), null, 0, null, null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> AthleteProfile.create(UUID.randomUUID(),
                new AthleteProfile.SaveFields("Marta", null, null, FootballPosition.FORWARD, Set.of(), null, null, null, null, "a".repeat(281)), NOW));
    }

    @Test
    void rejectsSecondaryPositionsThatDuplicatePrimaryPosition() {
        assertThrows(IllegalArgumentException.class, () -> AthleteProfile.create(UUID.randomUUID(),
                new AthleteProfile.SaveFields("Marta", null, null, FootballPosition.FORWARD, Set.of(FootballPosition.FORWARD), null, null, null, null, null), NOW));
    }

    @Test
    void updatesProfileFieldsAndTimestamp() {
        AthleteProfile profile = AthleteProfile.create(UUID.randomUUID(), validFields(), NOW);
        Instant later = NOW.plusSeconds(60);

        profile.update(new AthleteProfile.SaveFields("Rainha", null, null, FootballPosition.MIDFIELDER,
                Set.of(FootballPosition.FORWARD), DominantFoot.BOTH, 10, "Salvador", "BA", "Meia atacante"), later);

        assertEquals("Rainha", profile.displayName());
        assertEquals(FootballPosition.MIDFIELDER, profile.primaryPosition());
        assertEquals(later, profile.updatedAt());
    }

    private static AthleteProfile.SaveFields validFields() {
        return new AthleteProfile.SaveFields("Marta", "Marta Vieira", LocalDate.parse("1986-02-19"),
                FootballPosition.FORWARD, Set.of(FootballPosition.MIDFIELDER), DominantFoot.LEFT, 10,
                "Dois Riachos", "AL", "Football story");
    }
}
