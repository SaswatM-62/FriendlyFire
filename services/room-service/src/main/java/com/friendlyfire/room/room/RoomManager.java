package com.friendlyfire.room.room;

import com.friendlyfire.proto.friendlyfire.room.v1.ChatMessage;
import com.friendlyfire.proto.friendlyfire.room.v1.ConnectionQuality;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerDataStatus;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerStatus;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSettings;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSnapshot;
import com.friendlyfire.proto.friendlyfire.room.v1.EndReason;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.VoteOption;
import com.friendlyfire.room.clients.DataStatusClient;
import com.friendlyfire.room.clients.PlayerData;
import com.friendlyfire.room.room.Room.Member;
import com.friendlyfire.room.timing.RoomTimings;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Every room action from the contract. Each one changes a room under its lock and
 * publishes what changed; QuizRunner takes over for the quiz itself. Calls to Account
 * happen outside the lock, so a slow Account never holds up a room.
 */
public class RoomManager {

    private static final Logger log = LoggerFactory.getLogger(RoomManager.class);

    static final int MIN_QUESTIONS = 5;
    static final int MAX_QUESTIONS = 20;
    static final int MIN_SECONDS = 10;
    static final int MAX_SECONDS = 45;
    static final int MAX_CHAT_LENGTH = 200;
    static final String DELETED_PLAYER = "Deleted player";

    private final RoomRepository rooms;
    private final RoomCodeGenerator codes;
    private final RoomEventPublisher events;
    private final QuizRunner runner;
    private final DataStatusClient dataStatuses;
    private final Clock clock;
    private final Executor background;
    private final RoomTimings timings;
    /** The room each user is in: a user is in one room at a time. */
    private final Map<String, String> roomOf = new ConcurrentHashMap<>();

    public RoomManager(
            RoomRepository rooms,
            RoomCodeGenerator codes,
            RoomEventPublisher events,
            QuizRunner runner,
            DataStatusClient dataStatuses,
            Clock clock,
            Executor background,
            RoomTimings timings) {
        this.rooms = rooms;
        this.codes = codes;
        this.events = events;
        this.runner = runner;
        this.dataStatuses = dataStatuses;
        this.clock = clock;
        this.background = background;
        this.timings = timings;
    }

    // ---------- Create, join, leave ----------

    public RoomSnapshot createRoom(String userId, String displayName, String riotTagLine) {
        requireUser(userId);
        leaveCurrentRoom(userId, null);
        PlayerData data = fetchData(userId);
        while (true) {
            Room room = new Room(codes.generateCode(), clock.instant());
            Member host = new Member(userId, name(displayName), tag(riotTagLine));
            host.data = data;
            synchronized (room) {
                room.members.put(userId, host);
                room.hostUserId = userId;
                if (!rooms.save(room)) {
                    continue;
                }
                roomOf.put(userId, room.getCode());
                return room.snapshot();
            }
        }
    }

    /**
     * Joins a room, or comes back to it after a refresh or a dropped connection. Someone
     * new while a quiz is preparing or running is a spectator until it finishes.
     */
    public RoomSnapshot joinRoom(String code, String userId, String displayName, String riotTagLine) {
        requireUser(userId);
        Room room = find(code);
        boolean returning;
        synchronized (room) {
            returning = room.members.containsKey(userId);
            if (!returning && room.members.size() >= Room.MAX_PEOPLE) {
                throw RoomErrors.full();
            }
        }
        // Checked first, so a full room doesn't take them out of the room they're in.
        leaveCurrentRoom(userId, code);
        PlayerData data = returning ? null : fetchData(userId);
        synchronized (room) {
            if (room.closed) {
                throw RoomErrors.notFound();
            }
            Member member = room.members.get(userId);
            if (member != null) {
                return reconnect(room, member);
            }
            if (room.members.size() >= Room.MAX_PEOPLE) {
                throw RoomErrors.full();
            }
            member = new Member(userId, name(displayName), tag(riotTagLine));
            member.data = data;
            if (room.quizRunning()) {
                member.status = PlayerStatus.PLAYER_STATUS_SPECTATING;
            }
            room.members.put(userId, member);
            roomOf.put(userId, room.getCode());
            events.publish(room, RoomEventType.ROOM_EVENT_TYPE_PLAYER_JOINED, userId);
            return room.snapshot();
        }
    }

