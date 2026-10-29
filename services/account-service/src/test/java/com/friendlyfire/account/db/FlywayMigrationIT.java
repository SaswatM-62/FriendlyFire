package com.friendlyfire.account.db;

import static org.assertj.core.api.Assertions.assertThat;

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
}
