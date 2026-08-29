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

public final class User {
    private final UUID id;
    private final EmailAddress email;
    private final UserRole role;
    private final Instant createdAt;
    private String passwordHash;

    public User(UUID id, EmailAddress email, String passwordHash, UserRole role, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.email = Objects.requireNonNull(email, "email is required");
        this.passwordHash = requireHash(passwordHash);
        this.role = Objects.requireNonNull(role, "role is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }

    public UUID id() { return id; }
    public EmailAddress email() { return email; }
    public String passwordHash() { return passwordHash; }
    public UserRole role() { return role; }
    public Instant createdAt() { return createdAt; }

    public void replacePasswordHash(String replacement) {
        this.passwordHash = requireHash(replacement);
    }

    private static String requireHash(String hash) {
        Objects.requireNonNull(hash, "password hash is required");
        if (hash.isBlank()) {
            throw new IllegalArgumentException("password hash is required");
        }
        return hash;
    }
}
