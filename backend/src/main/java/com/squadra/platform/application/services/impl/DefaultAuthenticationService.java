package com.squadra.platform.application.services.impl;
import com.squadra.platform.application.services.interfaces.AuthenticationService;
import com.squadra.platform.application.services.interfaces.AuthenticationService.*;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.services.AccessTokenIssuer;
import com.squadra.platform.domain.valueobjects.EmailAddress;
import com.squadra.platform.domain.services.OpaqueTokenGenerator;
import com.squadra.platform.domain.services.PasswordHasher;
import com.squadra.platform.domain.services.PasswordPolicy;
import com.squadra.platform.domain.services.PasswordResetDelivery;
import com.squadra.platform.domain.entities.PasswordResetToken;
import com.squadra.platform.domain.repositories.PasswordResetTokenRepository;
import com.squadra.platform.domain.entities.RefreshSession;
import com.squadra.platform.domain.repositories.RefreshSessionRepository;
import com.squadra.platform.domain.entities.User;
import com.squadra.platform.domain.repositories.UserRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/** Orchestrates authentication use cases; secrets only cross this boundary in-memory. */
@ApplicationScoped
public final class DefaultAuthenticationService implements AuthenticationService {
    private static final long REFRESH_TOKEN_LIFETIME_DAYS = 30;
    private static final long PASSWORD_RESET_TOKEN_LIFETIME_MINUTES = 30;

    private final UserRepository users;
    private final RefreshSessionRepository refreshSessions;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final OpaqueTokenGenerator opaqueTokens;
    private final PasswordResetDelivery passwordResetDelivery;

    @Inject
    public DefaultAuthenticationService(
            UserRepository users,
            RefreshSessionRepository refreshSessions,
            PasswordResetTokenRepository passwordResetTokens,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer,
            OpaqueTokenGenerator opaqueTokens,
            PasswordResetDelivery passwordResetDelivery) {
        this.users = Objects.requireNonNull(users);
        this.refreshSessions = Objects.requireNonNull(refreshSessions);
        this.passwordResetTokens = Objects.requireNonNull(passwordResetTokens);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
        this.opaqueTokens = Objects.requireNonNull(opaqueTokens);
        this.passwordResetDelivery = Objects.requireNonNull(passwordResetDelivery);
    }

    @Override
    public Uni<RegisteredUser> register(RegisterCommand command) {
        Objects.requireNonNull(command, "registration is required");
        Objects.requireNonNull(command.email(), "email is required");
        Objects.requireNonNull(command.role(), "role is required");
        PasswordPolicy.validate(command.password());

        return users.findByEmail(command.email())
                .onItem().transformToUni(existing -> {
                    if (existing.isPresent()) {
                        return Uni.createFrom().failure(new DuplicateAccountException());
                    }
                    User user = new User(UUID.randomUUID(), command.email(), passwordHasher.hash(command.password()),
                            command.role(), Instant.now());
                    return users.save(user).map(saved -> new RegisteredUser(
                            saved.id().toString(), saved.email(), saved.role()));
                });
    }

    @Override
    public Uni<AuthenticatedSession> login(LoginCommand command) {
        Objects.requireNonNull(command, "login is required");
        Objects.requireNonNull(command.email(), "email is required");
        Objects.requireNonNull(command.password(), "password is required");

        return users.findByEmail(command.email())
                .onItem().transformToUni(user -> user.filter(candidate ->
                                passwordHasher.matches(command.password(), candidate.passwordHash()))
                        .map(this::createSession)
                        .orElseGet(() -> Uni.createFrom().failure(new AuthenticationFailureException())));
    }

    @Override
    public Uni<AuthenticatedSession> refresh(String refreshToken) {
        return requireRefreshSession(refreshToken)
                .onItem().transformToUni(session -> users.findById(session.userId())
                        .onItem().transformToUni(user -> user.map(found -> rotateSession(session, found))
                                .orElseGet(() -> Uni.createFrom().failure(new AuthenticationFailureException()))));
    }

    @Override
    public Uni<Void> logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Uni.createFrom().voidItem();
        }
        return refreshSessions.findByTokenHash(opaqueTokens.hash(refreshToken))
                .onItem().transformToUni(session -> session
                        .filter(candidate -> candidate.isUsableAt(Instant.now()))
                        .map(candidate -> refreshSessions.revokeById(candidate.id(), Instant.now()))
                        .orElseGet(() -> Uni.createFrom().voidItem()));
    }

    @Override
    public Uni<Void> requestPasswordRecovery(EmailAddress email) {
        Objects.requireNonNull(email, "email is required");
        return users.findByEmail(email)
                .onItem().transformToUni(user -> user.map(this::createAndDeliverPasswordResetToken)
                        // Account existence must not be observable through this operation.
                        .orElseGet(() -> Uni.createFrom().voidItem()));
    }

    @Override
    public Uni<Void> resetPassword(ResetPasswordCommand command) {
        Objects.requireNonNull(command, "password reset is required");
        requireNonBlank(command.token(), "token is required");
        PasswordPolicy.validate(command.password());
        String tokenHash = opaqueTokens.hash(command.token());

        return passwordResetTokens.findByTokenHash(tokenHash)
                .onItem().transformToUni(token -> token.filter(candidate -> candidate.isUsableAt(Instant.now()))
                        .map(candidate -> replacePasswordAndInvalidateSessions(candidate, command.password()))
                        .orElseGet(() -> Uni.createFrom().failure(new AuthenticationFailureException())));
    }

    private Uni<AuthenticatedSession> createSession(User user) {
        String refreshToken = opaqueTokens.newToken();
        Instant now = Instant.now();
        RefreshSession session = new RefreshSession(UUID.randomUUID(), user.id(), opaqueTokens.hash(refreshToken),
                now.plus(REFRESH_TOKEN_LIFETIME_DAYS, ChronoUnit.DAYS), now, null);
        return refreshSessions.save(session)
                .replaceWith(() -> new AuthenticatedSession(accessTokenIssuer.issue(user), refreshToken));
    }

    private Uni<AuthenticatedSession> rotateSession(RefreshSession previous, User user) {
        return refreshSessions.revokeById(previous.id(), Instant.now())
                .chain(() -> createSession(user));
    }

    private Uni<RefreshSession> requireRefreshSession(String refreshToken) {
        requireNonBlank(refreshToken, "refresh token is required");
        return refreshSessions.findByTokenHash(opaqueTokens.hash(refreshToken))
                .onItem().transformToUni(session -> session.filter(candidate -> candidate.isUsableAt(Instant.now()))
                        .map(Uni.createFrom()::item)
                        .orElseGet(() -> Uni.createFrom().failure(new AuthenticationFailureException())));
    }

    private Uni<Void> createAndDeliverPasswordResetToken(User user) {
        String rawToken = opaqueTokens.newToken();
        Instant now = Instant.now();
        PasswordResetToken token = new PasswordResetToken(UUID.randomUUID(), user.id(), opaqueTokens.hash(rawToken),
                now.plus(PASSWORD_RESET_TOKEN_LIFETIME_MINUTES, ChronoUnit.MINUTES), now, null);
        return passwordResetTokens.save(token)
                .chain(() -> passwordResetDelivery.deliver(user.email(), rawToken));
    }

    private Uni<Void> replacePasswordAndInvalidateSessions(PasswordResetToken token, String password) {
        Instant now = Instant.now();
        String replacementHash = passwordHasher.hash(password);
        return users.replacePasswordHash(token.userId(), replacementHash)
                .chain(() -> passwordResetTokens.consume(token.id(), now))
                .chain(() -> refreshSessions.revokeAllForUser(token.userId(), now));
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
