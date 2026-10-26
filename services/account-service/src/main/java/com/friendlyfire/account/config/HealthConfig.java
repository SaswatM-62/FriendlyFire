package com.friendlyfire.account.config;

import com.friendlyfire.account.health.DependencyHealthChecker;
import com.friendlyfire.account.health.GrpcDependencyCheck;
import com.friendlyfire.account.health.HttpDependencyCheck;
import com.friendlyfire.account.health.PostgresDependencyCheck;
import com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthServiceGrpc.DependencyHealthServiceBlockingStub;
import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * What /api/health/deep checks for account-service. When this service starts calling
 * something new, add it here, using the same client the feature uses.
 */
@Configuration(proxyBeanMethods = false)
public class HealthConfig {

    private static final Duration DEADLINE = Duration.ofSeconds(1);

    @Bean
    DependencyHealthChecker dependencyHealthChecker(
            @Value("${spring.application.name}") String serviceName,
            DataSource dataSource,
            @Qualifier("mockRsoRestClient") RestClient mockRso,
            @Qualifier("matchDataHealth") DependencyHealthServiceBlockingStub matchDataHealth,
            @Qualifier("matchDataChannel") ManagedChannel matchDataChannel) {
        return new DependencyHealthChecker(serviceName, List.of(
                new PostgresDependencyCheck(dataSource),
                new HttpDependencyCheck("mock-rso-service", mockRso, "/healthz"),
                new GrpcDependencyCheck("match-data-service", matchDataHealth, matchDataChannel)),
                DEADLINE);
    }
}
