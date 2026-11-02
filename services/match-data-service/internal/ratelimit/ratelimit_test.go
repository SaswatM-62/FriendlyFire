package ratelimit_test

import (
	"context"
	"sync"
	"testing"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/ratelimit"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/testutil"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
)

// None of these tests sleeps: time only moves when a test advances the fake clock. The
// 5-second guards below run out only when a test is already failing.

var start = time.Date(2026, 11, 2, 12, 0, 0, 0, time.UTC)

func TestMain(m *testing.M) { goleak.VerifyTestMain(m) }

func newLimiter() (*ratelimit.Limiter, *testutil.FakeClock) {
	clock := testutil.NewFakeClock(start)
	return ratelimit.New(clock), clock
}

// useUp lets n requests through; none of them may have to wait.
func useUp(t *testing.T, limiter *ratelimit.Limiter, n int) {
	t.Helper()
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	for range n {
		require.NoError(t, limiter.Wait(ctx))
	}
}

// startWait calls Wait in a goroutine; the result arrives on the returned channel.
func startWait(ctx context.Context, limiter *ratelimit.Limiter) <-chan error {
	done := make(chan error, 1)
	go func() { done <- limiter.Wait(ctx) }()
	return done
}

func requireDone(t *testing.T, done <-chan error) error {
	t.Helper()
	select {
	case err := <-done:
		return err
	case <-time.After(5 * time.Second):
		t.Fatal("Wait did not return")
		return nil
	}
}

func requireStillWaiting(t *testing.T, done <-chan error) {
	t.Helper()
	select {
	case err := <-done:
		t.Fatalf("Wait returned too early: %v", err)
	default:
	}
}

func TestThe31stRequestInAMinuteWaitsUntilTheFirstIsAMinuteOld(t *testing.T) {
	limiter, clock := newLimiter()
	useUp(t, limiter, 30)

	done := startWait(context.Background(), limiter)
	clock.BlockUntil(t, 1)
	clock.Advance(59 * time.Second)

	assert.Equal(t, 1, clock.Pending(), "still waiting one second before the minute is up")
	requireStillWaiting(t, done)

	clock.Advance(time.Second)
	require.NoError(t, requireDone(t, done))
}

func TestTheWindowSlidesInsteadOfResettingEveryMinute(t *testing.T) {
	limiter, clock := newLimiter()
	useUp(t, limiter, 15)
	clock.Advance(30 * time.Second)
	useUp(t, limiter, 15)

	done := startWait(context.Background(), limiter)
	clock.BlockUntil(t, 1)
	clock.Advance(29 * time.Second)
	assert.Equal(t, 1, clock.Pending(), "the first 15 are still inside the window")
	requireStillWaiting(t, done)

	clock.Advance(time.Second) // the first 15 are now a minute old
	require.NoError(t, requireDone(t, done))
}

func TestRequestsUnderTheLimitDoNotWait(t *testing.T) {
	limiter, clock := newLimiter()

	useUp(t, limiter, 30)

	assert.Zero(t, clock.Pending())
}

func TestEstimate(t *testing.T) {
	tests := []struct {
		name string
		n    int
		want time.Duration
	}{
		{"nothing", 0, 0},
		{"one", 1, 0},
		{"exactly the limit", 30, 0},
		{"one over", 31, time.Minute},
		{"two full minutes' worth", 60, time.Minute},
		{"one more than that", 61, 2 * time.Minute},
		{"negative", -5, 0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			limiter, _ := newLimiter()

			assert.Equal(t, tt.want, limiter.Estimate(tt.n))
		})
	}
}

func TestEstimateCountsRequestsAlreadyUsed(t *testing.T) {
	limiter, clock := newLimiter()
	useUp(t, limiter, 10)

	assert.Equal(t, time.Duration(0), limiter.Estimate(20), "20 are still free")
	assert.Equal(t, time.Minute, limiter.Estimate(21))

	useUp(t, limiter, 20)
	clock.Advance(20 * time.Second)

	assert.Equal(t, 40*time.Second, limiter.Estimate(1))
	assert.Equal(t, 100*time.Second, limiter.Estimate(31))
}

func TestEstimateCountsCallersAlreadyWaiting(t *testing.T) {
	limiter, clock := newLimiter()
	useUp(t, limiter, 30)
	var waiters []<-chan error
	for range 30 {
		waiters = append(waiters, startWait(context.Background(), limiter))
	}
	clock.BlockUntil(t, 30)

	assert.Equal(t, 2*time.Minute, limiter.Estimate(1), "30 callers go first, then this one")

	clock.Advance(ratelimit.Window)
	for _, done := range waiters {
		require.NoError(t, requireDone(t, done))
	}
}

func TestEstimateIncludesAPause(t *testing.T) {
	limiter, _ := newLimiter()

	limiter.PauseFor(30 * time.Second)

	assert.Equal(t, 30*time.Second, limiter.Estimate(1))
	assert.Equal(t, 90*time.Second, limiter.Estimate(31))
}

