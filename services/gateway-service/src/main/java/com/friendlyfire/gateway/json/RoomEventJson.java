package com.friendlyfire.gateway.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEvent;

/**
 * Every message on /topic/room/{code}. The browser replaces its state with
 * {@code snapshot} and uses {@code type} only for effects; {@code message} is a sentence
 * safe to show every player, when the event has one (QUIZ_START_FAILED).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomEventJson(
        String type, String subjectUserId, String occurredAt, RoomSnapshotJson snapshot, String message) {

    public static RoomEventJson from(RoomEvent event) {
        return new RoomEventJson(
                ProtoJson.enumName(event.getType(), "ROOM_EVENT_TYPE_"),
                ProtoJson.text(event.getSubjectUserId()),
                ProtoJson.timestamp(event.hasOccurredAt(), event.getOccurredAt()),
                RoomSnapshotJson.from(event.getSnapshot()),
                ProtoJson.text(event.getMessage()));
    }
}
