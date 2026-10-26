# FriendlyFire developer commands. Run `make` to list them.
# Works with the GNU Make 3.81 that ships with macOS and with make on WSL/Linux.
# Everything runs in Docker, so no local toolchains are needed.

SHELL := /bin/bash
.DEFAULT_GOAL := help

COMPOSE := docker compose

# Optional list of services, e.g. `make up s="gateway-service room-service"`.
s ?=

JAVA_SERVICES   := gateway-service account-service room-service
PYTHON_SERVICES := quiz-engine-service scoring-service
GO_SERVICES     := match-data-service mock-rso-service
APP_SERVICES    := $(JAVA_SERVICES) $(PYTHON_SERVICES) $(GO_SERVICES) frontend
# Services that have integration tests (Testcontainers, WireMock).
INTEGRATION_SERVICES := account-service scoring-service match-data-service
# Services that have linters or type checkers (Java has none yet).
LINT_SERVICES := $(PYTHON_SERVICES) $(GO_SERVICES) frontend

# Versions are pinned in one place: Buf in mise.toml, Playwright in e2e/package.json.
BUF_VERSION        := $(shell sed -n 's/^buf *= *"\(.*\)"/\1/p' mise.toml)
PLAYWRIGHT_VERSION := $(shell sed -n 's/.*"@playwright\/test": *"\([0-9.]*\)".*/\1/p' e2e/package.json 2>/dev/null)

# Buf runs as you (not root) so the files it writes under gen/ belong to you.
BUF := docker run --rm --user "$$(id -u):$$(id -g)" -e BUF_CACHE_DIR=/tmp/buf \
	-v "$(CURDIR):/workspace" -w /workspace bufbuild/buf:$(BUF_VERSION)

# The language of a service: $(call lang,account-service) -> java
lang = $(if $(filter $(1),$(JAVA_SERVICES)),java,$(if $(filter $(1),$(PYTHON_SERVICES)),python,$(if $(filter $(1),$(GO_SERVICES)),go,node)))

# What each target runs inside a service's dev container, per language.
TEST_java     := ./mvnw -B -ntp test
TEST_python   := uv run pytest
TEST_go       := go test -race ./...
TEST_node     := npm test
IT_java       := ./mvnw -B -ntp verify
IT_python     := uv run pytest -m integration || [ $$? -eq 5 ]  # 5: no integration tests yet
IT_go         := go test -race -tags integration ./...
LINT_python   := uv run ruff check && uv run ruff format --check && uv run mypy src
LINT_go       := go vet ./... && golangci-lint run
LINT_node     := npm run lint && npm run typecheck
COVERAGE_java   := ./mvnw -B -ntp test
COVERAGE_python := uv run pytest --cov --cov-report=term --cov-report=html
COVERAGE_go     := go test -race -coverprofile=coverage.out ./... && go tool cover -html=coverage.out -o coverage.html
COVERAGE_node   := npm run coverage

# Integration tests start containers through the host's Docker (Testcontainers).
IT_FLAGS := -v /var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal

# $(call in_dev,<service>,<command>,<extra docker compose run flags>)
in_dev = echo "==> $(1): $(2)" && $(COMPOSE) run --rm --no-deps $(3) $(1) sh -c '$(2)'
# $(call each,<services>,<command variable prefix>,<extra flags>) runs it in every service.
each = $(foreach svc,$(1),$(call in_dev,$(svc),$($(2)_$(call lang,$(svc))),$(3)) && ) true

# Docker creates missing mount points as root (Linux/WSL). Creating the ones inside the
# repo first keeps them yours, so a local `npm install` still works.
MOUNT_POINTS := frontend/node_modules e2e/node_modules

.PHONY: help proto build up down ps logs test test-integration e2e lint coverage clean mount-points

help: ## List the targets
	@echo 'Usage: make <target> [s="service ..."]'
	@echo
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z0-9_-]+:.*## / {printf "  %-18s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

proto: ## Lint the .proto files and regenerate gen/ (runs Buf in Docker)
	$(BUF) lint proto
	rm -rf gen/java/com gen/python/friendlyfire gen/go/friendlyfire
	$(BUF) generate
	@# protoc writes no __init__.py files; Python needs them to import gen/python.
	@# py.typed lets mypy use the generated .pyi stubs.
	find gen/python/friendlyfire -type d | while read -r dir; do touch "$$dir/__init__.py"; done
	touch gen/python/friendlyfire/py.typed

build: ## Build every dev image (docker compose build)
	$(COMPOSE) build

up: mount-points ## Start everything, or only s="a b" plus what they depend on
	$(COMPOSE) up -d $(s)

down: ## Stop everything
	$(COMPOSE) down

ps: ## Container status
	$(COMPOSE) ps

logs: ## Follow logs: s=<svc> for one service
	$(COMPOSE) logs -f $(s)

test: mount-points ## Unit tests in each service's container (s=<svc> for one)
	@$(call each,$(or $(s),$(APP_SERVICES)),TEST)

test-integration: mount-points ## Integration tests (Testcontainers, WireMock) with the Docker socket mounted
	@$(call each,$(or $(s),$(INTEGRATION_SERVICES)),IT,$(IT_FLAGS))

e2e: mount-points ## Playwright smoke tests against the running stack (run `make up` first)
	@curl -fsS -o /dev/null http://localhost:3000 || { echo "The stack isn't up: run 'make up' and wait until 'make ps' shows healthy."; exit 1; }
	@# Joins the stack's network and forwards localhost:3000/8080 to it (e2e/forward-ports.ts).
	docker run --rm --init --ipc=host --user "$$(id -u):$$(id -g)" -e HOME=/tmp -e CI \
		--network friendlyfire_default \
		-e E2E_FORWARD="3000=frontend:3000 8080=gateway-service:8080" \
		-v "$(CURDIR)/e2e:/work" --tmpfs /work/node_modules:rw,exec,mode=1777 -w /work \
		mcr.microsoft.com/playwright:v$(PLAYWRIGHT_VERSION)-noble \
		sh -c 'npm ci --no-audit --no-fund && npx playwright test'

lint: mount-points ## Every linter and type checker (buf, ruff, mypy, go vet, golangci-lint, ESLint, tsc)
	$(BUF) lint proto
	@$(call each,$(or $(s),$(LINT_SERVICES)),LINT)
	./scripts/check-versions.sh

coverage: mount-points ## Unit tests with coverage reports (s=<svc> for one)
	@$(call each,$(or $(s),$(APP_SERVICES)),COVERAGE)

clean: ## Stop everything and delete the volumes (database, caches)
	$(COMPOSE) down -v

mount-points:
	@mkdir -p $(MOUNT_POINTS)
