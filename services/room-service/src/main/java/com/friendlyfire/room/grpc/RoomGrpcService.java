package com.friendlyfire.room.grpc;

import com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.GetChatHistoryRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.GetChatHistoryResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.RemovePlayerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.RemovePlayerResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc;
import com.friendlyfire.proto.friendlyfire.room.v1.SendChatMessageRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.SendChatMessageResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse;
import com.friendlyfire.room.room.RoomManager;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * friendlyfire.room.v1.RoomService: each RPC hands its request to RoomManager and sends
 * back the result or the contract's error.
 *
 * <p>Rules every RPC follows: the user_id comes from the Gateway (from the checked
 * Auth0 token), never from the browser; NOT_FOUND if the room doesn't exist;
 * PERMISSION_DENIED if the user isn't in the room, or isn't the host for host-only
 * actions.
 */
@Service
public class RoomGrpcService extends RoomServiceGrpc.RoomServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(RoomGrpcService.class);

    private final RoomManager rooms;

    public RoomGrpcService(RoomManager rooms) {
        this.rooms = rooms;
    }

    @Override
    public void createRoom(CreateRoomRequest request, StreamObserver<CreateRoomResponse> responseObserver) {
        respond(responseObserver, () -> CreateRoomResponse.newBuilder()
                .setSnapshot(rooms.createRoom(
                        request.getHostUserId(), request.getHostDisplayName(), request.getHostRiotTagLine()))
                .build());
    }

    @Override
    public void joinRoom(JoinRoomRequest request, StreamObserver<JoinRoomResponse> responseObserver) {
        respond(responseObserver, () -> JoinRoomResponse.newBuilder()
                .setSnapshot(rooms.joinRoom(
                        request.getRoomCode(), request.getUserId(), request.getDisplayName(), request.getRiotTagLine()))
                .build());
    }

    @Override
    public void leaveRoom(LeaveRoomRequest request, StreamObserver<LeaveRoomResponse> responseObserver) {
        respond(responseObserver, () -> {
            rooms.leaveRoom(request.getRoomCode(), request.getUserId());
            return LeaveRoomResponse.getDefaultInstance();
        });
    }

    @Override
    public void kickPlayer(KickPlayerRequest request, StreamObserver<KickPlayerResponse> responseObserver) {
        respond(responseObserver, () -> KickPlayerResponse.newBuilder()
                .setSnapshot(rooms.kickPlayer(request.getRoomCode(), request.getUserId(), request.getTargetUserId()))
                .build());
    }

    @Override
    public void updateSettings(UpdateSettingsRequest request, StreamObserver<UpdateSettingsResponse> responseObserver) {
        respond(responseObserver, () -> UpdateSettingsResponse.newBuilder()
                .setSnapshot(rooms.updateSettings(request.getRoomCode(), request.getUserId(), request.getSettings()))
                .build());
    }

    @Override
    public void startQuiz(StartQuizRequest request, StreamObserver<StartQuizResponse> responseObserver) {
        respond(responseObserver, () -> StartQuizResponse.newBuilder()
                .setSnapshot(rooms.startQuiz(request.getRoomCode(), request.getUserId()))
                .build());
    }

    @Override
    public void submitAnswer(SubmitAnswerRequest request, StreamObserver<SubmitAnswerResponse> responseObserver) {
        respond(responseObserver, () -> {
            rooms.submitAnswer(request.getRoomCode(), request.getUserId(), request.getQuestionId(), request.getOptionId());
            return SubmitAnswerResponse.getDefaultInstance();
        });
    }

    @Override
    public void reportDisconnected(
            ReportDisconnectedRequest request, StreamObserver<ReportDisconnectedResponse> responseObserver) {
        respond(responseObserver, () -> {
            rooms.reportDisconnected(request.getRoomCode(), request.getUserId());
            return ReportDisconnectedResponse.getDefaultInstance();
        });
    }

    @Override
    public void reportConnectionQuality(
            ReportConnectionQualityRequest request, StreamObserver<ReportConnectionQualityResponse> responseObserver) {
        respond(responseObserver, () -> {
            rooms.reportConnectionQuality(request.getRoomCode(), request.getUserId(), request.getQuality());
            return ReportConnectionQualityResponse.getDefaultInstance();
        });
    }

    @Override
    public void castVote(CastVoteRequest request, StreamObserver<CastVoteResponse> responseObserver) {
        respond(responseObserver, () -> {
            rooms.castVote(request.getRoomCode(), request.getUserId(), request.getOption());
            return CastVoteResponse.getDefaultInstance();
        });
    }

    @Override
    public void getRoomSnapshot(GetRoomSnapshotRequest request, StreamObserver<GetRoomSnapshotResponse> responseObserver) {
        respond(responseObserver, () -> GetRoomSnapshotResponse.newBuilder()
                .setSnapshot(rooms.snapshot(request.getRoomCode(), request.getUserId()))
                .build());
    }

    @Override
    public void streamRoomEvents(
            StreamRoomEventsRequest request, StreamObserver<StreamRoomEventsResponse> responseObserver) {
        try {
            rooms.stream(request.getRoomCode(), responseObserver);
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void removePlayer(RemovePlayerRequest request, StreamObserver<RemovePlayerResponse> responseObserver) {
        respond(responseObserver, () -> {
            rooms.removePlayer(request.getUserId());
            return RemovePlayerResponse.getDefaultInstance();
        });
    }

    @Override
    public void sendChatMessage(
            SendChatMessageRequest request, StreamObserver<SendChatMessageResponse> responseObserver) {
        respond(responseObserver, () -> SendChatMessageResponse.newBuilder()
                .setMessage(rooms.sendChat(request.getRoomCode(), request.getUserId(), request.getText()))
                .build());
    }

    @Override
    public void getChatHistory(GetChatHistoryRequest request, StreamObserver<GetChatHistoryResponse> responseObserver) {
        respond(responseObserver, () -> GetChatHistoryResponse.newBuilder()
                .addAllMessages(rooms.chatHistory(request.getRoomCode(), request.getUserId()))
                .build());
    }

    private static <T> void respond(StreamObserver<T> observer, Supplier<T> call) {
        T response;
        try {
            response = call.get();
        } catch (StatusRuntimeException e) {
            observer.onError(e);
            return;
        } catch (RuntimeException e) {
            log.error("room RPC failed", e);
            observer.onError(Status.INTERNAL.withDescription("Something went wrong in the room.").asRuntimeException());
            return;
        }
        observer.onNext(response);
        observer.onCompleted();
    }
}
