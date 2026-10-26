// Package grpcserver builds match-data-service's gRPC server with every service on it.
package grpcserver

import (
	healthv1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/health/v1"
	matchdatav1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/matchdata/v1"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/health"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/matchdata"
	"google.golang.org/grpc"
	grpchealth "google.golang.org/grpc/health"
	grpchealthv1 "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/reflection"
)

// New returns the server with MatchDataService, DependencyHealthService,
// grpc.health.v1 (always SERVING: "am I running?") and reflection registered.
func New(matchData *matchdata.Server, checker *health.Checker) *grpc.Server {
	server := grpc.NewServer()
	matchdatav1.RegisterMatchDataServiceServer(server, matchData)
	healthv1.RegisterDependencyHealthServiceServer(server, health.NewServer(checker))

	liveness := grpchealth.NewServer()
	liveness.SetServingStatus("", grpchealthv1.HealthCheckResponse_SERVING)
	grpchealthv1.RegisterHealthServer(server, liveness)

	reflection.Register(server)
	return server
}
