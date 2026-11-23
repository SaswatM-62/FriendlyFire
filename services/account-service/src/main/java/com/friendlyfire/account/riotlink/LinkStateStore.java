package com.friendlyfire.account.riotlink;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * The one-time {@code state} values of Riot links in progress. StartRiotLink makes one;
 * CompleteRiotLink uses it up, which proves the code it got back belongs to a link this
 * user started from here (OAuth's protection against forged callbacks).
 *
 * <p>Kept in memory: a restart only cancels links started in the last 10 minutes, and the
 * player starts again.
 */
@Component
public class LinkStateStore {

    static final Duration LIFETIME = Duration.ofMinutes(10);

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, PendingLink> pending = new ConcurrentHashMap<>();

    public LinkStateStore(Clock clock) {
        this.clock = clock;
    }

    /** A new random state for this user and redirect URI, valid for 10 minutes. */
    public String create(UUID userId, String redirectUri) {
        Instant now = clock.instant();
        pending.values().removeIf(link -> !link.expiresAt().isAfter(now));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pending.put(state, new PendingLink(userId, redirectUri, now.plus(LIFETIME)));
        return state;
    }

    /**
     * Uses the state up. True only if it exists, hasn't expired, and was made for this
     * user and redirect URI. It's gone afterwards either way, so it can't be tried twice.
     */
    public boolean take(String state, UUID userId, String redirectUri) {
        PendingLink link = state == null ? null : pending.remove(state);
        return link != null
                && link.expiresAt().isAfter(clock.instant())
                && link.userId().equals(userId)
                && link.redirectUri().equals(redirectUri);
    }

    /** Drops a user's links in progress (their account was deleted). */
    public void forget(UUID userId) {
        pending.values().removeIf(link -> link.userId().equals(userId));
    }

    private record PendingLink(UUID userId, String redirectUri, Instant expiresAt) {}
}