    private RoomSnapshot reconnect(Room room, Member member) {
        roomOf.put(member.userId, room.getCode());
        if (member.connected()) {
            return room.snapshot();
        }
        member.status = room.quizRunning() && !room.isPlaying(member.userId)
                ? PlayerStatus.PLAYER_STATUS_SPECTATING
                : PlayerStatus.PLAYER_STATUS_CONNECTED;
        member.disconnectedAt = null;
        if (!runner.onReconnected(room, member.userId)) {
            events.publish(room, RoomEventType.ROOM_EVENT_TYPE_PLAYER_RECONNECTED, member.userId);
        }
        return room.snapshot();
    }

    public void leaveRoom(String code, String userId) {
        Room room = find(code);
        synchronized (room) {
            requireMember(room, userId);
            remove(room, userId, RoomEventType.ROOM_EVENT_TYPE_PLAYER_LEFT);
        }
    }

    /** Before an account is deleted (ADR-027): out of their room, and their chat shows "Deleted player". */
    public void removePlayer(String userId) {
        String code = roomOf.get(userId);
        if (code == null) {
            return;
        }
        rooms.findByCode(code).ifPresent(room -> {
            synchronized (room) {
                if (!room.members.containsKey(userId)) {
                    return;
                }
                List<ChatMessage> renamed = room.chat.stream()
                        .map(m -> m.getUserId().equals(userId) ? m.toBuilder().setDisplayName(DELETED_PLAYER).build() : m)
                        .toList();
                room.chat.clear();
                room.chat.addAll(renamed);
                remove(room, userId, RoomEventType.ROOM_EVENT_TYPE_PLAYER_LEFT);
            }
        });
        roomOf.remove(userId, code);
    }

    /** One room at a time: joining or creating another room leaves this one. */
    private void leaveCurrentRoom(String userId, String keepCode) {
        String current = roomOf.get(userId);
        if (current == null || current.equals(keepCode)) {
            return;
        }
        rooms.findByCode(current).ifPresent(old -> {
            synchronized (old) {
                if (old.members.containsKey(userId)) {
                    remove(old, userId, RoomEventType.ROOM_EVENT_TYPE_PLAYER_LEFT);
                }
            }
        });
        roomOf.remove(userId, current);
    }

    /** Takes someone out for good: the host role moves on, an empty room closes. */
    private void remove(Room room, String userId, RoomEventType type) {
        room.members.remove(userId);
        roomOf.remove(userId, room.getCode());
        if (room.members.isEmpty()) {
            close(room, EndReason.END_REASON_NOT_ENOUGH_PLAYERS);
            return;
        }
        boolean wasHost = userId.equals(room.hostUserId);
        if (wasHost) {
            room.hostUserId = nextHost(room);
        }
        if (!runner.afterLeft(room, userId, type)) {
            events.publish(room, type, userId);
        }
        if (wasHost && !room.closed) {
            events.publish(room, RoomEventType.ROOM_EVENT_TYPE_HOST_CHANGED, room.hostUserId);
            runner.watchHost(room);
        }
    }

    /** The player who joined next, preferring someone connected. */
    private static String nextHost(Room room) {
        return room.members.values().stream()
                .filter(Member::connected)
                .findFirst()
                .or(() -> room.members.values().stream().findFirst())
                .map(m -> m.userId)
                .orElse(null);
    }

    private void close(Room room, EndReason unfinishedReason) {
        if (room.closed) {
            return;
        }
        if (room.quizRunning()) {
            runner.finalizeLater(room.quiz, unfinishedReason);
        }
        room.closed = true;
        QuizRunner.stopTimers(room);
        events.close(room);
        rooms.deleteByCode(room.getCode());
        for (String userId : new ArrayList<>(room.members.keySet())) {
            roomOf.remove(userId, room.getCode());
        }
    }

    // ---------- The host's controls ----------

