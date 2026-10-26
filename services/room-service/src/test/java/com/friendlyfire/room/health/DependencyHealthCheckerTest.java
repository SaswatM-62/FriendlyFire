package com.friendlyfire.room.health;

import static com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus.COMPONENT_STATUS_DEGRADED;
import static com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus.COMPONENT_STATUS_DOWN;
import static com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus.COMPONENT_STATUS_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import io.grpc.Status;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class DependencyHealthCheckerTest {

    private static final Duration DEADLINE = Duration.ofSeconds(1);

    @Test
    void upWhenEveryDependencyIsUp() {
        try (var checker = checker(
                FakeDependencyCheck.up("quiz-engine-service"),
                FakeDependencyCheck.up("scoring-service"),
                FakeDependencyCheck.up("match-data-service"))) {
            ComponentHealth health = checker.check();

            assertThat(health.getName()).isEqualTo("room-service");
            assertThat(health.getStatus()).isEqualTo(COMPONENT_STATUS_UP);
            assertThat(health.getDependenciesList()).extracting(ComponentHealth::getStatus).containsOnly(COMPONENT_STATUS_UP);
        }
    }

    @Test
    void degradedWhenOneDependencyIsDown() {
        try (var checker = checker(
                FakeDependencyCheck.up("quiz-engine-service"),
                FakeDependencyCheck.failing("scoring-service", Status.UNAVAILABLE
                        .withCause(new ConnectException("Connection refused"))
                        .asRuntimeException()))) {
            ComponentHealth health = checker.check();

            assertThat(health.getStatus()).isEqualTo(COMPONENT_STATUS_DEGRADED);
            ComponentHealth scoring = health.getDependencies(1);
            assertThat(scoring.getName()).isEqualTo("scoring-service");
            assertThat(scoring.getStatus()).isEqualTo(COMPONENT_STATUS_DOWN);
            assertThat(scoring.getError()).isEqualTo("connection refused");
        }
    }

    @Test
    void dependencyThatNeverAnswersHitsTheDeadline() {
        try (var checker = checker(FakeDependencyCheck.neverAnswers("match-data-service"))) {
            CompletableFuture<ComponentHealth> result = CompletableFuture.supplyAsync(checker::check);

            // Loose on purpose: CI runners can be slow.
            await().atMost(Duration.ofSeconds(3)).until(result::isDone);
            ComponentHealth health = result.join();
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
        return new DependencyHealthChecker("room-service", List.of(checks), DEADLINE);
    }
}
