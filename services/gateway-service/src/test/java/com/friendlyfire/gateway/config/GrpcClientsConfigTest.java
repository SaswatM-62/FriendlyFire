package com.friendlyfire.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.AbstractStub;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GrpcClientsConfigTest {

    private final GrpcClientsConfig config = new GrpcClientsConfig();
    private final ManagedChannel channel = InProcessChannelBuilder.forName("grpc-clients-config-test").build();

    @AfterEach
    void close() {
        channel.shutdownNow();
    }

    @Test
    void noStubHasADeadlineFixedWhenItWasMade() {
        // A deadline on the stub counts from startup, so every call after 3 s would fail.
        // The default deadline is added to each call instead (DefaultDeadlineInterceptor).
        List<AbstractStub<?>> stubs = List.of(
                config.accountService(channel), config.accountHealth(channel),
                config.roomService(channel), config.roomHealth(channel),
                config.scoringService(channel), config.scoringHealth(channel),
                config.matchDataService(channel), config.matchDataHealth(channel));

        assertThat(stubs).allSatisfy(stub -> assertThat(stub.getCallOptions().getDeadline()).isNull());
    }

    @Test
    void defaultDeadlineIsThreeSeconds() {
        assertThat(GrpcClientsConfig.DEFAULT_DEADLINE).isEqualTo(Duration.ofSeconds(3));
    }
}
