package com.friendlyfire.room.health;

import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Checks every dependency of this service in parallel, each with the same deadline,
 * and builds this service's node of the health tree.
 *
 * <p>It never throws for an unhealthy dependency: a failed or slow check becomes a
 * DOWN node with a short error, and this service is then DEGRADED.
 */
public class DependencyHealthChecker implements AutoCloseable {

    /** Part of the caller's deadline kept for sending the answer back. */
    private static final Duration REPLY_MARGIN = Duration.ofMillis(200);
    private static final Duration MIN_DEADLINE = Duration.ofMillis(100);

    private final String serviceName;
    private final List<DependencyCheck> checks;
    private final Duration deadline;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public DependencyHealthChecker(String serviceName, List<DependencyCheck> checks, Duration deadline) {
        this.serviceName = serviceName;
        this.checks = List.copyOf(checks);
        this.deadline = deadline;
    }

    /** This service's node, with one child per dependency (in the order they were given). */
    public ComponentHealth check() {
        return checkWithin(deadline);
    }

    /**
     * Like {@link #check()}, but answers before a caller that only waits
     * {@code callerTimeLeft} gives up: each dependency then gets that time minus a
     * margin for the reply. A slow dependency deep in the tree shows up as DOWN where
     * it is, instead of every service above it timing out too.
     */
    public ComponentHealth check(Duration callerTimeLeft) {
        Duration budget = callerTimeLeft.minus(REPLY_MARGIN);
        if (budget.compareTo(MIN_DEADLINE) < 0) {
            budget = MIN_DEADLINE;
        }
        return checkWithin(budget.compareTo(deadline) < 0 ? budget : deadline);
    }

    private ComponentHealth checkWithin(Duration checkDeadline) {
        long deadlineNanos = System.nanoTime() + checkDeadline.toNanos();
        List<Future<ComponentHealth>> running = new ArrayList<>();
        for (DependencyCheck check : checks) {
            running.add(executor.submit(() -> run(check, checkDeadline)));
        }

        ComponentHealth.Builder node = ComponentHealth.newBuilder().setName(serviceName);
        boolean allUp = true;
        for (int i = 0; i < checks.size(); i++) {
            ComponentHealth dependency = await(checks.get(i), running.get(i), deadlineNanos, checkDeadline);
            allUp &= dependency.getStatus() == ComponentStatus.COMPONENT_STATUS_UP;
            node.addDependencies(dependency);
        }
        return node.setStatus(allUp ? ComponentStatus.COMPONENT_STATUS_UP : ComponentStatus.COMPONENT_STATUS_DEGRADED)
                .build();
    }

    private ComponentHealth run(DependencyCheck check, Duration checkDeadline) {
        long start = System.nanoTime();
        try {
            return check.check(checkDeadline).toBuilder()
                    .setName(check.name())
                    .setLatencyMs(millisSince(start))
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return down(check.name(), millisSince(start), "deadline exceeded");
        } catch (Exception e) {
            return down(check.name(), millisSince(start), HealthErrors.describe(e));
        }
    }

    private ComponentHealth await(
            DependencyCheck check, Future<ComponentHealth> result, long deadlineNanos, Duration checkDeadline) {
        try {
            return result.get(Math.max(0, deadlineNanos - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            result.cancel(true);
            return down(check.name(), (int) checkDeadline.toMillis(), "deadline exceeded");
        } catch (ExecutionException e) {
            return down(check.name(), (int) checkDeadline.toMillis(), HealthErrors.describe(e.getCause()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.cancel(true);
            return down(check.name(), 0, "interrupted");
        }
    }

    private static ComponentHealth down(String name, int latencyMs, String error) {
        return ComponentHealth.newBuilder()
                .setName(name)
                .setStatus(ComponentStatus.COMPONENT_STATUS_DOWN)
                .setLatencyMs(latencyMs)
                .setError(error)
                .build();
    }

    private static int millisSince(long startNanos) {
        return (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
