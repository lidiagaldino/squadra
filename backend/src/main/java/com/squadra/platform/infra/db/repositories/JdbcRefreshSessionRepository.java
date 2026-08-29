package com.squadra.platform.infra.db.repositories;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.entities.RefreshSession;
import com.squadra.platform.domain.repositories.RefreshSessionRepository;
import com.squadra.platform.infra.db.adapters.ReactiveJdbcSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Dependent
public class JdbcRefreshSessionRepository extends ReactiveJdbcSupport implements RefreshSessionRepository {
    public JdbcRefreshSessionRepository(DataSource dataSource) { super(dataSource); }

    @Override public Uni<Optional<RefreshSession>> findByTokenHash(String tokenHash) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                    "SELECT id, user_id, token_hash, expires_at, created_at, revoked_at FROM squadra.refresh_tokens WHERE token_hash = ?")) {
                statement.setString(1, tokenHash);
                try (var result = statement.executeQuery()) {
                    return result.next() ? Optional.of(new RefreshSession(result.getObject("id", UUID.class), result.getObject("user_id", UUID.class),
                            result.getString("token_hash"), getInstant(result, "expires_at"), getInstant(result, "created_at"), getInstant(result, "revoked_at"))) : Optional.empty();
                }
            }
        });
    }

    @Override public Uni<RefreshSession> save(RefreshSession session) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                    "INSERT INTO squadra.refresh_tokens (id, user_id, token_hash, expires_at, revoked_at, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                statement.setObject(1, session.id()); statement.setObject(2, session.userId()); statement.setString(3, session.tokenHash());
                setInstant(statement, 4, session.expiresAt()); setInstant(statement, 5, session.revokedAt()); setInstant(statement, 6, session.createdAt());
                statement.executeUpdate(); return session;
            }
        });
    }

    @Override public Uni<Void> revokeById(UUID sessionId, Instant revokedAt) { return revoke("UPDATE squadra.refresh_tokens SET revoked_at = ? WHERE id = ? AND revoked_at IS NULL", revokedAt, sessionId); }
    @Override public Uni<Void> revokeAllForUser(UUID userId, Instant revokedAt) { return revoke("UPDATE squadra.refresh_tokens SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL", revokedAt, userId); }

    private Uni<Void> revoke(String sql, Instant at, UUID id) {
        return blockingDatabaseCall(dataSource -> { try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) { setInstant(statement, 1, at); statement.setObject(2, id); statement.executeUpdate(); return null; } });
    }
}
