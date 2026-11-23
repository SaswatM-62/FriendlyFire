package com.friendlyfire.gateway.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** One live connection per player, the newest wins (FF-104, ADR-017), and what a closed session leaves (FF-308). */
class SessionRegistryTest {

    private final SessionRegistry sessions = new SessionRegistry();

    @Test
    void theFirstConnectionReplacesNothing() {
        assertThat(sessions.connect("s1", "user-maya", "10.0.0.1")).isNull();
        assertThat(sessions.currentSessionOf("user-maya")).isEqualTo("s1");
    }

    @Test
    void aNewerConnectionReplacesTheLiveOne() {
        sessions.connect("s1", "user-maya", null);

        assertThat(sessions.connect("s2", "user-maya", null)).isEqualTo("s1");
        assertThat(sessions.currentSessionOf("user-maya")).isEqualTo("s2");
        assertThat(sessions.userOf("s2")).isEqualTo("user-maya");
    }

    @Test
    void theReplacedSessionEndingIsntCurrent() {
        sessions.connect("s1", "user-maya", null);
        sessions.subscribed("s1", "sub-1", "K7QM2X");
        sessions.connect("s2", "user-maya", null);

        SessionRegistry.Ended ended = sessions.end("s1");

        assertThat(ended.wasCurrent()).isFalse();
        assertThat(sessions.currentSessionOf("user-maya")).isEqualTo("s2");
    }

    @Test
    void endingASessionReturnsItsRoomsOnce() {
        sessions.connect("s1", "user-maya", null);
        sessions.subscribed("s1", "sub-1", "K7QM2X");
        sessions.subscribed("s1", "sub-2", "P4CWRT");

        SessionRegistry.Ended ended = sessions.end("s1");

        assertThat(ended).isEqualTo(new SessionRegistry.Ended("user-maya", true, Set.of("K7QM2X", "P4CWRT")));
        assertThat(sessions.end("s1")).isNull();
        assertThat(sessions.currentSessionOf("user-maya")).isNull();
    }

    @Test
    void signedOutSessionsHaveNoPlayer() {
        assertThat(sessions.connect("s1", null, "10.0.0.1")).isNull();
        assertThat(sessions.userOf("s1")).isNull();
        assertThat(sessions.remoteIp("s1")).isEqualTo("10.0.0.1");
    }

    @Test
    void tracksWhichRoomsASessionWatches() {
        sessions.connect("s1", "user-maya", null);
        sessions.subscribed("s1", "sub-1", "K7QM2X");

        assertThat(sessions.isSubscribedTo("s1", "K7QM2X")).isTrue();
        assertThat(sessions.unsubscribed("s1", "sub-1")).isEqualTo("K7QM2X");
        assertThat(sessions.isSubscribedTo("s1", "K7QM2X")).isFalse();
        assertThat(sessions.unsubscribed("s1", "sub-unknown")).isNull();
    }
}
