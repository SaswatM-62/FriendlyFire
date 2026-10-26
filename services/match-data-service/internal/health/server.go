package health

import (
	"context"

	healthv1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/health/v1"
)

// Server is friendlyfire.health.v1.DependencyHealthService.
type Server struct {
	healthv1.UnimplementedDependencyHealthServiceServer
	checker *Checker
}

// NewServer serves the checker's results.
func NewServer(checker *Checker) *Server {
	return &Server{checker: checker}
}

// CheckDependencies checks every dependency. It never returns an error for an
// unhealthy dependency; the tree says what's wrong.
func (s *Server) CheckDependencies(
	ctx context.Context, _ *healthv1.CheckDependenciesRequest,
) (*healthv1.CheckDependenciesResponse, error) {
	return &healthv1.CheckDependenciesResponse{Component: s.checker.CheckAll(ctx)}, nil
}
