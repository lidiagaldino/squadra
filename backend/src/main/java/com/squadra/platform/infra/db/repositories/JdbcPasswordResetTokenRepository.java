package com.squadra.platform.infra.db.repositories;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.entities.PasswordResetToken;
import com.squadra.platform.domain.repositories.PasswordResetTokenRepository;
import com.squadra.platform.infra.db.adapters.ReactiveJdbcSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Dependent
public class JdbcPasswordResetTokenRepository extends ReactiveJdbcSupport implements PasswordResetTokenRepository {
    public JdbcPasswordResetTokenRepository(DataSource dataSource) { super(dataSource); }

    @Override public Uni<Optional<PasswordResetToken>> findByTokenHash(String tokenHash) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                    "SELECT id, user_id, token_hash, expires_at, created_at, used_at FROM squadra.password_reset_tokens WHERE token_hash = ?")) {
                statement.setString(1, tokenHash);
                try (var result = statement.executeQuery()) {
                    return result.next() ? Optional.of(new PasswordResetToken(result.getObject("id", UUID.class), result.getObject("user_id", UUID.class),
                            result.getString("token_hash"), getInstant(result, "expires_at"), getInstant(result, "created_at"), getInstant(result, "used_at"))) : Optional.empty();
                }
            }
        });
    }

    @Override public Uni<PasswordResetToken> save(PasswordResetToken token) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                    "INSERT INTO squadra.password_reset_tokens (id, user_id, token_hash, expires_at, used_at, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                statement.setObject(1, token.id()); statement.setObject(2, token.userId()); statement.setString(3, token.tokenHash());
                setInstant(statement, 4, token.expiresAt()); setInstant(statement, 5, token.usedAt()); setInstant(statement, 6, token.createdAt());
                statement.executeUpdate(); return token;
            }
        });
    }

    @Override public Uni<Void> consume(UUID tokenId, Instant usedAt) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                    "UPDATE squadra.password_reset_tokens SET used_at = ? WHERE id = ? AND used_at IS NULL")) {
                setInstant(statement, 1, usedAt); statement.setObject(2, tokenId); statement.executeUpdate(); return null;
            }
        });
    }
}
