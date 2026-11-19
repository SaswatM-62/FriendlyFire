package com.friendlyfire.room.timing;

import java.time.Duration;
import java.time.Instant;

/**
 * Runs tasks later, on the injected Clock's time. Production uses
 * {@link ExecutorScheduler}; tests use a fake that runs due tasks when the clock moves.
 */
public interface Scheduler {

    /** Runs {@code task} at {@code at} (right away if that's past). */
    Cancellable schedule(Instant at, Runnable task);

    /** Runs {@code task} every {@code period}, first after one period. */
    Cancellable every(Duration period, Runnable task);

    /** A scheduled task that hasn't run yet can be called off. */
    interface Cancellable {
        void cancel();
    }
}
