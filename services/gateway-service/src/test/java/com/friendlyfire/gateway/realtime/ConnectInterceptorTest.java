package com.friendlyfire.gateway.realtime;

import static com.friendlyfire.gateway.testsupport.FakeServices.ROOM;
import static com.friendlyfire.gateway.testsupport.StompTestClient.next;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.friendlyfire.gateway.testsupport.FakeTokens;
import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import com.friendlyfire.gateway.testsupport.StompTestClient;
import com.friendlyfire.gateway.testsupport.TestNames;
import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest;
import io.grpc.Status;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Who may connect, watch and send over the WebSocket (FF-104, FF-305). */
class ConnectInterceptorTest extends GatewayTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private RoomEventRelay relay;

    private final List<StompTestClient> clients = new ArrayList<>();

    @AfterEach
    void disconnect() {
        clients.forEach(StompTestClient::close);
    }

    @Test
    void signedInPlayerConnects() throws Exception {
        connect(FakeTokens.token(TestNames.player())).sync();
    }

    @Test
    void aTokenAuth0WouldRefuseIsRefused() throws Exception {
        assertThat(StompTestClient.refusal(port, FakeTokens.INVALID)).startsWith("UNAUTHENTICATED");
    }

    @Test
    void anUnverifiedEmailIsRefused() throws Exception {
        assertThat(StompTestClient.refusal(port, FakeTokens.unverified(TestNames.player()))).startsWith("EMAIL_NOT_VERIFIED");
    }

    @Test
    void theNewestConnectionWinsAndTheOldOneIsToldWhy() throws Exception {
        String player = TestNames.player();
        StompTestClient first = connect(FakeTokens.token(player));
        first.sync();

        StompTestClient second = connect(FakeTokens.token(player));

        assertThat(first.nextErrorFrame()).startsWith("ALREADY_CONNECTED");
        await().atMost(Duration.ofSeconds(5)).until(() -> !first.isConnected());
        second.sync();
    }

    @Test
    void replacingAConnectionIsntReportedAsADrop() throws Exception {
        String player = TestNames.player();
        String room = TestNames.roomCode();
        StompTestClient first = connect(FakeTokens.token(player));
        first.subscribe("/topic/room/" + room);
        first.sync();

        connect(FakeTokens.token(player)).sync();
        // The replaced session has ended once its room is no longer relayed here.
        await().atMost(Duration.ofSeconds(5)).until(() -> !relay.isRelaying(room));

        assertThat(ROOM.calls.received(ReportDisconnectedRequest.class)).isEmpty();
    }

    @Test
    void onceTheFirstConnectionClosesANewOneIsAccepted() throws Exception {
        String player = TestNames.player();
        StompTestClient first = StompTestClient.connect(port, FakeTokens.token(player));
        first.sync();
        first.close();

        await().atMost(Duration.ofSeconds(5)).ignoreExceptions()
                .until(() -> clients.add(StompTestClient.connect(port, FakeTokens.token(player))));
    }

    @Test
    void signedOutConnectionsCanPingOutsideProduction() throws Exception {
        connect(null).sync();
    }

    @Test
    void signedOutConnectionsCantWatchARoom() throws Exception {
        String room = TestNames.roomCode();
        StompTestClient client = connect(null);
        BlockingQueue<Map<String, Object>> errors = client.subscribe("/user/queue/errors");
        client.subscribe("/topic/room/" + room);

        Map<String, Object> error = next(errors);
        assertThat(code(error)).isEqualTo("UNAUTHENTICATED");
        assertThat(error).containsEntry("destination", "/topic/room/" + room);
        assertThat(ROOM.streamsRequested(room)).isZero();
    }

    @Test
    void signedOutConnectionsCantSendToARoom() throws Exception {
        StompTestClient client = connect(null);
        BlockingQueue<Map<String, Object>> errors = client.subscribe("/user/queue/errors");
        client.send("/app/room/K7QM2X/answer", Map.of("questionId", "q1", "optionId", "o1"));

        assertThat(code(next(errors))).isEqualTo("UNAUTHENTICATED");
        assertThat(ROOM.calls.received(SubmitAnswerRequest.class)).isEmpty();
    }

    @Test
    void watchingARoomAsksRoomWhetherTheSessionsPlayerIsIn() throws Exception {
        String player = TestNames.player();
        String room = TestNames.roomCode();
        StompTestClient client = connect(FakeTokens.token(player));
        client.subscribe("/topic/room/" + room);
        client.sync();

        GetRoomSnapshotRequest request = ROOM.calls.last(GetRoomSnapshotRequest.class);
        assertThat(request.getRoomCode()).isEqualTo(room);
        assertThat(request.getUserId()).isEqualTo(FakeTokens.userId(player));
    }

    @Test
    void playersCantWatchARoomTheyAreNotIn() throws Exception {
        ROOM.calls.fail(GetRoomSnapshotRequest.class, Status.PERMISSION_DENIED);
        String room = TestNames.roomCode();
        StompTestClient client = connect(FakeTokens.token(TestNames.player()));
        BlockingQueue<Map<String, Object>> errors = client.subscribe("/user/queue/errors");
        client.subscribe("/topic/room/" + room);

        assertThat(code(next(errors))).isEqualTo("NOT_IN_ROOM");
        client.sync();
        assertThat(ROOM.streamsRequested(room)).isZero();
    }

    @Test
    void aRoomThatDoesntExistCantBeWatched() throws Exception {
        ROOM.calls.fail(GetRoomSnapshotRequest.class, Status.NOT_FOUND);
        StompTestClient client = connect(FakeTokens.token(TestNames.player()));
        BlockingQueue<Map<String, Object>> errors = client.subscribe("/user/queue/errors");
        client.subscribe("/topic/room/" + TestNames.roomCode());

        assertThat(code(next(errors))).isEqualTo("ROOM_NOT_FOUND");
    }

    @Test
    void sendingToARoomTheSessionDoesntWatchIsRefused() throws Exception {
        StompTestClient client = connect(FakeTokens.token(TestNames.player()));
        BlockingQueue<Map<String, Object>> errors = client.subscribe("/user/queue/errors");
        client.send("/app/room/" + TestNames.roomCode() + "/answer", Map.of("questionId", "q1", "optionId", "o1"));

        assertThat(code(next(errors))).isEqualTo("NOT_IN_ROOM");
        assertThat(ROOM.calls.received(SubmitAnswerRequest.class)).isEmpty();
    }

    @Test
    void nobodyCanSubscribeToAnotherPlayersQueue() throws Exception {
        StompTestClient client = connect(FakeTokens.token(TestNames.player()));
        BlockingQueue<Map<String, Object>> errors = client.subscribe("/user/queue/errors");
        client.subscribe("/queue/errors-usersomeone");

        assertThat(code(next(errors))).isEqualTo("FORBIDDEN");
    }

    private StompTestClient connect(String token) throws Exception {
        StompTestClient client = StompTestClient.connect(port, token);
        clients.add(client);
        return client;
    }

    @SuppressWarnings("unchecked")
    private static String code(Map<String, Object> error) {
        return (String) ((Map<String, Object>) error.get("error")).get("code");
    }
}
