package com.friendlyfire.gateway.realtime;

import java.time.Clock;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/** Round-trip check for the network bar and the status page's realtime indicator. */
@Controller
public class PingController {

    private final Clock clock;

    public PingController(Clock clock) {
        this.clock = clock;
    }

    /**
     * /app/ping -> /user/queue/pong, only to the session that sent it: the browser's own
     * {@code t} back, plus this server's time so the browser can line its clock up with
     * phase deadlines (docs/realtime-messages.md).
     */
    @MessageMapping("/ping")
    @SendToUser(destinations = "/queue/pong", broadcast = false)
    public Pong ping(Ping ping) {
        return new Pong(ping.t(), clock.millis());
    }

    /** {@code t}: the browser's Date.now() when it sent the ping. */
    public record Ping(long t) {
    }

    /** {@code serverTime}: this server's clock, in milliseconds since the epoch, when it answered. */
    public record Pong(long t, long serverTime) {
    }
}
