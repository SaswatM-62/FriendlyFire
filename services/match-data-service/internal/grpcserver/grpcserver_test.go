package grpcserver_test

import (
	"context"
	"net"
	"testing"
	"time"

	healthv1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/health/v1"
	matchdatav1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/matchdata/v1"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/grpcserver"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/health"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/matchdata"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/testutil"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	grpchealthv1 "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/status"
	"google.golang.org/grpc/test/bufconn"
)

func TestMain(m *testing.M) {
	goleak.VerifyTestMain(m)
}

// connect starts the real server on an in-memory connection (bufconn).
func connect(t *testing.T) *grpc.ClientConn {
	t.Helper()
	listener := bufconn.Listen(1 << 20)
	checker := health.NewChecker("match-data-service", []health.Check{testutil.UpCheck("postgres")}, time.Second)
	server := grpcserver.New(matchdata.NewServer(), checker)
	go func() { _ = server.Serve(listener) }()

	conn, err := grpc.NewClient("passthrough:///bufnet",
		grpc.WithContextDialer(func(ctx context.Context, _ string) (net.Conn, error) {
			return listener.DialContext(ctx)
		}),
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	require.NoError(t, err)
	t.Cleanup(func() {
		_ = conn.Close()
		server.Stop()
	})
	return conn
}

func TestGRPCHealthIsServing(t *testing.T) {
	conn := connect(t)

	response, err := grpchealthv1.NewHealthClient(conn).Check(context.Background(), &grpchealthv1.HealthCheckRequest{})

	require.NoError(t, err)
	assert.Equal(t, grpchealthv1.HealthCheckResponse_SERVING, response.GetStatus())
}

func TestRPCsAreNotImplementedYet(t *testing.T) {
	conn := connect(t)

	_, err := matchdatav1.NewMatchDataServiceClient(conn).GetSyncStatus(context.Background(), &matchdatav1.GetSyncStatusRequest{})

	assert.Equal(t, codes.Unimplemented, status.Code(err))
	assert.Equal(t, "not implemented yet", status.Convert(err).Message())
}

func TestCheckDependenciesOverGRPC(t *testing.T) {
	conn := connect(t)

	response, err := healthv1.NewDependencyHealthServiceClient(conn).CheckDependencies(
		context.Background(), &healthv1.CheckDependenciesRequest{})

	require.NoError(t, err)
	assert.Equal(t, "match-data-service", response.GetComponent().GetName())
	assert.Equal(t, healthv1.ComponentStatus_COMPONENT_STATUS_UP, response.GetComponent().GetStatus())
}
