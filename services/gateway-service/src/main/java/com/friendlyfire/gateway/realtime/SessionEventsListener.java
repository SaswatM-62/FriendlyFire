package com.friendlyfire.gateway.realtime;

import com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * A WebSocket session closed (FF-308): stop relaying its rooms here, and tell room-service
 * the player dropped, so a quiz can wait for them and then vote. Nothing is reported when
 * the player already has a newer connection (it replaced this one).
 */
public class SessionEventsListener {

    private static final Logger log = LoggerFactory.getLogger(SessionEventsListener.class);

    private final RoomServiceBlockingStub rooms;
    private final SessionRegistry sessions;
    private final RoomEventRelay relay;

    public SessionEventsListener(RoomServiceBlockingStub rooms, SessionRegistry sessions, RoomEventRelay relay) {
        this.rooms = rooms;
        this.sessions = sessions;
        this.relay = relay;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        SessionRegistry.Ended ended = sessions.end(event.getSessionId());
        if (ended == null) {
            return;
        }
        for (String roomCode : ended.rooms()) {
            relay.unwatch(roomCode, event.getSessionId());
        }
        if (!ended.wasCurrent()) {
            return;
        }
        for (String roomCode : ended.rooms()) {
            try {
                rooms.reportDisconnected(ReportDisconnectedRequest.newBuilder()
                        .setRoomCode(roomCode)
                        .setUserId(ended.userId())
                        .build());
            } catch (StatusRuntimeException e) {
                Status.Code code = e.getStatus().getCode();
                if (code == Status.Code.NOT_FOUND || code == Status.Code.PERMISSION_DENIED) {
                    // They left or were kicked, or the room closed: nothing to report.
                    log.debug("No disconnect to report for room {}: {}", roomCode, code);
                } else {
                    log.warn("Reporting a dropped connection in room {} failed: {}", roomCode, e.getStatus());
                }
            }
        }
    }
}
