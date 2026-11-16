package com.friendlyfire.account.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.User;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** EnsureUser and GetUser over a real gRPC server, against a real Postgres. */
// A real server on a random port, and match-data-service at an address where nothing
// listens (see PostgresHealthIT).
@SpringBootTest(properties = {
    "spring.grpc.server.port=0",
    "spring.grpc.client.channel.matchdata.target=localhost:1",
})
@Testcontainers
class AccountGrpcServiceIT {

    // Same image as compose.yaml.
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.15-alpine");

    @LocalGrpcServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private ManagedChannel channel;
    private AccountServiceBlockingStub accounts;

    @BeforeEach
    void connect() {
        channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        accounts = AccountServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void disconnect() {
        channel.shutdownNow();
    }

    @Test
    void ensureUserCreatesTheUserOnceAndKeepsTheirFirstDetails() {
        EnsureUserRequest request = EnsureUserRequest.newBuilder()
                .setAuthSubject("auth0|" + UUID.randomUUID())
                .setEmail("player@example.test")
                .setSuggestedDisplayName("Sam")
                .build();

        EnsureUserResponse first = accounts.ensureUser(request);
        EnsureUserResponse second = accounts.ensureUser(request.toBuilder()
                .setEmail("changed@example.test")
                .setSuggestedDisplayName("Changed Name")
                .build());

        assertThat(first.getCreated()).isTrue();
        assertThat(first.getUser().getEmail()).isEqualTo("player@example.test");
        assertThat(first.getUser().getDisplayName()).isEqualTo("Sam");
        assertThat(first.getUser().hasCreatedAt()).isTrue();
        assertThat(second.getCreated()).isFalse();
        assertThat(second.getUser()).isEqualTo(first.getUser());
    }

    @Test
    void ensureUserNeverUsesAnEmailAddressAsTheName() {
        EnsureUserResponse response = accounts.ensureUser(EnsureUserRequest.newBuilder()
                .setAuthSubject("auth0|" + UUID.randomUUID())
                .setEmail("sam@example.test")
                .setSuggestedDisplayName("sam@example.test")
                .build());

        assertThat(response.getUser().getDisplayName()).matches("Player \\d{4}");
    }

    @Test
    void ensureUserAndGetUserReturnTheRiotLinkButNeverThePuuid() {
        String authSubject = "auth0|" + UUID.randomUUID();
        User user = accounts.ensureUser(EnsureUserRequest.newBuilder()
                        .setAuthSubject(authSubject)
                        .setEmail("player@example.test")
                        .setSuggestedDisplayName("Sam")
                        .build())
                .getUser();
        jdbc.update(
                "INSERT INTO account.riot_links (user_id, puuid, game_name, tag_line, region) VALUES (?, ?, ?, ?, ?)",
                UUID.fromString(user.getUserId()), "private-puuid", "Sam", "NA1", "na");

        // EnsureUser is what the Gateway's GET /api/me shows, so it needs the link too.
        User ensured = accounts.ensureUser(EnsureUserRequest.newBuilder().setAuthSubject(authSubject).build())
                .getUser();
        User got = accounts.getUser(GetUserRequest.newBuilder().setUserId(user.getUserId()).build()).getUser();

        for (User returned : new User[] {ensured, got}) {
            assertThat(returned.getRiotLink().getGameName()).isEqualTo("Sam");
            assertThat(returned.getRiotLink().getTagLine()).isEqualTo("NA1");
            assertThat(returned.getRiotLink().getRegion()).isEqualTo("na");
            assertThat(returned.getRiotLink().getLinkedAt().getSeconds()).isPositive();
            assertThat(returned.toString()).doesNotContain("private-puuid");
        }
    }

    @Test
    void getUserWithoutARiotLinkHasNone() {
        User user = accounts.ensureUser(EnsureUserRequest.newBuilder()
                        .setAuthSubject("auth0|" + UUID.randomUUID())
                        .setSuggestedDisplayName("Sam")
                        .build())
                .getUser();

        User got = accounts.getUser(GetUserRequest.newBuilder().setUserId(user.getUserId()).build()).getUser();

        assertThat(got.getUserId()).isEqualTo(user.getUserId());
        assertThat(got.hasRiotLink()).isFalse();
    }

    @Test
    void getUserForAnUnknownUserIsNotFound() {
        GetUserRequest request = GetUserRequest.newBuilder().setUserId(UUID.randomUUID().toString()).build();

        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> accounts.getUser(request))
                .satisfies(e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }
}
