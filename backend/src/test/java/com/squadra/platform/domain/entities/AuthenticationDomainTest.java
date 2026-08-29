package com.squadra.platform.domain.entities;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticationDomainTest {
    private static final String HASH = "a".repeat(64);

    @Test
    void normalizesEmailForUniqueAccountLookup() {
        assertEquals("organizer@example.com", new EmailAddress(" Organizer@Example.COM ").value());
    }

    @Test
    void limitsSelfServiceRolesToAthleteAndOrganizer() {
        assertEquals(UserRole.ATHLETE, UserRole.selfServiceValueOf("ATHLETE"));
        assertThrows(IllegalArgumentException.class, () -> UserRole.selfServiceValueOf("ADMIN"));
    }

    @Test
    void rejectsPasswordsOutsideBcryptSafePolicy() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate("short"));
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate("a".repeat(73)));
        PasswordPolicy.validate("a-safe-password");
    }

    @Test
    void acceptsRefreshSessionOnlyBeforeExpiryAndRevocation() {
        Instant now = Instant.parse("2026-08-27T12:00:00Z");
        RefreshSession session = new RefreshSession(UUID.randomUUID(), UUID.randomUUID(), HASH, now.plusSeconds(1), now, null);
        assertTrue(session.isUsableAt(now));
        session.revoke(now);
        assertFalse(session.isUsableAt(now));
    }

    @Test
    void consumesResetTokenOnlyOnce() {
        Instant now = Instant.parse("2026-08-27T12:00:00Z");
        PasswordResetToken token = new PasswordResetToken(UUID.randomUUID(), UUID.randomUUID(), HASH, now.plusSeconds(1), now, null);
        token.consume(now);
        assertFalse(token.isUsableAt(now));
        assertThrows(IllegalStateException.class, () -> token.consume(now));
    }
}
