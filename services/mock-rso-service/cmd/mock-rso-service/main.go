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
	players, err := rso.LoadPlayers(cfg.PlayersFile)
	if err != nil {
		return err
	}
	slog.Info("test players loaded", "file", cfg.PlayersFile, "count", len(players))

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
