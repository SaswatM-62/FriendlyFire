package com.friendlyfire.account.health;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest;
import com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse;
import com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class DependencyHealthGrpcServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void answersWithTheTreeAndCompletes() {
        try (var checker = new DependencyHealthChecker(
                "account-service", List.of(FakeDependencyCheck.up("postgres")), Duration.ofSeconds(1))) {
            // StreamObserver is gRPC's interface, so a Mockito mock is fine here; use fakes
            // (like FakeDependencyCheck) for our own interfaces.
            StreamObserver<CheckDependenciesResponse> responses = mock(StreamObserver.class);

            new DependencyHealthGrpcService(checker)
                    .checkDependencies(CheckDependenciesRequest.getDefaultInstance(), responses);

            var order = inOrder(responses);
            order.verify(responses).onNext(argThat(response ->
                    response.getComponent().getStatus() == ComponentStatus.COMPONENT_STATUS_UP));
            order.verify(responses).onCompleted();
        }
    }
}
