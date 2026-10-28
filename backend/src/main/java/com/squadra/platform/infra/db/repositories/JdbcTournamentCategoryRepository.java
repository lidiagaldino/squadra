package com.squadra.platform.infra.db.repositories;

import com.squadra.platform.domain.entities.TournamentCategory;
import com.squadra.platform.domain.enums.CategoryPaymentModel;
import com.squadra.platform.domain.enums.CompetitionFormat;
import com.squadra.platform.domain.enums.SchedulingStrategy;
import com.squadra.platform.domain.enums.TournamentModality;
import com.squadra.platform.domain.exceptions.TournamentCategoryConflictException;
import com.squadra.platform.domain.exceptions.TournamentCategoryNotFoundException;
import com.squadra.platform.domain.repositories.TournamentCategoryRepository;
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
public final class JdbcTournamentCategoryRepository extends ReactiveJdbcSupport implements TournamentCategoryRepository {
    public JdbcTournamentCategoryRepository(DataSource dataSource) { super(dataSource); }

    @Override
    public Uni<TournamentCategory> create(TournamentCategory category) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                insertCategory(connection, category);
                return find(connection, category.tournamentId(), category.id()).orElseThrow();
            }
        });
    }

    @Override
    public Uni<List<TournamentCategory>> listByTournament(UUID tournamentId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
                    SELECT *
                    FROM squadra.tournament_categories
                    WHERE tournament_id = ?
                    ORDER BY created_at ASC
                    """)) {
                statement.setObject(1, tournamentId);
                try (var result = statement.executeQuery()) {
                    List<TournamentCategory> categories = new ArrayList<>();
                    while (result.next()) categories.add(mapCategory(result));
                    return categories;
                }
            }
        });
    }

    @Override
    public Uni<Optional<TournamentCategory>> findByTournament(UUID tournamentId, UUID categoryId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                return find(connection, tournamentId, categoryId);
            }
        });
    }

    @Override
    public Uni<Boolean> nameExists(UUID tournamentId, String normalizedName, UUID excludingCategoryId) {
        return blockingDatabaseCall(dataSource -> {
            String sql = excludingCategoryId == null ? """
                    SELECT 1
                    FROM squadra.tournament_categories
                    WHERE tournament_id = ? AND normalized_name = ?
                    LIMIT 1
                    """ : """
                    SELECT 1
                    FROM squadra.tournament_categories
                    WHERE tournament_id = ? AND normalized_name = ? AND id <> ?
                    LIMIT 1
                    """;
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
                statement.setObject(1, tournamentId);
                statement.setString(2, normalizedName);
                if (excludingCategoryId != null) statement.setObject(3, excludingCategoryId);
                try (var result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }

    @Override
    public Uni<TournamentCategory> save(TournamentCategory category) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
                    UPDATE squadra.tournament_categories
                    SET name = ?, normalized_name = ?, modality = ?, min_athletes = ?, max_athletes = ?,
                        min_teams = ?, max_teams = ?, payment_model = ?, price_in_cents = ?,
                        competition_format = ?, swiss_rounds = ?, match_duration_minutes = ?,
                        interval_between_matches_minutes = ?, minimum_rest_minutes = ?,
                        scheduling_strategy = ?, updated_at = ?
                    WHERE id = ? AND tournament_id = ?
                    """)) {
                bindSave(statement, category);
                statement.setObject(17, category.id());
                statement.setObject(18, category.tournamentId());
                int updated = statement.executeUpdate();
                if (updated == 0) throw new TournamentCategoryNotFoundException();
                return find(connection, category.tournamentId(), category.id()).orElseThrow();
            }
        });
    }

    @Override
    public Uni<Boolean> delete(UUID tournamentId, UUID categoryId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
                    DELETE FROM squadra.tournament_categories
                    WHERE tournament_id = ? AND id = ?
                    """)) {
                statement.setObject(1, tournamentId);
                statement.setObject(2, categoryId);
                return statement.executeUpdate() > 0;
            } catch (SQLException exception) {
                if ("23503".equals(exception.getSQLState())) {
                    throw new TournamentCategoryConflictException("Category has registrations");
                }
                throw exception;
            }
        });
    }

    private static Optional<TournamentCategory> find(Connection connection, UUID tournamentId, UUID categoryId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT *
                FROM squadra.tournament_categories
                WHERE tournament_id = ? AND id = ?
                """)) {
            statement.setObject(1, tournamentId);
            statement.setObject(2, categoryId);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapCategory(result)) : Optional.empty();
            }
        }
    }

    private static void insertCategory(Connection connection, TournamentCategory category) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO squadra.tournament_categories (
                    id, tournament_id, name, normalized_name, modality, min_athletes, max_athletes,
                    min_teams, max_teams, payment_model, price_in_cents, competition_format, swiss_rounds,
                    match_duration_minutes, interval_between_matches_minutes, minimum_rest_minutes,
                    scheduling_strategy, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setObject(1, category.id());
            statement.setObject(2, category.tournamentId());
            statement.setString(3, category.name());
            statement.setString(4, category.normalizedName());
            statement.setString(5, category.modality().name());
            statement.setInt(6, category.minAthletes());
            statement.setInt(7, category.maxAthletes());
            statement.setInt(8, category.minTeams());
            statement.setInt(9, category.maxTeams());
            statement.setString(10, category.paymentModel().name());
            statement.setLong(11, category.priceInCents());
            statement.setString(12, category.competitionFormat().name());
            if (category.swissRounds() == null) statement.setNull(13, java.sql.Types.INTEGER);
            else statement.setInt(13, category.swissRounds());
            statement.setInt(14, category.matchDurationMinutes());
            statement.setInt(15, category.intervalBetweenMatchesMinutes());
            statement.setInt(16, category.minimumRestMinutes());
            statement.setString(17, category.schedulingStrategy().name());
            setInstant(statement, 18, category.createdAt());
            setInstant(statement, 19, category.updatedAt());
            statement.executeUpdate();
        }
    }

    private static void bindSave(java.sql.PreparedStatement statement, TournamentCategory category) throws SQLException {
        statement.setString(1, category.name());
        statement.setString(2, category.normalizedName());
        statement.setString(3, category.modality().name());
        statement.setInt(4, category.minAthletes());
        statement.setInt(5, category.maxAthletes());
        statement.setInt(6, category.minTeams());
        statement.setInt(7, category.maxTeams());
        statement.setString(8, category.paymentModel().name());
        statement.setLong(9, category.priceInCents());
        statement.setString(10, category.competitionFormat().name());
        if (category.swissRounds() == null) statement.setNull(11, java.sql.Types.INTEGER);
        else statement.setInt(11, category.swissRounds());
        statement.setInt(12, category.matchDurationMinutes());
        statement.setInt(13, category.intervalBetweenMatchesMinutes());
        statement.setInt(14, category.minimumRestMinutes());
        statement.setString(15, category.schedulingStrategy().name());
        setInstant(statement, 16, category.updatedAt());
    }

    private static TournamentCategory mapCategory(ResultSet result) throws SQLException {
        return new TournamentCategory(result.getObject("id", UUID.class), result.getObject("tournament_id", UUID.class),
                new TournamentCategory.SaveFields(result.getString("name"),
                        TournamentModality.valueOf(result.getString("modality")),
                        result.getInt("min_athletes"), result.getInt("max_athletes"),
                        result.getInt("min_teams"), result.getInt("max_teams"),
                        CategoryPaymentModel.valueOf(result.getString("payment_model")),
                        result.getLong("price_in_cents"),
                        CompetitionFormat.valueOf(result.getString("competition_format")),
                        (Integer) result.getObject("swiss_rounds"),
                        result.getInt("match_duration_minutes"),
                        result.getInt("interval_between_matches_minutes"),
                        result.getInt("minimum_rest_minutes"),
                        SchedulingStrategy.valueOf(result.getString("scheduling_strategy"))),
                getInstant(result, "created_at"), getInstant(result, "updated_at"));
    }
}
