package com.friendlyfire.gateway.realtime;

import com.friendlyfire.gateway.json.RoomEventJson;
import com.friendlyfire.gateway.json.RoomSnapshotJson;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceStub;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Forwards each room's events to the browsers in it (FF-304): one
 * RoomService.StreamRoomEvents per room that has sessions watching it on this Gateway,
 * each event sent to /topic/room/{code} as JSON.
 *
 * <ul>
 *   <li>The stream opens when the first session here subscribes to the room
 *       (ConnectInterceptor) and is cancelled when the last one leaves.</li>
 *   <li>ROOM_CLOSED is the last event: the stream ends and isn't reopened.</li>
 *   <li>If the stream drops (room-service restarted), it's reopened with backoff; its
 *       first event is a fresh SNAPSHOT. If the room no longer exists, the browsers get a
 *       ROOM_CLOSED with the last state seen.</li>
 * </ul>
 */
public class RoomEventRelay implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RoomEventRelay.class);

    /** Stream errors worth reopening for; anything else (UNIMPLEMENTED...) ends the relay. */
    private static final Set<Status.Code> RETRY = Set.of(
            Status.Code.UNAVAILABLE, Status.Code.UNKNOWN, Status.Code.INTERNAL, Status.Code.ABORTED,
            Status.Code.DEADLINE_EXCEEDED);

    private final RoomServiceStub rooms;
    private final ObjectProvider<SimpMessagingTemplate> messaging;
    private final ScheduledExecutorService scheduler;
    private final Clock clock;
    private final List<Duration> backoff;
    // Guarded by "this". gRPC may call back on the opening thread, so nothing here uses
    // ConcurrentHashMap.compute (which can't be re-entered).
    private final Map<String, RoomStream> streams = new HashMap<>();

    /**
     * @param rooms     an async stub without a default deadline: streams stay open for hours
     * @param messaging a provider, because the messaging template is built by the broker
     *                  configuration, which needs ConnectInterceptor and so this class
     * @param backoff   waits before each reopen attempt in a row; the last one repeats
     */
    public RoomEventRelay(
            RoomServiceStub rooms, ObjectProvider<SimpMessagingTemplate> messaging, ScheduledExecutorService scheduler,
            Clock clock, List<Duration> backoff) {
        this.rooms = rooms;
        this.messaging = messaging;
        this.scheduler = scheduler;
        this.clock = clock;
        this.backoff = List.copyOf(backoff);
    }

    /** A session here started watching the room: opens its stream if it's the first. */
    public synchronized void watch(String roomCode, String sessionId) {
        RoomStream stream = streams.get(roomCode);
        if (stream != null) {
            stream.watchers.add(sessionId);
            return;
        }
        stream = new RoomStream(roomCode);
        stream.watchers.add(sessionId);
        streams.put(roomCode, stream);
        open(stream);
    }

    /** A session here stopped watching the room: cancels its stream if it was the last. */
    public synchronized void unwatch(String roomCode, String sessionId) {
        RoomStream stream = streams.get(roomCode);
        if (stream == null) {
            return;
        }
        stream.watchers.remove(sessionId);
        if (stream.watchers.isEmpty()) {
            streams.remove(roomCode);
            stream.cancel();
        }
    }

    /** True while this Gateway relays the room (a stream is open or about to reopen). */
    public synchronized boolean isRelaying(String roomCode) {
        return streams.containsKey(roomCode);
    }

    @Override
    public synchronized void close() {
        streams.values().forEach(RoomStream::cancel);
        streams.clear();
        scheduler.shutdownNow();
    }

    private void open(RoomStream stream) {
        rooms.streamRoomEvents(StreamRoomEventsRequest.newBuilder().setRoomCode(stream.roomCode).build(), stream);
    }

    private void forward(String roomCode, RoomEventJson event) {
        messaging.getObject().convertAndSend(RoomDestinations.topic(roomCode), event);
    }

    private synchronized void ended(RoomStream stream, Status status) {
        if (streams.get(stream.roomCode) != stream || stream.cancelled) {
            return;
        }
        if (stream.sawRoomClosed) {
            streams.remove(stream.roomCode);
            return;
        }
        Status.Code code = status == null ? null : status.getCode();
        if (code == Status.Code.NOT_FOUND) {
            // room-service restarted and the room is gone (rooms live in its memory).
            streams.remove(stream.roomCode);
            RoomSnapshotJson last = stream.lastSnapshot;
            if (last != null) {
                forward(stream.roomCode, new RoomEventJson("ROOM_CLOSED", null, clock.instant().toString(), last));
            }
            return;
        }
        if (code == null || RETRY.contains(code)) {
            Duration wait = backoff.get(Math.min(stream.failures, backoff.size() - 1));
            stream.failures++;
            log.info("Room {}'s event stream ended ({}); reopening in {} ms", stream.roomCode,
                    code == null ? "completed" : code, wait.toMillis());
            scheduler.schedule(() -> reopen(stream), wait.toMillis(), TimeUnit.MILLISECONDS);
            return;
        }
        log.warn("Room {}'s event stream failed: {}", stream.roomCode, status);
        streams.remove(stream.roomCode);
    }

    private synchronized void reopen(RoomStream stream) {
        if (streams.get(stream.roomCode) == stream && !stream.cancelled) {
            open(stream);
        }
    }

    /** One room's stream, and the sessions here watching that room. */
    private final class RoomStream implements ClientResponseObserver<StreamRoomEventsRequest, StreamRoomEventsResponse> {

        private final String roomCode;
        private final Set<String> watchers = new HashSet<>();
        private volatile ClientCallStreamObserver<StreamRoomEventsRequest> call;
        private volatile RoomSnapshotJson lastSnapshot;
        private volatile boolean sawRoomClosed;
        private volatile boolean cancelled;
        private int failures;

        private RoomStream(String roomCode) {
            this.roomCode = roomCode;
        }

        @Override
        public void beforeStart(ClientCallStreamObserver<StreamRoomEventsRequest> requestStream) {
            call = requestStream;
        }

        @Override
        public void onNext(StreamRoomEventsResponse response) {
            synchronized (RoomEventRelay.this) {
                failures = 0;
            }
            RoomEventJson event = RoomEventJson.from(response.getEvent());
            lastSnapshot = event.snapshot();
            if (response.getEvent().getType() == RoomEventType.ROOM_EVENT_TYPE_ROOM_CLOSED) {
                sawRoomClosed = true;
            }
            if (!cancelled) {
                forward(roomCode, event);
            }
        }

        @Override
        public void onError(Throwable error) {
            ended(this, Status.fromThrowable(error));
        }

        @Override
        public void onCompleted() {
            ended(this, null);
        }

        private void cancel() {
            cancelled = true;
            ClientCallStreamObserver<StreamRoomEventsRequest> current = call;
            if (current != null) {
                current.cancel("No one here is watching the room", null);
            }
        }
    }
}
