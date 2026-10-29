package com.squadra.platform.infra.db.repositories;

import com.squadra.platform.domain.entities.TournamentInvitation;
import com.squadra.platform.domain.entities.TeamRegistration;
import com.squadra.platform.domain.enums.TournamentInvitationStatus;
import com.squadra.platform.domain.exceptions.TournamentInvitationConflictException;
import com.squadra.platform.domain.exceptions.TournamentInvitationNotFoundException;
import com.squadra.platform.domain.repositories.TournamentInvitationRepository;
import com.squadra.platform.infra.db.adapters.ReactiveJdbcSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@Dependent
public final class JdbcTournamentInvitationRepository extends ReactiveJdbcSupport implements TournamentInvitationRepository {
    public JdbcTournamentInvitationRepository(DataSource dataSource) { super(dataSource); }

    @Override
    public Uni<InvitationDetails> create(TournamentInvitation invitation) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    // Acceptance and public registrations use the same category lock.
                    // Recheck inside this transaction: the service precheck can become stale.
                    JdbcTeamRegistrationRepository.requireCategoryLock(connection, invitation.categoryId());
                    try (var active = connection.prepareStatement("SELECT 1 FROM squadra.tournament_registrations WHERE category_id = ? AND team_id = ? AND active_key = TRUE")) {
                        active.setObject(1, invitation.categoryId());
                        active.setObject(2, invitation.teamId());
                        try (var result = active.executeQuery()) {
                            if (result.next()) throw new TournamentInvitationConflictException("Team already has an active registration");
                        }
                    }
                    try (var statement = connection.prepareStatement("""
                            INSERT INTO squadra.tournament_invitations (
                                id, tournament_id, category_id, team_id, inviter_user_id, status, pending_key, created_at, updated_at, decided_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """)) {
                        statement.setObject(1, invitation.id());
                        statement.setObject(2, invitation.tournamentId());
                        statement.setObject(3, invitation.categoryId());
                        statement.setObject(4, invitation.teamId());
                        statement.setObject(5, invitation.inviterUserId());
                        statement.setString(6, invitation.status().name());
                        statement.setBoolean(7, true);
                        setInstant(statement, 8, invitation.createdAt());
                        setInstant(statement, 9, invitation.updatedAt());
                        setInstant(statement, 10, invitation.decidedAt());
                        statement.executeUpdate();
                    }
                    InvitationDetails details = find(connection, invitation.id()).orElseThrow(TournamentInvitationNotFoundException::new);
                    connection.commit();
                    return details;
                } catch (Exception exception) {
                    connection.rollback();
                    if (exception instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                        throw new TournamentInvitationConflictException("A pending invitation already exists");
                    }
                    throw exception;
                }
            }
        });
    }

    @Override
    public Uni<InvitationDetails> acceptWithRegistration(TournamentInvitation invitation, TeamRegistration registration,
            int maxTeams) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    if (!invitation.teamId().equals(registration.teamId()) ||
                            !invitation.tournamentId().equals(registration.tournamentId()) ||
                            !invitation.categoryId().equals(registration.categoryId()) ||
                            invitation.status() != TournamentInvitationStatus.ACCEPTED) {
                        throw new IllegalArgumentException("Invitation and registration context do not match");
                    }
                    JdbcTeamRegistrationRepository.requireCategoryLock(connection, invitation.categoryId());
                    try (var lock = connection.prepareStatement("SELECT status FROM squadra.tournament_invitations WHERE id = ? FOR UPDATE")) {
                        lock.setObject(1, invitation.id());
                        try (var result = lock.executeQuery()) {
                            if (!result.next() || !TournamentInvitationStatus.PENDING.name().equals(result.getString(1))) {
                                throw new TournamentInvitationConflictException("Invitation is no longer pending");
                            }
                        }
                    }
                    JdbcTeamRegistrationRepository.ensureCapacity(connection, registration.categoryId(), registration.teamId(), maxTeams);
                    JdbcTeamRegistrationRepository.insert(connection, registration);
                    try (var update = connection.prepareStatement("""
                            UPDATE squadra.tournament_invitations
                            SET status = 'ACCEPTED', pending_key = NULL, updated_at = ?, decided_at = ?
                            WHERE id = ? AND status = 'PENDING'
                            """)) {
                        setInstant(update, 1, invitation.updatedAt());
                        setInstant(update, 2, invitation.decidedAt());
                        update.setObject(3, invitation.id());
                        if (update.executeUpdate() != 1) throw new TournamentInvitationConflictException("Invitation is no longer pending");
                    }
                    connection.commit();
                    return find(connection, invitation.id()).orElseThrow(TournamentInvitationNotFoundException::new);
                } catch (Exception exception) {
                    connection.rollback();
                    if (exception instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                        throw new TournamentInvitationConflictException("Team already has an active registration or invitation decision");
                    }
                    throw exception;
                }
            }
        });
    }

    @Override
    public Uni<List<InvitationDetails>> listByCategory(UUID tournamentId, UUID categoryId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(select() + """
                    WHERE i.tournament_id = ? AND i.category_id = ? ORDER BY i.created_at ASC
                    """)) {
                statement.setObject(1, tournamentId);
                statement.setObject(2, categoryId);
                return list(statement.executeQuery());
            }
        });
    }

    @Override
    public Uni<List<InvitationDetails>> listByTeam(UUID teamId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(select() + """
                    WHERE i.team_id = ? ORDER BY i.created_at DESC
                    """)) {
                statement.setObject(1, teamId);
                return list(statement.executeQuery());
            }
        });
    }

    @Override
    public Uni<Optional<InvitationDetails>> findById(UUID invitationId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) { return find(connection, invitationId); }
        });
    }

    @Override
    public Uni<InvitationDetails> save(TournamentInvitation invitation) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
                    UPDATE squadra.tournament_invitations
                    SET status = ?, pending_key = ?, updated_at = ?, decided_at = ?
                    WHERE id = ? AND status = 'PENDING'
                    """)) {
                statement.setString(1, invitation.status().name());
                if (invitation.status() == TournamentInvitationStatus.PENDING) statement.setBoolean(2, true);
                else statement.setNull(2, java.sql.Types.BOOLEAN);
                setInstant(statement, 3, invitation.updatedAt());
                setInstant(statement, 4, invitation.decidedAt());
                statement.setObject(5, invitation.id());
                if (statement.executeUpdate() == 0) throw new TournamentInvitationConflictException("Invitation is no longer pending");
                return find(connection, invitation.id()).orElseThrow(TournamentInvitationNotFoundException::new);
            }
        });
    }

    @Override
    public Uni<Boolean> hasTeamAdminAccess(UUID tournamentId, UUID userId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
                    SELECT 1
                    FROM squadra.team_administrators a
                    WHERE a.user_id = ? AND (
                        EXISTS (
                            SELECT 1 FROM squadra.tournament_invitations i
                            WHERE i.team_id = a.team_id AND i.tournament_id = ? AND i.status IN ('PENDING', 'ACCEPTED')
                        ) OR EXISTS (
                            SELECT 1 FROM squadra.tournament_registrations r
                            WHERE r.team_id = a.team_id AND r.tournament_id = ? AND r.status NOT IN ('REJECTED', 'CANCELLED')
                        )
                    ) LIMIT 1
                    """)) {
                statement.setObject(1, userId);
                statement.setObject(2, tournamentId);
                statement.setObject(3, tournamentId);
                try (var result = statement.executeQuery()) { return result.next(); }
            }
        });
    }

    private static Optional<InvitationDetails> find(java.sql.Connection connection, UUID invitationId) throws SQLException {
        try (var statement = connection.prepareStatement(select() + "WHERE i.id = ?")) {
            statement.setObject(1, invitationId);
            try (var result = statement.executeQuery()) { return result.next() ? Optional.of(map(result)) : Optional.empty(); }
        }
    }

    private static List<InvitationDetails> list(ResultSet result) throws SQLException {
        try (result) {
            List<InvitationDetails> items = new ArrayList<>();
            while (result.next()) items.add(map(result));
            return items;
        }
    }

    private static String select() {
        return """
                SELECT i.id, i.tournament_id, i.category_id, i.team_id, i.inviter_user_id, i.status,
                       i.created_at, i.updated_at, i.decided_at, tm.name AS team_name,
                       t.name AS tournament_name, c.name AS category_name
                FROM squadra.tournament_invitations i
                JOIN squadra.teams tm ON tm.id = i.team_id
                JOIN squadra.tournaments t ON t.id = i.tournament_id
                JOIN squadra.tournament_categories c ON c.id = i.category_id
                """;
    }

    private static InvitationDetails map(ResultSet result) throws SQLException {
        TournamentInvitation invitation = new TournamentInvitation(result.getObject("id", UUID.class),
                result.getObject("tournament_id", UUID.class), result.getObject("category_id", UUID.class),
                result.getObject("team_id", UUID.class), result.getObject("inviter_user_id", UUID.class),
                TournamentInvitationStatus.valueOf(result.getString("status")), getInstant(result, "created_at"),
                getInstant(result, "updated_at"), getInstant(result, "decided_at"));
        return new InvitationDetails(invitation, result.getString("team_name"), result.getString("tournament_name"),
                result.getString("category_name"));
    }
}
