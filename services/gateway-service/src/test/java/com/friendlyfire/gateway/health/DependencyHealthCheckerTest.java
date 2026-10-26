package com.friendlyfire.gateway.health;

import static com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus.COMPONENT_STATUS_DEGRADED;
import static com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus.COMPONENT_STATUS_DOWN;
import static com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus.COMPONENT_STATUS_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class DependencyHealthCheckerTest {

    private static final Duration DEADLINE = Duration.ofMillis(1500);

    @Test
    void upWhenEveryDependencyIsUp() {
        try (var checker = checker(FakeDependencyCheck.up("account-service"), FakeDependencyCheck.up("match-data-service"))) {
            ComponentHealth health = checker.check();

            assertThat(health.getName()).isEqualTo("gateway-service");
            assertThat(health.getStatus()).isEqualTo(COMPONENT_STATUS_UP);
            assertThat(health.getDependenciesList())
                    .extracting(ComponentHealth::getName, ComponentHealth::getStatus)
                    .containsExactly(
                            tuple("account-service", COMPONENT_STATUS_UP), tuple("match-data-service", COMPONENT_STATUS_UP));
        }
    }

    @Test
    void degradedWhenOneDependencyIsDown() {
        try (var checker = checker(
                FakeDependencyCheck.failing("account-service", new ConnectException("Connection refused")),
                FakeDependencyCheck.up("match-data-service"))) {
            ComponentHealth health = checker.check();

            assertThat(health.getStatus()).isEqualTo(COMPONENT_STATUS_DEGRADED);
            ComponentHealth postgres = health.getDependencies(0);
            assertThat(postgres.getStatus()).isEqualTo(COMPONENT_STATUS_DOWN);
            assertThat(postgres.getError()).isEqualTo("connection refused");
            assertThat(health.getDependencies(1).getStatus()).isEqualTo(COMPONENT_STATUS_UP);
        }
    }

    @Test
    void dependencyThatNeverAnswersHitsTheDeadline() {
        try (var checker = checker(FakeDependencyCheck.neverAnswers("match-data-service"))) {
            long start = System.nanoTime();
            ComponentHealth health = checker.check();
            Duration took = Duration.ofNanos(System.nanoTime() - start);

            // Loose on purpose: CI runners can be slow.
            assertThat(took).isLessThan(Duration.ofSeconds(3));
            assertThat(health.getStatus()).isEqualTo(COMPONENT_STATUS_DEGRADED);
            assertThat(health.getDependencies(0).getStatus()).isEqualTo(COMPONENT_STATUS_DOWN);
            assertThat(health.getDependencies(0).getError()).isEqualTo("deadline exceeded");
        }
    }

    @Test
    void answersBeforeTheCallerGivesUp() {
        FakeDependencyCheck matchData = FakeDependencyCheck.up("match-data-service");
        try (var checker = checker(matchData)) {
            checker.check(Duration.ofSeconds(1));

            // The caller waits 1 s, so the dependency gets 1 s minus the reply margin.
            assertThat(matchData.lastDeadline()).isEqualTo(Duration.ofMillis(800));
        }
    }

    @Test
    void neverGivesADependencyMoreThanItsOwnDeadline() {
        FakeDependencyCheck matchData = FakeDependencyCheck.up("match-data-service");
        try (var checker = checker(matchData)) {
            checker.check(Duration.ofSeconds(30));

            assertThat(matchData.lastDeadline()).isEqualTo(DEADLINE);
        }
    }

    private static DependencyHealthChecker checker(DependencyCheck... checks) {
        return new DependencyHealthChecker("gateway-service", List.of(checks), DEADLINE);
    }
}
