package com.friendlyfire.gateway.protection;

import com.friendlyfire.gateway.web.ApiException;
import com.friendlyfire.gateway.web.ErrorCodes;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;

/**
 * Token buckets (Bucket4j) per action and player, and per action and IP address. Buckets
 * live in memory, on this Gateway only, and are dropped after an hour without use.
 */
public class RateLimiter {

    private final TimeMeter time;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(200_000)
            .build();

    public RateLimiter(Clock clock) {
        this.time = new TimeMeter() {
            @Override
            public long currentTimeNanos() {
                return TimeUnit.MILLISECONDS.toNanos(clock.millis());
            }

            @Override
            public boolean isWallClockBased() {
                return true;
            }
        };
    }

    /**
     * Uses up one of this player's and this IP's allowance for {@code limit}, or throws 429
     * RATE_LIMITED. A null {@code ip} (unknown) is only limited per player.
     */
    public void check(RateLimit limit, String userId, String ip) {
        boolean allowed = take(limit, "user", userId, limit.perUser());
        if (ip != null && !ip.isEmpty()) {
            allowed &= take(limit, "ip", ip, limit.perIp());
        }
        if (!allowed) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.RATE_LIMITED,
                    "You're going a bit fast. Wait a moment and try again.");
        }
    }

    private boolean take(RateLimit limit, String kind, String key, RateLimit.Allowance allowance) {
        Bucket bucket = buckets.get(limit.name() + '|' + kind + '|' + key, ignored -> Bucket.builder()
                .addLimit(bandwidth -> bandwidth.capacity(allowance.count())
                        .refillGreedy(allowance.count(), allowance.period()))
                .withCustomTimePrecision(time)
                .build());
        return bucket.tryConsume(1);
    }
}
