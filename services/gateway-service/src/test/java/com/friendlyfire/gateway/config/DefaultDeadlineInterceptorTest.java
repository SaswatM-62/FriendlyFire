package com.friendlyfire.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** A fake room-service in-process: GetRoomSnapshot answers at once, JoinRoom never does. */
class DefaultDeadlineInterceptorTest {

    private static final String SERVER = "deadline-interceptor-test";

    private static Server server;
    private static ManagedChannel channel;

    @BeforeAll
    static void start() throws IOException {
        var service = new RoomServiceGrpc.RoomServiceImplBase() {
            @Override
            public void getRoomSnapshot(
                    GetRoomSnapshotRequest request, StreamObserver<GetRoomSnapshotResponse> responseObserver) {
                responseObserver.onNext(GetRoomSnapshotResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }

            @Override
            public void joinRoom(JoinRoomRequest request, StreamObserver<JoinRoomResponse> responseObserver) {
                // Never answers.
            }
        };
        server = InProcessServerBuilder.forName(SERVER).addService(service).build().start();
        channel = InProcessChannelBuilder.forName(SERVER).build();
    }

    @AfterAll
    static void stop() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void deadlineCountsFromEachCallNotFromWhenTheStubWasMade() throws InterruptedException {
        RoomServiceBlockingStub rooms = stubWithDefault(Duration.ofMillis(200));

        // Longer than the default: a deadline fixed when the stub was made would have passed.
        Thread.sleep(400);

        assertThat(rooms.getRoomSnapshot(GetRoomSnapshotRequest.getDefaultInstance())).isNotNull();
    }

    @Test
    void callWithoutADeadlineGetsTheDefault() {
        RoomServiceBlockingStub rooms = stubWithDefault(Duration.ofMillis(200));
        long start = System.nanoTime();

        assertThatThrownBy(() -> rooms.joinRoom(JoinRoomRequest.getDefaultInstance()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED));
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void callThatSetsItsOwnDeadlineKeepsIt() {
        RoomServiceBlockingStub rooms = stubWithDefault(Duration.ofSeconds(30));
        long start = System.nanoTime();

        assertThatThrownBy(() -> rooms.withDeadlineAfter(200, TimeUnit.MILLISECONDS)
                        .joinRoom(JoinRoomRequest.getDefaultInstance()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED));
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
    }

    private static RoomServiceBlockingStub stubWithDefault(Duration timeout) {
        return RoomServiceGrpc.newBlockingStub(channel).withInterceptors(new DefaultDeadlineInterceptor(timeout));
    }
}
