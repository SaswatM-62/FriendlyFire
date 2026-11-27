package com.friendlyfire.room.room;

import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_CHAT_MESSAGE;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_CONNECTION_QUALITY_CHANGED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_HOST_CHANGED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_PLAYER_DATA_READY;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_PLAYER_DISCONNECTED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_PLAYER_JOINED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_PLAYER_KICKED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_PLAYER_LEFT;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_PLAYER_RECONNECTED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_ROOM_CLOSED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_SETTINGS_CHANGED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_SNAPSHOT;
import static com.friendlyfire.room.testsupport.RoomFixture.fails;
import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.room.v1.ConnectionQuality;
import com.friendlyfire.proto.friendlyfire.room.v1.Player;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerDataStatus;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerStatus;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSettings;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSnapshot;
import com.friendlyfire.room.testsupport.FakeDataStatusClient;
import com.friendlyfire.room.testsupport.RecordingStream;
import com.friendlyfire.room.testsupport.RoomFixture;
import io.grpc.Status.Code;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomManagerTest {

    private final RoomFixture f = new RoomFixture();

    private static List<String> ids(RoomSnapshot snapshot) {
        return snapshot.getPlayersList().stream().map(Player::getUserId).toList();
    }

    private static Player player(RoomSnapshot snapshot, String userId) {
        return snapshot.getPlayersList().stream().filter(p -> p.getUserId().equals(userId)).findFirst().orElseThrow();
    }

    // ---------- Create and join (FF-303) ----------

    @Test
    void favouriteLooksAreFirstComeFirstServedAndEveryoneKeepsTheirFace() {
        // FF-519: two players with the same favourite icon and colour.
        f.looks.set("alice", "ghost", "cyan", "smirk");
        f.looks.set("bob", "ghost", "cyan", "wink");
        String code = f.create("alice");

        RoomSnapshot room = f.join(code, "bob");

        Player alice = player(room, "alice");
        Player bob = player(room, "bob");
        assertThat(alice.getLook().getIcon()).isEqualTo("ghost");
        assertThat(alice.getLook().getColour()).isEqualTo("cyan");
        assertThat(alice.getLook().getExpression()).isEqualTo("smirk");
        assertThat(alice.getLook().getRandom()).isFalse();
        assertThat(bob.getLook().getIcon()).isNotEqualTo("ghost");
        assertThat(bob.getLook().getColour()).isNotEqualTo("cyan");
        assertThat(bob.getLook().getExpression()).isEqualTo("wink");
        assertThat(bob.getLook().getRandom()).isTrue();
    }

    @Test
    void aPlayerWithoutAFavouriteGetsTheNextDefaultLook() {
        f.looks.set("alice", "ghost", "cyan", "smirk");
        String code = f.create("alice");

        Player carol = player(f.join(code, "carol"), "carol");

        assertThat(carol.getLook().getIcon()).isEqualTo("blob");
        assertThat(carol.getLook().getColour()).isEqualTo("pink");
        assertThat(carol.getLook().getExpression()).isEqualTo("happy");
    }

    @Test
    void aPlayersLookStaysTheSameForTheWholeRoom() {
        f.looks.set("bob", "star", "lime", "gloat");
        String code = f.create("alice");
        Player first = player(f.join(code, "bob"), "bob");

        // Picking a new look mid-room, then coming back after a refresh, doesn't change it.
        f.looks.set("bob", "gem", "silver", "shades");
        Player again = player(f.join(code, "bob"), "bob");

        assertThat(again.getLook()).isEqualTo(first.getLook());
        assertThat(player(f.snapshot(code), "bob").getLook()).isEqualTo(first.getLook());
    }

    @Test
    void whenAccountCantSayTheFavouriteThePlayerStillJoinsWithADefaultLook() {
        String code = f.create("alice");
        f.looks.down(true);

        Player bob = player(f.join(code, "bob"), "bob");

        assertThat(bob.getLook().getIcon()).isEqualTo("ghost");
        assertThat(bob.getLook().getColour()).isEqualTo("cyan");
    }


    @Test
    void aNewRoomIsALobbyWithTheDefaultsAndTheHostsDataStatus() {
        f.data.set("alice", FakeDataStatusClient.syncing(120));

        RoomSnapshot room = f.manager.createRoom("alice", "Alice", "NA1");

        assertThat(room.getRoomCode()).hasSize(6);
        assertThat(room.getPhase()).isEqualTo(RoomPhase.ROOM_PHASE_LOBBY);
        assertThat(room.getSettings().getQuestionCount()).isEqualTo(10);
        assertThat(room.getSettings().getSecondsPerQuestion()).isEqualTo(20);
        assertThat(room.getHostUserId()).isEqualTo("alice");
        Player alice = room.getPlayers(0);
        assertThat(alice.getRiotTagLine()).isEqualTo("NA1");
        assertThat(alice.getDataStatus()).isEqualTo(PlayerDataStatus.PLAYER_DATA_STATUS_SYNCING);
        assertThat(alice.getDataEstimatedSeconds()).isEqualTo(120);
        assertThat(room.hasPhaseDeadline()).isFalse();
    }

    @Test
    void aRoomStillOpensWhenAccountIsDown() {
        f.data.down(true);

        RoomSnapshot room = f.manager.createRoom("alice", "Alice", "");

        assertThat(room.getPlayers(0).getDataStatus()).isEqualTo(PlayerDataStatus.PLAYER_DATA_STATUS_UNSPECIFIED);
    }

    @Test
    void joiningAddsThePlayerInOrderAndTellsTheRoom() {
        String code = f.create("alice");
        RecordingStream stream = f.stream(code);

        RoomSnapshot room = f.join(code, "bob");

        assertThat(ids(room)).containsExactly("alice", "bob");
        assertThat(stream.types()).containsExactly(ROOM_EVENT_TYPE_SNAPSHOT, ROOM_EVENT_TYPE_PLAYER_JOINED);
        assertThat(stream.last().getSubjectUserId()).isEqualTo("bob");
    }

    @Test
    void aReturningPlayerKeepsTheirPlace() {
        String code = f.create("alice");
        f.join(code, "bob");
        f.join(code, "cara");
        f.manager.reportDisconnected(code, "bob");
        RecordingStream stream = f.stream(code);

        RoomSnapshot room = f.join(code, "bob");

        assertThat(ids(room)).containsExactly("alice", "bob", "cara");
        assertThat(player(room, "bob").getStatus()).isEqualTo(PlayerStatus.PLAYER_STATUS_CONNECTED);
        assertThat(stream.types()).containsExactly(ROOM_EVENT_TYPE_SNAPSHOT, ROOM_EVENT_TYPE_PLAYER_RECONNECTED);
        // A refresh while still connected changes nothing.
        f.join(code, "bob");
        assertThat(stream.events).hasSize(2);
    }

    @Test
    void aNinthPersonIsTurnedAway() {
        String code = f.create("p1");
        for (int n = 2; n <= 8; n++) {
            f.join(code, "p" + n);
        }

        fails(Code.RESOURCE_EXHAUSTED, () -> f.join(code, "p9"));
    }

    @Test
    void joiningAnUnknownRoomIsNotFound() {
        fails(Code.NOT_FOUND, () -> f.join("ZZZZZZ", "bob"));
    }

    @Test
    void joiningASecondRoomLeavesTheFirst() {
        String first = f.create("alice");
        f.join(first, "bob");
        String second = f.create("cara");
        RecordingStream firstStream = f.stream(first);

        f.join(second, "alice");

        assertThat(ids(f.snapshot(first))).containsExactly("bob");
        assertThat(f.snapshot(first).getHostUserId()).isEqualTo("bob");
        assertThat(firstStream.types()).contains(ROOM_EVENT_TYPE_PLAYER_LEFT, ROOM_EVENT_TYPE_HOST_CHANGED);
        assertThat(ids(f.snapshot(second))).containsExactly("cara", "alice");
    }

    @Test
    void aFullRoomDoesntTakeYouOutOfYourRoom() {
        String full = f.create("p1");
        for (int n = 2; n <= 8; n++) {
            f.join(full, "p" + n);
        }
        String mine = f.create("alice");

        fails(Code.RESOURCE_EXHAUSTED, () -> f.join(full, "alice"));

        assertThat(ids(f.snapshot(mine))).containsExactly("alice");
    }

    // ---------- Leave, kick, settings (FF-306) ----------

    @Test
    void whenTheHostLeavesTheNextPlayerBecomesHost() {
        String code = f.create("alice");
        f.join(code, "bob");
        f.join(code, "cara");
        RecordingStream stream = f.stream(code);

        f.manager.leaveRoom(code, "alice");

        assertThat(f.snapshot(code).getHostUserId()).isEqualTo("bob");
        assertThat(stream.types()).containsExactly(
                ROOM_EVENT_TYPE_SNAPSHOT, ROOM_EVENT_TYPE_PLAYER_LEFT, ROOM_EVENT_TYPE_HOST_CHANGED);
        assertThat(stream.last().getSubjectUserId()).isEqualTo("bob");
    }

    @Test
    void theLastPlayerLeavingClosesTheRoomAndEndsItsStreams() {
        String code = f.create("alice");
        RecordingStream stream = f.stream(code);

        f.manager.leaveRoom(code, "alice");

        assertThat(stream.types()).containsExactly(ROOM_EVENT_TYPE_SNAPSHOT, ROOM_EVENT_TYPE_ROOM_CLOSED);
        assertThat(stream.completed).isTrue();
        assertThat(f.rooms.findByCode(code)).isEmpty();
        fails(Code.NOT_FOUND, () -> f.join(code, "bob"));
    }

    @Test
    void leavingARoomYoureNotInIsDenied() {
        String code = f.create("alice");
        fails(Code.PERMISSION_DENIED, () -> f.manager.leaveRoom(code, "mallory"));
    }

    @Test
    void onlyTheHostCanKickAndOnlyInTheLobbyOrAfterAQuiz() {
        String code = f.create("alice");
        f.join(code, "bob");
        f.join(code, "cara");
        RecordingStream stream = f.stream(code);

        fails(Code.PERMISSION_DENIED, () -> f.manager.kickPlayer(code, "bob", "cara"));
        fails(Code.INVALID_ARGUMENT, () -> f.manager.kickPlayer(code, "alice", "alice"));
        fails(Code.INVALID_ARGUMENT, () -> f.manager.kickPlayer(code, "alice", "nobody"));
        RoomSnapshot room = f.manager.kickPlayer(code, "alice", "cara");

        assertThat(ids(room)).containsExactly("alice", "bob");
        assertThat(stream.last().getType()).isEqualTo(ROOM_EVENT_TYPE_PLAYER_KICKED);
        assertThat(stream.last().getSubjectUserId()).isEqualTo("cara");
    }

    @Test
    void settingsAreHostOnlyAndInRange() {
        String code = f.create("alice");
        f.join(code, "bob");
        RecordingStream stream = f.stream(code);

        fails(Code.PERMISSION_DENIED, () -> f.manager.updateSettings(code, "bob", settings(10, 20)));
        fails(Code.INVALID_ARGUMENT, () -> f.manager.updateSettings(code, "alice", settings(10, 46)));
        fails(Code.INVALID_ARGUMENT, () -> f.manager.updateSettings(code, "alice", settings(4, 20)));
        fails(Code.INVALID_ARGUMENT, () -> f.manager.updateSettings(code, "alice", settings(21, 20)));
        RoomSnapshot room = f.manager.updateSettings(code, "alice", settings(15, 30));

        assertThat(room.getSettings()).isEqualTo(settings(15, 30));
        assertThat(stream.types()).containsExactly(ROOM_EVENT_TYPE_SNAPSHOT, ROOM_EVENT_TYPE_SETTINGS_CHANGED);
        // The same settings again: nothing to tell anyone.
        f.manager.updateSettings(code, "alice", settings(15, 30));
        assertThat(stream.events).hasSize(2);
    }

    static RoomSettings settings(int questions, int seconds) {
        return RoomSettings.newBuilder().setQuestionCount(questions).setSecondsPerQuestion(seconds).build();
    }

    // ---------- Streams and snapshots (FF-302) ----------

    @Test
    void aStreamGetsASnapshotFirstThenEveryEventInOrder() {
        String code = f.create("alice");
        RecordingStream stream = f.stream(code);

        f.join(code, "bob");
        f.manager.updateSettings(code, "alice", settings(5, 10));
        f.manager.leaveRoom(code, "bob");

        assertThat(stream.types()).containsExactly(ROOM_EVENT_TYPE_SNAPSHOT, ROOM_EVENT_TYPE_PLAYER_JOINED,
                ROOM_EVENT_TYPE_SETTINGS_CHANGED, ROOM_EVENT_TYPE_PLAYER_LEFT);
        assertThat(stream.events.get(0).getSnapshot().getPlayersCount()).isEqualTo(1);
        assertThat(stream.events.get(1).getSnapshot().getPlayersCount()).isEqualTo(2);
        assertThat(stream.events).allSatisfy(e -> assertThat(e.hasOccurredAt()).isTrue());
    }

    @Test
    void streamingAnUnknownRoomIsNotFound() {
        fails(Code.NOT_FOUND, () -> f.stream("ZZZZZZ"));
    }

    @Test
    void onlyPlayersInTheRoomCanReadIt() {
        String code = f.create("alice");
        assertThat(f.manager.snapshot(code, "alice").getRoomCode()).isEqualTo(code);
        fails(Code.PERMISSION_DENIED, () -> f.manager.snapshot(code, "mallory"));
    }

    // ---------- Connections (FF-308, FF-310) ----------

    @Test
    void aDroppedConnectionShowsAsDisconnected() {
        String code = f.create("alice");
        f.join(code, "bob");
        RecordingStream stream = f.stream(code);

        f.manager.reportDisconnected(code, "bob");
        f.manager.reportDisconnected(code, "bob");

        assertThat(player(f.snapshot(code), "bob").getStatus()).isEqualTo(PlayerStatus.PLAYER_STATUS_DISCONNECTED);
        assertThat(stream.types()).containsExactly(ROOM_EVENT_TYPE_SNAPSHOT, ROOM_EVENT_TYPE_PLAYER_DISCONNECTED);
    }

    @Test
    void aHostGoneFor30SecondsInTheLobbyHandsOverTheHostRole() {
        String code = f.create("alice");
        f.join(code, "bob");
        f.manager.reportDisconnected(code, "alice");
        RecordingStream stream = f.stream(code);

        f.advanceSeconds(29);
        assertThat(f.snapshot(code).getHostUserId()).isEqualTo("alice");
        f.advanceSeconds(1);

        assertThat(f.snapshot(code).getHostUserId()).isEqualTo("bob");
        assertThat(stream.last().getType()).isEqualTo(ROOM_EVENT_TYPE_HOST_CHANGED);
    }

    @Test
    void aHostWhoComesBackInTimeStaysHost() {
        String code = f.create("alice");
        f.join(code, "bob");
        f.manager.reportDisconnected(code, "alice");
        f.advanceSeconds(20);
        f.join(code, "alice");

        f.advanceSeconds(20);

        assertThat(f.snapshot(code).getHostUserId()).isEqualTo("alice");
    }

    @Test
    void connectionQualityIsAnEventOnlyWhenItChanges() {
        String code = f.create("alice");
        RecordingStream stream = f.stream(code);

        f.manager.reportConnectionQuality(code, "alice", ConnectionQuality.CONNECTION_QUALITY_GOOD);
        f.manager.reportConnectionQuality(code, "alice", ConnectionQuality.CONNECTION_QUALITY_GOOD);
        f.manager.reportConnectionQuality(code, "alice", ConnectionQuality.CONNECTION_QUALITY_POOR);

        assertThat(stream.types()).containsExactly(ROOM_EVENT_TYPE_SNAPSHOT,
                ROOM_EVENT_TYPE_CONNECTION_QUALITY_CHANGED, ROOM_EVENT_TYPE_CONNECTION_QUALITY_CHANGED);
        assertThat(player(f.snapshot(code), "alice").getConnectionQuality())
                .isEqualTo(ConnectionQuality.CONNECTION_QUALITY_POOR);
        fails(Code.INVALID_ARGUMENT, () -> f.manager.reportConnectionQuality(
                code, "alice", ConnectionQuality.CONNECTION_QUALITY_UNSPECIFIED));
    }

    // ---------- Data status (FF-307) ----------

    @Test
    void theLobbyAsksForDataStatusEveryFewSecondsAndSaysWhenItChanges() {
        f.data.set("bob", FakeDataStatusClient.syncing(60));
        String code = f.create("alice");
        f.join(code, "bob");
        RecordingStream stream = f.stream(code);

        f.advanceSeconds(5);
        assertThat(stream.types()).containsExactly(ROOM_EVENT_TYPE_SNAPSHOT);

        f.data.set("bob", FakeDataStatusClient.SYNCED);
        f.advanceSeconds(5);

        assertThat(stream.types()).containsExactly(ROOM_EVENT_TYPE_SNAPSHOT, ROOM_EVENT_TYPE_PLAYER_DATA_READY);
        assertThat(stream.last().getSubjectUserId()).isEqualTo("bob");
        assertThat(player(f.snapshot(code), "bob").getDataStatus())
                .isEqualTo(PlayerDataStatus.PLAYER_DATA_STATUS_SYNCED);
    }

    @Test
    void aPlayerWithFewMatchesIsMarked() {
        f.data.set("bob", FakeDataStatusClient.FEW_MATCHES);
        String code = f.create("alice");

        RoomSnapshot room = f.join(code, "bob");

        assertThat(player(room, "bob").getNotEnoughMatches()).isTrue();
        assertThat(player(room, "alice").getNotEnoughMatches()).isFalse();
    }

    // ---------- Abandoned rooms (FF-311) ----------

    @Test
    void aRoomWhosePlayersAllLeftTheirTabsClosesAfterFiveMinutes() {
        String code = f.create("alice");
        f.join(code, "bob");
        f.manager.reportDisconnected(code, "alice");
        f.manager.reportDisconnected(code, "bob");
        RecordingStream stream = f.stream(code);

        f.advanceSeconds(4 * 60 + 30);
        assertThat(f.rooms.findByCode(code)).isPresent();
        f.advanceSeconds(60);

        assertThat(f.rooms.findByCode(code)).isEmpty();
        assertThat(stream.last().getType()).isEqualTo(ROOM_EVENT_TYPE_ROOM_CLOSED);
        assertThat(stream.completed).isTrue();
    }

    @Test
    void aRoomWithOneConnectedPlayerStaysOpen() {
        String code = f.create("alice");
        f.join(code, "bob");
        f.manager.reportDisconnected(code, "bob");

        f.advanceSeconds(30 * 60);

        assertThat(f.rooms.findByCode(code)).isPresent();
    }

    @Test
    void aRoomWhereNothingHappensForTwoHoursCloses() {
        String code = f.create("alice");

        f.advanceSeconds(2 * 60 * 60 + 30);

        assertThat(f.rooms.findByCode(code)).isEmpty();
    }

    // ---------- Chat (FF-315) ----------

    @Test
    void aChatMessageGoesToEveryoneTrimmedAndStaysInTheHistory() {
        String code = f.create("alice");
        f.join(code, "bob");
        RecordingStream stream = f.stream(code);

        var message = f.manager.sendChat(code, "bob", "  gg  ");

        assertThat(message.getText()).isEqualTo("gg");
        assertThat(message.getDisplayName()).isEqualTo("Bob");
        assertThat(message.getMessageId()).isNotEmpty();
        assertThat(stream.last().getType()).isEqualTo(ROOM_EVENT_TYPE_CHAT_MESSAGE);
        assertThat(stream.last().getChat()).isEqualTo(message);
        assertThat(f.manager.chatHistory(code, "alice")).containsExactly(message);
    }

    @Test
    void chatMessagesMustBeOneTo200Characters() {
        String code = f.create("alice");

        fails(Code.INVALID_ARGUMENT, () -> f.manager.sendChat(code, "alice", "   "));
        fails(Code.INVALID_ARGUMENT, () -> f.manager.sendChat(code, "alice", "x".repeat(201)));
        // An emoji counts once.
        assertThat(f.manager.sendChat(code, "alice", "🙂".repeat(200)).getText()).isNotEmpty();
    }

    @Test
    void outsidersCantSendOrReadTheChat() {
        String code = f.create("alice");

        fails(Code.PERMISSION_DENIED, () -> f.manager.sendChat(code, "mallory", "hi"));
        fails(Code.PERMISSION_DENIED, () -> f.manager.chatHistory(code, "mallory"));
    }

    @Test
    void theRoomKeepsItsLast200Messages() {
        String code = f.create("alice");
        for (int n = 1; n <= 205; n++) {
            f.manager.sendChat(code, "alice", "message " + n);
        }

        var history = f.manager.chatHistory(code, "alice");

        assertThat(history).hasSize(200);
        assertThat(history.getFirst().getText()).isEqualTo("message 6");
        assertThat(history.getLast().getText()).isEqualTo("message 205");
    }

    @Test
    void aDeletedPlayersMessagesSayDeletedPlayer() {
        String code = f.create("alice");
        f.join(code, "bob");
        f.manager.sendChat(code, "bob", "bye");

        f.manager.removePlayer("bob");

        assertThat(ids(f.snapshot(code))).containsExactly("alice");
        assertThat(f.manager.chatHistory(code, "alice").getFirst().getDisplayName()).isEqualTo("Deleted player");
        // Not in a room: nothing to do, and not an error.
        f.manager.removePlayer("bob");
    }

    @Test
    void aRoomsMessagesAreGoneOnceItCloses() {
        String code = f.create("alice");
        f.manager.sendChat(code, "alice", "hello");

        f.manager.leaveRoom(code, "alice");

        assertThat(f.rooms.findByCode(code)).isEmpty();
        fails(Code.NOT_FOUND, () -> f.manager.chatHistory(code, "alice"));
    }
}
