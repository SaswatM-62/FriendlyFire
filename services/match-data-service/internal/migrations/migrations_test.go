package migrations

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestMigrateURLUsesPgx5AndAShortConnectTimeout(t *testing.T) {
	got, err := migrateURL("postgres://postgres:secret@localhost:5432/friendlyfire?search_path=matchdata")

	require.NoError(t, err)
	assert.Equal(t,
		"pgx5://postgres:secret@localhost:5432/friendlyfire?connect_timeout=5&search_path=matchdata", got)
}

func TestMigrateURLRejectsAnInvalidURL(t *testing.T) {
	_, err := migrateURL("postgres://host:notaport/db")

	assert.Error(t, err)
}

func TestEmbeddedMigrationsHaveAnUpAndADownFile(t *testing.T) {
	for _, name := range []string{"sql/000001_init.up.sql", "sql/000001_init.down.sql"} {
		_, err := files.ReadFile(name)
		assert.NoError(t, err, name)
	}
}

func TestWaitForPostgresRetriesConnectionErrorsUntilItSucceeds(t *testing.T) {
	calls := 0
	try := func(context.Context) error {
		calls++
		if calls < 3 {
			return errors.New("connection refused")
		}
		return nil
	}

	err := waitForPostgres(context.Background(), time.Minute, time.Millisecond, try)

	require.NoError(t, err)
	assert.Equal(t, 3, calls)
}

func TestWaitForPostgresDoesNotRetryAnErrorFromTheServer(t *testing.T) {
	calls := 0
	try := func(context.Context) error {
		calls++
		return &pgconn.PgError{Code: "28P01", Message: "password authentication failed"}
	}

	err := waitForPostgres(context.Background(), time.Minute, time.Millisecond, try)

	require.Error(t, err)
	assert.Equal(t, 1, calls)
}

func TestWaitForPostgresGivesUpAfterTheWindow(t *testing.T) {
	calls := 0
	try := func(context.Context) error {
		calls++
		return errors.New("connection refused")
	}

	err := waitForPostgres(context.Background(), 20*time.Millisecond, time.Millisecond, try)

	require.ErrorContains(t, err, "connection refused")
	assert.Greater(t, calls, 1)
}

func TestWaitForPostgresStopsWhenTheContextIsCancelled(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	try := func(context.Context) error {
		cancel()
		return errors.New("connection refused")
	}

	err := waitForPostgres(ctx, time.Minute, time.Hour, try)

	require.ErrorIs(t, err, context.Canceled)
}
