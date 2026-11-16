package com.friendlyfire.account.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
}
