package com.squadra.platform.infra.db.repositories;

import com.squadra.platform.domain.entities.OrganizerProfile;
import com.squadra.platform.domain.repositories.OrganizerProfileRepository;
import com.squadra.platform.infra.db.adapters.ReactiveJdbcSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@Dependent
public final class JdbcOrganizerProfileRepository extends ReactiveJdbcSupport implements OrganizerProfileRepository {
    private static final String SELECT_BY_USER_ID = """
            SELECT id, user_id, organization_name, contact_name, contact_email, phone, city, state, website,
                   description, created_at, updated_at
            FROM squadra.organizer_profiles
            WHERE user_id = ?
            """;

    public JdbcOrganizerProfileRepository(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Uni<Optional<OrganizerProfile>> findByUserId(UUID userId) {
        return blockingDatabaseCall(dataSource -> {
            try (var connection = dataSource.getConnection();
                    var statement = connection.prepareStatement(SELECT_BY_USER_ID)) {
                statement.setObject(1, userId);
                try (var result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Uni<OrganizerProfile> upsert(OrganizerProfile profile) {
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

    private static void insert(java.sql.Connection connection, OrganizerProfile profile) throws java.sql.SQLException {
        String sql = """
                INSERT INTO squadra.organizer_profiles
                (id, user_id, organization_name, contact_name, contact_email, phone, city, state, website, description, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, profile.id());
            statement.setObject(2, profile.userId());
            statement.setString(3, profile.organizationName());
            statement.setString(4, profile.contactName());
            statement.setString(5, profile.contactEmail().value());
            statement.setString(6, profile.phone());
            statement.setString(7, profile.city());
            statement.setString(8, profile.state());
            statement.setString(9, profile.website());
            statement.setString(10, profile.description());
            setInstant(statement, 11, profile.createdAt());
            setInstant(statement, 12, profile.updatedAt());
            statement.executeUpdate();
        }
    }

    private static int update(java.sql.Connection connection, OrganizerProfile profile) throws java.sql.SQLException {
        String sql = """
                UPDATE squadra.organizer_profiles SET
                organization_name = ?,
                contact_name = ?,
                contact_email = ?,
                phone = ?,
                city = ?,
                state = ?,
                website = ?,
                description = ?,
                updated_at = ?
                WHERE user_id = ?
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, profile.organizationName());
            statement.setString(2, profile.contactName());
            statement.setString(3, profile.contactEmail().value());
            statement.setString(4, profile.phone());
            statement.setString(5, profile.city());
            statement.setString(6, profile.state());
            statement.setString(7, profile.website());
            statement.setString(8, profile.description());
            setInstant(statement, 9, profile.updatedAt());
            statement.setObject(10, profile.userId());
            return statement.executeUpdate();
        }
    }

    private Optional<OrganizerProfile> findByUserIdBlocking(java.sql.Connection connection, UUID userId)
            throws java.sql.SQLException {
        try (var statement = connection.prepareStatement(SELECT_BY_USER_ID)) {
            statement.setObject(1, userId);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    private static OrganizerProfile map(ResultSet result) throws java.sql.SQLException {
        return new OrganizerProfile(result.getObject("id", UUID.class), result.getObject("user_id", UUID.class),
                result.getString("organization_name"), result.getString("contact_name"),
                result.getString("contact_email"), result.getString("phone"), result.getString("city"),
                result.getString("state"), result.getString("website"), result.getString("description"),
                getInstant(result, "created_at"), getInstant(result, "updated_at"));
    }
}
