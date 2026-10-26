# syntax=docker/dockerfile:1
# Build from the repo root: docker build -f frontend/Dockerfile --target dev .
# The Node version matches mise.toml.

# ---------- dev: Next.js dev server, source mounted by compose.yaml ----------
FROM node:24.22.1-slim AS dev
# node_modules is a Docker volume (frontend_node_modules) that starts from this image.
ENV NEXT_TELEMETRY_DISABLED=1 \
    WATCHPACK_POLLING=true \
    HOME=/home/node \
    DEV_WRITABLE_DIRS=/workspace/frontend/node_modules
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund \
    && sha256sum package-lock.json >node_modules/.package-lock.sha256 \
    && chown -R node:node /workspace/frontend
COPY infra/docker/dev-entrypoint.sh /usr/local/bin/dev-entrypoint
COPY infra/docker/node-dev.sh /usr/local/bin/node-dev
EXPOSE 3000
ENTRYPOINT ["dev-entrypoint"]
CMD ["node-dev"]

# ---------- build: the standalone Next.js server for prod ----------
FROM node:24.22.1-slim AS build
ENV NEXT_TELEMETRY_DISABLED=1
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend ./
# NEXT_PUBLIC_* values are built into the page.
ARG NEXT_PUBLIC_GATEWAY_URL=http://localhost:8080
ARG NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
RUN npm run build

# ---------- prod: slim runtime, non-root (nothing uses it yet) ----------
FROM node:24.22.1-slim AS prod
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 HOSTNAME=0.0.0.0 PORT=3000
WORKDIR /app
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
USER node
EXPOSE 3000
CMD ["node", "server.js"]
