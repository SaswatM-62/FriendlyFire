package com.friendlyfire.gateway.testsupport;

import com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.Player;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerStatus;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEvent;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSettings;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSnapshot;
import com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A fake RoomService. Every room exists and everyone is in it, unless a test says
 * otherwise. Event streams start with a SNAPSHOT, like the real one; tests push more
 * events and end streams.
 */
public final class FakeRoomService extends RoomServiceGrpc.RoomServiceImplBase {

    public final FakeCalls calls = new FakeCalls();

    private final Map<String, List<ServerCallStreamObserver<StreamRoomEventsResponse>>> streams = new ConcurrentHashMap<>();
    private volatile Status streamFailure;

    /** A lobby with one player: the host. */
    public static RoomSnapshot room(String roomCode, String hostUserId, String hostName) {
        return RoomSnapshot.newBuilder()
                .setRoomCode(roomCode)
                .setHostUserId(hostUserId)
                .setPhase(RoomPhase.ROOM_PHASE_LOBBY)
                .setSettings(RoomSettings.newBuilder().setQuestionCount(10).setSecondsPerQuestion(20))
                .addPlayers(Player.newBuilder()
                        .setUserId(hostUserId)
                        .setDisplayName(hostName)
                        .setStatus(PlayerStatus.PLAYER_STATUS_CONNECTED))
                .build();
    }

    public static RoomEvent event(RoomEventType type, RoomSnapshot snapshot) {
        return RoomEvent.newBuilder().setType(type).setSnapshot(snapshot).build();
    }

    /** Sends an event on every open stream for the room. */
    public void push(String roomCode, RoomEvent event) {
        for (var stream : streams.getOrDefault(roomCode, List.of())) {
            if (!stream.isCancelled()) {
                stream.onNext(StreamRoomEventsResponse.newBuilder().setEvent(event).build());
            }
        }
    }

    /** Ends every open stream for the room: with an error, or normally if {@code status} is OK. */
    public void endStreams(String roomCode, Status status) {
        List<ServerCallStreamObserver<StreamRoomEventsResponse>> open = streams.remove(roomCode);
        for (var stream : open == null ? List.<ServerCallStreamObserver<StreamRoomEventsResponse>>of() : open) {
            if (stream.isCancelled()) {
                continue;
            }
            if (status.isOk()) {
                stream.onCompleted();
            } else {
                stream.onError(status.asRuntimeException());
            }
        }
    }

    /** How many times the Gateway opened an event stream for the room. */
    public long streamsRequested(String roomCode) {
        return calls.received(StreamRoomEventsRequest.class).stream()
                .filter(request -> request.getRoomCode().equals(roomCode))
                .count();
    }

    /** Streams for the room the Gateway hasn't cancelled. */
    public long openStreams(String roomCode) {
        return streams.getOrDefault(roomCode, List.of()).stream().filter(stream -> !stream.isCancelled()).count();
    }

    /** New streams fail at once with {@code status} (null: they work). */
    public void failStreams(Status status) {
        streamFailure = status;
    }

    void reset() {
        calls.reset();
        streams.values().forEach(open -> open.forEach(stream -> {
            if (!stream.isCancelled()) {
                stream.onCompleted();
            }
        }));
        streams.clear();
        streamFailure = null;
    }

    @Override
    public void streamRoomEvents(StreamRoomEventsRequest request, StreamObserver<StreamRoomEventsResponse> observer) {
        calls.record(request);
        Status failure = streamFailure;
        if (failure != null) {
            observer.onError(failure.asRuntimeException());
            return;
        }
        var stream = (ServerCallStreamObserver<StreamRoomEventsResponse>) observer;
        streams.computeIfAbsent(request.getRoomCode(), code -> new CopyOnWriteArrayList<>()).add(stream);
        stream.onNext(StreamRoomEventsResponse.newBuilder()
                .setEvent(event(RoomEventType.ROOM_EVENT_TYPE_SNAPSHOT, room(request.getRoomCode(), "user-host", "Host")))
                .build());
    }

    @Override
    public void createRoom(CreateRoomRequest request, StreamObserver<CreateRoomResponse> observer) {
        calls.handle(request, CreateRoomResponse.newBuilder()
                .setSnapshot(room("K7QM2X", request.getHostUserId(), request.getHostDisplayName()))
                .build(), observer);
    }

    @Override
    public void joinRoom(JoinRoomRequest request, StreamObserver<JoinRoomResponse> observer) {
        calls.handle(request, JoinRoomResponse.newBuilder()
                .setSnapshot(room(request.getRoomCode(), "user-host", "Host").toBuilder()
                        .addPlayers(Player.newBuilder()
                                .setUserId(request.getUserId())
                                .setDisplayName(request.getDisplayName())
                                .setStatus(PlayerStatus.PLAYER_STATUS_CONNECTED)))
                .build(), observer);
    }

    @Override
    public void getRoomSnapshot(GetRoomSnapshotRequest request, StreamObserver<GetRoomSnapshotResponse> observer) {
        calls.handle(request, GetRoomSnapshotResponse.newBuilder()
                .setSnapshot(room(request.getRoomCode(), "user-host", "Host"))
                .build(), observer);
    }

    @Override
    public void leaveRoom(LeaveRoomRequest request, StreamObserver<LeaveRoomResponse> observer) {
        calls.handle(request, LeaveRoomResponse.getDefaultInstance(), observer);
    }

    @Override
    public void kickPlayer(KickPlayerRequest request, StreamObserver<KickPlayerResponse> observer) {
        calls.handle(request, KickPlayerResponse.getDefaultInstance(), observer);
    }

    @Override
    public void updateSettings(UpdateSettingsRequest request, StreamObserver<UpdateSettingsResponse> observer) {
        calls.handle(request, UpdateSettingsResponse.getDefaultInstance(), observer);
    }

    @Override
    public void startQuiz(StartQuizRequest request, StreamObserver<StartQuizResponse> observer) {
        calls.handle(request, StartQuizResponse.getDefaultInstance(), observer);
    }

    @Override
    public void submitAnswer(SubmitAnswerRequest request, StreamObserver<SubmitAnswerResponse> observer) {
        calls.handle(request, SubmitAnswerResponse.getDefaultInstance(), observer);
    }

    @Override
    public void reportDisconnected(ReportDisconnectedRequest request, StreamObserver<ReportDisconnectedResponse> observer) {
        calls.handle(request, ReportDisconnectedResponse.getDefaultInstance(), observer);
    }

    @Override
    public void reportConnectionQuality(
            ReportConnectionQualityRequest request, StreamObserver<ReportConnectionQualityResponse> observer) {
        calls.handle(request, ReportConnectionQualityResponse.getDefaultInstance(), observer);
    }

    @Override
    public void castVote(CastVoteRequest request, StreamObserver<CastVoteResponse> observer) {
        calls.handle(request, CastVoteResponse.getDefaultInstance(), observer);
    }
}
