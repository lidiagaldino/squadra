package com.squadra.platform.application.services.impl;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.squadra.platform.application.services.interfaces.AuthenticationService;
import com.squadra.platform.domain.services.AccessTokenIssuer;
import com.squadra.platform.domain.valueobjects.EmailAddress;
import com.squadra.platform.domain.services.OpaqueTokenGenerator;
import com.squadra.platform.domain.services.PasswordHasher;
import com.squadra.platform.domain.services.PasswordResetDelivery;
import com.squadra.platform.domain.entities.PasswordResetToken;
import com.squadra.platform.domain.repositories.PasswordResetTokenRepository;
import com.squadra.platform.domain.entities.RefreshSession;
import com.squadra.platform.domain.repositories.RefreshSessionRepository;
import com.squadra.platform.domain.entities.User;
import com.squadra.platform.domain.repositories.UserRepository;
import com.squadra.platform.domain.enums.UserRole;
import io.smallrye.mutiny.Uni;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultAuthenticationServiceTest {
    @Test
    void registersAthleteWithHashedPasswordAndRejectsDuplicateEmail() {
        Fixtures fixtures = new Fixtures();
        EmailAddress email = new EmailAddress("athlete@example.com");

        var result = fixtures.service.register(new AuthenticationService.RegisterCommand(email, "a-safe-password", UserRole.ATHLETE))
                .await().indefinitely();

        assertEquals(email, result.email());
        assertEquals(UserRole.ATHLETE, result.role());
        assertNotEquals("a-safe-password", fixtures.users.byId(UUID.fromString(result.id())).passwordHash());
        assertThrows(DuplicateAccountException.class, () -> fixtures.service.register(
                new AuthenticationService.RegisterCommand(email, "another-safe-password", UserRole.ATHLETE)).await().indefinitely());
    }

    @Test
    void logsInOnlyWithValidCredentialsAndCreatesAccessAndRefreshTokens() {
        Fixtures fixtures = new Fixtures();
        User user = fixtures.user("organizer@example.com", "correct-safe-password", UserRole.ORGANIZER);

        var session = fixtures.service.login(new AuthenticationService.LoginCommand(user.email(), "correct-safe-password"))
                .await().indefinitely();

        assertEquals("access-" + user.id(), session.accessToken());
        assertNotNull(session.refreshToken());
        assertTrue(fixtures.refreshTokens.byHash(fixtures.tokens.hash(session.refreshToken())).isUsableAt(Instant.now()));
        assertThrows(AuthenticationFailureException.class, () -> fixtures.service.login(
                new AuthenticationService.LoginCommand(user.email(), "incorrect-password")).await().indefinitely());
        assertThrows(AuthenticationFailureException.class, () -> fixtures.service.login(
                new AuthenticationService.LoginCommand(new EmailAddress("nobody@example.com"), "incorrect-password")).await().indefinitely());
    }

    @Test
    void rotatesRefreshTokenAndRejectsTheOldOne() {
        Fixtures fixtures = new Fixtures();
        User user = fixtures.user("athlete@example.com", "correct-safe-password", UserRole.ATHLETE);
        var first = fixtures.service.login(new AuthenticationService.LoginCommand(user.email(), "correct-safe-password")).await().indefinitely();

        var replacement = fixtures.service.refresh(first.refreshToken()).await().indefinitely();

        assertNotEquals(first.refreshToken(), replacement.refreshToken());
        assertFalse(fixtures.refreshTokens.byHash(fixtures.tokens.hash(first.refreshToken())).isUsableAt(Instant.now()));
        assertThrows(AuthenticationFailureException.class, () -> fixtures.service.refresh(first.refreshToken()).await().indefinitely());
    }

    @Test
    void logsOutByRevokingTheRefreshSession() {
        Fixtures fixtures = new Fixtures();
        User user = fixtures.user("athlete@example.com", "correct-safe-password", UserRole.ATHLETE);
        var session = fixtures.service.login(new AuthenticationService.LoginCommand(user.email(), "correct-safe-password")).await().indefinitely();

        fixtures.service.logout(session.refreshToken()).await().indefinitely();

        assertFalse(fixtures.refreshTokens.byHash(fixtures.tokens.hash(session.refreshToken())).isUsableAt(Instant.now()));
    }

    @Test
    void logoutIsIdempotentForMissingOrAlreadyRevokedRefreshTokens() {
        Fixtures fixtures = new Fixtures();
        User user = fixtures.user("athlete@example.com", "correct-safe-password", UserRole.ATHLETE);
        var session = fixtures.service.login(new AuthenticationService.LoginCommand(user.email(), "correct-safe-password")).await().indefinitely();

        fixtures.service.logout(null).await().indefinitely();
        fixtures.service.logout("").await().indefinitely();
        fixtures.service.logout(session.refreshToken()).await().indefinitely();
        fixtures.service.logout(session.refreshToken()).await().indefinitely();

        assertFalse(fixtures.refreshTokens.byHash(fixtures.tokens.hash(session.refreshToken())).isUsableAt(Instant.now()));
    }

    @Test
    void recoversKnownAccountsWithoutDisclosingUnknownAccounts() {
        Fixtures fixtures = new Fixtures();
        User user = fixtures.user("athlete@example.com", "correct-safe-password", UserRole.ATHLETE);

        fixtures.service.requestPasswordRecovery(user.email()).await().indefinitely();
        fixtures.service.requestPasswordRecovery(new EmailAddress("unknown@example.com")).await().indefinitely();

        assertEquals(user.email(), fixtures.delivery.recipient);
        assertNotNull(fixtures.delivery.rawToken);
        assertTrue(fixtures.passwordResets.byHash(fixtures.tokens.hash(fixtures.delivery.rawToken)).isUsableAt(Instant.now()));
    }

    @Test
    void resetsPasswordConsumesTokenAndRevokesEveryRefreshSession() {
        Fixtures fixtures = new Fixtures();
        User user = fixtures.user("organizer@example.com", "correct-safe-password", UserRole.ORGANIZER);
        var first = fixtures.service.login(new AuthenticationService.LoginCommand(user.email(), "correct-safe-password")).await().indefinitely();
        var second = fixtures.service.login(new AuthenticationService.LoginCommand(user.email(), "correct-safe-password")).await().indefinitely();
        fixtures.service.requestPasswordRecovery(user.email()).await().indefinitely();

        fixtures.service.resetPassword(new AuthenticationService.ResetPasswordCommand(fixtures.delivery.rawToken, "replacement-password"))
                .await().indefinitely();

        assertTrue(fixtures.passwordHasher.matches("replacement-password", fixtures.users.byId(user.id()).passwordHash()));
        assertFalse(fixtures.refreshTokens.byHash(fixtures.tokens.hash(first.refreshToken())).isUsableAt(Instant.now()));
        assertFalse(fixtures.refreshTokens.byHash(fixtures.tokens.hash(second.refreshToken())).isUsableAt(Instant.now()));
        assertThrows(AuthenticationFailureException.class, () -> fixtures.service.resetPassword(
                new AuthenticationService.ResetPasswordCommand(fixtures.delivery.rawToken, "another-safe-password")).await().indefinitely());
    }

    private static final class Fixtures {
        final InMemoryUsers users = new InMemoryUsers();
        final InMemoryRefreshTokens refreshTokens = new InMemoryRefreshTokens();
        final InMemoryPasswordResets passwordResets = new InMemoryPasswordResets();
        final TestPasswordHasher passwordHasher = new TestPasswordHasher();
        final TestTokens tokens = new TestTokens();
        final RecordingDelivery delivery = new RecordingDelivery();
        final AuthenticationService service = new DefaultAuthenticationService(users, refreshTokens, passwordResets,
                passwordHasher, user -> "access-" + user.id(), tokens, delivery);

        User user(String email, String password, UserRole role) {
            User user = new User(UUID.randomUUID(), new EmailAddress(email), passwordHasher.hash(password), role, Instant.now());
            users.save(user).await().indefinitely();
            return user;
        }
    }

    private static final class InMemoryUsers implements UserRepository {
        private final Map<UUID, User> users = new HashMap<>();
        @Override public Uni<Optional<User>> findByEmail(EmailAddress email) { return Uni.createFrom().item(users.values().stream().filter(user -> user.email().equals(email)).findFirst()); }
        @Override public Uni<Optional<User>> findById(UUID id) { return Uni.createFrom().item(Optional.ofNullable(users.get(id))); }
        @Override public Uni<User> save(User user) { users.put(user.id(), user); return Uni.createFrom().item(user); }
        @Override public Uni<Void> replacePasswordHash(UUID id, String hash) { users.get(id).replacePasswordHash(hash); return Uni.createFrom().voidItem(); }
        User byId(UUID id) { return users.get(id); }
    }

    private static final class InMemoryRefreshTokens implements RefreshSessionRepository {
        private final Map<String, RefreshSession> tokens = new HashMap<>();
        @Override public Uni<Optional<RefreshSession>> findByTokenHash(String hash) { return Uni.createFrom().item(Optional.ofNullable(tokens.get(hash))); }
        @Override public Uni<RefreshSession> save(RefreshSession token) { tokens.put(token.tokenHash(), token); return Uni.createFrom().item(token); }
        @Override public Uni<Void> revokeById(UUID id, Instant at) { tokens.values().stream().filter(token -> token.id().equals(id)).forEach(token -> token.revoke(at)); return Uni.createFrom().voidItem(); }
        @Override public Uni<Void> revokeAllForUser(UUID userId, Instant at) { tokens.values().stream().filter(token -> token.userId().equals(userId)).forEach(token -> token.revoke(at)); return Uni.createFrom().voidItem(); }
        RefreshSession byHash(String hash) { return tokens.get(hash); }
    }

    private static final class InMemoryPasswordResets implements PasswordResetTokenRepository {
        private final Map<String, PasswordResetToken> tokens = new HashMap<>();
        @Override public Uni<Optional<PasswordResetToken>> findByTokenHash(String hash) { return Uni.createFrom().item(Optional.ofNullable(tokens.get(hash))); }
        @Override public Uni<PasswordResetToken> save(PasswordResetToken token) { tokens.put(token.tokenHash(), token); return Uni.createFrom().item(token); }
        @Override public Uni<Void> consume(UUID id, Instant at) { tokens.values().stream().filter(token -> token.id().equals(id)).forEach(token -> token.consume(at)); return Uni.createFrom().voidItem(); }
        PasswordResetToken byHash(String hash) { return tokens.get(hash); }
    }

    private static final class TestPasswordHasher implements PasswordHasher {
        @Override public String hash(String password) { return "hash:" + password; }
        @Override public boolean matches(String password, String hash) { return hash.equals(hash(password)); }
    }

    private static final class TestTokens implements OpaqueTokenGenerator {
        private int index;
        @Override public String newToken() { return "token-" + ++index; }
        @Override public String hash(String token) {
            try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
            catch (Exception exception) { throw new IllegalStateException(exception); }
        }
    }

    private static final class RecordingDelivery implements PasswordResetDelivery {
        EmailAddress recipient;
        String rawToken;
        @Override public Uni<Void> deliver(EmailAddress email, String token) { recipient = email; rawToken = token; return Uni.createFrom().voidItem(); }
    }
}
