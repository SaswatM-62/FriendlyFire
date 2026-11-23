package com.friendlyfire.account.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.account.v1.FinishOnboardingRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.client.GrpcChannelFactory;

/** Talks to the real gRPC server, in-process, with no database (see AccountGrpcServiceIT). */
// Flyway needs a database at startup; FlywayMigrationIT covers it against a real Postgres.
// The database URL points where nothing listens, and the pool starts without connecting,
// as if Postgres went down after startup.
@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:postgresql://localhost:1/none",
    "spring.datasource.hikari.initialization-fail-timeout=-1",
})
@AutoConfigureTestGrpcTransport
class AccountGrpcServiceTest {

    @Autowired
    private GrpcChannelFactory channels;

    private ManagedChannel channel;
    private AccountServiceBlockingStub accounts;

    @BeforeEach
    void connect() {
        channel = channels.createChannel("account-service");
        accounts = AccountServiceGrpc.newBlockingStub(channel);
    }

    @Test
    void grpcHealthIsServing() {
        var response = HealthGrpc.newBlockingStub(channel).check(HealthCheckRequest.getDefaultInstance());

        assertThat(response.getStatus()).isEqualTo(ServingStatus.SERVING);
    }

    @Test
    void getUserWithAnIdThatIsNotAUuidIsNotFound() {
        for (String userId : new String[] {"", "abc", "not-a-uuid"}) {
            assertThatExceptionOfType(StatusRuntimeException.class)
                    .isThrownBy(() -> accounts.getUser(GetUserRequest.newBuilder().setUserId(userId).build()))
                    .satisfies(e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
        }
    }

    @Test
    void finishingTheTutorialWithAnIdThatIsNotAUuidIsNotFound() {
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> accounts.finishOnboarding(
                        FinishOnboardingRequest.newBuilder().setUserId("not-a-uuid").build()))
                .satisfies(e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void aDatabaseThatIsDownIsUnavailable() {
        GetUserRequest request = GetUserRequest.newBuilder().setUserId(UUID.randomUUID().toString()).build();

        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> accounts.getUser(request))
                .satisfies(e -> {
                    assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
                    // From DatabaseErrorHandler, not some other part of the call.
                    assertThat(e.getStatus().getDescription()).isEqualTo("the database is unavailable, try again");
                });
    }
}
