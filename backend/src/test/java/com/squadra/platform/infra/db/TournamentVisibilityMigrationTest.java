package com.squadra.platform.infra.db;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class TournamentVisibilityMigrationTest {
    @Test
    void migrationBackfillsVisibilityAndPreservesActiveAndHistoricalRegistrations() throws Exception {
        String url = "jdbc:h2:mem:migration-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").schemas("squadra").target("8").load().migrate();
        String id = UUID.randomUUID().toString();
        String historical = UUID.randomUUID().toString();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.execute("INSERT INTO squadra.users VALUES ('" + id + "', 'migration@example.test', 'unused', 'ORGANIZER', CURRENT_TIMESTAMP)");
            sql.execute("INSERT INTO squadra.teams(id,owner_user_id,name,created_at,updated_at) VALUES ('" + id + "','" + id + "','Estrelas',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            sql.execute("INSERT INTO squadra.tournaments(id,owner_organizer_user_id,status,name,city,state,venue_name,start_date,end_date,registration_deadline,created_at,updated_at) VALUES ('" + id + "','" + id + "','OPEN','Copa Anterior','Recife','PE','Arena','2027-01-10','2027-01-12','2027-01-01',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            sql.execute("INSERT INTO squadra.tournament_categories VALUES ('" + id + "','" + id + "','Livre','livre','FUTSAL',7,12,2,16,'TEAM',0,'GROUP_STAGE',NULL,40,10,60,'BALANCED_REST',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            sql.execute("INSERT INTO squadra.tournament_registrations VALUES ('" + id + "','" + id + "','" + id + "','" + id + "','DRAFT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            sql.execute("INSERT INTO squadra.tournament_registrations VALUES ('" + historical + "','" + id + "','" + id + "','" + id + "','CANCELLED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        }
        Flyway.configure().dataSource(url, "sa", "").schemas("squadra").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            try (var rows = sql.executeQuery("SELECT visibility, name FROM squadra.tournaments")) {
                assertTrue(rows.next());
                assertEquals("PUBLIC", rows.getString("visibility"));
                assertEquals("Copa Anterior", rows.getString("name"));
            }
            try (var rows = sql.executeQuery("SELECT status, active_key FROM squadra.tournament_registrations ORDER BY status")) {
                assertTrue(rows.next());
                assertEquals("CANCELLED", rows.getString("status"));
                assertNull(rows.getObject("active_key"));
                assertTrue(rows.next());
                assertEquals("DRAFT", rows.getString("status"));
                assertTrue(rows.getBoolean("active_key"));
                assertFalse(rows.next());
            }
            assertEquals(1, sql.executeUpdate("INSERT INTO squadra.tournament_registrations VALUES ('" + UUID.randomUUID() + "','" + id + "','" + id + "','" + id + "','CANCELLED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL)"));
            assertThrows(java.sql.SQLException.class, () -> sql.executeUpdate("INSERT INTO squadra.tournament_registrations VALUES ('" + UUID.randomUUID() + "','" + id + "','" + id + "','" + id + "','APPROVED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,TRUE)"));
        }
    }
}
