# syntax=docker/dockerfile:1
# Build from the repo root: docker build -f services/quiz-engine-service/Dockerfile --target dev .
# Versions match mise.toml (python, uv).

# ---------- dev: full toolchain, hot reload, source mounted by compose.yaml ----------
FROM python:3.12.15-slim AS dev
ARG TARGETARCH
ARG GRPC_HEALTH_PROBE_VERSION=0.4.60
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL -o /usr/local/bin/grpc_health_probe \
      "https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/v${GRPC_HEALTH_PROBE_VERSION}/grpc_health_probe-linux-${TARGETARCH}" \
    && chmod +x /usr/local/bin/grpc_health_probe \
    && pip install --no-cache-dir uv==0.13.5
# Tools run as "dev" (uid 1000); dev-entrypoint switches to the owner of your checkout.
RUN useradd --create-home --home-dir /home/dev --uid 1000 dev
# The virtualenv lives outside the mounted source, so it never ends up in the repo.
ENV HOME=/home/dev \
    UV_PROJECT_ENVIRONMENT=/opt/venv \
    UV_LINK_MODE=copy \
    UV_PYTHON_DOWNLOADS=never \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    DEV_WRITABLE_DIRS=/opt/venv
WORKDIR /workspace/services/quiz-engine-service
COPY gen/python /workspace/gen/python
COPY services/quiz-engine-service/pyproject.toml services/quiz-engine-service/uv.lock ./
RUN uv sync --frozen --no-install-project
COPY services/quiz-engine-service/src src
RUN uv sync --frozen && chown -R dev:dev /opt/venv /home/dev
COPY infra/docker/dev-entrypoint.sh /usr/local/bin/dev-entrypoint
EXPOSE 9093
ENTRYPOINT ["dev-entrypoint"]
CMD ["uv", "run", "watchfiles", "python -m quiz_engine", "src", "../../gen/python"]

# ---------- build: the virtualenv for prod, without dev tools ----------
FROM python:3.12.15-slim AS build
RUN pip install --no-cache-dir uv==0.13.5
ENV UV_PROJECT_ENVIRONMENT=/opt/venv UV_LINK_MODE=copy UV_PYTHON_DOWNLOADS=never
WORKDIR /workspace/services/quiz-engine-service
COPY gen/python /workspace/gen/python
COPY services/quiz-engine-service/pyproject.toml services/quiz-engine-service/uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project --no-editable
COPY services/quiz-engine-service/src src
RUN uv sync --frozen --no-dev --no-editable

# ---------- prod: slim runtime, non-root (nothing uses it yet) ----------
FROM python:3.12.15-slim AS prod
RUN useradd --system --uid 10001 app
COPY --from=build /opt/venv /opt/venv
ENV PYTHONUNBUFFERED=1
USER app
EXPOSE 9093
ENTRYPOINT ["/opt/venv/bin/python", "-m", "quiz_engine"]
