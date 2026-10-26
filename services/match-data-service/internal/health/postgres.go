package health

import (
	"context"

	healthv1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/health/v1"
)

// Pinger is the part of *pgxpool.Pool the Postgres check uses.
type Pinger interface {
	Ping(ctx context.Context) error
}

// PostgresCheck borrows a connection from the pool and pings the database.
type PostgresCheck struct {
	pool Pinger
}

// NewPostgresCheck checks the pool the features use.
func NewPostgresCheck(pool Pinger) *PostgresCheck {
	return &PostgresCheck{pool: pool}
}

// Name is "postgres".
func (p *PostgresCheck) Name() string { return "postgres" }

// Check pings Postgres through the pool.
func (p *PostgresCheck) Check(ctx context.Context) (*healthv1.ComponentHealth, error) {
	if err := p.pool.Ping(ctx); err != nil {
		return nil, err
	}
	return Up(), nil
}
