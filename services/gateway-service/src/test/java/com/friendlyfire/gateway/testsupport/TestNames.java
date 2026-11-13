package com.friendlyfire.gateway.testsupport;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fresh player names and room codes, so tests sharing a Spring context (and its one
 * connection per player, rate limits and relayed rooms) never meet each other's state.
 */
public final class TestNames {

    private static final String ROOM_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final AtomicInteger NEXT = new AtomicInteger();

    private TestNames() {
    }

    /** "player17": user "user-player17", display name "Player17". */
    public static String player() {
        return "player" + NEXT.incrementAndGet();
    }

    /** A valid room code no other test uses. */
    public static String roomCode() {
        int n = NEXT.incrementAndGet();
        StringBuilder code = new StringBuilder("T");
        for (int i = 0; i < 5; i++) {
            code.append(ROOM_ALPHABET.charAt(n % ROOM_ALPHABET.length()));
            n /= ROOM_ALPHABET.length();
        }
        return code.toString();
    }
}
