package com.friendlyfire.gateway.protection;

import java.time.Duration;

/**
 * The rate-limited actions (FF-601), each limited per player and per IP address. The IP
 * limits leave room for a party of 8 friends on the same Wi-Fi.
 */
public enum RateLimit {
    CREATE_ROOM(new Allowance(10, Duration.ofMinutes(1)), new Allowance(30, Duration.ofMinutes(1))),
    JOIN_ROOM(new Allowance(30, Duration.ofMinutes(1)), new Allowance(80, Duration.ofMinutes(1))),
    START_RIOT_LINK(new Allowance(5, Duration.ofMinutes(1)), new Allowance(20, Duration.ofMinutes(1))),
    /** One answer per question, plus room for a few repeated taps. */
    ANSWER(new Allowance(10, Duration.ofSeconds(10)), new Allowance(80, Duration.ofSeconds(10)));

    private final Allowance perUser;
    private final Allowance perIp;

    RateLimit(Allowance perUser, Allowance perIp) {
        this.perUser = perUser;
        this.perIp = perIp;
    }

    Allowance perUser() {
        return perUser;
    }

    Allowance perIp() {
        return perIp;
    }

    /** At most {@code count} in a burst, refilled evenly over {@code period}. */
    record Allowance(int count, Duration period) {
    }
}
