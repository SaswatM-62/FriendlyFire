package com.friendlyfire.account.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.account.grpc.AccountGrpcService;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Exercises user persistence against PostgreSQL and the Flyway migrations. */
@SpringBootTest(properties = {
    "spring.grpc.server.port=0",
    "spring.grpc.client.channel.matchdata.target=localhost:1",
})
@Testcontainers
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.15-alpine");

    @Autowired
    private UserRepository users;

    @Autowired
    private AccountGrpcService accounts;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void ensureUserCreatesOnlyOnFirstCallForAuthSubject() {
        String authSubject = "auth0|" + UUID.randomUUID();
        EnsureUserRequest request = EnsureUserRequest.newBuilder()
                .setAuthSubject(authSubject)
                .setEmail("player@example.test")
                .setSuggestedDisplayName("Player")
                .build();

        EnsureUserResponse first = ensureUser(request);
        EnsureUserResponse second = ensureUser(request.toBuilder()
                .setEmail("changed@example.test")
                .setSuggestedDisplayName("Changed Name")
                .build());

        assertThat(first.getCreated()).isTrue();
        assertThat(first.getUser().getUserId()).isNotBlank();
        assertThat(first.getUser().getEmail()).isEqualTo("player@example.test");
        assertThat(first.getUser().getDisplayName()).isEqualTo("Player");
        assertThat(second.getCreated()).isFalse();
        assertThat(second.getUser().getUserId()).isEqualTo(first.getUser().getUserId());
        assertThat(second.getUser().getEmail()).isEqualTo("player@example.test");
        assertThat(second.getUser().getDisplayName()).isEqualTo("Player");
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

    @Test
    void getUserReturnsUserWithRiotLinkButNeverPuuid() {
        AccountUser user = users.create("test|" + UUID.randomUUID(), "player@example.test", "Player");
        jdbc.update(
                """
                INSERT INTO account.riot_links (user_id, puuid, game_name, tag_line, region)
                VALUES (?, ?, ?, ?, ?)
                """,
                user.userId(),
                "private-puuid",
                "Player",
                "NA1",
                "na");

        GetUserResponse response = getUser(user.userId());

        assertThat(response.getUser().getUserId()).isEqualTo(user.userId().toString());
        assertThat(response.getUser().getDisplayName()).isEqualTo("Player");
        assertThat(response.getUser().hasRiotLink()).isTrue();
        assertThat(response.getUser().getRiotLink().getGameName()).isEqualTo("Player");
        assertThat(response.getUser().getRiotLink().getTagLine()).isEqualTo("NA1");
        assertThat(response.getUser().getRiotLink().getRegion()).isEqualTo("na");
        assertThat(response.getUser().getRiotLink().getLinkedAt().getSeconds()).isPositive();
        assertThat(response.getUser().getRiotLink().toString()).doesNotContain("private-puuid");
    }

    @Test
    void getUserWithoutRiotLinkReturnsUserAndMissingUserIsNotFound() {
        AccountUser user = users.create("test|" + UUID.randomUUID(), "player@example.test", "Player");

        GetUserResponse response = getUser(user.userId());
        assertThat(response.getUser().getUserId()).isEqualTo(user.userId().toString());
        assertThat(response.getUser().hasRiotLink()).isFalse();

        List<Throwable> errors = new ArrayList<>();
        accounts.getUser(GetUserRequest.newBuilder()
                .setUserId(UUID.randomUUID().toString())
                .build(), new StreamObserver<>() {
                    @Override
                    public void onNext(GetUserResponse value) {
                        throw new AssertionError("Unexpected user response");
                    }

                    @Override
                    public void onError(Throwable error) {
                        errors.add(error);
                    }

                    @Override
                    public void onCompleted() {}
                });
        assertThat(errors).singleElement()
                .satisfies(error -> assertThat(Status.fromThrowable(error).getCode())
                        .isEqualTo(Status.Code.NOT_FOUND));
    }

    private EnsureUserResponse ensureUser(EnsureUserRequest request) {
        List<EnsureUserResponse> responses = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        accounts.ensureUser(request, new StreamObserver<>() {
            @Override
            public void onNext(EnsureUserResponse response) {
                responses.add(response);
            }

            @Override
            public void onError(Throwable error) {
                errors.add(error);
            }

            @Override
            public void onCompleted() {}
        });

        assertThat(errors).isEmpty();
        assertThat(responses).hasSize(1);
        return responses.getFirst();
    }

    private GetUserResponse getUser(UUID userId) {
        List<GetUserResponse> responses = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        accounts.getUser(GetUserRequest.newBuilder()
                .setUserId(userId.toString())
                .build(), new StreamObserver<>() {
                    @Override
                    public void onNext(GetUserResponse response) {
                        responses.add(response);
                    }

                    @Override
                    public void onError(Throwable error) {
                        errors.add(error);
                    }

                    @Override
                    public void onCompleted() {}
                });

        assertThat(errors).isEmpty();
        assertThat(responses).hasSize(1);
        return responses.getFirst();
    }
}
