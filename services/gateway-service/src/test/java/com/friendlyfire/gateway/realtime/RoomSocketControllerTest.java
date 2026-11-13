package com.friendlyfire.gateway.realtime;

import static com.friendlyfire.gateway.testsupport.FakeServices.ROOM;
import static com.friendlyfire.gateway.testsupport.StompTestClient.next;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.friendlyfire.gateway.testsupport.FakeTokens;
import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import com.friendlyfire.gateway.testsupport.StompTestClient;
import com.friendlyfire.gateway.testsupport.TestNames;
import com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.ConnectionQuality;
import com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.VoteOption;
import com.google.protobuf.Message;
import io.grpc.Status;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

/** A player's room actions over STOMP become RoomService calls with the session's user (FF-306, 310, 416, 418, 419). */
class RoomSocketControllerTest extends GatewayTestBase {

    @LocalServerPort
    private int port;

    private String player;
    private String room;
    private StompTestClient client;
    private BlockingQueue<Map<String, Object>> errors;

    @BeforeEach
    void joinARoom() throws Exception {
        player = TestNames.player();
        room = TestNames.roomCode();
        client = StompTestClient.connect(port, FakeTokens.token(player));
        errors = client.subscribe("/user/queue/errors");
        client.subscribe("/topic/room/" + room);
        client.sync();
    }

    @AfterEach
    void disconnect() {
        client.close();
    }

    @Test
    void leave() {
        client.send(action("leave"), Map.of());

        LeaveRoomRequest request = awaitCall(LeaveRoomRequest.class);
        assertThat(request.getRoomCode()).isEqualTo(room);
        assertThat(request.getUserId()).isEqualTo(FakeTokens.userId(player));
    }

    @Test
    void kickUsesTheSessionsPlayerNeverOneFromTheBody() {
        client.send(action("kick"), Map.of("targetUserId", "user-sam", "userId", "user-someone-else"));

        KickPlayerRequest request = awaitCall(KickPlayerRequest.class);
        assertThat(request.getUserId()).isEqualTo(FakeTokens.userId(player));
        assertThat(request.getTargetUserId()).isEqualTo("user-sam");
    }

    @Test
    void settings() {
        client.send(action("settings"), Map.of("questionCount", 15, "secondsPerQuestion", 30));

        UpdateSettingsRequest request = awaitCall(UpdateSettingsRequest.class);
        assertThat(request.getSettings().getQuestionCount()).isEqualTo(15);
        assertThat(request.getSettings().getSecondsPerQuestion()).isEqualTo(30);
    }

    @Test
    void startGivesRoomLongerThanTheDefaultDeadline() {
        client.send(action("start"), Map.of());

        assertThat(awaitCall(StartQuizRequest.class).getUserId()).isEqualTo(FakeTokens.userId(player));
        assertThat(ROOM.calls.deadlineMillis(StartQuizRequest.class)).isGreaterThan(10_000L);
    }

    @Test
    void aRefusedStartTellsTheHostWhy() throws Exception {
        ROOM.calls.fail(StartQuizRequest.class, Status.FAILED_PRECONDITION.withDescription("Sam is still syncing."));
        client.send(action("start"), Map.of());

        Map<String, Object> error = next(errors);
        assertThat(error).containsEntry("destination", action("start"));
        assertThat(error.get("error")).isEqualTo(Map.of("code", "FAILED_PRECONDITION", "message", "Sam is still syncing."));
    }

    @Test
    void answer() {
        client.send(action("answer"), Map.of("questionId", "q-3", "optionId", "user-sam"));

        SubmitAnswerRequest request = awaitCall(SubmitAnswerRequest.class);
        assertThat(request.getRoomCode()).isEqualTo(room);
        assertThat(request.getUserId()).isEqualTo(FakeTokens.userId(player));
        assertThat(request.getQuestionId()).isEqualTo("q-3");
        assertThat(request.getOptionId()).isEqualTo("user-sam");
    }

    @Test
    void thePauseVoteIsTheContractsWaitOption() {
        client.send(action("vote"), Map.of("option", "PAUSE"));

        assertThat(awaitCall(CastVoteRequest.class).getOption()).isEqualTo(VoteOption.VOTE_OPTION_WAIT);
    }

    @Test
    void anUnknownVoteIsInvalid() throws Exception {
        client.send(action("vote"), Map.of("option", "MAYBE"));

        Map<String, Object> error = next(errors);
        assertThat(error.get("error")).extracting("code").isEqualTo("INVALID_ARGUMENT");
        assertThat(ROOM.calls.received(CastVoteRequest.class)).isEmpty();
    }

    @Test
    void connectionQuality() {
        client.send(action("connection-quality"), Map.of("quality", "FAIR"));

        assertThat(awaitCall(ReportConnectionQualityRequest.class).getQuality())
                .isEqualTo(ConnectionQuality.CONNECTION_QUALITY_FAIR);
    }

    private String action(String name) {
        return "/app/room/" + room + "/" + name;
    }

    private static <T extends Message> T awaitCall(Class<T> type) {
        await().atMost(Duration.ofSeconds(5)).until(() -> ROOM.calls.last(type) != null);
        return ROOM.calls.last(type);
    }
}
