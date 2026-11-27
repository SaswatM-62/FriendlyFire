// Command mock-rso-service stands in for Riot Sign-On until Riot approves the app.
package main

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/config"
	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/lookup"
	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/rso"
)

const serviceName = "mock-rso-service"

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
	players, err := loadPlayers(cfg)
	if err != nil {
		return err
	}
	if missing := rso.WithoutEmail(players); missing > 0 {
		slog.Warn("some test players have no email, so nobody can link them", "count", missing)
	}

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGTERM, os.Interrupt)
	defer stop()

	server := &http.Server{
		Addr:              fmt.Sprintf(":%d", cfg.HTTPPort),
		Handler:           rso.NewHandler(players, cfg.RedirectURIs, time.Now),
		ReadHeaderTimeout: 5 * time.Second,
	}
	go func() {
		<-ctx.Done()
		slog.Info("shutting down")
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = server.Shutdown(shutdownCtx)
	}()

	slog.Info("listening", "port", cfg.HTTPPort)
	if err := server.ListenAndServe(); !errors.Is(err, http.ErrServerClosed) {
		return err
	}
	return nil
}

// loadPlayers reads the test players, looking up missing PUUIDs through HenrikDev when
// HENRIKDEV_API_KEY is set. A player whose lookup fails is left out and logged.
func loadPlayers(cfg config.Config) ([]rso.Player, error) {
	var accounts rso.AccountLookup
	if cfg.HenrikDevAPIKey != "" {
		accounts = lookup.NewHenrikDev(cfg.HenrikDevBaseURL, cfg.HenrikDevAPIKey, &http.Client{Timeout: 10 * time.Second})
	}
	ctx, cancel := context.WithTimeout(context.Background(), time.Minute)
	defer cancel()
	loaded, err := rso.LoadPlayers(ctx, cfg.PlayersFile, accounts)
	if err != nil {
		return nil, err
	}
	for _, left := range loaded.LeftOut {
		slog.Warn("test player left out: couldn't look up their puuid", "player", left.Position, "riot_id", left.RiotID, "reason", left.Reason)
	}
	slog.Info("test players loaded", "file", cfg.PlayersFile, "count", len(loaded.Players), "puuids_looked_up", loaded.LookedUp)
	return loaded.Players, nil
}
