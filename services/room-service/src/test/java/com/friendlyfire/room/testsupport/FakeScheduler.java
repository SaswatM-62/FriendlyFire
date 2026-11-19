package com.friendlyfire.room.testsupport;

import com.friendlyfire.room.timing.Scheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Runs scheduled tasks on the test's thread when {@link #advance} moves the clock past
 * them, in time order, so a whole quiz runs in milliseconds.
 */
public final class FakeScheduler implements Scheduler {

    private final ManualClock clock;
    private final List<Task> tasks = new ArrayList<>();
    private long sequence;

    public FakeScheduler(ManualClock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized Cancellable schedule(Instant at, Runnable task) {
        Task scheduled = new Task(at, null, task, sequence++);
        tasks.add(scheduled);
        return () -> cancel(scheduled);
    }

    @Override
    public synchronized Cancellable every(Duration period, Runnable task) {
        Task scheduled = new Task(clock.instant().plus(period), period, task, sequence++);
        tasks.add(scheduled);
        return () -> cancel(scheduled);
    }

    private synchronized void cancel(Task task) {
        tasks.remove(task);
    }

    /** Moves the clock forward, running every task that comes due on the way. */
    public void advance(Duration duration) {
        Instant end = clock.instant().plus(duration);
        while (true) {
            Task next;
            synchronized (this) {
                next = tasks.stream()
                        .filter(t -> !t.at.isAfter(end))
                        .min(Comparator.comparing((Task t) -> t.at).thenComparingLong(t -> t.sequence))
                        .orElse(null);
                if (next == null) {
                    break;
                }
                tasks.remove(next);
                if (next.at.isAfter(clock.instant())) {
                    clock.set(next.at);
                }
                if (next.period != null) {
                    tasks.add(new Task(next.at.plus(next.period), next.period, next.task, sequence++));
                }
            }
            next.task.run();
        }
        clock.set(end);
    }

    /** Runs whatever is due now without moving the clock. */
    public void runDue() {
        advance(Duration.ZERO);
    }

    private record Task(Instant at, Duration period, Runnable task, long sequence) {
    }
}
