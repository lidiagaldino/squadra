package com.squadra.platform.infra.db.adapters;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.Supplier;

/**
 * JDBC is blocking, so every database operation is deliberately isolated on Quarkus' worker pool.
 * Repository ports remain reactive and no caller needs to block awaiting a result.
 */
public abstract class ReactiveJdbcSupport {
    private final DataSource dataSource;

    protected ReactiveJdbcSupport(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected final <T> Uni<T> blockingDatabaseCall(SqlSupplier<T> work) {
        Supplier<T> supplier = () -> {
            try {
                return work.get(dataSource);
            } catch (SQLException exception) {
                throw new IllegalStateException("Authentication persistence operation failed", exception);
            }
        };
        return Uni.createFrom().item(supplier).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    protected static void setInstant(PreparedStatement statement, int index, Instant value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
            return;
        }
        statement.setObject(index, OffsetDateTime.ofInstant(value, ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
    }

    protected static Instant getInstant(ResultSet result, String column) throws SQLException {
        OffsetDateTime value = result.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    protected static void setLocalDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
            return;
        }
        statement.setObject(index, value, Types.DATE);
    }

    @FunctionalInterface
    protected interface SqlSupplier<T> {
        T get(DataSource dataSource) throws SQLException;
    }
}
