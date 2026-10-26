# syntax=docker/dockerfile:1
# Build from the repo root: docker build -f services/room-service/Dockerfile --target dev .
# The Java version matches mise.toml: java = "temurin-21.0.12+101.0.LTS" is Docker's
# eclipse-temurin:21.0.12.1_1.

# ---------- dev: full toolchain, hot reload, source mounted by compose.yaml ----------
FROM eclipse-temurin:21.0.12.1_1-jdk AS dev
ARG TARGETARCH
ARG WATCHEXEC_VERSION=2.8.0
ARG GRPC_HEALTH_PROBE_VERSION=0.4.60
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl xz-utils \
    && rm -rf /var/lib/apt/lists/*
RUN case "$TARGETARCH" in \
      amd64) arch=x86_64 ;; \
      arm64) arch=aarch64 ;; \
      *) echo "unsupported architecture: $TARGETARCH" >&2; exit 1 ;; \
    esac \
    && curl -fsSL "https://github.com/watchexec/watchexec/releases/download/v${WATCHEXEC_VERSION}/watchexec-${WATCHEXEC_VERSION}-${arch}-unknown-linux-musl.tar.xz" \
      | tar -xJ -C /tmp \
    && mv /tmp/watchexec-*/watchexec /usr/local/bin/ \
    && rm -rf /tmp/watchexec-* \
    && curl -fsSL -o /usr/local/bin/grpc_health_probe \
      "https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/v${GRPC_HEALTH_PROBE_VERSION}/grpc_health_probe-linux-${TARGETARCH}" \
    && chmod +x /usr/local/bin/grpc_health_probe
# Tools run as "dev" (uid 1000); dev-entrypoint switches to the owner of your checkout.
RUN userdel --remove ubuntu 2>/dev/null || true; useradd --create-home --home-dir /home/dev --uid 1000 dev
ENV HOME=/home/dev \
    DEV_WRITABLE_DIRS=/home/dev/.m2
WORKDIR /workspace/services/room-service
COPY --chown=dev:dev services/room-service/mvnw services/room-service/pom.xml ./
COPY --chown=dev:dev services/room-service/.mvn .mvn
# Download Maven and the dependencies now; the shared maven_repo volume starts from them.
USER dev
RUN ./mvnw -B -ntp -q dependency:go-offline
USER root
COPY infra/docker/dev-entrypoint.sh /usr/local/bin/dev-entrypoint
COPY infra/docker/java-dev.sh /usr/local/bin/java-dev
EXPOSE 9092
ENTRYPOINT ["dev-entrypoint"]
CMD ["java-dev"]

# ---------- build: compiles the jar for prod ----------
FROM eclipse-temurin:21.0.12.1_1-jdk AS build
WORKDIR /workspace/services/room-service
COPY services/room-service/mvnw services/room-service/pom.xml ./
COPY services/room-service/.mvn .mvn
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -q dependency:go-offline
COPY gen/java /workspace/gen/java
COPY services/room-service/src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -q -DskipTests package \
    && cp target/room-service-*.jar /tmp/app.jar

# ---------- prod: slim runtime, non-root (nothing uses it yet) ----------
FROM eclipse-temurin:21.0.12.1_1-jre AS prod
RUN userdel --remove ubuntu 2>/dev/null || true; useradd --system --uid 10001 app
COPY --from=build /tmp/app.jar /app/app.jar
USER app
EXPOSE 9092
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
