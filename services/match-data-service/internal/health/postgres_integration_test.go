//go:build integration

package health_test

import (
	"context"
	"testing"
	"time"

	healthv1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/health/v1"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/health"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

// Runs against a real Postgres in Docker (Testcontainers).
func TestPostgresCheckReportsUpAgainstARealDatabase(t *testing.T) {
	ctx := context.Background()
	// Same image as compose.yaml.
	container, err := postgres.Run(ctx, "postgres:16.15-alpine",
		postgres.WithDatabase("friendlyfire"),
		postgres.BasicWaitStrategies(),
	)
	testcontainers.CleanupContainer(t, container)
	require.NoError(t, err)

	connString, err := container.ConnectionString(ctx, "sslmode=disable", "search_path=matchdata")
	require.NoError(t, err)
	pool, err := pgxpool.New(ctx, connString)
	require.NoError(t, err)
	defer pool.Close()

	checker := health.NewChecker("match-data-service", []health.Check{health.NewPostgresCheck(pool)}, time.Second)
	checker.CheckAll(ctx) // warm up: on a slow CI runner the first call can miss the deadline
	node := checker.CheckAll(ctx)

	assert.Equal(t, healthv1.ComponentStatus_COMPONENT_STATUS_UP, node.GetDependencies()[0].GetStatus())
}