    public RoomSnapshot kickPlayer(String code, String userId, String targetUserId) {
        Room room = find(code);
        synchronized (room) {
            requireHost(room, userId);
            requireSetup(room, "You can only remove players in the lobby.");
            if (userId.equals(targetUserId)) {
                throw RoomErrors.invalid("You can't remove yourself. Leave the room instead.");
            }
            if (!room.members.containsKey(targetUserId)) {
                throw RoomErrors.invalid("That player isn't in this room.");
            }
            remove(room, targetUserId, RoomEventType.ROOM_EVENT_TYPE_PLAYER_KICKED);
            return room.snapshot();
        }
    }

    public RoomSnapshot updateSettings(String code, String userId, RoomSettings settings) {
        Room room = find(code);
        synchronized (room) {
            requireHost(room, userId);
            requireSetup(room, "You can only change the settings in the lobby.");
            int questions = settings.getQuestionCount();
            int seconds = settings.getSecondsPerQuestion();
            if (questions < MIN_QUESTIONS || questions > MAX_QUESTIONS) {
                throw RoomErrors.invalid("A quiz has " + MIN_QUESTIONS + " to " + MAX_QUESTIONS + " questions.");
            }
            if (seconds < MIN_SECONDS || seconds > MAX_SECONDS) {
                throw RoomErrors.invalid("Each question lasts " + MIN_SECONDS + " to " + MAX_SECONDS + " seconds.");
            }
            if (!settings.equals(room.settings)) {
                room.settings = RoomSettings.newBuilder()
                        .setQuestionCount(questions)
                        .setSecondsPerQuestion(seconds)
                        .build();
                events.publish(room, RoomEventType.ROOM_EVENT_TYPE_SETTINGS_CHANGED, userId);
            }
            return room.snapshot();
        }
    }

    /**
     * Checks everyone's match data again, then PREPARING. The questions are built in the
     * background (QuizRunner); a failure there comes later as QUIZ_START_FAILED.
     */
    public RoomSnapshot startQuiz(String code, String userId) {
        Room room = find(code);
        List<String> ids;
        synchronized (room) {
            requireHost(room, userId);
            requireCanStart(room);
            ids = List.copyOf(room.members.keySet());
        }
        Map<String, PlayerData> fresh;
        try {
            fresh = dataStatuses.statuses(ids);
        } catch (StatusRuntimeException e) {
            log.warn("couldn't check data status for room {}: {}", code, e.getStatus());
            throw RoomErrors.unavailable("Couldn't check everyone's match data. Try again in a moment.");
        }
        Runnable prepare;
        synchronized (room) {
            if (room.closed) {
                throw RoomErrors.notFound();
            }
            requireHost(room, userId);
            requireCanStart(room);
            fresh.forEach((id, data) -> room.member(id).ifPresent(m -> m.data = data));
            checkReady(room);
            prepare = runner.beginPreparing(room);
        }
        background.execute(prepare);
        synchronized (room) {
            return room.snapshot();
        }
    }

    private static void requireCanStart(Room room) {
        if (room.phase == RoomPhase.ROOM_PHASE_PREPARING) {
            throw RoomErrors.precondition("The quiz is already starting.");
        }
        if (room.phase != RoomPhase.ROOM_PHASE_LOBBY && room.phase != RoomPhase.ROOM_PHASE_FINISHED) {
            throw RoomErrors.precondition("A quiz is already running.");
        }
        if (room.members.size() < Room.MIN_PLAYERS) {
            throw RoomErrors.precondition("You need at least 2 players to start.");
        }
    }

    /** Everyone SYNCED; at least 2 with enough matches, and at most half the room without. */
    private static void checkReady(Room room) {
        List<String> waiting = room.members.values().stream()
                .filter(m -> m.data.status() != PlayerDataStatus.PLAYER_DATA_STATUS_SYNCED)
                .map(m -> m.displayName)
                .toList();
        if (!waiting.isEmpty()) {
            throw RoomErrors.precondition("Waiting for " + names(waiting)
                    + ": everyone needs a linked Riot account with synced match data.");
        }
        long few = room.members.values().stream().filter(m -> m.data.notEnoughMatches()).count();
        long enough = room.members.size() - few;
        if (enough < Room.MIN_PLAYERS) {
            throw RoomErrors.precondition(
                    "At least 2 players need 5 or more Competitive or Unrated matches in the last 30 days.");
        }
        if (few * 2 > room.members.size()) {
            throw RoomErrors.precondition(
                    "More than half the room has too few recent matches for questions. Play a few more matches first.");
        }
    }

