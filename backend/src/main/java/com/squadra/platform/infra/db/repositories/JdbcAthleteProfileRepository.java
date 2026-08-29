package com.squadra.platform.infra.db.repositories;

import com.squadra.platform.domain.entities.AthleteProfile;
import com.squadra.platform.domain.enums.DominantFoot;
import com.squadra.platform.domain.enums.FootballPosition;
import com.squadra.platform.domain.repositories.AthleteProfileRepository;
import com.squadra.platform.infra.db.adapters.ReactiveJdbcSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;

@Dependent
public final class JdbcAthleteProfileRepository extends ReactiveJdbcSupport implements AthleteProfileRepository {
    public JdbcAthleteProfileRepository(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Uni<Optional<AthleteProfile>> findByUserId(UUID userId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                    "SELECT id, user_id, display_name, full_name, birth_date, primary_position, secondary_positions, dominant_foot, jersey_number, city, state, bio, created_at, updated_at FROM squadra.athlete_profiles WHERE user_id = ?")) {
                statement.setObject(1, userId);
                try (var result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Uni<AthleteProfile> upsert(AthleteProfile profile) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection()) {
                int updated = update(connection, profile);
                if (updated == 0) {
                    insert(connection, profile);
                }
                return findByUserIdBlocking(connection, profile.userId()).orElseThrow();
            }
        });
    }

    private static void insert(java.sql.Connection connection, AthleteProfile profile) throws java.sql.SQLException {
        String sql = """
                INSERT INTO squadra.athlete_profiles
                (id, user_id, display_name, full_name, birth_date, primary_position, secondary_positions, dominant_foot, jersey_number, city, state, bio, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql)) {
                statement.setObject(1, profile.id());
                statement.setObject(2, profile.userId());
                statement.setString(3, profile.displayName());
                statement.setString(4, profile.fullName());
                setLocalDate(statement, 5, profile.birthDate());
                statement.setString(6, profile.primaryPosition().name());
                statement.setString(7, serializePositions(profile.secondaryPositions()));
                statement.setString(8, profile.dominantFoot() == null ? null : profile.dominantFoot().name());
                if (profile.jerseyNumber() == null) statement.setNull(9, Types.INTEGER); else statement.setInt(9, profile.jerseyNumber());
                statement.setString(10, profile.city());
                statement.setString(11, profile.state());
                statement.setString(12, profile.bio());
                setInstant(statement, 13, profile.createdAt());
                setInstant(statement, 14, profile.updatedAt());
                statement.executeUpdate();
        }
    }

    private static int update(java.sql.Connection connection, AthleteProfile profile) throws java.sql.SQLException {
        String sql = """
                UPDATE squadra.athlete_profiles SET
                display_name = ?,
                full_name = ?,
                birth_date = ?,
                primary_position = ?,
                secondary_positions = ?,
                dominant_foot = ?,
                jersey_number = ?,
                city = ?,
                state = ?,
                bio = ?,
                updated_at = ?
                WHERE user_id = ?
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, profile.displayName());
            statement.setString(2, profile.fullName());
            setLocalDate(statement, 3, profile.birthDate());
            statement.setString(4, profile.primaryPosition().name());
            statement.setString(5, serializePositions(profile.secondaryPositions()));
            statement.setString(6, profile.dominantFoot() == null ? null : profile.dominantFoot().name());
            if (profile.jerseyNumber() == null) statement.setNull(7, Types.INTEGER); else statement.setInt(7, profile.jerseyNumber());
            statement.setString(8, profile.city());
            statement.setString(9, profile.state());
            statement.setString(10, profile.bio());
            setInstant(statement, 11, profile.updatedAt());
            statement.setObject(12, profile.userId());
            return statement.executeUpdate();
        }
    }

    private Optional<AthleteProfile> findByUserIdBlocking(java.sql.Connection connection, UUID userId) throws java.sql.SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT id, user_id, display_name, full_name, birth_date, primary_position, secondary_positions, dominant_foot, jersey_number, city, state, bio, created_at, updated_at FROM squadra.athlete_profiles WHERE user_id = ?")) {
            statement.setObject(1, userId);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    private static AthleteProfile map(ResultSet result) throws java.sql.SQLException {
        return new AthleteProfile(result.getObject("id", UUID.class), result.getObject("user_id", UUID.class),
                result.getString("display_name"), result.getString("full_name"), localDate(result, "birth_date"),
                FootballPosition.valueOf(result.getString("primary_position")), parsePositions(result.getString("secondary_positions")),
                dominantFoot(result.getString("dominant_foot")), integerOrNull(result, "jersey_number"),
                result.getString("city"), result.getString("state"), result.getString("bio"),
                getInstant(result, "created_at"), getInstant(result, "updated_at"));
    }

    private static LocalDate localDate(ResultSet result, String column) throws java.sql.SQLException {
        var date = result.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private static Integer integerOrNull(ResultSet result, String column) throws java.sql.SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }

    private static DominantFoot dominantFoot(String value) {
        return value == null ? null : DominantFoot.valueOf(value);
    }

    private static String serializePositions(Set<FootballPosition> positions) {
        return positions.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    private static Set<FootballPosition> parsePositions(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        EnumSet<FootballPosition> positions = Arrays.stream(value.split(","))
                .map(FootballPosition::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FootballPosition.class)));
        return positions;
    }
}
