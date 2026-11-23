package com.friendlyfire.gateway.realtime;

import static com.friendlyfire.gateway.testsupport.FakeRoomService.event;
import static com.friendlyfire.gateway.testsupport.FakeRoomService.room;
import static com.friendlyfire.gateway.testsupport.FakeServices.ROOM;
import static com.friendlyfire.gateway.testsupport.StompTestClient.next;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.friendlyfire.gateway.testsupport.FakeTokens;
import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import com.friendlyfire.gateway.testsupport.StompTestClient;
import com.friendlyfire.gateway.testsupport.TestNames;
import com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase;
import com.google.protobuf.Timestamp;
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

/** Room events reach the browsers watching the room (FF-304), and dropped connections reach Room (FF-308). */
class RoomEventRelayTest extends GatewayTestBase {

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
    void forwardsEachEventAsJson() throws Exception {
        String room = TestNames.roomCode();
        BlockingQueue<Map<String, Object>> events = watch(TestNames.player(), room);

        ROOM.push(room, event(RoomEventType.ROOM_EVENT_TYPE_PLAYER_JOINED, room(room, "user-host", "Host").toBuilder()
                .setPhase(RoomPhase.ROOM_PHASE_STARTING)
                .setPhaseDeadline(Timestamp.newBuilder().setSeconds(1_762_700_000L).setNanos(120_000_000))
                .build()).toBuilder().setSubjectUserId("user-sam").build());

        Map<String, Object> event = nextOfType(events, "PLAYER_JOINED");
        assertThat(event).containsEntry("subjectUserId", "user-sam");
        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot = (Map<String, Object>) event.get("snapshot");
        assertThat(snapshot)
                .containsEntry("roomCode", room)
                .containsEntry("phase", "STARTING")
                .containsEntry("phaseDeadline", "2025-11-09T14:53:20.120Z")
                .doesNotContainKey("activeVote");
    }

    @Test
    void oneStreamPerRoomHoweverManyWatch() throws Exception {
        String room = TestNames.roomCode();
        watch(TestNames.player(), room);
        watch(TestNames.player(), room);

        await().atMost(Duration.ofSeconds(5)).until(() -> ROOM.streamsRequested(room) >= 1);
        assertThat(ROOM.streamsRequested(room)).isEqualTo(1);
    }

    @Test
    void theStreamIsCancelledWhenTheLastWatcherLeaves() throws Exception {
        String room = TestNames.roomCode();
        watch(TestNames.player(), room);
        await().atMost(Duration.ofSeconds(5)).until(() -> ROOM.openStreams(room) == 1);

        clients.getFirst().close();

        await().atMost(Duration.ofSeconds(5)).until(() -> ROOM.openStreams(room) == 0 && !relay.isRelaying(room));
    }

    @Test
    void aDroppedConnectionIsReportedToRoom() throws Exception {
        String player = TestNames.player();
        String room = TestNames.roomCode();
        watch(player, room);

        clients.getFirst().close();

        await().atMost(Duration.ofSeconds(5)).until(() -> ROOM.calls.received(ReportDisconnectedRequest.class).stream()
                .anyMatch(request -> request.getRoomCode().equals(room)
                        && request.getUserId().equals(FakeTokens.userId(player))));
    }

    @Test
    void reopensTheStreamAfterRoomServiceRestarts() throws Exception {
        String room = TestNames.roomCode();
        BlockingQueue<Map<String, Object>> events = watch(TestNames.player(), room);
        await().atMost(Duration.ofSeconds(5)).until(() -> ROOM.openStreams(room) == 1);
        events.clear();

        ROOM.endStreams(room, Status.UNAVAILABLE);

        await().atMost(Duration.ofSeconds(5)).until(() -> ROOM.streamsRequested(room) == 2);
        assertThat(nextOfType(events, "SNAPSHOT").get("snapshot")).extracting("roomCode").isEqualTo(room);
    }

    @Test
    void aRoomLostInARestartIsClosedForTheBrowsers() throws Exception {
        String room = TestNames.roomCode();
        BlockingQueue<Map<String, Object>> events = watch(TestNames.player(), room);
        await().atMost(Duration.ofSeconds(5)).until(() -> ROOM.openStreams(room) == 1);
        ROOM.push(room, event(RoomEventType.ROOM_EVENT_TYPE_PLAYER_JOINED, room(room, "user-host", "Host")));
        nextOfType(events, "PLAYER_JOINED");

        ROOM.failStreams(Status.NOT_FOUND);
        ROOM.endStreams(room, Status.UNAVAILABLE);

        Map<String, Object> closed = nextOfType(events, "ROOM_CLOSED");
        assertThat(closed.get("snapshot")).extracting("roomCode").isEqualTo(room);
        await().atMost(Duration.ofSeconds(5)).until(() -> !relay.isRelaying(room));
    }

    @Test
    void stopsAfterRoomClosed() throws Exception {
        String room = TestNames.roomCode();
        BlockingQueue<Map<String, Object>> events = watch(TestNames.player(), room);
        await().atMost(Duration.ofSeconds(5)).until(() -> ROOM.openStreams(room) == 1);

        ROOM.push(room, event(RoomEventType.ROOM_EVENT_TYPE_ROOM_CLOSED, room(room, "user-host", "Host")));
        ROOM.endStreams(room, Status.OK);

        nextOfType(events, "ROOM_CLOSED");
        await().atMost(Duration.ofSeconds(5)).until(() -> !relay.isRelaying(room));
        assertThat(ROOM.streamsRequested(room)).isEqualTo(1);
    }

    /** Connects as the player and watches the room; returns the room's events. */
    private BlockingQueue<Map<String, Object>> watch(String player, String room) throws Exception {
        StompTestClient client = StompTestClient.connect(port, FakeTokens.token(player));
        clients.add(client);
        BlockingQueue<Map<String, Object>> events = client.subscribe("/topic/room/" + room);
        client.sync();
        return events;
    }

    private static Map<String, Object> nextOfType(BlockingQueue<Map<String, Object>> events, String type)
            throws InterruptedException {
        while (true) {
            Map<String, Object> event = next(events);
            if (type.equals(event.get("type"))) {
                return event;
            }
        }
    }
}
