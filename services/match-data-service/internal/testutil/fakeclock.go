package testutil

import (
	"sync"
	"testing"
	"time"
)

// FakeClock is a clock that only moves when a test calls Advance, so tests never sleep.
// It implements ratelimit.Clock.
type FakeClock struct {
	mu     sync.Mutex
	now    time.Time
	timers []fakeTimer
	// changed is closed, and replaced, each time a timer is added.
	changed chan struct{}
}

type fakeTimer struct {
	at time.Time
	ch chan time.Time
}

// NewFakeClock starts the clock at start.
func NewFakeClock(start time.Time) *FakeClock {
	return &FakeClock{now: start, changed: make(chan struct{})}
}

// Now is the fake time.
func (c *FakeClock) Now() time.Time {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.now
}

// After returns a channel that receives the time once the clock has been advanced by d.
func (c *FakeClock) After(d time.Duration) <-chan time.Time {
	c.mu.Lock()
	defer c.mu.Unlock()

	ch := make(chan time.Time, 1)
	if d <= 0 {
		ch <- c.now
		return ch
	}
	c.timers = append(c.timers, fakeTimer{at: c.now.Add(d), ch: ch})
	close(c.changed)
	c.changed = make(chan struct{})
	return ch
}

// Advance moves the clock forward by d and fires every timer that is now due.
func (c *FakeClock) Advance(d time.Duration) {
	c.mu.Lock()
	defer c.mu.Unlock()

	c.now = c.now.Add(d)
	pending := c.timers[:0]
	for _, timer := range c.timers {
		if timer.at.After(c.now) {
			pending = append(pending, timer)
			continue
		}
		timer.ch <- c.now
	}
	c.timers = pending
}

// Pending is how many timers have not fired yet, which is how many callers are waiting
// on the clock.
func (c *FakeClock) Pending() int {
	c.mu.Lock()
	defer c.mu.Unlock()
	return len(c.timers)
}

// BlockUntil returns once at least n timers are pending. Tests call it after starting a
// goroutine that waits, so they advance the clock only once it is really waiting.
func (c *FakeClock) BlockUntil(t testing.TB, n int) {
	t.Helper()
	// Only reached when the test is failing: the wait normally ends at once.
	giveUp := time.After(5 * time.Second)
	for {
		c.mu.Lock()
		enough := len(c.timers) >= n
		changed := c.changed
		c.mu.Unlock()
		if enough {
			return
		}
		select {
		case <-changed:
		case <-giveUp:
			t.Fatalf("fake clock: %d timers pending, wanted %d", c.Pending(), n)
		}
	}
}
