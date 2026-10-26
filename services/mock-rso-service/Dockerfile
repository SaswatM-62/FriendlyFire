# syntax=docker/dockerfile:1
# Build from the repo root: docker build -f services/mock-rso-service/Dockerfile --target dev .
# Versions match mise.toml (go, golangci-lint).

# ---------- dev: full toolchain, hot reload, source mounted by compose.yaml ----------
FROM golang:1.27.3 AS dev
ARG TARGETARCH
ARG GOLANGCI_LINT_VERSION=2.15.0
ARG AIR_VERSION=1.67.4
RUN curl -fsSL "https://github.com/golangci/golangci-lint/releases/download/v${GOLANGCI_LINT_VERSION}/golangci-lint-${GOLANGCI_LINT_VERSION}-linux-${TARGETARCH}.tar.gz" \
      | tar -xz -C /tmp \
    && mv /tmp/golangci-lint-*/golangci-lint /usr/local/bin/ \
    && rm -rf /tmp/golangci-lint-*
ENV GOTOOLCHAIN=local \
    GOWORK=off \
    GOFLAGS=-buildvcs=false \
    GOMODCACHE=/go/pkg/mod \
    GOCACHE=/go/build-cache \
    GOLANGCI_LINT_CACHE=/go/build-cache/golangci-lint \
    HOME=/home/dev \
    DEV_WRITABLE_DIRS="/go/pkg/mod /go/build-cache"
RUN go install github.com/air-verse/air@v${AIR_VERSION}
# Tools run as "dev" (uid 1000); dev-entrypoint switches to the owner of your checkout.
RUN useradd --create-home --home-dir /home/dev --uid 1000 dev
WORKDIR /workspace/services/mock-rso-service
COPY services/mock-rso-service/go.mod services/mock-rso-service/go.sum ./
RUN go mod download && mkdir -p /go/build-cache && chown -R dev:dev /go/pkg/mod /go/build-cache /home/dev
COPY infra/docker/dev-entrypoint.sh /usr/local/bin/dev-entrypoint
EXPOSE 8090
ENTRYPOINT ["dev-entrypoint"]
CMD ["air"]

# ---------- build: a static binary for prod ----------
FROM golang:1.27.3 AS build
ENV GOTOOLCHAIN=local GOWORK=off CGO_ENABLED=0
WORKDIR /workspace/services/mock-rso-service
COPY services/mock-rso-service/go.mod services/mock-rso-service/go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod go mod download
COPY services/mock-rso-service ./
RUN --mount=type=cache,target=/go/pkg/mod --mount=type=cache,target=/root/.cache/go-build \
    go build -trimpath -ldflags="-s -w" -o /out/mock-rso-service ./cmd/mock-rso-service

# ---------- prod: just the binary, non-root (nothing uses it yet) ----------
FROM scratch AS prod
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=build /out/mock-rso-service /mock-rso-service
USER 65532:65532
EXPOSE 8090
ENTRYPOINT ["/mock-rso-service"]
