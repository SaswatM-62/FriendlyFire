# FriendlyFire

FriendlyFire is free, personalized trivia built from your friends' own VALORANT matches:
link your Riot account, open a room with up to 7 friends, and answer questions based on your recent Valorant stats. 

> **Status: stubs.** Every service starts, reports healthy and answers `UNIMPLEMENTED`
> for everything else. The health checks already run end to end (browser → Gateway →
> every service → Postgres). Planned work is tracked on the project board, and each piece
> is marked in the code with a `TODO(ff)` comment saying what to build there.

## Services

| Service | Stack | Port | Owns tables? |
|---|---|---|---|
| gateway-service | Java 21, Spring Boot, Maven | 8080 (HTTP + `/ws`) | No |
| account-service | Java 21, Spring Boot, Maven, Spring gRPC | 9091 (gRPC) | `account` schema |
| room-service | Java 21, Spring Boot, Maven, Spring gRPC | 9092 (gRPC) | No (rooms live in memory) |
| quiz-engine-service | Python 3.12, `grpc.aio`, uv | 9093 (gRPC) | No |
| scoring-service | Python 3.12, `grpc.aio`, uv | 9094 (gRPC) | `scoring` schema |
| match-data-service | Go, `grpc-go` | 9095 (gRPC) | `matchdata` schema |
| mock-rso-service | Go, `net/http` | 8090 (HTTP) | No |
| frontend | Next.js (App Router, TypeScript), npm | 3000 | — |
| postgres | `postgres:16.15-alpine` | 5432 inside Docker; host port from `POSTGRES_HOST_PORT` | — |

**Who calls whom**

| Service | Depends on |
|---|---|
| frontend | gateway-service (HTTP and WebSocket) |
| gateway-service | account, room, scoring, matchdata (gRPC) |
| account-service | matchdata (gRPC), postgres (`account` schema), mock-rso-service (HTTP) |
| room-service | quiz, scoring, matchdata (gRPC) |
| quiz-engine-service | matchdata (gRPC) |
| scoring-service | postgres (`scoring` schema) |
| match-data-service | postgres (`matchdata` schema). Later also HenrikDev / Riot over HTTPS |
| mock-rso-service | nothing |

The contracts live in [`proto/`](proto/) (gRPC + Protobuf, managed with Buf). The generated
code in [`gen/`](gen/) is committed; never edit it by hand, run `make proto` instead.

## Getting started

You need **Docker, `make` and `git`**; everything else runs in containers.
- **macOS:** Docker Desktop (or OrbStack) with at least 6 GB of memory.
- **Windows:** WSL2 with Ubuntu and Docker Desktop with WSL integration turned on. Clone
  the repo inside WSL (e.g. `~/code/FriendlyFire`), never under `/mnt/c`.
- **Linux:** Docker Engine with the Compose plugin.

