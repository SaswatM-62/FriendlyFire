// Package health answers "am I running AND can I reach everything I call?"
// (friendlyfire.health.v1.DependencyHealthService).
//
// Every dependency is checked in parallel, each with the same deadline. A failed or
// slow check becomes a DOWN node with a short error; nothing here fails because a
// dependency is unhealthy.
package health

import (
	"context"
	"errors"
	"net"
	"strings"
	"sync"
	"syscall"
	"time"

	healthv1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/health/v1"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/proto"
)

// DefaultDeadline is how long each dependency check may take.
const DefaultDeadline = time.Second

const (
	// replyMargin is the part of the caller's deadline kept for sending the answer back.
	replyMargin    = 200 * time.Millisecond
	minDeadline    = 100 * time.Millisecond
	maxErrorLength = 120
)

// Check is one thing this service depends on: Postgres, an HTTP API or a gRPC peer.
type Check interface {
	// Name is the node's name in the health tree, e.g. "postgres".
	Name() string
	// Check returns the dependency's node (just a status for a leaf, or the tree a
	// gRPC peer reported), or an error if it can't be reached. The Checker fills in
	// the name and latency and turns errors into DOWN nodes.
	Check(ctx context.Context) (*healthv1.ComponentHealth, error)
}

// Checker checks every dependency of one service.
type Checker struct {
	service  string
	checks   []Check
	deadline time.Duration
}

// NewChecker returns a Checker for service with one Check per dependency.
func NewChecker(service string, checks []Check, deadline time.Duration) *Checker {
	return &Checker{service: service, checks: checks, deadline: deadline}
}

// CheckAll returns this service's node, with one child per dependency (in order).
// This service is UP if every dependency is UP, DEGRADED otherwise.
//
// When ctx has a deadline (the caller's), each dependency gets the time left minus a
// margin for the reply, so a slow dependency deep in the tree shows up as DOWN where
// it is instead of every service above it timing out too.
func (c *Checker) CheckAll(ctx context.Context) *healthv1.ComponentHealth {
	deadline := c.deadline
	if callerDeadline, ok := ctx.Deadline(); ok {
		deadline = min(deadline, max(time.Until(callerDeadline)-replyMargin, minDeadline))
	}

	nodes := make([]*healthv1.ComponentHealth, len(c.checks))
	var wg sync.WaitGroup
	for i, check := range c.checks {
		wg.Go(func() { nodes[i] = c.run(ctx, check, deadline) })
	}
	wg.Wait()

	status := healthv1.ComponentStatus_COMPONENT_STATUS_UP
	for _, node := range nodes {
		if node.GetStatus() != healthv1.ComponentStatus_COMPONENT_STATUS_UP {
			status = healthv1.ComponentStatus_COMPONENT_STATUS_DEGRADED
		}
	}
	return &healthv1.ComponentHealth{Name: c.service, Status: status, Dependencies: nodes}
}

func (c *Checker) run(ctx context.Context, check Check, deadline time.Duration) *healthv1.ComponentHealth {
	ctx, cancel := context.WithTimeout(ctx, deadline)
	defer cancel()
	start := time.Now()

	type result struct {
		node *healthv1.ComponentHealth
		err  error
	}
	done := make(chan result, 1) // buffered: a late check can still finish and exit
	go func() {
		node, err := check.Check(ctx)
		done <- result{node, err}
	}()

	select {
	case r := <-done:
		if r.err != nil {
			return down(check.Name(), start, describe(r.err))
		}
		node := proto.CloneOf(r.node)
		node.Name = check.Name()
		node.LatencyMs = millisSince(start)
		return node
	case <-ctx.Done():
		return down(check.Name(), start, "deadline exceeded")
	}
}

// Up is the node for a leaf dependency that answered.
func Up() *healthv1.ComponentHealth {
	return &healthv1.ComponentHealth{Status: healthv1.ComponentStatus_COMPONENT_STATUS_UP}
}

func down(name string, start time.Time, reason string) *healthv1.ComponentHealth {
	return &healthv1.ComponentHealth{
		Name:      name,
		Status:    healthv1.ComponentStatus_COMPONENT_STATUS_DOWN,
		LatencyMs: millisSince(start),
		Error:     reason,
	}
}

// describe turns an error into the short reason shown in the tree.
func describe(err error) string {
	if errors.Is(err, context.DeadlineExceeded) {
		return "deadline exceeded"
	}
	if errors.Is(err, syscall.ECONNREFUSED) {
		return "connection refused"
	}
	var dnsErr *net.DNSError
	if errors.As(err, &dnsErr) {
		return "unknown host"
	}
	if s, ok := status.FromError(err); ok {
		switch {
		case s.Code() == codes.DeadlineExceeded:
			return "deadline exceeded"
		case s.Code() == codes.Unavailable && strings.Contains(s.Message(), "connection refused"):
			return "connection refused"
		default:
			return shorten(strings.ToLower(strings.ReplaceAll(s.Code().String(), "_", " ")) + ": " + s.Message())
		}
	}
	return shorten(err.Error())
}

func millisSince(start time.Time) int32 {
	return int32(time.Since(start).Milliseconds())
}

func shorten(message string) string {
	if len(message) <= maxErrorLength {
		return message
	}
	return message[:maxErrorLength-3] + "..."
}
