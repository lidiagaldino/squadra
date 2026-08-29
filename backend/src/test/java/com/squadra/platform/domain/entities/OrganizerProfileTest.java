package com.squadra.platform.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizerProfileTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");

    @Test
    void createsValidOrganizerOwnedProfile() {
        UUID userId = UUID.randomUUID();

        OrganizerProfile profile = OrganizerProfile.create(userId, validFields(), NOW);

        assertEquals(userId, profile.userId());
        assertEquals("Liga Miga", profile.organizationName());
        assertEquals("Ana Silva", profile.contactName());
        assertEquals("contato@ligamiga.test", profile.contactEmail().value());
        assertEquals("+55 11 99999-0000", profile.phone());
        assertEquals("Sao Paulo", profile.city());
        assertEquals("SP", profile.state());
        assertEquals("https://ligamiga.test", profile.website());
        assertEquals("Organizes women's football tournaments.", profile.description());
        assertEquals(NOW, profile.createdAt());
        assertEquals(NOW, profile.updatedAt());
    }

    @Test
    void rejectsRequiredFieldBoundsAndMalformedEmail() {
        assertThrows(IllegalArgumentException.class, () -> OrganizerProfile.create(UUID.randomUUID(),
                new OrganizerProfile.SaveFields("", "Ana Silva", "contato@ligamiga.test", null, "Sao Paulo", "SP",
                        null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> OrganizerProfile.create(UUID.randomUUID(),
                new OrganizerProfile.SaveFields("L", "Ana Silva", "contato@ligamiga.test", null, "Sao Paulo", "SP",
                        null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> OrganizerProfile.create(UUID.randomUUID(),
                new OrganizerProfile.SaveFields("a".repeat(101), "Ana Silva", "contato@ligamiga.test", null,
                        "Sao Paulo", "SP", null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> OrganizerProfile.create(UUID.randomUUID(),
                new OrganizerProfile.SaveFields("Liga Miga", "A", "contato@ligamiga.test", null, "Sao Paulo", "SP",
                        null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> OrganizerProfile.create(UUID.randomUUID(),
                new OrganizerProfile.SaveFields("Liga Miga", "Ana Silva", "not-an-email", null, "Sao Paulo", "SP",
                        null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> OrganizerProfile.create(UUID.randomUUID(),
                new OrganizerProfile.SaveFields("Liga Miga", "Ana Silva", "contato@ligamiga.test", null, "S",
                        "SP", null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> OrganizerProfile.create(UUID.randomUUID(),
                new OrganizerProfile.SaveFields("Liga Miga", "Ana Silva", "contato@ligamiga.test", null,
                        "Sao Paulo", "S", null, null), NOW));
    }

    @Test
    void rejectsOptionalFieldBoundsAndInvalidWebsiteScheme() {
        assertThrows(IllegalArgumentException.class, () -> OrganizerProfile.create(UUID.randomUUID(),
                new OrganizerProfile.SaveFields("Liga Miga", "Ana Silva", "contato@ligamiga.test", "1".repeat(31),
                        "Sao Paulo", "SP", null, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> OrganizerProfile.create(UUID.randomUUID(),
                new OrganizerProfile.SaveFields("Liga Miga", "Ana Silva", "contato@ligamiga.test", null,
                        "Sao Paulo", "SP", "ftp://ligamiga.test", null), NOW));
        assertThrows(IllegalArgumentException.class, () -> OrganizerProfile.create(UUID.randomUUID(),
                new OrganizerProfile.SaveFields("Liga Miga", "Ana Silva", "contato@ligamiga.test", null,
                        "Sao Paulo", "SP", "https://" + "a".repeat(193), null), NOW));
        assertThrows(IllegalArgumentException.class, () -> OrganizerProfile.create(UUID.randomUUID(),
                new OrganizerProfile.SaveFields("Liga Miga", "Ana Silva", "contato@ligamiga.test", null,
                        "Sao Paulo", "SP", null, "a".repeat(501)), NOW));
    }

    @Test
    void updatesProfileFieldsAndTimestamp() {
        OrganizerProfile profile = OrganizerProfile.create(UUID.randomUUID(), validFields(), NOW);
        Instant later = NOW.plusSeconds(60);

        profile.update(new OrganizerProfile.SaveFields("Copa Minas", "Bia Costa", "info@copaminas.test", null,
                "Belo Horizonte", "MG", "http://copaminas.test", null), later);

        assertEquals("Copa Minas", profile.organizationName());
        assertEquals("Bia Costa", profile.contactName());
        assertEquals("info@copaminas.test", profile.contactEmail().value());
        assertEquals("Belo Horizonte", profile.city());
        assertEquals("MG", profile.state());
        assertEquals("http://copaminas.test", profile.website());
        assertEquals(later, profile.updatedAt());
    }

    private static OrganizerProfile.SaveFields validFields() {
        return new OrganizerProfile.SaveFields("Liga Miga", "Ana Silva", "contato@ligamiga.test",
                "+55 11 99999-0000", "Sao Paulo", "SP", "https://ligamiga.test",
                "Organizes women's football tournaments.");
    }
}
