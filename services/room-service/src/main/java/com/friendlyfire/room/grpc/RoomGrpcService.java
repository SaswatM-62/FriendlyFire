package com.friendlyfire.room.grpc;

import com.friendlyfire.proto.friendlyfire.matchdata.v1.MatchDataServiceGrpc.MatchDataServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.quiz.v1.QuizServiceGrpc.QuizServiceBlockingStub;
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
import com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc;
import com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceGrpc.ScoringServiceBlockingStub;
import com.friendlyfire.room.room.RoomRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

/**
 * friendlyfire.room.v1.RoomService. Every RPC is a stub for now.
 *
 * <p>Rules every RPC follows: the user_id comes from the Gateway (from the checked
 * Auth0 token), never from the browser; NOT_FOUND if the room doesn't exist;
 * PERMISSION_DENIED if the user isn't in the room, or isn't the host for host-only
 * actions.
 */
@Service
public class RoomGrpcService extends RoomServiceGrpc.RoomServiceImplBase {

    private final RoomRepository rooms;
    private final QuizServiceBlockingStub quiz;
    private final ScoringServiceBlockingStub scoring;
    private final MatchDataServiceBlockingStub matchData;

    public RoomGrpcService(
            RoomRepository rooms,
            QuizServiceBlockingStub quiz,
            ScoringServiceBlockingStub scoring,
            MatchDataServiceBlockingStub matchData) {
        this.rooms = rooms;
        this.quiz = quiz;
        this.scoring = scoring;
        this.matchData = matchData;
    }

    @Override
    public void createRoom(CreateRoomRequest request, StreamObserver<CreateRoomResponse> responseObserver) {
        // TODO(ff): create a room with the caller as host and return its snapshot
        //   - new code (RoomCodeGenerator), LOBBY phase, default settings (10 questions,
        //     20 seconds each), the host as the only player
        //   - ask Match Data whether the host's matches are synced (match_data_ready)
        //   - errors: none in the contract
        //   - uses: rooms, RoomCodeGenerator, matchData (GetSyncStatus)
        notImplemented(responseObserver);
    }

