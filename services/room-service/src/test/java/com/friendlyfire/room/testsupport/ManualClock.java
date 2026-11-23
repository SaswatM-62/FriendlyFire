package com.friendlyfire.room.testsupport;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that only moves when a test moves it (through FakeScheduler.advance). */
public final class ManualClock extends Clock {

    private volatile Instant now;

    public ManualClock(Instant now) {
        this.now = now;
    }

    void set(Instant instant) {
        now = instant;
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
