package testutil_test

import (
	"testing"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/testutil"
	"github.com/stretchr/testify/assert"
)

var start = time.Date(2026, 11, 2, 12, 0, 0, 0, time.UTC)

func fired(ch <-chan time.Time) bool {
	select {
	case <-ch:
		return true
	default:
		return false
	}
}

func TestFakeClockStandsStillUntilAdvanced(t *testing.T) {
	clock := testutil.NewFakeClock(start)

	assert.Equal(t, start, clock.Now())
	clock.Advance(time.Minute)
	assert.Equal(t, start.Add(time.Minute), clock.Now())
}

func TestFakeClockFiresTimersOnlyOnceTheyAreDue(t *testing.T) {
	clock := testutil.NewFakeClock(start)
	soon, later := clock.After(10*time.Second), clock.After(20*time.Second)
	assert.Equal(t, 2, clock.Pending())

	clock.Advance(15 * time.Second)

	assert.True(t, fired(soon))
	assert.False(t, fired(later))
	assert.Equal(t, 1, clock.Pending())

	clock.Advance(5 * time.Second)
	assert.True(t, fired(later))
	assert.Zero(t, clock.Pending())
}

func TestFakeClockFiresAZeroDurationTimerAtOnce(t *testing.T) {
	clock := testutil.NewFakeClock(start)

	assert.True(t, fired(clock.After(0)))
	assert.Zero(t, clock.Pending())
}

func TestFakeClockBlockUntilReturnsOnceEnoughTimersArePending(t *testing.T) {
	clock := testutil.NewFakeClock(start)
	go clock.After(time.Second)

	clock.BlockUntil(t, 1)

	assert.Equal(t, 1, clock.Pending())
}
