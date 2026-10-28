package com.squadra.platform.infra.db.repositories;

import com.squadra.platform.domain.entities.TeamRegistration;
import com.squadra.platform.domain.enums.RegistrationStatus;
import com.squadra.platform.domain.exceptions.TeamRegistrationConflictException;
import com.squadra.platform.domain.exceptions.TeamRegistrationNotFoundException;
import com.squadra.platform.domain.repositories.TeamRegistrationRepository;
import com.squadra.platform.infra.db.adapters.ReactiveJdbcSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@Dependent
public final class JdbcTeamRegistrationRepository extends ReactiveJdbcSupport implements TeamRegistrationRepository {
    public JdbcTeamRegistrationRepository(DataSource dataSource) { super(dataSource); }

    @Override
    public Uni<RegistrationDetails> create(TeamRegistration registration) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                try {
                    insert(connection, registration);
                } catch (SQLException exception) {
                    if ("23505".equals(exception.getSQLState())) {
                        throw new TeamRegistrationConflictException("Team is already registered in this category");
                    }
                    throw exception;
                }
                return find(connection, registration.id()).orElseThrow(TeamRegistrationNotFoundException::new);
            }
        });
    }

    @Override
    public Uni<RegistrationDetails> createWithinCapacity(TeamRegistration registration, int maxTeams) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    requireCategoryLock(connection, registration.categoryId());
                    ensureCapacity(connection, registration.categoryId(), registration.teamId(), maxTeams);
                    insert(connection, registration);
                    connection.commit();
                    return find(connection, registration.id()).orElseThrow(TeamRegistrationNotFoundException::new);
                } catch (Exception exception) {
                    connection.rollback();
                    if (exception instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                        throw new TeamRegistrationConflictException("Team already has an active registration or category is full");
                    }
                    throw exception;
                }
            }
        });
    }

    @Override
    public Uni<List<RegistrationDetails>> listByCategory(UUID tournamentId, UUID categoryId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(detailsSelect() + """
                    WHERE r.tournament_id = ? AND r.category_id = ?
                    ORDER BY r.created_at ASC
                    """)) {
                statement.setObject(1, tournamentId);
                statement.setObject(2, categoryId);
                return list(statement.executeQuery());
            }
        });
    }

    @Override
    public Uni<List<RegistrationDetails>> listByTeam(UUID teamId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(detailsSelect() + """
                    WHERE r.team_id = ?
                    ORDER BY r.created_at ASC
                    """)) {
                statement.setObject(1, teamId);
                return list(statement.executeQuery());
            }
        });
    }

    @Override
    public Uni<Optional<RegistrationDetails>> findByTeam(UUID teamId, UUID registrationId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(detailsSelect() + """
                    WHERE r.team_id = ? AND r.id = ?
                    """)) {
                statement.setObject(1, teamId);
                statement.setObject(2, registrationId);
                try (var result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Uni<Boolean> activeExists(UUID categoryId, UUID teamId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
                    SELECT 1
                    FROM squadra.tournament_registrations
                    WHERE category_id = ? AND team_id = ? AND status NOT IN ('REJECTED', 'CANCELLED')
                    LIMIT 1
                    """)) {
                statement.setObject(1, categoryId);
                statement.setObject(2, teamId);
                try (var result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }

    @Override
    public Uni<Integer> activeCountByCategory(UUID categoryId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
                    SELECT count(*)::int
                    FROM squadra.tournament_registrations
                    WHERE category_id = ? AND status NOT IN ('REJECTED', 'CANCELLED')
                    """)) {
                statement.setObject(1, categoryId);
                try (var result = statement.executeQuery()) {
                    result.next();
                    return result.getInt(1);
                }
            }
        });
    }

    @Override
    public Uni<RegistrationDetails> save(TeamRegistration registration) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
                    UPDATE squadra.tournament_registrations
                    SET status = ?, active_key = ?, updated_at = ?
                    WHERE id = ? AND team_id = ?
                    """)) {
                statement.setString(1, registration.status().name());
                if (registration.status() == RegistrationStatus.CANCELLED || registration.status() == RegistrationStatus.REJECTED) {
                    statement.setNull(2, java.sql.Types.BOOLEAN);
                } else {
                    statement.setBoolean(2, true);
                }
                setInstant(statement, 3, registration.updatedAt());
                statement.setObject(4, registration.id());
                statement.setObject(5, registration.teamId());
                if (statement.executeUpdate() == 0) throw new TeamRegistrationNotFoundException();
                return find(connection, registration.id()).orElseThrow(TeamRegistrationNotFoundException::new);
            }
        });
    }

    static void insert(Connection connection, TeamRegistration registration) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO squadra.tournament_registrations (
                    id, team_id, tournament_id, category_id, status, active_key, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, TRUE, ?, ?)
                """)) {
            statement.setObject(1, registration.id());
            statement.setObject(2, registration.teamId());
            statement.setObject(3, registration.tournamentId());
            statement.setObject(4, registration.categoryId());
            statement.setString(5, registration.status().name());
            setInstant(statement, 6, registration.createdAt());
            setInstant(statement, 7, registration.updatedAt());
            statement.executeUpdate();
        }
    }

    static void requireCategoryLock(Connection connection, UUID categoryId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT id FROM squadra.tournament_categories WHERE id = ? FOR UPDATE")) {
            statement.setObject(1, categoryId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) throw new TeamRegistrationConflictException("Category is no longer available");
            }
        }
    }

    static void ensureCapacity(Connection connection, UUID categoryId, UUID teamId, int maxTeams) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT count(*)::int,
                       sum(CASE WHEN team_id = ? THEN 1 ELSE 0 END)::int
                FROM squadra.tournament_registrations
                WHERE category_id = ? AND active_key = TRUE
                """)) {
            statement.setObject(1, teamId);
            statement.setObject(2, categoryId);
            try (var result = statement.executeQuery()) {
                result.next();
                if (result.getInt(2) > 0) throw new TeamRegistrationConflictException("Team is already registered in this category");
                if (result.getInt(1) >= maxTeams) throw new TeamRegistrationConflictException("Category team limit has been reached");
            }
        }
    }

    static Optional<RegistrationDetails> find(Connection connection, UUID registrationId) throws SQLException {
        try (var statement = connection.prepareStatement(detailsSelect() + "WHERE r.id = ?")) {
            statement.setObject(1, registrationId);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    private static List<RegistrationDetails> list(ResultSet result) throws SQLException {
        try (result) {
            List<RegistrationDetails> items = new ArrayList<>();
            while (result.next()) items.add(map(result));
            return items;
        }
    }

    private static String detailsSelect() {
        return """
                SELECT r.id, r.team_id, r.tournament_id, r.category_id, r.status, r.created_at, r.updated_at,
                       tm.name AS team_name, t.name AS tournament_name, c.name AS category_name
                FROM squadra.tournament_registrations r
                JOIN squadra.teams tm ON tm.id = r.team_id
                JOIN squadra.tournaments t ON t.id = r.tournament_id
                JOIN squadra.tournament_categories c ON c.id = r.category_id
                """;
    }

    private static RegistrationDetails map(ResultSet result) throws SQLException {
        TeamRegistration registration = new TeamRegistration(result.getObject("id", UUID.class),
                result.getObject("team_id", UUID.class), result.getObject("tournament_id", UUID.class),
                result.getObject("category_id", UUID.class), RegistrationStatus.valueOf(result.getString("status")),
                getInstant(result, "created_at"), getInstant(result, "updated_at"));
        return new RegistrationDetails(registration, result.getString("team_name"),
                result.getString("tournament_name"), result.getString("category_name"));
    }
}
