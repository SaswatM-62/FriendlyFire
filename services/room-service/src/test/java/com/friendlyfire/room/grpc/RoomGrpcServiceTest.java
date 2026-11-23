package com.friendlyfire.room.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc;
import com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest;
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
@SpringBootTest
@AutoConfigureTestGrpcTransport
class RoomGrpcServiceTest {

    @Autowired
    private GrpcChannelFactory channels;

    private ManagedChannel channel;

    @BeforeEach
    void connect() {
        channel = channels.createChannel("room-service");
    }

    @Test
    void grpcHealthIsServing() {
        var response = HealthGrpc.newBlockingStub(channel).check(HealthCheckRequest.getDefaultInstance());

        assertThat(response.getStatus()).isEqualTo(ServingStatus.SERVING);
    }

    @Test
    void anUnknownRoomIsNotFound() {
        var rooms = RoomServiceGrpc.newBlockingStub(channel);

        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> rooms.getRoomSnapshot(GetRoomSnapshotRequest.getDefaultInstance()))
                .satisfies(e -> {
                    assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                    assertThat(e.getStatus().getDescription()).isEqualTo("Room not found.");
                });
    }

    @Test
    void aRoomCreatedOverGrpcCanBeReadByItsHostOnly() {
        var rooms = RoomServiceGrpc.newBlockingStub(channel);

        var created = rooms.createRoom(CreateRoomRequest.newBuilder()
                .setHostUserId("alice").setHostDisplayName("Alice").build()).getSnapshot();
        var read = rooms.getRoomSnapshot(GetRoomSnapshotRequest.newBuilder()
                .setRoomCode(created.getRoomCode()).setUserId("alice").build()).getSnapshot();

        assertThat(read.getHostUserId()).isEqualTo("alice");
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> rooms.getRoomSnapshot(GetRoomSnapshotRequest.newBuilder()
                        .setRoomCode(created.getRoomCode()).setUserId("mallory").build()))
                .satisfies(e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED));
    }
}
