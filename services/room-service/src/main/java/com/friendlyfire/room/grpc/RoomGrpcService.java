package com.friendlyfire.room.grpc;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
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
import com.friendlyfire.proto.friendlyfire.room.v1.RemovePlayerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.RemovePlayerResponse;
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
    private final AccountServiceBlockingStub accounts;

    public RoomGrpcService(
            RoomRepository rooms,
            QuizServiceBlockingStub quiz,
            ScoringServiceBlockingStub scoring,
            AccountServiceBlockingStub accounts) {
        this.rooms = rooms;
        this.quiz = quiz;
        this.scoring = scoring;
        this.accounts = accounts;
    }

    @Override
    public void createRoom(CreateRoomRequest request, StreamObserver<CreateRoomResponse> responseObserver) {
        // TODO(ff): create a room with the caller as host and return its snapshot
        //   - new code (RoomCodeGenerator), LOBBY phase, default settings (10 questions,
        //     20 seconds each), the host as the only player
        //   - the host's data status from Account (GetDataStatus): data_status,
        //     data_estimated_seconds, and not_enough_matches (fewer than 5 matches stored);
        //     riot_tag_line from the request
        //   - errors: none in the contract
        //   - uses: rooms, RoomCodeGenerator, accounts (GetDataStatus)
        notImplemented(responseObserver);
    }

    @Override
    public void joinRoom(JoinRoomRequest request, StreamObserver<JoinRoomResponse> responseObserver) {
        // TODO(ff): add the user to a room, or mark them connected again if they're in it
        //   - a returning player (page refresh, dropped connection) keeps their place
        //   - publish PLAYER_JOINED or PLAYER_RECONNECTED
        //   - someone new joining while the room is PREPARING or in a quiz joins as
        //     PLAYER_STATUS_SPECTATING: in players (they see the room and chat) but not in
        //     the questions, options, answers, votes or scoreboard
        //   - errors: NOT_FOUND; RESOURCE_EXHAUSTED if the room already has 8 people
        //     (spectators count)
        //   - the player's data status from Account, as in createRoom; riot_tag_line from
        //     the request
        //   - uses: rooms, accounts (GetDataStatus), RoomEventPublisher
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
        // TODO(ff): host only: remove another player, only allowed in the lobby (not PREPARING)
        //   - publish PLAYER_KICKED and return the new snapshot
        //   - errors: NOT_FOUND; PERMISSION_DENIED if the caller isn't the host;
        //     FAILED_PRECONDITION outside the lobby
        //   - uses: rooms, RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void updateSettings(UpdateSettingsRequest request, StreamObserver<UpdateSettingsResponse> responseObserver) {
        // TODO(ff): host only: change the settings, only allowed in the lobby (not PREPARING)
        //   - 5–20 questions, 10–45 seconds per question; publish SETTINGS_CHANGED
        //   - errors: INVALID_ARGUMENT if a value is out of range; NOT_FOUND;
        //     PERMISSION_DENIED if the caller isn't the host; FAILED_PRECONDITION
        //     outside the lobby
        //   - uses: rooms, RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void startQuiz(StartQuizRequest request, StreamObserver<StartQuizResponse> responseObserver) {
        // TODO(ff): host only: check the room, move it to PREPARING and return; build the
        //   questions in the background, then start a short countdown
        //   - allowed in the lobby, or after a quiz finishes to play again; refused while
        //     PREPARING (no second Start, and the host can't cancel)
        //   - check every player's data status again (AccountService.GetDataStatus): all
        //     SYNCED, at least 2 with 5+ matches, at most half the room under 5
        //   - then set ROOM_PHASE_PREPARING with phase_deadline = now + 8 s, publish
        //     QUIZ_PREPARING and return; every browser shows the loading screen
        //   - in the background: get the questions (QuizService.GenerateQuiz, marking
        //     not_enough_matches), register the quiz (ScoringService.StartQuiz), then hand
        //     the room to QuizRunner when the questions are ready AND phase_deadline has
        //     passed (STARTING; or VOTE first if someone is disconnected, ADR-026)
        //   - if GenerateQuiz fails (e.g. not enough match data for the chosen number of
        //     questions) or takes over 60 s: back to LOBBY, publish QUIZ_START_FAILED with
        //     RoomEvent.message saying why ("Not enough matches for 15 questions. Try 10.")
        //   - a player who drops while preparing stays in the quiz (player and option)
        //   - send GenerateQuiz the template_id of every question already asked in this
        //     room, oldest first (asked_template_ids), and remember the new questions'
        //     template_ids for the next quiz
        //   - errors: FAILED_PRECONDITION, with a message for the host, if there are fewer
        //     than 2 players or one of the data checks above fails (generation failures
        //     come later as QUIZ_START_FAILED); NOT_FOUND; PERMISSION_DENIED if not the host
        //   - uses: rooms, accounts, quiz, scoring, QuizRunner, RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void submitAnswer(SubmitAnswerRequest request, StreamObserver<SubmitAnswerResponse> responseObserver) {
        // TODO(ff): record a player's answer to the current question
        //   - use the time it arrives here (the Clock), never a time from the browser
        //   - publish PLAYER_ANSWERED: that they answered, never what they picked
        //   - errors: FAILED_PRECONDITION if no question is open, question_id isn't the
        //     current one, time is up, or they're SPECTATING; ALREADY_EXISTS if they
        //     already answered;
        //     INVALID_ARGUMENT if option_id isn't one of the question's options
        //   - uses: rooms, QuizRunner, the Clock, RoomEventPublisher
        notImplemented(responseObserver);
    }

    @Override
    public void reportDisconnected(
            ReportDisconnectedRequest request, StreamObserver<ReportDisconnectedResponse> responseObserver) {
        // TODO(ff): a player's WebSocket connection dropped (called by the Gateway)
        //   - mark them DISCONNECTED and publish PLAYER_DISCONNECTED
        //   - during a quiz the current question finishes (ADR-026); after its reveal, if
        //     they're still gone, start a vote (VOTE phase, VOTE_STARTED)
        //   - while PREPARING nothing changes until the questions are ready; then, if
        //     they're still gone, the vote happens before the countdown
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
        //   - publish VOTE_CAST with the new counts; the vote ends when everyone has voted
        //     or its deadline passes; most votes wins; a tie pauses if pause time is left,
        //     otherwise continues; publish VOTE_RESOLVED
        //   - PAUSE: ROOM_PHASE_PAUSED for up to 60 seconds (QUIZ_PAUSED), at most 180
        //     seconds per quiz (pause_seconds_left, Vote.pause_allowed); if the minute ends
        //     with the player still gone, vote again
        //   - END_QUIZ makes the scores so far final (ScoringService.FinalizeQuiz)
        //   - errors: FAILED_PRECONDITION outside the VOTE phase or for a SPECTATING player;
        //     NOT_FOUND; PERMISSION_DENIED if the user isn't in the room
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

    @Override
    public void removePlayer(RemovePlayerRequest request, StreamObserver<RemovePlayerResponse> responseObserver) {
        // TODO(ff): take a user who is deleting their account out of their room (ADR-027)
        //   - the Gateway calls this before AccountService.DeleteAccount
        //   - like LeaveRoom: the host role moves on, an empty room closes; publish PLAYER_LEFT
        //   - not an error if they aren't in a room
        //   - uses: rooms, RoomEventPublisher
        notImplemented(responseObserver);
    }

    private static void notImplemented(StreamObserver<?> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.withDescription("not implemented yet").asRuntimeException());
    }
}
