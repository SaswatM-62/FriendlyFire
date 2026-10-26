package com.friendlyfire.gateway.realtime;

import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** Reacts to WebSocket sessions closing. Does nothing yet. */
@Component
public class SessionEventsListener {

    private final RoomServiceBlockingStub rooms;

    public SessionEventsListener(RoomServiceBlockingStub rooms) {
        this.rooms = rooms;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        // TODO(ff): tell room-service that a player's connection dropped
        //   - find the room and user for this session, then call
        //     RoomService.ReportDisconnected (they have 15 seconds to come back)
        //   - uses: rooms
    }
}
