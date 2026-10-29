// Package migrations applies the `matchdata` schema's SQL migrations (golang-migrate).
// The files in sql/ are embedded in the binary.
package migrations

import (
	"embed"
	"errors"
	"fmt"
	"net/url"

	"github.com/golang-migrate/migrate/v4"
	// Registers the pgx5:// database driver.
	_ "github.com/golang-migrate/migrate/v4/database/pgx/v5"
	"github.com/golang-migrate/migrate/v4/source/iofs"
)

//go:embed sql/*.sql
var files embed.FS

// Up applies every pending migration. connString is a postgres:// URL whose search_path
// is `matchdata`, which is also where the history table (schema_migrations) is kept.
// It returns an error when Postgres can't be reached within a few seconds.
func Up(connString string) (err error) {
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

// migrateURL turns the service's postgres:// URL into the pgx5:// one golang-migrate
// expects, with a short connect timeout so an unreachable Postgres fails fast.
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
