//go:build integration

package migrations_test

import (
	"context"
	"testing"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/migrations"
	"github.com/jackc/pgx/v5"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

// Runs the migrations against a fresh Postgres in Docker (Testcontainers).
func startPostgres(t *testing.T) (connString string, conn *pgx.Conn) {
	t.Helper()
	ctx := context.Background()
	// Same image as compose.yaml.
	container, err := postgres.Run(ctx, "postgres:16.15-alpine",
		postgres.WithDatabase("friendlyfire"),
		postgres.BasicWaitStrategies(),
	)
	testcontainers.CleanupContainer(t, container)
	require.NoError(t, err)

	connString, err = container.ConnectionString(ctx, "sslmode=disable", "search_path=matchdata")
	require.NoError(t, err)
	conn, err = pgx.Connect(ctx, connString)
	require.NoError(t, err)
	t.Cleanup(func() { _ = conn.Close(ctx) })
	// infra/local/postgres/init creates the schemas in Compose; a bare container has none.
	_, err = conn.Exec(ctx, "CREATE SCHEMA matchdata")
	require.NoError(t, err)
	return connString, conn
}

func appliedVersion(t *testing.T, conn *pgx.Conn) (version int, dirty bool) {
	t.Helper()
	err := conn.QueryRow(context.Background(),
		"SELECT version, dirty FROM matchdata.schema_migrations").Scan(&version, &dirty)
	require.NoError(t, err)
	return version, dirty
}

func TestUpAppliesTheFirstMigrationToTheMatchdataSchema(t *testing.T) {
	connString, conn := startPostgres(t)

	require.NoError(t, migrations.Up(connString))

	version, dirty := appliedVersion(t, conn)
	assert.Equal(t, 1, version)
	assert.False(t, dirty)
}

func TestUpAgainChangesNothing(t *testing.T) {
	connString, conn := startPostgres(t)
	require.NoError(t, migrations.Up(connString))

	require.NoError(t, migrations.Up(connString))

	version, dirty := appliedVersion(t, conn)
	assert.Equal(t, 1, version)
	assert.False(t, dirty)
}
