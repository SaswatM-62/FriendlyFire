package com.friendlyfire.account.riotlink;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.account.testsupport.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LinkStateStoreTest {

    private static final String CALLBACK = "http://localhost:3000/link/riot/callback";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-16T12:00:00Z"));
    private final LinkStateStore states = new LinkStateStore(clock);
    private final UUID maya = UUID.randomUUID();
    private final UUID sam = UUID.randomUUID();

    @Test
    void aStateWorksOnceForTheUserAndRedirectItWasMadeFor() {
        String state = states.create(maya, CALLBACK);

        assertThat(states.take(state, maya, CALLBACK)).isTrue();
        assertThat(states.take(state, maya, CALLBACK)).isFalse();
    }

    @Test
    void anotherUsersStateDoesntWorkAndIsUsedUpTrying() {
        String state = states.create(maya, CALLBACK);

        assertThat(states.take(state, sam, CALLBACK)).isFalse();
        assertThat(states.take(state, maya, CALLBACK)).isFalse();
    }

    @Test
    void aDifferentRedirectDoesntWork() {
        String state = states.create(maya, CALLBACK);

        assertThat(states.take(state, maya, "https://evil.example/callback")).isFalse();
    }

    @Test
    void aStateExpiresAfterTenMinutes() {
        String fresh = states.create(maya, CALLBACK);
        String stale = states.create(maya, CALLBACK);

        clock.advance(Duration.ofMinutes(9).plusSeconds(59));
        assertThat(states.take(fresh, maya, CALLBACK)).isTrue();
        clock.advance(Duration.ofSeconds(1));
        assertThat(states.take(stale, maya, CALLBACK)).isFalse();
    }

    @Test
    void unknownAndMissingStatesDontWork() {
        assertThat(states.take("made-up", maya, CALLBACK)).isFalse();
        assertThat(states.take(null, maya, CALLBACK)).isFalse();
    }

    @Test
    void forgettingAUserDropsTheirLinksInProgress() {
        String mine = states.create(maya, CALLBACK);
        String theirs = states.create(sam, CALLBACK);

        states.forget(maya);

        assertThat(states.take(mine, maya, CALLBACK)).isFalse();
        assertThat(states.take(theirs, sam, CALLBACK)).isTrue();
    }

    @Test
    void statesAreLongRandomAndUrlSafe() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String state = states.create(maya, CALLBACK);
            assertThat(state).matches("[A-Za-z0-9_-]{43}");
            seen.add(state);
        }
        assertThat(seen).hasSize(100);
    }
}
