package com.friendlyfire.account.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Starts the service against a fresh Postgres and checks Flyway ran. */
@SpringBootTest(properties = {
    "spring.grpc.server.port=0",
    "spring.grpc.client.channel.matchdata.target=localhost:1",
})
@Testcontainers
class FlywayMigrationIT {

    // Same image as compose.yaml.
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.15-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void appliesFirstMigrationToAccountSchema() {
        Map<String, Object> first = jdbc.queryForMap(
                "SELECT version, success FROM account.flyway_schema_history WHERE version = '1'");

        assertThat(first.get("success")).isEqualTo(true);
    }

    @Test
    void createsTheUsersTable() {
        assertThat(columns("users"))
                .containsExactly("user_id", "auth_subject", "email", "display_name", "created_at", "onboarded_at");
        assertThat(uniqueColumns("users")).containsExactlyInAnyOrder("user_id", "auth_subject");
    }

    @Test
    void createsTheRiotLinksTable() {
        assertThat(columns("riot_links"))
                .containsExactly("user_id", "puuid", "game_name", "tag_line", "region", "linked_at");
        // One link per user, and a Riot account can belong to only one user.
        assertThat(uniqueColumns("riot_links")).containsExactlyInAnyOrder("user_id", "puuid");
    }

    private List<String> columns(String table) {
        return jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_schema = 'account' AND table_name = ? ORDER BY ordinal_position",
                String.class,
                table);
    }

    /** Columns that are the primary key or have a UNIQUE constraint of their own. */
    private List<String> uniqueColumns(String table) {
        return jdbc.queryForList(
                "SELECT kcu.column_name FROM information_schema.table_constraints tc "
                        + "JOIN information_schema.key_column_usage kcu "
                        + "ON kcu.constraint_name = tc.constraint_name AND kcu.table_schema = tc.table_schema "
                        + "WHERE tc.table_schema = 'account' AND tc.table_name = ? "
                        + "AND tc.constraint_type IN ('PRIMARY KEY', 'UNIQUE')",
                String.class,
                table);
    }
}
