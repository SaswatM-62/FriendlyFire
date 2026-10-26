package health_test

import (
	"context"
	"fmt"
	"syscall"
	"testing"
	"time"

	healthv1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/health/v1"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/health"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/testutil"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
)

// Fails the package's tests if any goroutine is still running at the end.
func TestMain(m *testing.M) {
	goleak.VerifyTestMain(m)
}

func TestUpWhenEveryDependencyIsUp(t *testing.T) {
	checker := health.NewChecker("match-data-service", []health.Check{testutil.UpCheck("postgres")}, time.Second)

	node := checker.CheckAll(context.Background())

	assert.Equal(t, "match-data-service", node.GetName())
	assert.Equal(t, healthv1.ComponentStatus_COMPONENT_STATUS_UP, node.GetStatus())
	require.Len(t, node.GetDependencies(), 1)
	assert.Equal(t, "postgres", node.GetDependencies()[0].GetName())
	assert.Equal(t, healthv1.ComponentStatus_COMPONENT_STATUS_UP, node.GetDependencies()[0].GetStatus())
}

func TestDegradedWhenOneDependencyIsDown(t *testing.T) {
	refused := fmt.Errorf("dial tcp 172.18.0.2:5432: %w", syscall.ECONNREFUSED)
	checker := health.NewChecker("match-data-service", []health.Check{
		testutil.FailingCheck("postgres", refused),
	}, time.Second)

	node := checker.CheckAll(context.Background())

	assert.Equal(t, healthv1.ComponentStatus_COMPONENT_STATUS_DEGRADED, node.GetStatus())
	postgres := node.GetDependencies()[0]
	assert.Equal(t, healthv1.ComponentStatus_COMPONENT_STATUS_DOWN, postgres.GetStatus())
	assert.Equal(t, "connection refused", postgres.GetError())
}

func TestDependencyThatNeverAnswersHitsTheDeadline(t *testing.T) {
	checker := health.NewChecker("match-data-service", []health.Check{
		testutil.NeverAnswersCheck("postgres"),
	}, time.Second)

	start := time.Now()
	node := checker.CheckAll(context.Background())

	// Loose on purpose: CI runners can be slow.
	assert.Less(t, time.Since(start), 3*time.Second)
	assert.Equal(t, healthv1.ComponentStatus_COMPONENT_STATUS_DEGRADED, node.GetStatus())
	assert.Equal(t, healthv1.ComponentStatus_COMPONENT_STATUS_DOWN, node.GetDependencies()[0].GetStatus())
	assert.Equal(t, "deadline exceeded", node.GetDependencies()[0].GetError())
}

func TestAnswersBeforeTheCallerGivesUp(t *testing.T) {
	postgres := testutil.UpCheck("postgres")
	checker := health.NewChecker("match-data-service", []health.Check{postgres}, time.Second)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	checker.CheckAll(ctx)

	// The caller waits 1 s, so the dependency gets 1 s minus the reply margin (800 ms).
	assert.LessOrEqual(t, postgres.TimeLeft, 800*time.Millisecond)
	assert.Greater(t, postgres.TimeLeft, 500*time.Millisecond)
}

func TestNeverGivesADependencyMoreThanItsOwnDeadline(t *testing.T) {
	postgres := testutil.UpCheck("postgres")
	checker := health.NewChecker("match-data-service", []health.Check{postgres}, time.Second)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	checker.CheckAll(ctx)

	assert.LessOrEqual(t, postgres.TimeLeft, time.Second)
	assert.Greater(t, postgres.TimeLeft, 700*time.Millisecond)
}
