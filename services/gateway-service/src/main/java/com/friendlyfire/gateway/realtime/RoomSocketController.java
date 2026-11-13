package com.friendlyfire.gateway.realtime;

import com.friendlyfire.gateway.protection.RateLimit;
import com.friendlyfire.gateway.protection.RateLimiter;
import com.friendlyfire.gateway.web.ApiException;
import com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.ConnectionQuality;
import com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSettings;
import com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.VoteOption;
import java.security.Principal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * A player's actions in a room, sent over STOMP to /app/room/{code}/... (bodies in
 * docs/realtime-messages.md). Each one calls the matching RoomService RPC with the
 * session's user_id; what changes reaches everyone through RoomEventRelay. A failed
 * action goes back to the player on /user/queue/errors (StompExceptionHandler).
 * ConnectInterceptor has already checked that the session watches this room.
 */
@Controller
public class RoomSocketController {

    /** StartQuiz generates the whole quiz before it answers (FF-006). */
    static final Duration START_QUIZ_DEADLINE = Duration.ofSeconds(15);

    private final RoomServiceBlockingStub rooms;
    private final RateLimiter rateLimiter;
    private final SessionRegistry sessions;

    public RoomSocketController(RoomServiceBlockingStub rooms, RateLimiter rateLimiter, SessionRegistry sessions) {
        this.rooms = rooms;
        this.rateLimiter = rateLimiter;
        this.sessions = sessions;
    }

    /** The player leaves for good (FF-306). */
    @MessageMapping("/room/{code}/leave")
    public void leave(@DestinationVariable String code, Principal user) {
        rooms.leaveRoom(LeaveRoomRequest.newBuilder().setRoomCode(code).setUserId(userId(user)).build());
    }

    /** Host only, in the lobby: removes a player (FF-306). */
    @MessageMapping("/room/{code}/kick")
    public void kick(@DestinationVariable String code, @Payload(required = false) Kick body, Principal user) {
        rooms.kickPlayer(KickPlayerRequest.newBuilder()
                .setRoomCode(code)
                .setUserId(userId(user))
                .setTargetUserId(text(require(body).targetUserId()))
                .build());
    }

    /** Host only, in the lobby: question count and time per question (FF-306). */
    @MessageMapping("/room/{code}/settings")
    public void settings(@DestinationVariable String code, @Payload(required = false) Settings body, Principal user) {
        Settings settings = require(body);
        rooms.updateSettings(UpdateSettingsRequest.newBuilder()
                .setRoomCode(code)
                .setUserId(userId(user))
                .setSettings(RoomSettings.newBuilder()
                        .setQuestionCount(number(settings.questionCount()))
                        .setSecondsPerQuestion(number(settings.secondsPerQuestion())))
                .build());
    }

    /**
     * Host only: starts the quiz, or plays again after one (FF-416). Room's
     * FAILED_PRECONDITION message (fewer than 2 players, someone still syncing...) goes
     * back to the host.
     */
    @MessageMapping("/room/{code}/start")
    public void start(@DestinationVariable String code, Principal user) {
        rooms.withDeadlineAfter(START_QUIZ_DEADLINE.toMillis(), TimeUnit.MILLISECONDS)
                .startQuiz(StartQuizRequest.newBuilder().setRoomCode(code).setUserId(userId(user)).build());
    }

    /** An answer to the current question (FF-418). Room times it on arrival. */
    @MessageMapping("/room/{code}/answer")
    public void answer(
            @DestinationVariable String code, @Payload(required = false) Answer body, Principal user,
            @Header(SimpMessageHeaderAccessor.SESSION_ID_HEADER) String sessionId) {
        String userId = userId(user);
        rateLimiter.check(RateLimit.ANSWER, userId, sessions.remoteIp(sessionId));
        Answer answer = require(body);
        rooms.submitAnswer(SubmitAnswerRequest.newBuilder()
                .setRoomCode(code)
                .setUserId(userId)
                .setQuestionId(text(answer.questionId()))
                .setOptionId(text(answer.optionId()))
                .build());
    }

    /** A vote about a disconnected player (FF-419). */
    @MessageMapping("/room/{code}/vote")
    public void vote(@DestinationVariable String code, @Payload(required = false) Vote body, Principal user) {
        rooms.castVote(CastVoteRequest.newBuilder()
                .setRoomCode(code)
                .setUserId(userId(user))
                .setOption(voteOption(require(body).option()))
                .build());
    }

    /** The player's connection quality from their pings, sent when the level changes (FF-310). */
    @MessageMapping("/room/{code}/connection-quality")
    public void connectionQuality(
            @DestinationVariable String code, @Payload(required = false) Quality body, Principal user) {
        rooms.reportConnectionQuality(ReportConnectionQualityRequest.newBuilder()
                .setRoomCode(code)
                .setUserId(userId(user))
                .setQuality(quality(require(body).quality()))
                .build());
    }

    /**
     * "PAUSE" is the frontend's name for the contract's WAIT option (ADR-026 turns it into
     * a pause); "WAIT" is accepted too.
     */
    static VoteOption voteOption(String option) {
        return switch (option == null ? "" : option) {
            case "PAUSE", "WAIT" -> VoteOption.VOTE_OPTION_WAIT;
            case "CONTINUE_WITHOUT_PLAYER" -> VoteOption.VOTE_OPTION_CONTINUE_WITHOUT_PLAYER;
            case "END_QUIZ" -> VoteOption.VOTE_OPTION_END_QUIZ;
            default -> throw invalid("That isn't one of the vote options.");
        };
    }

    static ConnectionQuality quality(String quality) {
        return switch (quality == null ? "" : quality) {
            case "GOOD" -> ConnectionQuality.CONNECTION_QUALITY_GOOD;
            case "FAIR" -> ConnectionQuality.CONNECTION_QUALITY_FAIR;
            case "POOR" -> ConnectionQuality.CONNECTION_QUALITY_POOR;
            default -> throw invalid("The connection quality must be GOOD, FAIR or POOR.");
        };
    }

    private static String userId(Principal user) {
        if (user == null) {
            throw ApiException.unauthenticated();
        }
        return user.getName();
    }

    private static <T> T require(T body) {
        if (body == null) {
            throw invalid("That message isn't valid.");
        }
        return body;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static int number(Integer value) {
        return value == null ? 0 : value;
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", message);
    }

    /** /kick body. */
    public record Kick(String targetUserId) {
    }

    /** /settings body: 5–20 questions, 10–45 seconds each (Room checks). */
    public record Settings(Integer questionCount, Integer secondsPerQuestion) {
    }

    /** /answer body. */
    public record Answer(String questionId, String optionId) {
    }

    /** /vote body: "PAUSE" (or "WAIT"), "CONTINUE_WITHOUT_PLAYER" or "END_QUIZ". */
    public record Vote(String option) {
    }

    /** /connection-quality body: "GOOD", "FAIR" or "POOR". */
    public record Quality(String quality) {
    }
}
