package com.squadra.platform.infra.security;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.valueobjects.EmailAddress;
import com.squadra.platform.domain.entities.User;
import com.squadra.platform.domain.enums.UserRole;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityAdaptersTest {
    @Test void shouldHashAndVerifyPasswordWithoutKeepingPlaintext() {
        var hasher = new BcryptPasswordHasher();
        String hash = hasher.hash("an-adequately-long-password");
        assertNotEquals("an-adequately-long-password", hash);
        assertTrue(hasher.matches("an-adequately-long-password", hash));
        assertFalse(hasher.matches("incorrect-password", hash));
    }

    @Test void shouldCreateUnpredictableTokensAndStableSha256Hash() {
        var generator = new SecureOpaqueTokenGenerator();
        String first = generator.newToken();
        String second = generator.newToken();
        assertNotEquals(first, second);
        assertEquals(64, generator.hash(first).length());
        assertEquals(generator.hash(first), generator.hash(first));
        assertNotEquals(generator.hash(first), generator.hash(second));
    }

    @Test void shouldIssueASignedAccessTokenWithTheAuthenticatedUserClaims() {
        var issuer = new HmacJwtAccessTokenIssuer(new HmacJwtAccessTokenIssuer.JwtConfig() {
            @Override public String signingKey() { return "test-signing-key"; }
            @Override public String jwtIssuer() { return "squadra-test"; }
            @Override public long accessTokenLifetimeSeconds() { return 900; }
        });
        var user = new User(UUID.randomUUID(), new EmailAddress("athlete@example.test"), "hash", UserRole.ATHLETE, Instant.now());

        String token = issuer.issue(user);
        String[] parts = token.split("\\.");

        assertEquals(3, parts.length);
        assertTrue(parts[2].length() > 20);
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        assertTrue(payload.contains("\"iss\":\"squadra-test\""));
        assertTrue(payload.contains("\"sub\":\"" + user.id() + "\""));
        assertTrue(payload.contains("\"upn\":\"athlete@example.test\""));
        assertTrue(payload.contains("\"role\":\"ATHLETE\""));
        assertTrue(payload.contains("\"groups\":[\"ATHLETE\"]"));
    }

    @Test void shouldVerifyIssuedTokensAndRejectTampering() {
        var signing = new HmacJwtAccessTokenIssuer.JwtConfig() {
            @Override public String signingKey() { return "test-signing-key-with-at-least-thirty-two-bytes"; }
            @Override public String jwtIssuer() { return "squadra-test"; }
            @Override public long accessTokenLifetimeSeconds() { return 900; }
        };
        var verifier = new HmacJwtAccessTokenVerifier(new HmacJwtAccessTokenVerifier.JwtConfig() {
            @Override public String signingKey() { return signing.signingKey(); }
            @Override public String jwtIssuer() { return signing.jwtIssuer(); }
        });
        var user = new User(UUID.randomUUID(), new EmailAddress("organizer@example.test"), "hash", UserRole.ORGANIZER, Instant.now());
        String token = new HmacJwtAccessTokenIssuer(signing).issue(user);

        var verified = verifier.verify(token);

        assertEquals(user.id(), verified.userId());
        assertEquals(user.email(), verified.email());
        assertEquals(UserRole.ORGANIZER, verified.role());
        assertThrows(IllegalArgumentException.class, () -> verifier.verify(token.substring(0, token.length() - 1) + "x"));
        assertThrows(IllegalArgumentException.class, () -> verifier.verify("not-a-jwt"));
    }
}