    @Override
    public void joinRoom(JoinRoomRequest request, StreamObserver<JoinRoomResponse> responseObserver) {
        // TODO(ff): add the user to a room, or mark them connected again if they're in it
        //   - a returning player (page refresh, dropped connection) keeps their place
        //   - publish PLAYER_JOINED or PLAYER_RECONNECTED
        //   - errors: NOT_FOUND; RESOURCE_EXHAUSTED if the room already has 8 players;
        //     FAILED_PRECONDITION if the quiz has started and the user wasn't in it
        //   - uses: rooms, matchData (GetSyncStatus), RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void leaveRoom(LeaveRoomRequest request, StreamObserver<LeaveRoomResponse> responseObserver) {
        // TODO(ff): the user leaves for good
        //   - if the host leaves, the player who joined next becomes host (HOST_CHANGED)
        //   - the room closes when the last player leaves (ROOM_CLOSED ends the streams)
        //   - errors: NOT_FOUND; PERMISSION_DENIED if the user isn't in the room
        //   - uses: rooms, RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void kickPlayer(KickPlayerRequest request, StreamObserver<KickPlayerResponse> responseObserver) {
        // TODO(ff): host only: remove another player, only allowed in the lobby
        //   - publish PLAYER_KICKED and return the new snapshot
        //   - errors: NOT_FOUND; PERMISSION_DENIED if the caller isn't the host;
        //     FAILED_PRECONDITION outside the lobby
        //   - uses: rooms, RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void updateSettings(UpdateSettingsRequest request, StreamObserver<UpdateSettingsResponse> responseObserver) {
        // TODO(ff): host only: change the settings, only allowed in the lobby
        //   - 5–20 questions, 10–45 seconds per question; publish SETTINGS_CHANGED
        //   - errors: INVALID_ARGUMENT if a value is out of range; NOT_FOUND;
        //     PERMISSION_DENIED if the caller isn't the host; FAILED_PRECONDITION
        //     outside the lobby
        //   - uses: rooms, RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void startQuiz(StartQuizRequest request, StreamObserver<StartQuizResponse> responseObserver) {
        // TODO(ff): host only: generate every question, then start a short countdown
        //   - allowed in the lobby, or after a quiz finishes to play again
        //   - check every player's sync is COMPLETE (MatchDataService.GetSyncStatus), get
        //     the questions (QuizService.GenerateQuiz), register the quiz
        //     (ScoringService.StartQuiz), then hand the room to QuizRunner (STARTING)
        //   - errors: FAILED_PRECONDITION, with a message for the host, if there are fewer
        //     than 2 players, someone's matches haven't finished syncing, or there isn't
        //     enough match data; NOT_FOUND; PERMISSION_DENIED if not the host
        //   - uses: rooms, matchData, quiz, scoring, QuizRunner, RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void submitAnswer(SubmitAnswerRequest request, StreamObserver<SubmitAnswerResponse> responseObserver) {
        // TODO(ff): record a player's answer to the current question
        //   - use the time it arrives here (the Clock), never a time from the browser
        //   - publish PLAYER_ANSWERED: that they answered, never what they picked
        //   - errors: FAILED_PRECONDITION if no question is open, question_id isn't the
        //     current one, or time is up; ALREADY_EXISTS if they already answered;
        //     INVALID_ARGUMENT if option_id isn't one of the question's options
        //   - uses: rooms, QuizRunner, the Clock, RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void reportDisconnected(
            ReportDisconnectedRequest request, StreamObserver<ReportDisconnectedResponse> responseObserver) {
        // TODO(ff): a player's WebSocket connection dropped (called by the Gateway)
        //   - mark them DISCONNECTED and publish PLAYER_DISCONNECTED
        //   - during a quiz, if they don't come back within 15 seconds, pause the room
        //     and start a vote (VOTE phase, VOTE_STARTED)
        //   - errors: NOT_FOUND; PERMISSION_DENIED if the user isn't in the room
        //   - uses: rooms, QuizRunner (timers), RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void reportConnectionQuality(
            ReportConnectionQualityRequest request, StreamObserver<ReportConnectionQualityResponse> responseObserver) {
        // TODO(ff): store a player's latest connection quality (from the network bar's pings)
        //   - publish CONNECTION_QUALITY_CHANGED only when the level actually changes
        //   - errors: NOT_FOUND; PERMISSION_DENIED if the user isn't in the room
        //   - uses: rooms, RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void castVote(CastVoteRequest request, StreamObserver<CastVoteResponse> responseObserver) {
        // TODO(ff): a connected player's vote while the room is in the VOTE phase
        //   - the vote ends when everyone has voted or its deadline passes; most votes
        //     wins, a tie means WAIT (another 30 seconds); publish VOTE_RESOLVED
        //   - END_QUIZ makes the scores so far final (ScoringService.FinalizeQuiz)
        //   - errors: FAILED_PRECONDITION outside the VOTE phase; NOT_FOUND;
        //     PERMISSION_DENIED if the user isn't in the room
        //   - uses: rooms, QuizRunner, scoring, RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void getRoomSnapshot(GetRoomSnapshotRequest request, StreamObserver<GetRoomSnapshotResponse> responseObserver) {
        // TODO(ff): return the room's current state
        //   - never include the answer before the reveal (QuestionView.reveal unset)
        //   - errors: NOT_FOUND; PERMISSION_DENIED if the user isn't in the room
        //   - uses: rooms
        notImplemented(responseObserver);
    }

    @Override
    public void streamRoomEvents(
            StreamRoomEventsRequest request, StreamObserver<StreamRoomEventsResponse> responseObserver) {
        // TODO(ff): a live feed of everything that happens in a room
        //   - the first message is always SNAPSHOT; end the stream after ROOM_CLOSED
        //   - the Gateway opens one stream per room and forwards every event to the STOMP
        //     topic /topic/room/{room_code}
        //   - errors: NOT_FOUND
        //   - uses: rooms, RoomEventPublisher
        notImplemented(responseObserver);
    }

    private static void notImplemented(StreamObserver<?> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.withDescription("not implemented yet").asRuntimeException());
    }
}
