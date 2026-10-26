package com.friendlyfire.room.config;

import com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthServiceGrpc.DependencyHealthServiceBlockingStub;
import com.friendlyfire.room.health.DependencyHealthChecker;
import com.friendlyfire.room.health.GrpcDependencyCheck;
import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * What /api/health/deep checks for room-service. When this service starts calling
 * something new, add it here, using the same client the feature uses.
 */
@Configuration(proxyBeanMethods = false)
public class HealthConfig {

    private static final Duration DEADLINE = Duration.ofSeconds(1);

    @Bean
    DependencyHealthChecker dependencyHealthChecker(
            @Value("${spring.application.name}") String serviceName,
            @Qualifier("quizHealth") DependencyHealthServiceBlockingStub quizHealth,
            @Qualifier("quizChannel") ManagedChannel quizChannel,
            @Qualifier("scoringHealth") DependencyHealthServiceBlockingStub scoringHealth,
            @Qualifier("scoringChannel") ManagedChannel scoringChannel,
            @Qualifier("matchDataHealth") DependencyHealthServiceBlockingStub matchDataHealth,
            @Qualifier("matchDataChannel") ManagedChannel matchDataChannel) {
        return new DependencyHealthChecker(serviceName, List.of(
                new GrpcDependencyCheck("quiz-engine-service", quizHealth, quizChannel),
                new GrpcDependencyCheck("scoring-service", scoringHealth, scoringChannel),
                new GrpcDependencyCheck("match-data-service", matchDataHealth, matchDataChannel)),
                DEADLINE);
    }
}
