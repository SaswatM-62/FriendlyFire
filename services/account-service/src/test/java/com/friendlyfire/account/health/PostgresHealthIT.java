package com.friendlyfire.account.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Runs against a real Postgres in Docker (Testcontainers). */
// A real gRPC server on a random port, and match-data-service at an address where
// nothing listens. (@AutoConfigureTestGrpcTransport would route every channel back to
// this service, so its match-data-service check would call itself over and over.)
@SpringBootTest(properties = {
    "spring.grpc.server.port=0",
    "spring.grpc.client.channel.matchdata.target=localhost:1",
})
@Testcontainers
class PostgresHealthIT {

    // Same image as compose.yaml.
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.15-alpine");

    @Autowired
    private DependencyHealthChecker checker;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void reportsPostgresUp() {
        // Warm up: on a slow CI runner the very first call can miss the 1-second deadline.
        checker.check();

        ComponentHealth postgresNode = checker.check().getDependenciesList().stream()
                .filter(node -> node.getName().equals("postgres"))
                .findFirst()
                .orElseThrow();

        assertThat(postgresNode.getStatus()).isEqualTo(ComponentStatus.COMPONENT_STATUS_UP);
    }

    @Test
    void migratesUsersTable() {
        List<String> columns = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_schema = 'account' AND table_name = 'users' "
                        + "ORDER BY ordinal_position",
                String.class);

        assertThat(columns).containsExactly("user_id", "auth_subject", "email", "display_name", "created_at");
        Integer uniqueConstraints = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE table_schema = 'account' AND table_name = 'users' "
                        + "AND constraint_type IN ('PRIMARY KEY', 'UNIQUE')",
                Integer.class);
        assertThat(uniqueConstraints).isEqualTo(2);
    }

    @Test
    void migratesRiotLinksTable() {
        List<String> columns = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_schema = 'account' AND table_name = 'riot_links' "
                        + "ORDER BY ordinal_position",
                String.class);

        assertThat(columns).containsExactly("user_id", "puuid", "game_name", "tag_line", "region", "linked_at");
    }
}
