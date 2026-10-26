package com.friendlyfire.gateway.config;

import com.friendlyfire.gateway.health.DependencyHealthChecker;
import com.friendlyfire.gateway.health.GrpcDependencyCheck;
import com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthServiceGrpc.DependencyHealthServiceBlockingStub;
import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * What GET /api/health/deep checks: every service the Gateway calls. Each of them
 * checks its own dependencies with a 1-second deadline, so the Gateway waits 1.5 s.
 */
@Configuration(proxyBeanMethods = false)
public class HealthConfig {

    private static final Duration DEADLINE = Duration.ofMillis(1500);

    @Bean
    DependencyHealthChecker dependencyHealthChecker(
            @Value("${spring.application.name}") String serviceName,
            @Qualifier("accountHealth") DependencyHealthServiceBlockingStub accountHealth,
            @Qualifier("accountChannel") ManagedChannel accountChannel,
            @Qualifier("roomHealth") DependencyHealthServiceBlockingStub roomHealth,
            @Qualifier("roomChannel") ManagedChannel roomChannel,
            @Qualifier("scoringHealth") DependencyHealthServiceBlockingStub scoringHealth,
            @Qualifier("scoringChannel") ManagedChannel scoringChannel,
            @Qualifier("matchDataHealth") DependencyHealthServiceBlockingStub matchDataHealth,
            @Qualifier("matchDataChannel") ManagedChannel matchDataChannel) {
        return new DependencyHealthChecker(serviceName, List.of(
                new GrpcDependencyCheck("account-service", accountHealth, accountChannel),
                new GrpcDependencyCheck("room-service", roomHealth, roomChannel),
                new GrpcDependencyCheck("scoring-service", scoringHealth, scoringChannel),
                new GrpcDependencyCheck("match-data-service", matchDataHealth, matchDataChannel)),
                DEADLINE);
    }
}
