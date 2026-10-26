package com.friendlyfire.account.health;

import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import javax.sql.DataSource;

/** Borrows a connection from the pool and runs {@code SELECT 1}. */
public class PostgresDependencyCheck implements DependencyCheck {

    private final DataSource dataSource;

    public PostgresDependencyCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String name() {
        return "postgres";
    }

    @Override
    public ComponentHealth check(Duration deadline) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.setQueryTimeout((int) Math.max(1, deadline.toSeconds()));
            statement.execute("SELECT 1");
        }
        return DependencyCheck.up();
    }
}
