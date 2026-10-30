package com.friendlyfire.account.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.client.GrpcChannelFactory;

/** Talks to the real gRPC server, in-process. */
// Flyway needs a database at startup; FlywayMigrationIT covers it against a real Postgres.
@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureTestGrpcTransport
class AccountGrpcServiceTest {

    @Autowired
    private GrpcChannelFactory channels;

    private ManagedChannel channel;

    @BeforeEach
    void connect() {
        channel = channels.createChannel("account-service");
    }

    @Test
    void grpcHealthIsServing() {
        var response = HealthGrpc.newBlockingStub(channel).check(HealthCheckRequest.getDefaultInstance());

        assertThat(response.getStatus()).isEqualTo(ServingStatus.SERVING);
    }

    @Test
    void rpcsAreNotImplementedYet() {
        var accounts = AccountServiceGrpc.newBlockingStub(channel);

        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> accounts.getUser(GetUserRequest.getDefaultInstance()))
                .satisfies(e -> {
                    assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.UNIMPLEMENTED);
                    assertThat(e.getStatus().getDescription()).isEqualTo("not implemented yet");
                });
    }
}
