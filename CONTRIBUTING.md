# Contributing to FriendlyFire

This guide takes you from a fresh laptop to a running stack, then explains how we work
and how code is written in each service. Read the [README](README.md) first: it says what
each service does and who calls whom.

1. [Set up your machine](#1-set-up-your-machine)
2. [Run the stack](#2-run-the-stack)
3. [Editor setup (optional)](#3-editor-setup-optional)
4. [How we work](#4-how-we-work)
5. [Coding guidelines](#5-coding-guidelines):
   [every service](#every-service) ·
   [Java](#java-gateway-service-account-service-room-service) ·
   [Python](#python-quiz-engine-service-scoring-service) ·
   [Go](#go-match-data-service-mock-rso-service) ·
   [Frontend](#frontend-nextjs) ·
   [Contracts](#contracts-proto)
6. [Troubleshooting](#6-troubleshooting)

Using an AI coding agent? [AGENTS.md](AGENTS.md) has the same rules in short form. Most
agents read it directly; Claude Code reads it through `CLAUDE.md`.

## 1. Set up your machine

Everything runs in Docker, so you only need **Docker, `make` and `git`**. You don't need
Java, Python, Go or Node installed to run the app or its tests.

### macOS (Apple Silicon or Intel)

1. Install Apple's command line tools (they include `git` and `make`):
   `xcode-select --install`
2. Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) (or
   OrbStack). In Docker Desktop → Settings → Resources, give it **at least 6 GB** of memory.
3. Clone the repo:
   ```bash
   git clone https://github.com/SaswatM-62/FriendlyFire.git
   cd FriendlyFire
   ```

### Windows

Everything happens **inside WSL** (Ubuntu), never in PowerShell or `cmd`.

1. In PowerShell as administrator: `wsl --install -d Ubuntu`, restart, then open
   "Ubuntu" from the Start menu and create your Linux user.
2. Install [Docker Desktop](https://www.docker.com/products/docker-desktop/). In
   Settings → Resources → **WSL integration**, turn it on for Ubuntu.
3. In the Ubuntu terminal: `sudo apt update && sudo apt install -y git make curl`
4. Clone the repo **inside WSL's home folder, never under `/mnt/c`** (files under
   `/mnt/c` are slow in Docker, and hot reload doesn't see your edits there):
   ```bash
   mkdir -p ~/code && cd ~/code
   git clone https://github.com/SaswatM-62/FriendlyFire.git
   cd FriendlyFire
   code .    # opens VS Code connected to WSL
   ```
5. Optional, on an 8 GB laptop: cap WSL's memory in `%UserProfile%\.wslconfig`, then run
   `wsl --shutdown` in PowerShell:
   ```ini
   [wsl2]
   memory=6GB
   ```

### Linux

Install Docker Engine with the Compose plugin, `git` and `make`. Add yourself to the
`docker` group (`sudo usermod -aG docker $USER`), then log out and back in.

### Check your setup

```bash
docker info --format '{{.ServerVersion}}'   # prints a version, not an error
docker compose version
make help                                   # lists the make targets
```

## 2. Run the stack

```bash
cp .env.example .env    # once; .env holds your local settings and is never committed
make up
```

The first `make up` builds every image, and the Java services download their Maven
dependencies, so it takes a few minutes. Later starts take about a minute.

### Is everything working?

1. `make ps`: all 9 containers say `(healthy)`. The Java services are the last ones.
2. Open **http://localhost:3000** (use `localhost`, not `127.0.0.1`: the Gateway only
   accepts requests from `FRONTEND_ORIGIN`). The **System status** page should say
   **"17 of 17 checks passing"** and **"Realtime: connected · N ms"**.
3. From a terminal:
   ```bash
   curl localhost:8080/api/health                                  # {"status":"UP"}
   curl -s localhost:8080/api/health/deep | python3 -m json.tool   # the whole tree
   ```
4. Or let the smoke test check it: `make e2e`.

If something is red, see [Troubleshooting](#6-troubleshooting).

### Everyday commands

| Command | What it does |
|---|---|
| `make up s="room-service"` | Starts one service plus what it depends on (saves memory) |
| `make logs s=<service>` | Follows one service's logs |
| `make down` | Stops everything; the database is kept |
| `make clean` | Stops everything and deletes the volumes (database, caches): a fresh start |
| `make build` | Rebuilds the images (after changing a Dockerfile, `infra/docker/` or `compose.yaml`) |
| `docker compose exec postgres psql -U postgres -d friendlyfire` | A SQL prompt on the local database |

### What reloads by itself

| You change | What happens |
|---|---|
| Java code or `application.yml` | Recompiles, then the app restarts (a few seconds) |
| Python code | `watchfiles` restarts the server |
| Go code | `air` rebuilds and restarts |
| Frontend code | Next.js hot-reloads the page |
| A `.proto` file | Nothing yet: run `make proto`, then the services reload |
| `pom.xml`, `pyproject.toml`, `go.mod`, `package.json` | Nothing: `docker compose restart <service>` (it installs the new dependencies when it starts) |
| `.env` | Nothing: `make up` recreates the containers with the new values |
| A Dockerfile, `infra/docker/*`, `compose.yaml` | Nothing: `make build && make up` |

## 3. Editor setup (optional)

Docker runs everything. Local toolchains only help your editor (autocomplete, go to
definition, running one test from the IDE). We pin them with [mise](https://mise.jdx.dev)
from [`mise.toml`](mise.toml), **only inside this repo**, so your editor uses the same
versions as Docker and CI, and your other projects are untouched.

**Install mise**
- macOS: `brew install mise`
- WSL / Linux (no sudo; installs to `~/.local/bin/mise`): `curl https://mise.run | sh`

**Set it up so it never touches your other projects**
1. Back up your shell config: `cp ~/.bashrc ~/.bashrc.bak-$(date +%F)` (macOS: `~/.zshrc`).
2. Keep mise's shims off your `PATH`, so `node`, `python`, ... only change inside this
   repo: `mise settings set activate_shims false`. This only writes a `[settings]` entry
   to `~/.config/mise/config.toml`.
3. Add **one** line at the very end of `~/.bashrc`, after any nvm, pyenv, SDKMAN or conda
   lines:
   ```bash
   eval "$(~/.local/bin/mise activate bash)"   # macOS: eval "$(mise activate zsh)" in ~/.zshrc
   ```
4. Open a new terminal. In the repo: `mise trust && mise install`.
5. Check: inside the repo `java -version` says 21; outside it, everything is as before.
   `mise doctor` should report no problems.

**Rules:** never run `mise use -g` / `--global`; tools only come from the repo's
`mise.toml`; only `mise trust` this repo; leave `idiomatic_version_file_enable_tools` empty
(the default) so mise ignores `.nvmrc` / `.python-version` files in other projects.
Maven comes from each Java service's `./mvnw`, not from mise.

**Undo:** `mise implode --config`, then delete the `mise activate` line from your shell
config (or restore the backup).

**Per language, once**

| Language | Do this |
|---|---|
| Java | Open the repo in VS Code with the Java extension pack; it imports each `services/*/pom.xml` (including the generated code in `gen/java`). |
| Python | `cd services/<service> && uv sync`, then pick `.venv` as the interpreter. |
| Go | Open the repo root: `go.work` covers `gen/go` and both Go services. |
| Frontend | `cd frontend && npm ci` |

Running tests locally (`./mvnw test`, `uv run pytest`, `go test ./...`, `npm test`) is
fine, even while `make up` runs. A Java service shares `target/` with its container, but
the app only restarts after the container's own compile. `make test s=<service>` is the
reference: same versions, nothing to install.

VS Code suggests the recommended extensions from
[`.vscode/extensions.json`](.vscode/extensions.json) when you open the repo.

## 4. How we work

### Finding work

The **project board** is the backlog: take a card, move it along as you work, and link
your pull request to it.

Most cards point to a `TODO(ff)` comment in the code. The comment marks where the work
goes and lists what to build, which errors to return and which injected dependencies to
use. Search for them with `grep -rn "TODO(ff)" services frontend`. When you finish the
work, delete the comment in the same pull request.

### Branches, commits and pull requests

- Branch from `main`: `feat/room-join`, `fix/scoring-ranks`, `chore/bump-node`.
- Commit messages: `type(scope): summary`, with type `feat`, `fix`, `test`, `docs`,
  `refactor` or `chore`, and the service (or `repo`) as scope. Example:
  `feat(room-service): join a room by code`.
- Open a pull request to `main`. The **`ci-ok`** check must be green.
- Changes to `proto/` or `gen/` need a project manager's review (see `.github/CODEOWNERS`).
- For a risky change, add the **`run-e2e`** label to also run the end-to-end test.

**Before you push**

```bash
make lint
make test s=<your-service>
make test-integration s=<your-service>   # if you touched Postgres or an HTTP API
```

### Common changes, step by step

**Change a contract.** Edit `proto/friendlyfire/<area>/v1/*.proto`, run `make proto`, and
commit `proto/` and `gen/` together. See [Contracts](#contracts-proto) for the rules.

**Add a library.** Pin the exact version and commit the lockfile.

| Service | Command (from the repo root) |
|---|---|
| Java | Add it to the service's `pom.xml` (leave out `<version>` when Spring Boot manages it), then `docker compose restart <service>` |
| Python | `docker compose run --rm --no-deps <service> uv add <package>==<version>` (`--dev` for test and lint tools); commit `pyproject.toml` and `uv.lock` |
| Go | `docker compose run --rm --no-deps <service> sh -c 'go get <module>@<version> && go mod tidy'`; commit `go.mod` and `go.sum` |
| Frontend | `docker compose run --rm --no-deps frontend npm install --save-exact <package>@<version>`, then `docker compose restart frontend` |

With mise you can run the same commands (`uv add`, `go get`, `npm install`) in the
service folder instead.

**Add an environment variable.** Add it to `.env.example` (leave secrets empty: real
values never go in the repo), read it in the service's config (`application.yml`,
`settings.py` or `internal/config`) and nowhere else, and tell the team to update their
`.env`.

**Call a new service, database or API.** Inject the client through the constructor, and
add it to the service's dependency health check (`config/HealthConfig.java`,
`bootstrap.py` or `cmd/<service>/main.go`). It then shows up on the status page.

**Bump a tool version.** Change `mise.toml`, then run `./scripts/check-versions.sh`: it
lists the Dockerfiles, `go.mod` and `pom.xml` files that still need the same change.

## 5. Coding guidelines

### Every service

- **Config comes from environment variables only.** No hard-coded hosts, ports or
  secrets, and never commit `.env`.
- **Inject dependencies through constructors.** Objects are wired in one place per
  service: `config/` in Java, `bootstrap.py` in Python, `cmd/<service>/main.go` in Go.
  No globals, no field injection.
- **The browser only talks to the Gateway.** Services talk to each other over gRPC, using
  the contracts in `proto/`.
- **The `user_id` always comes from the Gateway** (the signed-in user), never from a
  request body sent by the browser.
- **Keep private data private:** never send a PUUID or a Riot token to the browser, and
  never log tokens, API keys or passwords.
- **Return the gRPC status codes the `.proto` comments name** (`NOT_FOUND`,
  `INVALID_ARGUMENT`, ...). The Gateway turns them into HTTP status codes.
- **Every change comes with tests:** unit tests by default, integration tests when the
  change touches Postgres or an HTTP API.
- **Use fakes for our own interfaces** (`FakeDependencyCheck`, `tests/fakes.py`,
  `internal/testutil`). Use a mocking library only for third-party types.
- **Keep timing asserts loose:** "under 3 seconds", never "about 1 second". CI runners
  are slow.
- **Never edit `gen/` by hand.**
- **Mark planned work** in the code like this, and create a card for it on the board:
  ```java
  // TODO(ff): one-line summary
  //   - what it must do
  //   - errors to return, with their gRPC status codes
  //   - which injected dependencies it uses
  ```
- LF line endings (Git handles this), and no file names that differ only by case.

### Java: gateway-service, account-service, room-service

```
services/account-service/src/main/java/com/friendlyfire/account/
├── AccountServiceApplication.java
├── config/      wiring: GrpcClientsConfig (gRPC clients), HealthConfig, ...
├── grpc/        the gRPC service (AccountGrpcService)
├── health/      dependency health checks
└── user/, riotlink/, rso/   features
```
The Gateway has `web/` (REST under `/api`) and `realtime/` (STOMP) instead of `grpc/`.

- Spring Boot 4.1 with Spring gRPC. Configuration classes are
  `@Configuration(proxyBeanMethods = false)`; beans are injected through constructors.
- **gRPC clients** are created in `config/GrpcClientsConfig.java`: one channel per service
  (its address comes from the `*_GRPC_ADDR` env var), shared by that service's feature stub
  and health stub. Inject the stub type you need, e.g. `MatchDataServiceBlockingStub`.
- **A gRPC service** extends the generated `...ImplBase` and reports errors with
  `responseObserver.onError(Status.NOT_FOUND.withDescription("...").asRuntimeException())`.
  Generated classes live in `com.friendlyfire.proto.friendlyfire.<area>.v1`.
- **Gateway REST:** controllers in `web/`, under `/api/...`. Map gRPC errors to HTTP:
  `NOT_FOUND` → 404, `INVALID_ARGUMENT` → 400, `PERMISSION_DENIED` → 403,
  `ALREADY_EXISTS` / `FAILED_PRECONDITION` / `RESOURCE_EXHAUSTED` → 409, `UNAVAILABLE` → 503.
- **Gateway STOMP:** `@MessageMapping` methods in `realtime/`. Browsers send to
  `/app/...`; room updates go to `/topic/room/{code}`; messages for one player go to
  `/user/queue/...`.
- **Postgres** (account-service only, so far): Spring JDBC (`JdbcClient`) on the `account`
  schema. There are no tables yet: agree on a migration tool with the project managers
  before adding the first one.
- **Time:** room-service injects a `java.time.Clock`; never call `Instant.now()` directly
  in game logic.
- 4-space indents. No Java formatter or linter is set up yet, so follow the surrounding code.

**Tests**
- In `src/test/java`, same package as the class. `*Test` = unit test (`make test`),
  `*IT` = integration test (`make test-integration`).
- JUnit 5 and AssertJ. MockMvc (`@WebMvcTest`) for REST, `WebSocketStompClient` for STOMP.
- To call the service over gRPC: `@SpringBootTest` + `@AutoConfigureTestGrpcTransport`,
  then `GrpcChannelFactory.createChannel("any")` reaches it in-process. Careful: this also
  routes the service's own clients back to itself, so don't call `CheckDependencies`
  that way. Integration tests use a real server on a random port instead
  (`spring.grpc.server.port=0`, see `PostgresHealthIT`).
- Postgres: Testcontainers with `@ServiceConnection` and the same image as `compose.yaml`
  (`postgres:16.15-alpine`). HTTP APIs: WireMock (`@EnableWireMock`).
- Waiting for something async: Awaitility. Controlling time: replace the `Clock` bean
  with `@TestBean` (see `ClockConfigTest`).
- Run: `make test s=room-service`, or with mise `./mvnw test` / `./mvnw verify` in the
  service folder.

### Python: quiz-engine-service, scoring-service

```
services/scoring-service/
├── src/scoring/
│   ├── __main__.py           python -m scoring
│   ├── settings.py           every env var (pydantic-settings)
│   ├── bootstrap.py          build_server(settings, overrides): the only wiring place
│   ├── servicer.py           the gRPC service
│   └── dependency_health.py
└── tests/
    ├── fakes.py
    ├── unit/
    └── integration/
```

- Python 3.12, **async all the way** (`grpc.aio`). Never block the event loop: no
  `time.sleep`, no synchronous network or database calls.
- Errors: `await context.abort(grpc.StatusCode.NOT_FOUND, "message")`.
- A class gets its dependencies through `__init__`. `bootstrap.py` builds them, and its
  `Overrides` let tests swap them for fakes.
- **Types:** `mypy --strict` on `src/`, so annotate everything. **Style:** `ruff check`
  and `ruff format` (line length 100); `uv run ruff format` fixes formatting for you.
- quiz-engine-service: the same seed and data must give the same questions. Use
  `random.Random(seed)`, never the module-level `random` functions.
- scoring-service: Postgres through the injected psycopg `AsyncConnectionPool`, on the
  `scoring` schema. No tables yet (the migration tool is still to be agreed).
- Dependencies: uv, exact versions, commit `uv.lock`. `grpcio*` and `protobuf` must match
  the Buf plugins in `buf.gen.yaml`.

**Tests**
- Unit tests in `tests/unit/test_*.py`. Async tests need no decorator
  (`asyncio_mode = "auto"`), and every test times out after 60 seconds.
- Integration tests in `tests/integration/` with `pytestmark = pytest.mark.integration`;
  plain `pytest` skips them. Postgres: `testcontainers.community.postgres`.
- To test over gRPC, start the real server on a free port:
  `build_server(Settings(grpc_port=0), Overrides(...))` (see `tests/unit/test_health.py`).
- Logic with many possible inputs gets Hypothesis property tests (see the example in
  `tests/unit/test_question_generator.py`).
- Run: `make test s=scoring-service` and `make lint`, or with mise `uv run pytest`,
  `uv run pytest -m integration`, `uv run mypy src`.

### Go: match-data-service, mock-rso-service

```
services/match-data-service/
├── cmd/match-data-service/main.go   reads config, wires everything, stops on SIGTERM
└── internal/
    ├── config/  grpcserver/  health/  matchdata/ (the gRPC service)
    ├── source/  store/  sync/  ratelimit/
    └── testutil/                    fakes for tests
```

- The standard library first: `log/slog`, `net/http`, `context`.
- `context.Context` is the first parameter of anything that does I/O; respect its
  deadline and cancellation.
- Errors: wrap with `fmt.Errorf("loading matches: %w", err)`. gRPC handlers return
  `status.Error(codes.NotFound, "...")`.
- Logging: `slog` with key/value pairs. Never log API keys or tokens.
- Constructors take their dependencies (`NewPostgresCheck(pool)`); keep interfaces small
  and declare them where they're used (like `health.Pinger`).
- Background goroutines (the sync worker) take a context and stop on shutdown. Tests
  check for leaked goroutines with goleak.
- Postgres: the `pgxpool.Pool` from `main.go`, on the `matchdata` schema.
- HenrikDev: tests never call the real API. Save real responses (without the API key) in
  `internal/source/testdata/henrikdev/` and serve them with `httptest`. Stay under 30
  requests per minute (`internal/ratelimit`).
- `gofmt`, `go vet` and `golangci-lint` must be clean. Check every error, even in tests:
  `defer func() { _ = resp.Body.Close() }()`.

**Tests**
- `_test.go` files next to the code. testify: `require` stops the test on failure,
  `assert` doesn't.
- gRPC: an in-memory `bufconn` server (see `internal/grpcserver/grpcserver_test.go`).
- Integration tests start with `//go:build integration` (Testcontainers' Postgres module).
- Run: `make test s=match-data-service`, or with mise `go test -race ./...`,
  `go test -race -tags integration ./...` and `golangci-lint run`.
- Dependencies: `go get <module>@<version>` and `go mod tidy` in the service folder. The
  `go` line in `go.mod` matches `mise.toml`.

### Frontend: Next.js

```
frontend/src/
├── app/          routes (App Router): one page.tsx per route
├── components/   React components, each with its *.test.tsx next to it
├── lib/          config.ts (NEXT_PUBLIC_* values) and Gateway API helpers
└── test/         Vitest setup, the fake Gateway (MSW) and test data
```

- TypeScript in strict mode; avoid `any` and type every Gateway response (see
  `lib/health.ts`).
- Server components by default. Add `"use client"` only for state, effects or browser APIs.
- The frontend only talks to the Gateway: REST through helpers in `src/lib` (using
  `config.gatewayUrl`), realtime over STOMP (`@stomp/stompjs`, `config.wsUrl`).
- `NEXT_PUBLIC_*` values are read only in `src/lib/config.ts`.
- Style with Tailwind classes.
- The React lint rules don't allow calling `setState` directly in an effect. Set state
  in callbacks instead (a promise's `.then`, an event handler, a subscription), as
  `SystemStatus.tsx` does.
- `cacheComponents` is on: when a page reads request data (route params, cookies,
  uncached fetches), wrap that part in `<Suspense>`.

**Tests**
- Vitest and React Testing Library. Query the page like a user would (`getByRole`,
  `getByText`), interact with `user-event`, and assert with jest-dom matchers.
- Fake the Gateway with MSW inside the test:
  `server.use(http.get(url, () => HttpResponse.json(...)))`. A request without a handler
  fails the test.
- End-to-end: Playwright specs in `e2e/tests/`, run with `make e2e` while `make up` runs.
- Run: `make test s=frontend` and `make lint`, or with mise in `frontend/`: `npm test`,
  `npm run test:watch`, `npm run lint`, `npm run typecheck`.

### Contracts (`proto/`)

- One package per area and version: `friendlyfire.<area>.v1`. `buf lint` uses the
  STANDARD rules.
- The comments are the spec: say what each RPC does and which errors it returns. They
  become the `TODO(ff)` checklists.
- Never change, reuse or remove field numbers, fields or RPCs in `v1`; add new ones
  instead (CI runs `buf breaking`). A real breaking change means a new `v2` package.
- Run `make proto` and commit `proto/` and `gen/` together; a project manager approves.

## 6. Troubleshooting

Start here:

```bash
make ps                   # which container isn't healthy?
make logs s=<service>     # why?
```

Then the status page (http://localhost:3000) shows which connection fails, and why.

| Problem | Fix |
|---|---|
| `docker: command not found` or "Cannot connect to the Docker daemon" (WSL) | Start Docker Desktop, turn on WSL integration for Ubuntu (Settings → Resources), and open a new terminal. |
| `make: command not found` | macOS: `xcode-select --install`. WSL: `sudo apt install make`. |
| `make up` stops with "dependency failed to start: container ... is unhealthy" | `make logs s=<that service>`. On the very first start, a Java service can be slow while it downloads dependencies: run `make up` again. |
| "port is already allocated" | Another program uses that port. For Postgres, set `POSTGRES_HOST_PORT=5433` in `.env`. For 3000, 8080, 8090 or 9091–9095, find it with `lsof -i :3000` (macOS) or `ss -ltnp \| grep 3000` (Linux/WSL), often an old stack from another clone (`docker ps`). |
| Status page says "Can't reach the Gateway" or "Realtime: disconnected" | gateway-service isn't healthy (`make logs s=gateway-service`), or you opened `127.0.0.1:3000` instead of `localhost:3000`. |
| A node is DOWN with "connection refused" or "unknown host" | That service (or Postgres) isn't running: `make up`. |
| A node is DOWN with "deadline exceeded" | It answered too slowly, usually because it or something below it is still starting. Wait a few seconds. |
| A service with migrations (scoring-service) stays DOWN after Postgres was stopped and started again | A service may fail to start while Postgres is unreachable, and in `make up` the hot-reload watcher (`watchfiles`) can restart the process, sometimes at that moment (Docker Desktop reports file changes in the shared `gen/python` folder). The process then exits and stays down, because the container keeps running and Docker's restart policy never fires. Start Postgres, then `docker compose restart <service>`. |
| A Java service doesn't pick up my change | Look for a compile error in `make logs s=<service>`: the app only restarts after a successful compile. |
| Hot reload does nothing on Windows | The repo is under `/mnt/c`. Clone it inside WSL (`~/code/FriendlyFire`). |
| A new dependency isn't found | `docker compose restart <service>`. |
| I changed a Dockerfile, `infra/docker/` or `compose.yaml` | `make build && make up` |
| Docker is slow, or the laptop runs out of memory | Start only what you need (`make up s="..."`), give Docker Desktop at least 6 GB, and `make down` when you're done. |
| Integration tests: "Could not find a valid Docker environment" | Docker must be running. Run them with `make test-integration`, which gives the container access to Docker. |
| `mvnw: /bin/sh^M: bad interpreter` | The repo was cloned on the Windows side. Clone it again inside WSL. |
| Folders owned by root in the repo (Linux/WSL) | Run things through `make` (it creates mount points as you), and fix the existing ones with `sudo chown -R "$USER": <folder>`. |
| Everything is in a weird state | `make clean && make up` (deletes the local database and caches). |
