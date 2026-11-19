package com.friendlyfire.account.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** V4 adds onboarded_at; users who existed before it count as having finished the tutorial. */
@Testcontainers
class OnboardedAtMigrationIT {

    // Same image as compose.yaml.
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.15-alpine");

    @Test
    void existingUsersCountAsOnboardedAndNewOnesDoNot() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        flyway(dataSource, "3").migrate();
        jdbc.update("INSERT INTO account.users (user_id, auth_subject, email, display_name, created_at) "
                + "VALUES (gen_random_uuid(), 'auth0|old', 'old@example.test', 'Old', '2026-11-01T10:00:00Z')");

        flyway(dataSource, "latest").migrate();
        jdbc.update("INSERT INTO account.users (user_id, auth_subject, email, display_name) "
                + "VALUES (gen_random_uuid(), 'auth0|new', 'new@example.test', 'New')");

        Timestamp[] old = jdbc.queryForObject(
                "SELECT created_at, onboarded_at FROM account.users WHERE auth_subject = 'auth0|old'",
                (rs, n) -> new Timestamp[] {rs.getTimestamp("created_at"), rs.getTimestamp("onboarded_at")});
        Timestamp newer = jdbc.queryForObject(
                "SELECT onboarded_at FROM account.users WHERE auth_subject = 'auth0|new'", Timestamp.class);

        assertThat(old[1]).isEqualTo(old[0]);
        assertThat(newer).isNull();
    }

    private static Flyway flyway(PGSimpleDataSource dataSource, String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("account")
                .defaultSchema("account")
                .target(target)
                .load();
    }
}