func TestCancellingWaitReturnsTheContextErrorAndKeepsTheBudget(t *testing.T) {
	limiter, clock := newLimiter()
	useUp(t, limiter, 30)
	ctx, cancel := context.WithCancel(context.Background())
	done := startWait(ctx, limiter)
	clock.BlockUntil(t, 1)

	cancel()

	assert.ErrorIs(t, requireDone(t, done), context.Canceled)
	assert.Equal(t, time.Minute, limiter.Estimate(1), "the cancelled caller no longer counts")
	clock.Advance(ratelimit.Window)
	useUp(t, limiter, 30) // none of the 30 had to wait: the cancelled call used no slot
}

func TestWaitWithAnAlreadyCancelledContextFailsAtOnce(t *testing.T) {
	limiter, clock := newLimiter()
	ctx, cancel := context.WithCancel(context.Background())
	cancel()

	err := limiter.Wait(ctx)

	assert.ErrorIs(t, err, context.Canceled)
	assert.Zero(t, clock.Pending())
	useUp(t, limiter, 30)
}

func TestPauseHoldsBackNewCallers(t *testing.T) {
	limiter, clock := newLimiter()
	limiter.PauseFor(10 * time.Second)

	done := startWait(context.Background(), limiter)
	clock.BlockUntil(t, 1)
	clock.Advance(9 * time.Second)

	assert.Equal(t, 1, clock.Pending())
	requireStillWaiting(t, done)
	clock.Advance(time.Second)
	require.NoError(t, requireDone(t, done))
}

func TestPauseHoldsBackEveryCaller(t *testing.T) {
	limiter, clock := newLimiter()
	limiter.PauseFor(10 * time.Second)
	var waiters []<-chan error
	for range 3 {
		waiters = append(waiters, startWait(context.Background(), limiter))
	}
	clock.BlockUntil(t, 3)

	clock.Advance(10 * time.Second)

	for _, done := range waiters {
		require.NoError(t, requireDone(t, done))
	}
}

func TestPauseAlsoHoldsBackCallersAlreadyWaitingForTheWindow(t *testing.T) {
	limiter, clock := newLimiter()
	useUp(t, limiter, 30)
	done := startWait(context.Background(), limiter)
	clock.BlockUntil(t, 1) // would be let through 60 s from the start
	clock.Advance(10 * time.Second)

	limiter.PauseFor(90 * time.Second) // until 100 s from the start
	clock.Advance(50 * time.Second)    // 60 s: the window alone would let it through now

	clock.BlockUntil(t, 1) // it woke up, saw the pause and waits again
	requireStillWaiting(t, done)
	clock.Advance(39 * time.Second)
	assert.Equal(t, 1, clock.Pending())
	requireStillWaiting(t, done)
	clock.Advance(time.Second)
	require.NoError(t, requireDone(t, done))
}

func TestAShorterPauseDoesNotCutALongerOneShort(t *testing.T) {
	limiter, clock := newLimiter()
	limiter.PauseFor(60 * time.Second)
	limiter.PauseFor(10 * time.Second)

	done := startWait(context.Background(), limiter)
	clock.BlockUntil(t, 1)
	clock.Advance(10 * time.Second)

	assert.Equal(t, 1, clock.Pending())
	requireStillWaiting(t, done)
	clock.Advance(50 * time.Second)
	require.NoError(t, requireDone(t, done))
}

func TestPausingForZeroOrLessDoesNothing(t *testing.T) {
	limiter, clock := newLimiter()

	limiter.PauseFor(0)
	limiter.PauseFor(-time.Second)

	useUp(t, limiter, 1)
	assert.Zero(t, clock.Pending())
}

// Many goroutines at once (run with -race): exactly 30 requests go out per minute.
func TestManyCallersNeverExceedTheLimit(t *testing.T) {
	limiter, clock := newLimiter()
	const callers = 90
	granted := make(chan struct{}, callers)
	var wg sync.WaitGroup
	for range callers {
		wg.Go(func() {
			if err := limiter.Wait(context.Background()); err == nil {
				granted <- struct{}{}
			}
		})
	}

	// The clock moves only after every other caller is blocked on it, so each batch of
	// grants below is one window: it must be exactly Limit requests, never more.
	for batch := 1; batch <= callers/ratelimit.Limit; batch++ {
		clock.BlockUntil(t, callers-batch*ratelimit.Limit)
		for range ratelimit.Limit {
			select {
			case <-granted:
			case <-time.After(5 * time.Second):
				t.Fatalf("batch %d: fewer than %d requests went out", batch, ratelimit.Limit)
			}
		}
		require.Empty(t, granted, "batch %d: more than %d requests in one minute", batch, ratelimit.Limit)
		clock.Advance(ratelimit.Window)
	}
	wg.Wait()
}
