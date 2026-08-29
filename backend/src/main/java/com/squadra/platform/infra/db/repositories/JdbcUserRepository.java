package com.squadra.platform.infra.db.repositories;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.valueobjects.EmailAddress;
import com.squadra.platform.domain.entities.User;
import com.squadra.platform.domain.repositories.UserRepository;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.infra.db.adapters.ReactiveJdbcSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Dependent
public class JdbcUserRepository extends ReactiveJdbcSupport implements UserRepository {
    public JdbcUserRepository(DataSource dataSource) { super(dataSource); }

    @Override
    public Uni<Optional<User>> findByEmail(EmailAddress email) {
        return find("SELECT id, email, password_hash, role, created_at FROM squadra.users WHERE email = ?", email.value());
    }

    @Override
    public Uni<Optional<User>> findById(UUID id) {
        return find("SELECT id, email, password_hash, role, created_at FROM squadra.users WHERE id = ?", id);
    }

    private Uni<Optional<User>> find(String sql, Object argument) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, argument);
                try (ResultSet results = statement.executeQuery()) {
                    return results.next() ? Optional.of(map(results)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Uni<User> save(User user) {
        return blockingDatabaseCall(dataSource -> {
            String sql = "INSERT INTO squadra.users (id, email, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)";
            try (var connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, user.id());
                statement.setString(2, user.email().value());
                statement.setString(3, user.passwordHash());
                statement.setString(4, user.role().name());
                setInstant(statement, 5, user.createdAt());
                statement.executeUpdate();
                return user;
            }
        });
    }

    @Override
    public Uni<Void> replacePasswordHash(UUID userId, String passwordHash) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "UPDATE squadra.users SET password_hash = ? WHERE id = ?")) {
                statement.setString(1, passwordHash);
                statement.setObject(2, userId);
                if (statement.executeUpdate() != 1) throw new IllegalArgumentException("User does not exist");
                return null;
            }
        });
    }

    private static User map(ResultSet result) throws java.sql.SQLException {
        return new User(result.getObject("id", UUID.class), new EmailAddress(result.getString("email")),
                result.getString("password_hash"), UserRole.valueOf(result.getString("role")),
                getInstant(result, "created_at"));
    }
}
