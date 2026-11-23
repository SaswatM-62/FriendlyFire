package com.friendlyfire.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.gateway.testsupport.FakeAccountService;
import com.friendlyfire.gateway.testsupport.FakeTokens;
import com.friendlyfire.gateway.testsupport.MutableClock;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class UserResolverTest {

    private final FakeAccountService account = new FakeAccountService();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-13T10:00:00Z"));
    private Server server;
    private ManagedChannel channel;
    private UserResolver users;

    @BeforeEach
    void start() throws Exception {
        server = InProcessServerBuilder.forName("user-resolver-test").addService(account).directExecutor().build().start();
        channel = InProcessChannelBuilder.forName("user-resolver-test").directExecutor().build();
        users = new UserResolver(AccountServiceGrpc.newBlockingStub(channel), new TokenClaims(FakeTokens.NAMESPACE),
                Duration.ofMinutes(5), clock);
    }

    @AfterEach
    void stop() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void createsTheUserFromTheTokensClaims() {
        assertThat(users.resolve(token("maya")).getUserId()).isEqualTo("user-maya");

        EnsureUserRequest request = account.calls.last(EnsureUserRequest.class);
        assertThat(request.getAuthSubject()).isEqualTo("auth0|maya");
        assertThat(request.getEmail()).isEqualTo("maya@example.com");
        assertThat(request.getSuggestedDisplayName()).isEqualTo("Maya");
    }

    @Test
    void reusesTheUserForAFewMinutes() {
        users.resolve(token("maya"));
        clock.advance(Duration.ofMinutes(4));
        users.resolve(token("maya"));

        assertThat(account.calls.received(EnsureUserRequest.class)).hasSize(1);
    }

    @Test
    void asksAgainAfterThat() {
        users.resolve(token("maya"));
        clock.advance(Duration.ofMinutes(6));
        users.resolve(token("maya"));

        assertThat(account.calls.received(EnsureUserRequest.class)).hasSize(2);
    }

    @Test
    void refreshAlwaysAsksAndUpdatesTheCache() {
        users.resolve(token("maya"));
        users.refresh(token("maya"));
        users.resolve(token("maya"));

        assertThat(account.calls.received(EnsureUserRequest.class)).hasSize(2);
    }

    @Test
    void anUpdatedUserIsWhatTheNextRequestSees() {
        users.resolve(token("maya"));
        users.update("auth0|maya", FakeAccountService.user("maya", "Maya B"));

        assertThat(users.resolve(token("maya")).getDisplayName()).isEqualTo("Maya B");
    }

    @Test
    void anEvictedUserIsAskedForAgain() {
        users.resolve(token("maya"));
        users.evict("auth0|maya");
        users.resolve(token("maya"));

        assertThat(account.calls.received(EnsureUserRequest.class)).hasSize(2);
    }

    private static Jwt token(String name) {
        return FakeTokens.decoder().decode(FakeTokens.token(name));
    }
}
