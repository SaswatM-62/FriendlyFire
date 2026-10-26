package com.friendlyfire.room.room;

import org.springframework.stereotype.Component;

@Component
public class RoomEventPublisher {
    // TODO(ff): feed StreamRoomEvents
    //   - keep the open streams per room; send every RoomEvent (with a full snapshot)
    //     to each of them, in order
    //   - a new stream gets a SNAPSHOT event first; ROOM_CLOSED ends every stream
}
