// Package ratelimit keeps match-data-service under HenrikDev's limit of 30 requests
// per minute, shared by every sync.
//
// Call Limiter.Wait before each request. When the API answers 429 with a Retry-After
// header, pass the header to RetryAfter and the result to Limiter.PauseFor: every caller
// then waits. Limiter.Estimate says how long some number of requests will take.
package ratelimit

import (
	"context"
	"slices"
	"sync"
	"time"
)

const (
	// Limit is how many requests may go out in any Window.
	Limit = 30
	// Window is the length of the rolling period Limit applies to.
	Window = time.Minute
)

// Clock is the time source. Tests pass a fake one so they don't sleep.
type Clock interface {
	Now() time.Time
	// After returns a channel that receives the time once d has passed.
	After(d time.Duration) <-chan time.Time
}

// SystemClock is the real clock.
type SystemClock struct{}

// Now implements Clock.
func (SystemClock) Now() time.Time { return time.Now() }

// After implements Clock.
func (SystemClock) After(d time.Duration) <-chan time.Time { return time.After(d) }

// Limiter lets at most Limit requests through in any rolling Window. It is safe for
// concurrent use; one Limiter is shared by everything that calls HenrikDev.
type Limiter struct {
	clock Clock

	mu sync.Mutex
	// granted is when recent requests were let through, oldest first.
	granted     []time.Time
	pausedUntil time.Time
	// waiting is how many callers are blocked in Wait.
	waiting int
}

// New returns a Limiter for 30 requests per minute.
func New(clock Clock) *Limiter {
	return &Limiter{clock: clock}
}

// Wait blocks until a request may go out, and counts it. It returns ctx's error, without
// using up a request, if ctx is done first.
func (l *Limiter) Wait(ctx context.Context) error {
	joined := false
	for {
		if err := ctx.Err(); err != nil {
			l.leave(joined)
			return err
		}
		delay := l.acquire(&joined)
		if delay == 0 {
			return nil
		}
		select {
		case <-ctx.Done():
			l.leave(joined)
			return ctx.Err()
		case <-l.clock.After(delay):
			// Check again: another caller may have taken the slot, or a pause started.
		}
	}
}

// acquire counts a request now and returns 0, or returns how long to wait before
// trying again. joined tracks whether this caller is counted in l.waiting.
func (l *Limiter) acquire(joined *bool) time.Duration {
	l.mu.Lock()
	defer l.mu.Unlock()

	now := l.clock.Now()
	delay := nextSlot(l.granted, later(now, l.pausedUntil)).Sub(now)
	if delay <= 0 {
		l.granted = append(l.dropExpired(now), now)
		if *joined {
			l.waiting--
			*joined = false
		}
		return 0
	}
	if !*joined {
		l.waiting++
		*joined = true
	}
	return delay
}

// leave takes a caller that gave up out of the waiting count.
func (l *Limiter) leave(joined bool) {
	if !joined {
		return
	}
	l.mu.Lock()
	defer l.mu.Unlock()
	l.waiting--
}

// dropExpired forgets requests that are more than a Window old; they no longer count.
func (l *Limiter) dropExpired(now time.Time) []time.Time {
	return slices.DeleteFunc(l.granted, func(t time.Time) bool {
		return !t.Add(Window).After(now)
	})
}

// Estimate is how long n more requests will take to all go out: 0 when they fit in
// the budget left. It counts the requests already used, a pause in effect, and the
// callers already waiting (it assumes they go first), so it is pessimistic.
func (l *Limiter) Estimate(n int) time.Duration {
	if n <= 0 {
		return 0
	}
	l.mu.Lock()
	defer l.mu.Unlock()

	now := l.clock.Now()
	history := slices.Clone(l.granted)
	at := later(now, l.pausedUntil)
	for range l.waiting + n {
		at = nextSlot(history, at)
		history = append(history, at)
	}
	return at.Sub(now)
}

// PauseFor holds back every caller, those already waiting included, for d from now.
// A shorter pause never cuts a longer one short.
func (l *Limiter) PauseFor(d time.Duration) {
	if d <= 0 {
		return
	}
	l.mu.Lock()
	defer l.mu.Unlock()
	l.pausedUntil = later(l.pausedUntil, l.clock.Now().Add(d))
}

// nextSlot is the earliest time, not before notBefore, that one more request fits after
// the ones in history (oldest first): the Limit-th most recent must be a Window old.
func nextSlot(history []time.Time, notBefore time.Time) time.Time {
	if len(history) >= Limit {
		if free := history[len(history)-Limit].Add(Window); free.After(notBefore) {
			return free
		}
	}
	return notBefore
}

func later(a, b time.Time) time.Time {
	if b.After(a) {
		return b
	}
	return a
}
