package com.friendlyfire.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GrpcClientsConfigTest {

    private static final String FAKE_ROOM = "grpc-clients-config-test";

    private final GrpcClientsConfig config = new GrpcClientsConfig();
    private final ManagedChannel channel = InProcessChannelBuilder.forName(FAKE_ROOM).build();
    private Server server;

    @AfterEach
    void close() {
        channel.shutdownNow();
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void noStubHasADeadlineFixedWhenItWasMade() {
        // A deadline on the stub counts from startup, so every call after 3 s would fail.
        // The default deadline is added to each call instead (DefaultDeadlineInterceptor).
        List<AbstractStub<?>> stubs = List.of(
                config.accountService(channel), config.accountHealth(channel),
                config.roomService(channel), config.roomHealth(channel), config.roomEvents(channel),
                config.scoringService(channel), config.scoringHealth(channel));

        assertThat(stubs).allSatisfy(stub -> assertThat(stub.getCallOptions().getDeadline()).isNull());
    }

    @Test
    void defaultDeadlineIsThreeSeconds() {
        assertThat(GrpcClientsConfig.DEFAULT_DEADLINE).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void theRoomEventStreamHasNoDeadline() throws Exception {
        // A room's event stream stays open as long as the room does.
        CompletableFuture<Optional<Deadline>> deadline = new CompletableFuture<>();
        server = InProcessServerBuilder.forName(FAKE_ROOM).addService(new RoomServiceGrpc.RoomServiceImplBase() {
            @Override
            public void streamRoomEvents(StreamRoomEventsRequest request, StreamObserver<StreamRoomEventsResponse> observer) {
                deadline.complete(Optional.ofNullable(Context.current().getDeadline()));
                observer.onCompleted();
            }
        }).build().start();

        config.roomEvents(channel).streamRoomEvents(StreamRoomEventsRequest.getDefaultInstance(), new StreamObserver<>() {
            @Override
            public void onNext(StreamRoomEventsResponse value) {
            }

            @Override
            public void onError(Throwable error) {
            }

            @Override
            public void onCompleted() {
            }
        });

        assertThat(deadline.get(3, TimeUnit.SECONDS)).isEmpty();
    }
}
