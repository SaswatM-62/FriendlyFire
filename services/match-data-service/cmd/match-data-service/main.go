// Command match-data-service fetches players' matches, stores them and hands out
// game-neutral stats over gRPC.
package main

import (
	"context"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/config"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/grpcserver"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/health"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/lookup"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/matchdata"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/migrations"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/ratelimit"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/source"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/store"
	matchsync "github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/sync"
	"github.com/jackc/pgx/v5/pgxpool"
)

const serviceName = "match-data-service"

func main() {
	slog.SetDefault(slog.New(slog.NewTextHandler(os.Stdout, nil)).With("service", serviceName))
	if err := run(); err != nil {
		slog.Error("stopped", "err", err)
		os.Exit(1)
	}
}

func run() error {
	cfg, err := config.Load(os.Getenv)
	if err != nil {
		return err
	}
	slog.Info("match source", "source", cfg.MatchSource)
	if cfg.MatchSource == config.SourceHenrikDev && cfg.HenrikDevAPIKey == "" {
		slog.Warn("MATCH_SOURCE=henrikdev but HENRIKDEV_API_KEY is empty: syncs will fail")
	}
	tables, err := lookup.Load()
	if err != nil {
		return err
	}

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGTERM, os.Interrupt)
	defer stop()

	// Waits up to a minute for Postgres, then fails to start. In `make up` (air) a failed
	// start leaves the container running, so this wait is what rides out a short outage.
	if err := migrations.Up(ctx, cfg.Postgres.ConnString()); err != nil {
		return fmt.Errorf("migrate: %w", err)
	}

	// Once running, the pool reconnects by itself after a Postgres outage, and the
	// health check reports DOWN meanwhile.
	poolConfig, err := pgxpool.ParseConfig(cfg.Postgres.ConnString())
	if err != nil {
		return fmt.Errorf("postgres config: %w", err)
	}
	pool, err := pgxpool.NewWithConfig(ctx, poolConfig)
	if err != nil {
		return fmt.Errorf("postgres pool: %w", err)
	}
	defer pool.Close()
	// pgx connects lazily; connect now so a bad address or password stops the start.
	if err := pool.Ping(ctx); err != nil {
		return fmt.Errorf("postgres ping: %w", err)
	}

	matchStore := store.New(pool)
	src, estimate, sourceCheck, err := newSource(cfg)
	if err != nil {
		return err
	}
	worker := matchsync.New(src, matchStore, tables, estimate, time.Now, slog.Default())
	if err := worker.RecoverInterrupted(ctx); err != nil {
		return fmt.Errorf("recover interrupted syncs: %w", err)
	}
	workerDone := make(chan struct{})
	go func() {
		defer close(workerDone)
		_ = worker.Run(ctx)
	}()
	defer func() { <-workerDone }()

	// What /api/health/deep checks for this service. When it starts calling something
	// new, add it here, using the same client the feature uses.
	checker := health.NewChecker(serviceName, []health.Check{
		health.NewPostgresCheck(pool),
		sourceCheck,
	}, health.DefaultDeadline)
	server := grpcserver.New(matchdata.NewServer(matchStore, worker, slog.Default()), checker)

	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", cfg.GRPCPort))
	if err != nil {
		return err
	}
	go func() {
		<-ctx.Done()
		slog.Info("shutting down")
		stopped := make(chan struct{})
		go func() {
			server.GracefulStop()
			close(stopped)
		}()
		select {
		case <-stopped:
		case <-time.After(5 * time.Second):
			server.Stop()
		}
	}()

	slog.Info("listening", "port", cfg.GRPCPort)
	return server.Serve(listener)
}

// newSource picks the match source (MATCH_SOURCE), how long a number of its requests
// takes, and its health check.
func newSource(cfg config.Config) (source.MatchSource, matchsync.Estimator, health.Check, error) {
	if cfg.MatchSource == config.SourceFixtures {
		fixtures, err := source.NewFixtures(time.Now)
		if err != nil {
			return nil, nil, nil, err
		}
		// No rate limit: a fixtures sync takes a moment.
		estimate := func(requests int) time.Duration { return time.Duration(requests) * 200 * time.Millisecond }
		return fixtures, estimate, health.NewSourceCheck("fixtures", nil), nil
	}
	clock := ratelimit.SystemClock{}
	limiter := ratelimit.New(clock)
	henrikDev := source.NewHenrikDev(&http.Client{Timeout: 20 * time.Second}, cfg.HenrikDevBaseURL,
		cfg.HenrikDevAPIKey, limiter, clock)
	return henrikDev, limiter.Estimate, health.NewSourceCheck("henrikdev", henrikDev.Health), nil
}
