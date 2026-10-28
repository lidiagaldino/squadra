package com.squadra.platform.infra.db.repositories;

import com.squadra.platform.domain.entities.Tournament;
import com.squadra.platform.domain.enums.TournamentStatus;
import com.squadra.platform.domain.enums.TournamentVisibility;
import com.squadra.platform.domain.exceptions.TournamentNotFoundException;
import com.squadra.platform.domain.repositories.TournamentRepository;
import com.squadra.platform.infra.db.adapters.ReactiveJdbcSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@Dependent
public final class JdbcTournamentRepository extends ReactiveJdbcSupport implements TournamentRepository {
    public JdbcTournamentRepository(DataSource dataSource) { super(dataSource); }

    @Override
    public Uni<TournamentDetails> create(Tournament tournament) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                insertTournament(connection, tournament);
                return details(connection, tournament.id(), tournament.ownerOrganizerUserId()).orElseThrow();
            }
        });
    }

    @Override
    public Uni<List<TournamentSummary>> listForOwner(UUID ownerOrganizerUserId) {
        return blockingDatabaseCall(dataSource -> {
            String sql = baseSelect() + """
                    WHERE t.owner_organizer_user_id = ?
                    ORDER BY t.updated_at DESC
                    """;
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
                statement.setObject(1, ownerOrganizerUserId);
                try (var result = statement.executeQuery()) {
                    List<TournamentSummary> tournaments = new ArrayList<>();
                    while (result.next()) tournaments.add(new TournamentSummary(mapTournament(result), result.getInt("category_count")));
                    return tournaments;
                }
            }
        });
    }

    @Override
    public Uni<Optional<TournamentDetails>> findById(UUID tournamentId) {
        return blockingDatabaseCall(dataSource -> {
            String sql = baseSelect() + "WHERE t.id = ?";
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
                statement.setObject(1, tournamentId);
                try (var result = statement.executeQuery()) {
                    return result.next() ? Optional.of(new TournamentDetails(mapTournament(result), result.getInt("category_count"))) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Uni<List<TournamentSummary>> listAvailable(int limit, int offset) {
        return blockingDatabaseCall(dataSource -> {
            String sql = baseSelect() + """
                    WHERE t.status = 'OPEN' AND t.visibility = 'PUBLIC'
                    ORDER BY t.start_date ASC, t.id ASC
                    LIMIT ? OFFSET ?
                    """;
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                statement.setInt(2, offset);
                try (var result = statement.executeQuery()) {
                    List<TournamentSummary> available = new ArrayList<>();
                    while (result.next()) available.add(new TournamentSummary(mapTournament(result), result.getInt("category_count")));
                    return available;
                }
            }
        });
    }

    @Override
    public Uni<Optional<TournamentDetails>> findPublicOpen(UUID tournamentId) {
        return blockingDatabaseCall(dataSource -> {
            String sql = baseSelect() + "WHERE t.id = ? AND t.status = 'OPEN' AND t.visibility = 'PUBLIC'";
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
                statement.setObject(1, tournamentId);
                try (var result = statement.executeQuery()) {
                    return result.next() ? Optional.of(new TournamentDetails(mapTournament(result), result.getInt("category_count"))) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Uni<Optional<TournamentDetails>> findOwnedByOrganizer(UUID tournamentId, UUID ownerOrganizerUserId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                return details(connection, tournamentId, ownerOrganizerUserId);
            }
        });
    }

    @Override
    public Uni<TournamentDetails> save(Tournament tournament) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
                    UPDATE squadra.tournaments
                    SET status = ?, name = ?, description = ?, city = ?, state = ?, venue_name = ?,
                        start_date = ?, end_date = ?, registration_deadline = ?, rules = ?,
                        published_at = ?, closed_at = ?, cancelled_at = ?, updated_at = ?, visibility = ?
                    WHERE id = ? AND owner_organizer_user_id = ?
                    """)) {
                statement.setString(1, tournament.status().name());
                statement.setString(2, tournament.name());
                statement.setString(3, tournament.description());
                statement.setString(4, tournament.city());
                statement.setString(5, tournament.state());
                statement.setString(6, tournament.venueName());
                setLocalDate(statement, 7, tournament.startDate());
                setLocalDate(statement, 8, tournament.endDate());
                setLocalDate(statement, 9, tournament.registrationDeadline());
                statement.setString(10, tournament.rules());
                setInstant(statement, 11, tournament.publishedAt());
                setInstant(statement, 12, tournament.closedAt());
                setInstant(statement, 13, tournament.cancelledAt());
                setInstant(statement, 14, tournament.updatedAt());
                statement.setString(15, tournament.visibility().name());
                statement.setObject(16, tournament.id());
                statement.setObject(17, tournament.ownerOrganizerUserId());
                statement.executeUpdate();
                return details(connection, tournament.id(), tournament.ownerOrganizerUserId()).orElseThrow(TournamentNotFoundException::new);
            }
        });
    }

    private static Optional<TournamentDetails> details(Connection connection, UUID tournamentId, UUID ownerOrganizerUserId) throws SQLException {
        String sql = baseSelect() + "WHERE t.id = ? AND t.owner_organizer_user_id = ?";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, tournamentId);
            statement.setObject(2, ownerOrganizerUserId);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(new TournamentDetails(mapTournament(result), result.getInt("category_count"))) : Optional.empty();
            }
        }
    }

    private static String baseSelect() {
        return """
                SELECT t.id, t.owner_organizer_user_id, t.status, t.name, t.description, t.city, t.state, t.venue_name,
                       t.start_date, t.end_date, t.registration_deadline, t.rules,
                       t.published_at, t.closed_at, t.cancelled_at, t.created_at, t.updated_at, t.visibility,
                       (
                           SELECT count(*)::int
                           FROM squadra.tournament_categories c
                           WHERE c.tournament_id = t.id
                       ) AS category_count
                FROM squadra.tournaments t
                """;
    }

    private static void insertTournament(Connection connection, Tournament tournament) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO squadra.tournaments (
                    id, owner_organizer_user_id, status, name, description, city, state, venue_name,
                    start_date, end_date, registration_deadline, rules, published_at, closed_at, cancelled_at, created_at, updated_at, visibility
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setObject(1, tournament.id());
            statement.setObject(2, tournament.ownerOrganizerUserId());
            statement.setString(3, tournament.status().name());
            statement.setString(4, tournament.name());
            statement.setString(5, tournament.description());
            statement.setString(6, tournament.city());
            statement.setString(7, tournament.state());
            statement.setString(8, tournament.venueName());
            setLocalDate(statement, 9, tournament.startDate());
            setLocalDate(statement, 10, tournament.endDate());
            setLocalDate(statement, 11, tournament.registrationDeadline());
            statement.setString(12, tournament.rules());
            setInstant(statement, 13, tournament.publishedAt());
            setInstant(statement, 14, tournament.closedAt());
            setInstant(statement, 15, tournament.cancelledAt());
            setInstant(statement, 16, tournament.createdAt());
            setInstant(statement, 17, tournament.updatedAt());
            statement.setString(18, tournament.visibility().name());
            statement.executeUpdate();
        }
    }

    private static Tournament mapTournament(ResultSet result) throws SQLException {
        return new Tournament(result.getObject("id", UUID.class), result.getObject("owner_organizer_user_id", UUID.class),
                TournamentStatus.valueOf(result.getString("status")),
                TournamentVisibility.valueOf(result.getString("visibility")),
                new Tournament.SaveFields(result.getString("name"), result.getString("description"),
                        result.getString("city"), result.getString("state"), result.getString("venue_name"),
                        result.getObject("start_date", LocalDate.class), result.getObject("end_date", LocalDate.class),
                        result.getObject("registration_deadline", LocalDate.class), result.getString("rules"),
                        TournamentVisibility.valueOf(result.getString("visibility"))),
                getInstant(result, "published_at"), getInstant(result, "closed_at"), getInstant(result, "cancelled_at"),
                getInstant(result, "created_at"), getInstant(result, "updated_at"));
    }
}
