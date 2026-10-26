package com.friendlyfire.gateway.realtime;

import org.springframework.stereotype.Component;

@Component
public class RoomEventRelay {
    // TODO(ff): forward each room's events to the browsers in it
    //   - open one RoomService.StreamRoomEvents per room that has players connected
    //     here (async stub on roomChannel); the first event is a SNAPSHOT
    //   - send every event to /topic/room/{room_code} (SimpMessagingTemplate)
    //   - stop after ROOM_CLOSED; reopen the stream if room-service restarts
}
