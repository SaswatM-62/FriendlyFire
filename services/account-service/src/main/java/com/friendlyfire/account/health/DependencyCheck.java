package com.friendlyfire.account.health;

import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus;
import java.time.Duration;

/** One thing this service depends on: a gRPC peer, Postgres or an HTTP API. */
public interface DependencyCheck {

    /** The node's name in the health tree, e.g. "postgres". */
    String name();

    /**
     * Checks the dependency once and returns its node: just a status for a leaf, or the
     * tree a gRPC peer reported. Throws if the dependency can't be reached.
     * {@link DependencyHealthChecker} fills in the name and latency and turns
     * exceptions into DOWN nodes.
     */
    ComponentHealth check(Duration deadline) throws Exception;

    /** The node for a leaf dependency that answered. */
    static ComponentHealth up() {
        return ComponentHealth.newBuilder().setStatus(ComponentStatus.COMPONENT_STATUS_UP).build();
    }
}
