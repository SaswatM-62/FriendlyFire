#!/usr/bin/env bash
# Checks that every place that repeats a tool version agrees with mise.toml, the single
# source of truth: Dockerfiles, go.mod / go.work, pom.xml, package.json and CI.
# Part of `make lint`. Works with the bash 3.2 that ships with macOS.
set -euo pipefail
cd "$(dirname "$0")/.."

failures=0

tool() {
  sed -n "s/^$1 *= *\"\(.*\)\"/\1/p" mise.toml
}

# expect <file> <fixed string> <what>: the file must contain the string.
expect() {
  if ! grep -qF -- "$2" "$1"; then
    echo "MISMATCH: $1 should contain '$2' ($3 in mise.toml)"
    failures=$((failures + 1))
  fi
}

java=$(tool java)            # e.g. temurin-21.0.12+101.0.LTS
go=$(tool go)
python=$(tool python)
uv=$(tool uv)
node=$(tool node)
golangci=$(tool golangci-lint)

# Temurin's Docker tags write build 101 of 21.0.12 as 21.0.12.1_1.
java_version=${java#temurin-}
java_version=${java_version%%.LTS}
java_version=${java_version%.0}
java_semver=${java_version%%+*}
java_build=${java_version#*+}
if [ "$java_build" -ge 100 ]; then
  java_tag="${java_semver}.$((java_build / 100))_$((java_build % 100))"
else
  java_tag="${java_semver}_${java_build}"
fi
java_major=${java_semver%%.*}

for svc in gateway-service account-service room-service; do
  expect "services/$svc/Dockerfile" "eclipse-temurin:${java_tag}-jdk" "java = $java"
  expect "services/$svc/Dockerfile" "eclipse-temurin:${java_tag}-jre" "java = $java"
  expect "services/$svc/pom.xml" "<java.version>${java_major}</java.version>" "java = $java"
done

for svc in match-data-service mock-rso-service; do
  expect "services/$svc/Dockerfile" "golang:${go}" "go = $go"
  expect "services/$svc/Dockerfile" "GOLANGCI_LINT_VERSION=${golangci}" "golangci-lint = $golangci"
  expect "services/$svc/go.mod" "go ${go}" "go = $go"
done
expect gen/go/go.mod "go ${go}" "go = $go"
expect go.work "go ${go}" "go = $go"

for svc in quiz-engine-service scoring-service; do
  expect "services/$svc/Dockerfile" "python:${python}-slim" "python = $python"
  expect "services/$svc/Dockerfile" "uv==${uv}" "uv = $uv"
done

expect frontend/Dockerfile "node:${node}-slim" "node = $node"
expect frontend/package.json "\"node\": \"${node}\"" "node = $node"

for workflow in .github/workflows/*.yml; do
  [ -f "$workflow" ] || continue
  if grep -qE 'actions/setup-(java|go|node|python)' "$workflow"; then
    echo "MISMATCH: $workflow should install tools with jdx/mise-action, not actions/setup-*"
    failures=$((failures + 1))
  fi
done

if [ "$failures" -gt 0 ]; then
  echo "$failures version mismatch(es). mise.toml is the source of truth."
  exit 1
fi
echo "Versions match mise.toml."
