package com.friendlyfire.account;

import org.junit.jupiter.api.Test;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;

// Flyway needs a database at startup; FlywayMigrationIT covers it against a real Postgres.
@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureTestGrpcTransport
class AccountServiceApplicationTest {

    @Test
    void contextLoads() {
        // Starts even though Postgres, the mock RSO and match-data-service aren't running
        // (Flyway is switched off above, since it would need Postgres).
    }
}
