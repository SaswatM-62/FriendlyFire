#!/bin/sh
# Runs once, when the pgdata volume is created (`make clean` deletes it).
# Creates the database and one schema per service that owns tables. Each service
# creates its own tables later (no migrations yet).
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<'SQL'
SELECT 'CREATE DATABASE friendlyfire'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'friendlyfire')\gexec
SQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname friendlyfire <<'SQL'
CREATE SCHEMA IF NOT EXISTS account;
CREATE SCHEMA IF NOT EXISTS matchdata;
CREATE SCHEMA IF NOT EXISTS scoring;
SQL
