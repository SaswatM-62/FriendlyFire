package com.friendlyfire.room.health;

import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/** A dependency check with a fixed outcome, for tests. */
public final class FakeDependencyCheck implements DependencyCheck {

    private final String name;
    private final Callable<ComponentHealth> outcome;
    private volatile Duration lastDeadline;

    private FakeDependencyCheck(String name, Callable<ComponentHealth> outcome) {
        this.name = name;
        this.outcome = outcome;
    }

    /** Always answers UP. */
    public static FakeDependencyCheck up(String name) {
        return new FakeDependencyCheck(name, DependencyCheck::up);
    }

    /** Always fails with {@code error}. */
    public static FakeDependencyCheck failing(String name, Exception error) {
        return new FakeDependencyCheck(name, () -> {
            throw error;
        });
    }

    /** Never answers (until the checker gives up and interrupts it). */
    public static FakeDependencyCheck neverAnswers(String name) {
        return new FakeDependencyCheck(name, () -> {
            new CountDownLatch(1).await();
            return DependencyCheck.up();
        });
    }

    @Override
    public String name() {
        return name;
    }

    /** The deadline the last check was given. */
    public Duration lastDeadline() {
        return lastDeadline;
    }

    @Override
    public ComponentHealth check(Duration deadline) throws Exception {
        lastDeadline = deadline;
        return outcome.call();
    }
}
