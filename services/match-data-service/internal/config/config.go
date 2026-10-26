// Package config reads match-data-service's settings from environment variables
// (see .env.example at the repo root).
package config

import (
	"fmt"
	"net/url"
	"strconv"
)

// Config is everything match-data-service reads from the environment.
type Config struct {
	GRPCPort        int
	Postgres        Postgres
	HenrikDevAPIKey string
}

// Postgres is where the `matchdata` schema lives.
type Postgres struct {
	Host     string
	Port     int
	DB       string
	User     string
	Password string
}

// ConnString is a pgx connection string that uses the `matchdata` schema.
func (p Postgres) ConnString() string {
	u := url.URL{
		Scheme:   "postgres",
		User:     url.UserPassword(p.User, p.Password),
		Host:     fmt.Sprintf("%s:%d", p.Host, p.Port),
		Path:     "/" + p.DB,
		RawQuery: "search_path=matchdata",
	}
	return u.String()
}

// Load reads the config; getenv is os.Getenv outside tests.
func Load(getenv func(string) string) (Config, error) {
	grpcPort, err := intOr(getenv, "GRPC_PORT", 9095)
	if err != nil {
		return Config{}, err
	}
	postgresPort, err := intOr(getenv, "POSTGRES_PORT", 5432)
	if err != nil {
		return Config{}, err
	}
	return Config{
		GRPCPort: grpcPort,
		Postgres: Postgres{
			Host:     stringOr(getenv, "POSTGRES_HOST", "localhost"),
			Port:     postgresPort,
			DB:       stringOr(getenv, "POSTGRES_DB", "friendlyfire"),
			User:     stringOr(getenv, "POSTGRES_USER", "postgres"),
			Password: getenv("POSTGRES_PASSWORD"),
		},
		HenrikDevAPIKey: getenv("HENRIKDEV_API_KEY"),
	}, nil
}

func stringOr(getenv func(string) string, key, fallback string) string {
	if value := getenv(key); value != "" {
		return value
	}
	return fallback
}

func intOr(getenv func(string) string, key string, fallback int) (int, error) {
	value := getenv(key)
	if value == "" {
		return fallback, nil
	}
	n, err := strconv.Atoi(value)
	if err != nil {
		return 0, fmt.Errorf("%s must be a number, got %q", key, value)
	}
	return n, nil
}
