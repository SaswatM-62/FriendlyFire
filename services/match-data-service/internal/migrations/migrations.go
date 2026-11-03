// Package migrations applies the `matchdata` schema's SQL migrations (golang-migrate).
// The files in sql/ are embedded in the binary.
package migrations

import (
	"context"
	"embed"
	"errors"
	"fmt"
	"log/slog"
	"net/url"
	"time"

	"github.com/golang-migrate/migrate/v4"
	// Registers the pgx5:// database driver.
	_ "github.com/golang-migrate/migrate/v4/database/pgx/v5"
	"github.com/golang-migrate/migrate/v4/source/iofs"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
)

const (
	// How long Up keeps trying to reach Postgres before the service gives up and exits.
	waitWindow   = 60 * time.Second
	waitInterval = 2 * time.Second
	// How long one attempt to reach Postgres may take.
	attemptTimeout = 5 * time.Second
)

//go:embed sql/*.sql
var files embed.FS

// Up applies every pending migration. connString is a postgres:// URL whose search_path
// is `matchdata`, which is also where the history table (schema_migrations) is kept.
// It waits up to a minute for Postgres to accept connections (it may still be starting),
// and returns an error after that, or when ctx is cancelled.
func Up(ctx context.Context, connString string) (err error) {
	if err := waitForPostgres(ctx, waitWindow, waitInterval, func(ctx context.Context) error {
		return connect(ctx, connString)
	}); err != nil {
		return err
	}
	databaseURL, err := migrateURL(connString)
	if err != nil {
		return err
	}
	source, err := iofs.New(files, "sql")
	if err != nil {
		return fmt.Errorf("migration files: %w", err)
	}
	m, err := migrate.NewWithSourceInstance("iofs", source, databaseURL)
	if err != nil {
		return fmt.Errorf("connect: %w", err)
	}
	defer func() {
		sourceErr, databaseErr := m.Close()
		err = errors.Join(err, sourceErr, databaseErr)
	}()

	if err := m.Up(); err != nil && !errors.Is(err, migrate.ErrNoChange) {
		return fmt.Errorf("apply: %w", err)
	}
	return nil
}

// connect opens one connection and closes it again.
func connect(ctx context.Context, connString string) error {
	ctx, cancel := context.WithTimeout(ctx, attemptTimeout)
	defer cancel()
	conn, err := pgx.Connect(ctx, connString)
	if err != nil {
		return err
	}
	return conn.Close(ctx)
}

// waitForPostgres calls try until it succeeds. It retries only when Postgres couldn't be
// reached (connection refused, unknown host, timeout). An error from the server itself
// (wrong password, missing database) won't fix itself, so it is returned at once.
func waitForPostgres(ctx context.Context, window, interval time.Duration, try func(context.Context) error) error {
	deadline := time.Now().Add(window)
	for {
		err := try(ctx)
		if err == nil {
			return nil
		}
		if ctx.Err() != nil {
			return fmt.Errorf("postgres: %w", ctx.Err())
		}
		var serverErr *pgconn.PgError
		if errors.As(err, &serverErr) || time.Now().Add(interval).After(deadline) {
			return fmt.Errorf("postgres: %w", err)
		}
		slog.Warn("waiting for postgres", "err", err)
		select {
		case <-ctx.Done():
			return fmt.Errorf("postgres: %w", ctx.Err())
		case <-time.After(interval):
		}
	}
}

// migrateURL turns the service's postgres:// URL into the pgx5:// one golang-migrate
// expects, with a short connect timeout.
func migrateURL(connString string) (string, error) {
	u, err := url.Parse(connString)
	if err != nil {
		return "", fmt.Errorf("postgres URL: %w", err)
	}
	u.Scheme = "pgx5"
	query := u.Query()
	query.Set("connect_timeout", "5")
	u.RawQuery = query.Encode()
	return u.String(), nil
}
