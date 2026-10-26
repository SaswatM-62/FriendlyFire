#!/bin/sh
# Dev loop of the frontend: reinstall node_modules (a Docker volume) when
# package-lock.json changed, then run Next.js's dev server, reachable from the host.
set -eu

if ! sha256sum -c --status node_modules/.package-lock.sha256 2>/dev/null; then
  npm ci --no-audit --no-fund
  sha256sum package-lock.json >node_modules/.package-lock.sha256
fi

exec npm run dev -- -H 0.0.0.0
