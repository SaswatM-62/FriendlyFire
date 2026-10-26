package com.friendlyfire.account.health;

import com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest;
import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthServiceGrpc.DependencyHealthServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Calls a gRPC peer's CheckDependencies and returns the tree it reports. */
public class GrpcDependencyCheck implements DependencyCheck {

    private final String name;
    private final DependencyHealthServiceBlockingStub health;
    private final ManagedChannel channel;

    /** {@code health} must be a stub on {@code channel}, the same channel the feature stub uses. */
    public GrpcDependencyCheck(String name, DependencyHealthServiceBlockingStub health, ManagedChannel channel) {
        this.name = name;
        this.health = health;
        this.channel = channel;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ComponentHealth check(Duration deadline) {
        try {
            return health.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                    .checkDependencies(CheckDependenciesRequest.getDefaultInstance())
                    .getComponent();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                // Reconnect right away next time instead of waiting out gRPC's reconnect
                // backoff (up to 2 minutes), so a restarted peer turns UP quickly.
                channel.resetConnectBackoff();
            }
            throw e;
        }
    }
}
