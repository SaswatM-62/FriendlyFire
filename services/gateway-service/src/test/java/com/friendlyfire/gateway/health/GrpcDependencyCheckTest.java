package com.friendlyfire.gateway.health;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class GrpcDependencyCheckTest {

    @Test
    void resetsTheReconnectBackoffWhenThePeerIsUnavailable() {
        ManagedChannel nowhere = InProcessChannelBuilder.forName("nobody-listens-here").build();
        // ManagedChannel is gRPC's class, so a Mockito mock is fine here.
        ManagedChannel channel = mock(ManagedChannel.class);
        var check = new GrpcDependencyCheck("room-service", DependencyHealthServiceGrpc.newBlockingStub(nowhere), channel);

        try {
            assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> check.check(Duration.ofSeconds(1)));
            verify(channel).resetConnectBackoff();
        } finally {
            nowhere.shutdownNow();
        }
    }
}
