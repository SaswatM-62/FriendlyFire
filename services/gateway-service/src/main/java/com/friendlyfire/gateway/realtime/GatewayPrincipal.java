package com.friendlyfire.gateway.realtime;

import java.security.Principal;

/**
 * The signed-in player of a WebSocket session, set once on CONNECT. Its name is our
 * user_id: STOMP handlers take it from here (never from a message body), and
 * /user/queue/... messages are routed by it.
 */
public record GatewayPrincipal(String userId) implements Principal {

    @Override
    public String getName() {
        return userId;
    }
}
