package com.friendlyfire.account.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** UserRepository against a real Postgres. The RPCs are tested in AccountGrpcServiceIT. */
@SpringBootTest(properties = {
    "spring.grpc.server.port=0",
    "spring.grpc.client.channel.matchdata.target=localhost:1",
})
@Testcontainers
class UserRepositoryIT {

    // Same image as compose.yaml.
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.15-alpine");

    @Autowired
    private UserRepository users;

    @Test
    void ensureCreatesTheUserOnlyOnTheFirstCallForASubject() {
        String authSubject = "auth0|" + UUID.randomUUID();

        EnsuredUser first = users.ensure(authSubject, "player@example.test", "Sam");
        EnsuredUser second = users.ensure(authSubject, "changed@example.test", "Changed Name");

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.user()).isEqualTo(first.user());
    }

    @Test
    void createsFindsUpdatesAndDeletesUser() {
        String authSubject = "test|" + UUID.randomUUID();
        AccountUser created = users.create(authSubject, "player@example.test", "Player");

        assertThat(created.userId()).isNotNull();
        assertThat(created.createdAt()).isNotNull();
        assertThat(users.findById(created.userId())).contains(created);
        assertThat(users.findByAuthSubject(authSubject)).contains(created);

        AccountUser updated = users.changeDisplayName(created.userId(), "Updated Player")
                .orElseThrow();
        assertThat(updated.displayName()).isEqualTo("Updated Player");
        assertThat(updated.email()).isEqualTo("player@example.test");

        assertThat(users.delete(created.userId())).isTrue();
        assertThat(users.findById(created.userId())).isEmpty();
        assertThat(users.findByAuthSubject(authSubject)).isEmpty();
        assertThat(users.delete(created.userId())).isFalse();
    }

    @Test
    void updateOfMissingUserReturnsEmpty() {
        assertThat(users.changeDisplayName(UUID.randomUUID(), "Nobody")).isEmpty();
    }
}
