package com.squadra.platform.domain.entities;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RefreshSession {
    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant revokedAt;

    public RefreshSession(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant createdAt, Instant revokedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.tokenHash = requireTokenHash(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.revokedAt = revokedAt;
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String tokenHash() { return tokenHash; }
    public Instant expiresAt() { return expiresAt; }
    public Instant createdAt() { return createdAt; }
    public Instant revokedAt() { return revokedAt; }
    public boolean isUsableAt(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }
    public void revoke(Instant now) { if (revokedAt == null) revokedAt = Objects.requireNonNull(now, "now is required"); }

    private static String requireTokenHash(String value) {
        Objects.requireNonNull(value, "token hash is required");
        if (!value.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("token hash must be a SHA-256 hex digest");
        return value;
    }
}
