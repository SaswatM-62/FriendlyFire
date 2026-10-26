package com.friendlyfire.gateway.realtime;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/** Round-trip check for the browser's realtime indicator (and later the network bar). */
@Controller
public class PingController {

    /** /app/ping -> /user/queue/pong, only to the session that sent it, unchanged. */
    @MessageMapping("/ping")
    @SendToUser(destinations = "/queue/pong", broadcast = false)
    public Ping ping(Ping ping) {
        return ping;
    }

    /** {@code t}: the browser's Date.now() when it sent the ping. */
    public record Ping(long t) {
    }
}
