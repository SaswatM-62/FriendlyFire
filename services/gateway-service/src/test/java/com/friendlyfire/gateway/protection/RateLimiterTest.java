package com.friendlyfire.gateway.protection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.friendlyfire.gateway.testsupport.MutableClock;
import com.friendlyfire.gateway.web.ApiException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-13T10:00:00Z"));
    private final RateLimiter limiter = new RateLimiter(clock);

    @Test
    void allowsABurstThenRefuses() {
        for (int i = 0; i < 5; i++) {
            limiter.check(RateLimit.START_RIOT_LINK, "user-maya", "10.0.0.1");
        }

        ApiException error = catchThrowableOfType(ApiException.class,
                () -> limiter.check(RateLimit.START_RIOT_LINK, "user-maya", "10.0.0.1"));
        assertThat(error.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(error.code()).isEqualTo("RATE_LIMITED");
    }

    @Test
    void refillsOverTime() {
        for (int i = 0; i < 5; i++) {
            limiter.check(RateLimit.START_RIOT_LINK, "user-maya", null);
        }
        clock.advance(Duration.ofMinutes(1));

        assertThatCode(() -> limiter.check(RateLimit.START_RIOT_LINK, "user-maya", null)).doesNotThrowAnyException();
    }

    @Test
    void eachPlayerHasTheirOwnAllowance() {
        for (int i = 0; i < 5; i++) {
            limiter.check(RateLimit.START_RIOT_LINK, "user-maya", null);
        }

        assertThatCode(() -> limiter.check(RateLimit.START_RIOT_LINK, "user-sam", null)).doesNotThrowAnyException();
    }

    @Test
    void oneAddressCantGetAroundItWithManyAccounts() {
        for (int i = 0; i < 20; i++) {
            limiter.check(RateLimit.START_RIOT_LINK, "user-" + i, "10.0.0.1");
        }

        assertThatCode(() -> limiter.check(RateLimit.START_RIOT_LINK, "user-new", "10.0.0.1"))
                .isInstanceOf(ApiException.class);
        assertThatCode(() -> limiter.check(RateLimit.START_RIOT_LINK, "user-new", "10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void eightFriendsOnOneWifiCanAllAnswer() {
        for (int player = 0; player < 8; player++) {
            String userId = "user-" + player;
            assertThatCode(() -> limiter.check(RateLimit.ANSWER, userId, "10.0.0.1")).doesNotThrowAnyException();
        }
    }
}
