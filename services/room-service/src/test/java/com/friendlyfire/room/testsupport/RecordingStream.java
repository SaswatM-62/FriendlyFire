package com.friendlyfire.room.testsupport;

import com.friendlyfire.proto.friendlyfire.room.v1.RoomEvent;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType;
import com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** A StreamRoomEvents subscriber that keeps everything it's sent. */
public final class RecordingStream implements StreamObserver<StreamRoomEventsResponse> {

    public final List<RoomEvent> events = new CopyOnWriteArrayList<>();
    public volatile boolean completed;
    public volatile Throwable error;

    @Override
    public void onNext(StreamRoomEventsResponse value) {
        events.add(value.getEvent());
    }

    @Override
    public void onError(Throwable t) {
        error = t;
    }

    @Override
    public void onCompleted() {
        completed = true;
    }

    public List<RoomEventType> types() {
        return events.stream().map(RoomEvent::getType).toList();
    }

    public RoomEvent last() {
        return events.getLast();
    }

    public void clear() {
        events.clear();
    }
}
