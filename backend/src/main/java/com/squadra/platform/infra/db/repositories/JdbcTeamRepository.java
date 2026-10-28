package com.squadra.platform.infra.db.repositories;

import com.squadra.platform.domain.entities.Team;
import com.squadra.platform.domain.entities.TeamAdministrator;
import com.squadra.platform.domain.enums.TeamAdminRole;
import com.squadra.platform.domain.enums.TeamAthleteMembershipStatus;
import com.squadra.platform.domain.exceptions.TeamNotFoundException;
import com.squadra.platform.domain.repositories.TeamRepository;
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
public final class JdbcTeamRepository extends ReactiveJdbcSupport implements TeamRepository {
    public JdbcTeamRepository(DataSource dataSource) { super(dataSource); }

    @Override
    public Uni<TeamDetails> createWithOwner(Team team, TeamAdministrator owner) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    insertTeam(connection, team);
                    upsertAdminBlocking(connection, team.id(), owner.userId(), owner.role());
                    TeamDetails details = details(connection, team.id(), owner.userId()).orElseThrow();
                    connection.commit();
                    return details;
                } catch (SQLException | RuntimeException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
        });
    }

    @Override
    public Uni<List<TeamSummary>> listForUser(UUID userId) {
        return blockingDatabaseCall(dataSource -> {
            String sql = """
                    SELECT DISTINCT t.id, t.owner_user_id, t.name, t.city, t.state, t.founded_year, t.description, t.created_at, t.updated_at,
                           a.role AS administration_role, m.status AS athlete_membership_status
                    FROM squadra.teams t
                    LEFT JOIN squadra.team_administrators a ON a.team_id = t.id AND a.user_id = ?
                    LEFT JOIN squadra.team_athlete_memberships m ON m.team_id = t.id AND m.athlete_user_id = ?
                    WHERE a.user_id IS NOT NULL OR m.athlete_user_id IS NOT NULL
                    ORDER BY t.updated_at DESC
                    """;
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
                statement.setObject(1, userId);
                statement.setObject(2, userId);
                try (var result = statement.executeQuery()) {
                    List<TeamSummary> teams = new ArrayList<>();
                    while (result.next()) {
                        teams.add(new TeamSummary(mapTeam(result), adminRole(result, "administration_role"), athleteStatus(result, "athlete_membership_status")));
                    }
                    return teams;
                }
            }
        });
    }

    @Override
    public Uni<List<TeamDirectorySummary>> searchDirectory(String query, int limit, int offset) {
        return blockingDatabaseCall(dataSource -> {
            String sql = """
                    SELECT id, name, city, state
                    FROM squadra.teams
                    WHERE lower(name) LIKE lower(?)
                    ORDER BY name ASC, id ASC
                    LIMIT ? OFFSET ?
                    """;
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
                statement.setString(1, "%" + query.replace("%", "\\%").replace("_", "\\_") + "%");
                statement.setInt(2, limit);
                statement.setInt(3, offset);
                try (var result = statement.executeQuery()) {
                    List<TeamDirectorySummary> teams = new ArrayList<>();
                    while (result.next()) teams.add(new TeamDirectorySummary(result.getObject("id", UUID.class),
                            result.getString("name"), result.getString("city"), result.getString("state")));
                    return teams;
                }
            }
        });
    }

    @Override
    public Uni<Boolean> directoryTeamExists(UUID teamId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("SELECT 1 FROM squadra.teams WHERE id = ?")) {
                statement.setObject(1, teamId);
                try (var result = statement.executeQuery()) { return result.next(); }
            }
        });
    }

    @Override
    public Uni<Optional<TeamDetails>> findVisibleToUser(UUID teamId, UUID userId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                return details(connection, teamId, userId);
            }
        });
    }

    @Override
    public Uni<TeamDetails> saveTeam(Team team, UUID viewerUserId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection();
                    var statement = connection.prepareStatement("""
                            UPDATE squadra.teams SET name = ?, city = ?, state = ?, founded_year = ?, description = ?, updated_at = ?
                            WHERE id = ?
                            """)) {
                statement.setString(1, team.name());
                statement.setString(2, team.city());
                statement.setString(3, team.state());
                if (team.foundedYear() == null) statement.setObject(4, null); else statement.setInt(4, team.foundedYear());
                statement.setString(5, team.description());
                setInstant(statement, 6, team.updatedAt());
                statement.setObject(7, team.id());
                statement.executeUpdate();
                return details(connection, team.id(), viewerUserId).orElseThrow(TeamNotFoundException::new);
            }
        });
    }

    @Override
    public Uni<TeamDetails> upsertAdmin(UUID teamId, UUID userId, TeamAdminRole role, UUID viewerUserId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                upsertAdminBlocking(connection, teamId, userId, role);
                return details(connection, teamId, viewerUserId).orElseThrow(TeamNotFoundException::new);
            }
        });
    }

    @Override
    public Uni<TeamDetails> removeAdmin(UUID teamId, UUID userId, UUID viewerUserId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection();
                    var statement = connection.prepareStatement("DELETE FROM squadra.team_administrators WHERE team_id = ? AND user_id = ? AND role <> 'TEAM_OWNER'")) {
                statement.setObject(1, teamId);
                statement.setObject(2, userId);
                statement.executeUpdate();
                return details(connection, teamId, viewerUserId).orElseThrow(TeamNotFoundException::new);
            }
        });
    }

    @Override
    public Uni<TeamDetails> transferOwnership(UUID teamId, UUID fromUserId, UUID toUserId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    try (var team = connection.prepareStatement("UPDATE squadra.teams SET owner_user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND owner_user_id = ?")) {
                        team.setObject(1, toUserId);
                        team.setObject(2, teamId);
                        team.setObject(3, fromUserId);
                        if (team.executeUpdate() != 1) throw new TeamNotFoundException();
                    }
                    upsertAdminBlocking(connection, teamId, fromUserId, TeamAdminRole.TEAM_ADMIN);
                    upsertAdminBlocking(connection, teamId, toUserId, TeamAdminRole.TEAM_OWNER);
                    TeamDetails details = details(connection, teamId, fromUserId).orElseThrow(TeamNotFoundException::new);
                    connection.commit();
                    return details;
                } catch (SQLException | RuntimeException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
        });
    }

    @Override
    public Uni<TeamDetails> upsertAthleteMembership(UUID teamId, UUID athleteUserId, TeamAthleteMembershipStatus status, UUID viewerUserId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                upsertAthleteBlocking(connection, teamId, athleteUserId, status);
                return details(connection, teamId, viewerUserId).orElseThrow(TeamNotFoundException::new);
            }
        });
    }

    @Override
    public Uni<TeamDetails> activateInvitation(UUID teamId, UUID athleteUserId) {
        return upsertAthleteMembership(teamId, athleteUserId, TeamAthleteMembershipStatus.ACTIVE, athleteUserId);
    }

    @Override
    public Uni<TeamDetails> removeAthleteMembership(UUID teamId, UUID athleteUserId, UUID viewerUserId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection();
                    var statement = connection.prepareStatement("DELETE FROM squadra.team_athlete_memberships WHERE team_id = ? AND athlete_user_id = ?")) {
                statement.setObject(1, teamId);
                statement.setObject(2, athleteUserId);
                statement.executeUpdate();
                return details(connection, teamId, viewerUserId).orElseThrow(TeamNotFoundException::new);
            }
        });
    }

    @Override
    public Uni<Void> deleteAthleteMembership(UUID teamId, UUID athleteUserId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection();
                    var statement = connection.prepareStatement("DELETE FROM squadra.team_athlete_memberships WHERE team_id = ? AND athlete_user_id = ?")) {
                statement.setObject(1, teamId);
                statement.setObject(2, athleteUserId);
                statement.executeUpdate();
                return null;
            }
        });
    }

    private static void insertTeam(Connection connection, Team team) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO squadra.teams (id, owner_user_id, name, city, state, founded_year, description, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setObject(1, team.id());
            statement.setObject(2, team.ownerUserId());
            statement.setString(3, team.name());
            statement.setString(4, team.city());
            statement.setString(5, team.state());
            if (team.foundedYear() == null) statement.setObject(6, null); else statement.setInt(6, team.foundedYear());
            statement.setString(7, team.description());
            setInstant(statement, 8, team.createdAt());
            setInstant(statement, 9, team.updatedAt());
            statement.executeUpdate();
        }
    }

    private static void upsertAdminBlocking(Connection connection, UUID teamId, UUID userId, TeamAdminRole role) throws SQLException {
        try (var update = connection.prepareStatement("UPDATE squadra.team_administrators SET role = ?, updated_at = CURRENT_TIMESTAMP WHERE team_id = ? AND user_id = ?")) {
            update.setString(1, role.name());
            update.setObject(2, teamId);
            update.setObject(3, userId);
            if (update.executeUpdate() == 1) return;
        }
        try (var insert = connection.prepareStatement("""
                INSERT INTO squadra.team_administrators (team_id, user_id, role, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)) {
            insert.setObject(1, teamId);
            insert.setObject(2, userId);
            insert.setString(3, role.name());
            insert.executeUpdate();
        }
    }

    private static void upsertAthleteBlocking(Connection connection, UUID teamId, UUID userId, TeamAthleteMembershipStatus status) throws SQLException {
        try (var update = connection.prepareStatement("UPDATE squadra.team_athlete_memberships SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE team_id = ? AND athlete_user_id = ?")) {
            update.setString(1, status.name());
            update.setObject(2, teamId);
            update.setObject(3, userId);
            if (update.executeUpdate() == 1) return;
        }
        try (var insert = connection.prepareStatement("""
                INSERT INTO squadra.team_athlete_memberships (team_id, athlete_user_id, status, invited_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)) {
            insert.setObject(1, teamId);
            insert.setObject(2, userId);
            insert.setString(3, status.name());
            insert.executeUpdate();
        }
    }

    private static Optional<TeamDetails> details(Connection connection, UUID teamId, UUID viewerUserId) throws SQLException {
        Team team = findTeam(connection, teamId).orElse(null);
        if (team == null) return Optional.empty();
        TeamAdminRole adminRole = findAdminRole(connection, teamId, viewerUserId);
        TeamAthleteMembershipStatus status = findAthleteStatus(connection, teamId, viewerUserId);
        if (adminRole == null && status == null) return Optional.empty();
        return Optional.of(new TeamDetails(team, adminRole, status, admins(connection, teamId), athletes(connection, teamId)));
    }

    private static Optional<Team> findTeam(Connection connection, UUID teamId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT id, owner_user_id, name, city, state, founded_year, description, created_at, updated_at FROM squadra.teams WHERE id = ?")) {
            statement.setObject(1, teamId);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapTeam(result)) : Optional.empty();
            }
        }
    }

    private static TeamAdminRole findAdminRole(Connection connection, UUID teamId, UUID userId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT role FROM squadra.team_administrators WHERE team_id = ? AND user_id = ?")) {
            statement.setObject(1, teamId);
            statement.setObject(2, userId);
            try (var result = statement.executeQuery()) {
                return result.next() ? TeamAdminRole.valueOf(result.getString("role")) : null;
            }
        }
    }

    private static TeamAthleteMembershipStatus findAthleteStatus(Connection connection, UUID teamId, UUID userId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT status FROM squadra.team_athlete_memberships WHERE team_id = ? AND athlete_user_id = ?")) {
            statement.setObject(1, teamId);
            statement.setObject(2, userId);
            try (var result = statement.executeQuery()) {
                return result.next() ? TeamAthleteMembershipStatus.valueOf(result.getString("status")) : null;
            }
        }
    }

    private static List<AdminView> admins(Connection connection, UUID teamId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT a.user_id, u.email, a.role FROM squadra.team_administrators a JOIN squadra.users u ON u.id = a.user_id
                WHERE a.team_id = ? ORDER BY CASE a.role WHEN 'TEAM_OWNER' THEN 0 ELSE 1 END, u.email
                """)) {
            statement.setObject(1, teamId);
            try (var result = statement.executeQuery()) {
                List<AdminView> items = new ArrayList<>();
                while (result.next()) items.add(new AdminView(result.getObject("user_id", UUID.class), result.getString("email"), TeamAdminRole.valueOf(result.getString("role"))));
                return List.copyOf(items);
            }
        }
    }

    private static List<AthleteView> athletes(Connection connection, UUID teamId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT m.athlete_user_id, u.email, m.status FROM squadra.team_athlete_memberships m JOIN squadra.users u ON u.id = m.athlete_user_id
                WHERE m.team_id = ? ORDER BY CASE m.status WHEN 'ACTIVE' THEN 0 ELSE 1 END, u.email
                """)) {
            statement.setObject(1, teamId);
            try (var result = statement.executeQuery()) {
                List<AthleteView> items = new ArrayList<>();
                while (result.next()) items.add(new AthleteView(result.getObject("athlete_user_id", UUID.class), result.getString("email"), TeamAthleteMembershipStatus.valueOf(result.getString("status"))));
                return List.copyOf(items);
            }
        }
    }

    private static Team mapTeam(ResultSet result) throws SQLException {
        Object foundedYear = result.getObject("founded_year");
        return new Team(result.getObject("id", UUID.class), result.getObject("owner_user_id", UUID.class),
                result.getString("name"), result.getString("city"), result.getString("state"),
                foundedYear == null ? null : ((Number) foundedYear).intValue(), result.getString("description"),
                getInstant(result, "created_at"), getInstant(result, "updated_at"));
    }

    private static TeamAdminRole adminRole(ResultSet result, String column) throws SQLException {
        String value = result.getString(column);
        return value == null ? null : TeamAdminRole.valueOf(value);
    }

    private static TeamAthleteMembershipStatus athleteStatus(ResultSet result, String column) throws SQLException {
        String value = result.getString(column);
        return value == null ? null : TeamAthleteMembershipStatus.valueOf(value);
    }
}
