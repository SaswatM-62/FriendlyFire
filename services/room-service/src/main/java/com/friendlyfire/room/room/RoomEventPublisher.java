package com.friendlyfire.room.room;

import com.friendlyfire.proto.friendlyfire.room.v1.ChatMessage;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEvent;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feeds StreamRoomEvents: keeps each room's open streams and sends every event, with a
 * full snapshot, to all of them. Callers hold the room's lock, so events go out in order.
 */
public class RoomEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RoomEventPublisher.class);

    private final Clock clock;
    private final Map<String, List<StreamObserver<StreamRoomEventsResponse>>> streams = new ConcurrentHashMap<>();

    public RoomEventPublisher(Clock clock) {
        this.clock = clock;
    }

    /** A new stream: a SNAPSHOT first, then every later event until the room closes. */
    void subscribe(Room room, StreamObserver<StreamRoomEventsResponse> observer) {
        var subscribers = streams.computeIfAbsent(room.getCode(), code -> new CopyOnWriteArrayList<>());
        if (observer instanceof ServerCallStreamObserver<StreamRoomEventsResponse> server) {
            server.setOnCancelHandler(() -> subscribers.remove(observer));
        }
        if (send(observer, event(room, RoomEventType.ROOM_EVENT_TYPE_SNAPSHOT, null, null, null))) {
            subscribers.add(observer);
        }
    }

    void publish(Room room, RoomEventType type, String subjectUserId) {
        publish(room, type, subjectUserId, null, null);
    }

    void publish(Room room, RoomEventType type, String subjectUserId, String message, ChatMessage chat) {
        room.lastActivity = clock.instant();
        var subscribers = streams.get(room.getCode());
        if (subscribers == null) {
            return;
        }
        StreamRoomEventsResponse event = event(room, type, subjectUserId, message, chat);
        for (var observer : subscribers) {
            if (!send(observer, event)) {
                subscribers.remove(observer);
            }
        }
    }

    /** ROOM_CLOSED to every stream, then each stream ends. */
    void close(Room room) {
        var subscribers = streams.remove(room.getCode());
        if (subscribers == null) {
            return;
        }
        StreamRoomEventsResponse event = event(room, RoomEventType.ROOM_EVENT_TYPE_ROOM_CLOSED, null, null, null);
        for (var observer : subscribers) {
            if (send(observer, event)) {
                try {
                    observer.onCompleted();
                } catch (RuntimeException e) {
                    log.debug("couldn't end a room stream", e);
                }
            }
        }
    }

    /** Open streams for a room, for tests. */
    int streamCount(String roomCode) {
        var subscribers = streams.get(roomCode);
        return subscribers == null ? 0 : subscribers.size();
    }

    private StreamRoomEventsResponse event(
            Room room, RoomEventType type, String subjectUserId, String message, ChatMessage chat) {
        RoomEvent.Builder event = RoomEvent.newBuilder()
                .setType(type)
                .setOccurredAt(Room.timestamp(clock.instant()))
                .setSnapshot(room.snapshot());
        if (subjectUserId != null) {
            event.setSubjectUserId(subjectUserId);
        }
        if (message != null) {
            event.setMessage(message);
        }
        if (chat != null) {
            event.setChat(chat);
        }
        return StreamRoomEventsResponse.newBuilder().setEvent(event).build();
    }

    private static boolean send(StreamObserver<StreamRoomEventsResponse> observer, StreamRoomEventsResponse event) {
        try {
            observer.onNext(event);
            return true;
        } catch (RuntimeException e) {
            // The stream was cancelled (the Gateway went away); drop it.
            log.debug("dropping a room stream", e);
            return false;
        }
    }
}
