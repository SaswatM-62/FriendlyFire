package com.friendlyfire.room.health;

import com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest;
import com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse;
import com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthServiceGrpc;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/** friendlyfire.health.v1.DependencyHealthService: "can I reach everything I depend on?" */
@Service
public class DependencyHealthGrpcService extends DependencyHealthServiceGrpc.DependencyHealthServiceImplBase {

    private final DependencyHealthChecker checker;

    public DependencyHealthGrpcService(DependencyHealthChecker checker) {
        this.checker = checker;
    }

    @Override
    public void checkDependencies(
            CheckDependenciesRequest request, StreamObserver<CheckDependenciesResponse> responseObserver) {
        // Answer before the caller's own deadline (e.g. room-service waits 1 second).
        Deadline callerDeadline = Context.current().getDeadline();
        var health = callerDeadline == null
                ? checker.check()
                : checker.check(Duration.ofMillis(callerDeadline.timeRemaining(TimeUnit.MILLISECONDS)));
        responseObserver.onNext(CheckDependenciesResponse.newBuilder().setComponent(health).build());
        responseObserver.onCompleted();
    }
}
