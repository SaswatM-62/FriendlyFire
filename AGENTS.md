# AGENTS.md

Instructions for AI coding agents working in this repo (Claude Code reads this file
through `CLAUDE.md`, which only imports it).

FriendlyFire: trivia built from friends' VALORANT matches. Polyglot microservices that
talk gRPC, run locally with Docker Compose. Currently **stubs**: every RPC returns
`UNIMPLEMENTED` except the health services. Planned work is tracked on the team's
project board and marked in the code with `TODO(ff)` comments.

The human-facing guide is `CONTRIBUTING.md`: setup, workflow, per-service coding
guidelines and troubleshooting. Follow it; when you change a rule here, change it there too.

## Repo map

```
proto/friendlyfire/<area>/v1/*.proto   contracts (Buf); buf.gen.yaml at the root
gen/{java,python,go}                   GENERATED from proto/, committed. Never edit.
services/gateway-service               Java/Spring Boot: REST + STOMP (/ws) for the browser
services/account-service               Java/Spring gRPC :9091, Postgres schema `account`
services/room-service                  Java/Spring gRPC :9092, rooms in memory
services/quiz-engine-service           Python grpc.aio :9093
services/scoring-service               Python grpc.aio :9094, Postgres schema `scoring`
services/match-data-service            Go grpc-go :9095, Postgres schema `matchdata`
services/mock-rso-service              Go net/http :8090, stands in for Riot Sign-On
frontend/                              Next.js (App Router, TypeScript) :3000
e2e/                                   Playwright end-to-end tests
infra/local/postgres/init/             creates the database and schemas
compose.yaml, Makefile, mise.toml      local dev; mise.toml pins every tool version
```

## Who calls whom

| Service | Depends on |
|---|---|
| frontend | gateway-service (HTTP and WebSocket) |
| gateway-service | account, room, scoring, matchdata (gRPC) |
| account-service | matchdata (gRPC), postgres (`account`), mock-rso-service (HTTP) |
| room-service | quiz, scoring, matchdata (gRPC) |
| quiz-engine-service | matchdata (gRPC) |
| scoring-service | postgres (`scoring`) |
| match-data-service | postgres (`matchdata`); later HenrikDev / Riot over HTTPS |
| mock-rso-service | nothing |

## Commands

```
make up [s="svc ..."]   start the stack (or some services + their dependencies)
make down | ps | logs s=<svc> | build | clean
make proto              buf lint + buf generate (in Docker)
make test [s=<svc>]     unit tests, inside the dev containers
make test-integration   Testcontainers / WireMock tests (Docker socket mounted)
make e2e                Playwright against a running `make up`
make lint               ruff, mypy, go vet, golangci-lint, ESLint, tsc, buf lint, version check
make coverage           unit tests + coverage reports
```

## Rules

- **Never edit `gen/` by hand.** After changing a `.proto`, run `make proto` and commit
  the result. Contract changes (`proto/`, `gen/`) need a project manager's approval;
  CI's `buf breaking` only lets a break through when a PM adds the `breaking-change` label.
- **Config comes from environment variables only** (`.env`, copied from `.env.example`).
  Never commit secrets or `.env`.
- **Inject dependencies through constructors.** No field injection, no globals; wiring
  happens in one place per service (`config/` in Java, `bootstrap.py`, `cmd/<svc>/main.go`).
- **Every change comes with tests**: unit tests by default, integration tests when it
  touches Postgres or an HTTP API. Follow the naming and folders below.
- **Keep `/api/health/deep` working.** When a service starts calling something new
  (a service, a database, an external API), add it to that service's dependency health
  check, using the same injected client the feature uses.
- **Follow the Mac and Windows rules:** only multi-arch official images, never
  `platform:` / `--platform`; downloaded binaries pick the arch with `ARG TARGETARCH`;
  `node_modules`, virtualenvs and caches live in Docker volumes, not the repo; LF line
  endings; no file names that differ only by case.
- **Versions live in `mise.toml`.** When you bump a tool, bump the Dockerfiles, `go.mod`
  and `pom.xml` too; `scripts/check-versions.sh` (in `make lint`) catches mismatches.
- Mark planned work with `// TODO(ff): <summary>` (or `#`) followed by `- ` checklist
  lines, and add a card for it on the project board. When you finish the work, delete
  the comment.

## Tests

| Kind | Covers | Needs Docker? | Runs |
|---|---|---|---|
| Unit | one class/function, dependencies replaced by fakes | No | `make test`, every PR |
| Integration | a service against real Postgres or a fake HTTP server (Testcontainers / WireMock / httptest) | Yes | `make test-integration`, every PR |
| End-to-end | the whole stack through the browser (Playwright) | Yes (whole stack) | `make e2e`, nightly and on demand |

Where tests live and how they're named:
- **Java:** `src/test/java`, same package as the class. Unit tests `*Test` (Surefire).
  Integration tests `*IT` (Failsafe, during `./mvnw verify`).
- **Python:** `tests/unit/` and `tests/integration/`. Integration tests carry
  `@pytest.mark.integration` and are skipped by plain `pytest`
  (`addopts = "-m 'not integration'"` in `pyproject.toml`).
- **Go:** `_test.go` next to the code. Integration tests start with `//go:build integration`.
- **Frontend:** `*.test.ts(x)` next to the component. Playwright specs in `e2e/tests/`.
- **Shared fakes:** one place per service: `FakeDependencyCheck` (Java),
  `tests/fakes.py` (Python), `internal/testutil` (Go). Prefer fakes over mocking
  libraries for our own interfaces.
- **Timing asserts are loose:** assert "under 3 seconds", never "about 1 second".