Step-by-step setup for each system is in
[CONTRIBUTING.md](CONTRIBUTING.md#1-set-up-your-machine). Then:

```bash
cp .env.example .env && make up
```

The first start builds every image and takes a few minutes. When `make ps` shows every
container as `(healthy)`, open **http://localhost:3000**: the **System status** page
should say "17 of 17 checks passing" and "Realtime: connected · N ms". `make e2e` checks
the same thing in a real browser.

**Low on memory?** Start only what you need (plus what it depends on):

```bash
make up s="gateway-service room-service"
```

## Everyday commands

| Command | What it does |
|---|---|
| `make` / `make help` | Lists the targets |
| `make up` / `make down` | Starts / stops the stack (`s="a b"` for some services) |
| `make ps`, `make logs s=<svc>` | Container status, one service's logs |
| `make build` | Rebuilds the dev images |
| `make proto` | `buf lint` + `buf generate` into `gen/` (Buf runs in Docker) |
| `make test` | Unit tests for every service (or `s=<svc>`), inside the dev containers |
| `make test-integration` | Integration tests (Testcontainers, WireMock) |
| `make e2e` | Playwright smoke test against the running stack |
| `make lint` | Every linter and type checker |
| `make coverage` | Unit tests with coverage reports (`target/site/jacoco`, `htmlcov`, `coverage.out`, `coverage/`) |
| `make clean` | Stops everything and deletes the volumes (database, caches) |

Code changes reload automatically: Spring DevTools for Java, `watchfiles` for Python,
`air` for Go and Next.js's dev server for the frontend. Some changes need a restart or a
rebuild; [CONTRIBUTING.md](CONTRIBUTING.md#what-reloads-by-itself) lists which.

## Health checks

- `GET http://localhost:8080/api/health` → `{"status":"UP"}`: is the Gateway running?
- `GET http://localhost:8080/api/health/deep` → the whole dependency tree. Each service
  checks everything it calls (gRPC peers, Postgres, the mock RSO) in parallel with a
  1-second deadline and reports `UP`, `DEGRADED` or `DOWN`. It always answers HTTP 200.
- Every gRPC service also serves the standard `grpc.health.v1` (used by Docker) and our
  `friendlyfire.health.v1.DependencyHealthService`, e.g.
  `grpcurl -plaintext localhost:9092 friendlyfire.health.v1.DependencyHealthService/CheckDependencies`.

**When a service starts calling something new** (a service, a database, an external API),
add it to that service's dependency health check.

## Tests

| Kind | What it covers | Runs |
|---|---|---|
| Unit | One class or function, dependencies replaced by fakes | `make test`, every PR |
| Integration | A service against a real Postgres or a fake HTTP server | `make test-integration`, every PR |
| End-to-end | The whole stack in Docker Compose, driven through the browser | `make e2e`, on every push to `main`, nightly and on demand |

Where tests live, how they're named and which libraries each service uses are described
per service in [CONTRIBUTING.md](CONTRIBUTING.md#5-coding-guidelines).

**CI.** Every pull request to `main` runs `.github/workflows/ci.yml`; the single required
check is `ci-ok`. The end-to-end workflow (`e2e.yml`) runs on pushes to `main`, nightly,
on manual dispatch and on pull requests labelled `run-e2e`. It is not required yet: the
plan is to make it a required PR check from Sprint 3, once the quiz flow exists.

## Contributing

[CONTRIBUTING.md](CONTRIBUTING.md) is the guide for working on FriendlyFire:
- setting up macOS, Windows (WSL) or Linux, and checking that the stack runs;
- optional editor setup, with the exact toolchain versions from `mise.toml`;
- how we work: picking up a card from the project board, branches, commits, pull
  requests and common changes (contracts, libraries, env vars);
- coding guidelines for each service (Java, Python, Go, frontend, contracts);
- troubleshooting.

**Something not working?** The two most common problems:
- **Port 5432 is already taken** (e.g. Homebrew Postgres): set `POSTGRES_HOST_PORT=5433`
  in `.env`, then `make up` again.
- **Hot reload doesn't work on Windows:** the repo is probably under `/mnt/c`. Clone it
  inside WSL instead (`~/code/FriendlyFire`).

Everything else is in [Troubleshooting](CONTRIBUTING.md#6-troubleshooting).

## Pinned versions

[`mise.toml`](mise.toml) is the single source of truth for toolchain versions; the
Dockerfiles, `go.mod` files, `pom.xml` files and CI use the same ones
(`./scripts/check-versions.sh`, part of `make lint`, checks that they match).

| What | Version |
|---|---|
| Java (Temurin) | 21.0.12.1+1 (`eclipse-temurin:21.0.12.1_1`) |
| Maven (wrapper) | 3.10.0 (Maven Wrapper 3.3.4) |
| Go | 1.27.3 |
| Python | 3.12.15 |
| uv | 0.13.5 |
| Node.js (LTS) | 24.22.1 |
| Buf | 1.75.0 |
| golangci-lint | 2.15.0 |
| Postgres | 16.15 (`postgres:16.15-alpine`) |
| Spring Boot | 4.1.2 (with Spring gRPC 1.1.2) |
| gRPC | Java and Python 1.84.1, Go 1.84.0 |
| Protobuf | Java 4.36.2, Python 7.36.2, Go 1.36.12 |
| Buf plugins | `protocolbuffers/java` v36.2, `grpc/java` v1.84.1, `protocolbuffers/python` v36.2, `protocolbuffers/pyi` v36.2, `grpc/python` v1.84.1, `protocolbuffers/go` v1.36.12, `grpc/go` v1.6.2 |
| Frontend | Next.js 16.4.3, React 19.3.1, TypeScript 5.9.3, Tailwind CSS 4.3.3, @stomp/stompjs 7.5.0 |
| Java tests | JUnit 6.0.3, Mockito 5.23.0, AssertJ 3.27.7, Testcontainers 2.0.5, Awaitility 4.3.0 (all via Spring Boot), wiremock-spring-boot 4.4.3, JaCoCo 0.8.16 |
| Python tests and lint | pytest 9.1.1, pytest-asyncio 1.4.1, pytest-timeout 2.4.0, pytest-cov 7.1.0, hypothesis 6.170.11, testcontainers 4.15.0, ruff 0.17.2, mypy 2.4.0 |
| Go tests | testify 1.12.1, goleak 1.3.0, testcontainers-go 0.44.0 |
| Frontend tests | Vitest 5.0.5, React Testing Library 16.3.3, jest-dom 7.0.1, user-event 14.6.8, MSW 3.0.3, jsdom 30.1.3, Playwright 1.64.1 |
| Dev images | grpc_health_probe 0.4.60, watchexec 2.8.0, air 1.67.4 |

The full list of library versions is in each service's build file (`pom.xml`,
`pyproject.toml` + `uv.lock`, `go.mod`, `package.json` + `package-lock.json`). Saswat