    private static String names(List<String> names) {
        if (names.size() == 1) {
            return names.getFirst();
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.getLast();
    }

    // ---------- During a quiz ----------

    public void submitAnswer(String code, String userId, String questionId, String optionId) {
        Room room = find(code);
        synchronized (room) {
            runner.submitAnswer(room, requireMember(room, userId), questionId, optionId);
        }
    }

    public void castVote(String code, String userId, VoteOption option) {
        Room room = find(code);
        synchronized (room) {
            runner.castVote(room, requireMember(room, userId), option);
        }
    }

    // ---------- Connections ----------

    /** The Gateway saw the player's WebSocket close. The current question still finishes (ADR-026). */
    public void reportDisconnected(String code, String userId) {
        Room room = find(code);
        synchronized (room) {
            Member member = requireMember(room, userId);
            if (!member.connected()) {
                return;
            }
            member.status = PlayerStatus.PLAYER_STATUS_DISCONNECTED;
            member.disconnectedAt = clock.instant();
            member.quality = ConnectionQuality.CONNECTION_QUALITY_UNSPECIFIED;
            events.publish(room, RoomEventType.ROOM_EVENT_TYPE_PLAYER_DISCONNECTED, userId);
            if (room.isPlaying(userId)) {
                runner.afterDisconnected(room);
            }
            if (userId.equals(room.hostUserId)) {
                runner.watchHost(room);
            }
        }
    }

    /** The network bar: an event only when the level changes. */
    public void reportConnectionQuality(String code, String userId, ConnectionQuality quality) {
        if (quality == null || quality == ConnectionQuality.CONNECTION_QUALITY_UNSPECIFIED
                || quality == ConnectionQuality.UNRECOGNIZED) {
            throw RoomErrors.invalid("The connection quality must be GOOD, FAIR or POOR.");
        }
        Room room = find(code);
        synchronized (room) {
            Member member = requireMember(room, userId);
            if (member.quality != quality) {
                member.quality = quality;
                events.publish(room, RoomEventType.ROOM_EVENT_TYPE_CONNECTION_QUALITY_CHANGED, userId);
            }
        }
    }

    // ---------- Reading ----------

    public RoomSnapshot snapshot(String code, String userId) {
        Room room = find(code);
        synchronized (room) {
            requireMember(room, userId);
            return room.snapshot();
        }
    }

    public void stream(String code, StreamObserver<StreamRoomEventsResponse> observer) {
        Room room = find(code);
        synchronized (room) {
            if (room.closed) {
                throw RoomErrors.notFound();
            }
            events.subscribe(room, observer);
        }
    }

    // ---------- Chat (FF-315) ----------

    public ChatMessage sendChat(String code, String userId, String text) {
        String trimmed = text == null ? "" : text.strip();
        int length = trimmed.codePointCount(0, trimmed.length());
        if (length == 0) {
            throw RoomErrors.invalid("Type a message first.");
        }
        if (length > MAX_CHAT_LENGTH) {
            throw RoomErrors.invalid("Messages can be up to " + MAX_CHAT_LENGTH + " characters.");
        }
        Room room = find(code);
        synchronized (room) {
            Member member = requireMember(room, userId);
            ChatMessage message = ChatMessage.newBuilder()
                    .setMessageId(UUID.randomUUID().toString())
                    .setUserId(userId)
                    .setDisplayName(member.displayName)
                    .setText(trimmed)
                    .setSentAt(Room.timestamp(clock.instant()))
                    .build();
            room.chat.addLast(message);
            while (room.chat.size() > Room.CHAT_KEEP) {
                room.chat.removeFirst();
            }
            events.publish(room, RoomEventType.ROOM_EVENT_TYPE_CHAT_MESSAGE, userId, null, message);
            return message;
        }
    }

    public List<ChatMessage> chatHistory(String code, String userId) {
        Room room = find(code);
        synchronized (room) {
            requireMember(room, userId);
            return List.copyOf(room.chat);
        }
    }

    // ---------- Scheduled checks ----------

    /**
     * Every few seconds: players' data status in rooms that could start a quiz (FF-307).
     * PLAYER_DATA_READY when someone's status, estimate or match count changes.
     */
    public void refreshDataStatuses() {
        for (Room room : rooms.findAll()) {
            List<String> ids;
            synchronized (room) {
                if (room.closed || !(room.phase == RoomPhase.ROOM_PHASE_LOBBY || room.phase == RoomPhase.ROOM_PHASE_FINISHED)) {
                    continue;
                }
                ids = List.copyOf(room.members.keySet());
            }
            Map<String, PlayerData> fresh;
            try {
                fresh = dataStatuses.statuses(ids);
            } catch (StatusRuntimeException e) {
                log.debug("data status refresh failed for room {}: {}", room.getCode(), e.getStatus());
                continue;
            }
            synchronized (room) {
                if (room.closed) {
                    continue;
                }
                fresh.forEach((id, data) -> room.member(id).ifPresent(m -> {
                    if (!Objects.equals(m.data, data)) {
                        m.data = data;
                        events.publish(room, RoomEventType.ROOM_EVENT_TYPE_PLAYER_DATA_READY, id);
                    }
                }));
            }
        }
    }

    /**
     * Every half minute: close a room whose players have all been gone for 5 minutes, or
     * where nothing has happened for 2 hours (FF-311). An unfinished quiz ends with
     * NOT_ENOUGH_PLAYERS.
     */
    public void closeAbandonedRooms() {
        Instant now = clock.instant();
        for (Room room : rooms.findAll()) {
            synchronized (room) {
                if (room.closed) {
                    continue;
                }
                boolean idle = !room.lastActivity.plus(timings.idle()).isAfter(now);
                boolean everyoneGone = room.members.values().stream().noneMatch(Member::connected)
                        && room.members.values().stream()
                                .map(m -> m.disconnectedAt)
                                .filter(Objects::nonNull)
                                .allMatch(at -> !at.plus(timings.abandoned()).isAfter(now));
                if (idle || everyoneGone) {
                    log.info("closing abandoned room {}", room.getCode());
                    close(room, EndReason.END_REASON_NOT_ENOUGH_PLAYERS);
                }
            }
        }
    }

    // ---------- Helpers ----------

    private Room find(String code) {
        Room room = rooms.findByCode(code == null ? "" : code).orElseThrow(RoomErrors::notFound);
        if (room.closed) {
            throw RoomErrors.notFound();
        }
        return room;
    }

    private static Member requireMember(Room room, String userId) {
        if (room.closed) {
            throw RoomErrors.notFound();
        }
        Member member = userId == null ? null : room.members.get(userId);
        if (member == null) {
            throw RoomErrors.notInRoom();
        }
        return member;
    }

    private static void requireHost(Room room, String userId) {
        requireMember(room, userId);
        if (!userId.equals(room.hostUserId)) {
            throw RoomErrors.notHost();
        }
    }

    /** The lobby, or after a quiz before playing again. */
    private static void requireSetup(Room room, String message) {
        if (room.phase != RoomPhase.ROOM_PHASE_LOBBY && room.phase != RoomPhase.ROOM_PHASE_FINISHED) {
            throw RoomErrors.precondition(message);
        }
    }

    private static void requireUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw RoomErrors.invalid("user_id is required.");
        }
    }

    /** The player's data status; unknown if Account can't answer (the refresh tries again). */
    private PlayerData fetchData(String userId) {
        try {
            return dataStatuses.statuses(List.of(userId)).getOrDefault(userId, PlayerData.UNKNOWN);
        } catch (StatusRuntimeException e) {
            log.warn("couldn't get data status for a joining player: {}", e.getStatus());
            return PlayerData.UNKNOWN;
        }
    }

    private static String name(String displayName) {
        return displayName == null || displayName.isBlank() ? "Player" : displayName;
    }

    private static String tag(String riotTagLine) {
        return riotTagLine == null ? "" : riotTagLine;
    }

    /** Open rooms, for tests and logs. */
    Collection<Room> openRooms() {
        return rooms.findAll();
    }
}
