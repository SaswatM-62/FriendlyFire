// Package testutil has fakes for tests. Prefer these over mocking libraries for our
// own interfaces.
package testutil

import (
	"context"
	"time"

	healthv1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/health/v1"
)

// FakeCheck is a health.Check with a fixed outcome.
type FakeCheck struct {
	name    string
	outcome func(ctx context.Context) (*healthv1.ComponentHealth, error)
	// TimeLeft is how long the last check was given (until its context's deadline).
	TimeLeft time.Duration
}

// Name implements health.Check.
func (f *FakeCheck) Name() string { return f.name }

// Check implements health.Check.
func (f *FakeCheck) Check(ctx context.Context) (*healthv1.ComponentHealth, error) {
	if deadline, ok := ctx.Deadline(); ok {
		f.TimeLeft = time.Until(deadline)
	}
	return f.outcome(ctx)
}

// UpCheck always answers UP.
func UpCheck(name string) *FakeCheck {
	return &FakeCheck{name: name, outcome: func(context.Context) (*healthv1.ComponentHealth, error) {
		return &healthv1.ComponentHealth{Status: healthv1.ComponentStatus_COMPONENT_STATUS_UP}, nil
	}}
}

// FailingCheck always fails with err.
func FailingCheck(name string, err error) *FakeCheck {
	return &FakeCheck{name: name, outcome: func(context.Context) (*healthv1.ComponentHealth, error) {
		return nil, err
	}}
}

// NeverAnswersCheck blocks until its context ends.
func NeverAnswersCheck(name string) *FakeCheck {
	return &FakeCheck{name: name, outcome: func(ctx context.Context) (*healthv1.ComponentHealth, error) {
		<-ctx.Done()
		return nil, ctx.Err()
	}}
}
