// Command match-data-service fetches players' matches, stores them and hands out
// game-neutral stats over gRPC.
package main

import (
	"context"
	"fmt"
	"log/slog"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/config"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/grpcserver"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/health"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/matchdata"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/migrations"
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
	if cfg.HenrikDevAPIKey == "" {
		slog.Warn("HENRIKDEV_API_KEY is empty: match syncs won't work, everything else will")
	}

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGTERM, os.Interrupt)
	defer stop()

	// Fails to start if Postgres is unreachable; Compose restarts the service until it is up.
	if err := migrations.Up(cfg.Postgres.ConnString()); err != nil {
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

	// What /api/health/deep checks for this service. When it starts calling something
	// new, add it here, using the same client the feature uses.
	checker := health.NewChecker(serviceName, []health.Check{
		health.NewPostgresCheck(pool),
	}, health.DefaultDeadline)
	server := grpcserver.New(matchdata.NewServer(), checker)

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
