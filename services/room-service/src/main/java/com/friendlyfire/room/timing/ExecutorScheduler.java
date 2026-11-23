package com.friendlyfire.room.timing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The real scheduler: one timer thread that hands each due task to {@code workers}
 * (virtual threads), so a slow task never delays the others.
 */
public class ExecutorScheduler implements Scheduler, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ExecutorScheduler.class);

    private final Clock clock;
    private final Executor workers;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("room-timer").daemon().factory());

    public ExecutorScheduler(Clock clock, Executor workers) {
        this.clock = clock;
        this.workers = workers;
    }

    @Override
    public Cancellable schedule(Instant at, Runnable task) {
        long delay = Math.max(0, Duration.between(clock.instant(), at).toMillis());
        var future = timer.schedule(() -> workers.execute(safely(task)), delay, TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    @Override
    public Cancellable every(Duration period, Runnable task) {
        long millis = period.toMillis();
        var future = timer.scheduleWithFixedDelay(
                () -> workers.execute(safely(task)), millis, millis, TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    private static Runnable safely(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                log.error("scheduled room task failed", e);
            }
        };
    }

    @Override
    public void close() {
        timer.shutdownNow();
    }
}
