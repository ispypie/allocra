#!/usr/bin/env bash
#
# Starts the Allocra embedded demo — a real PostgreSQL is downloaded and run in-process
# (no Docker required), the schema is migrated, sample data is seeded, and the app starts
# on http://localhost:8080. A cheat sheet (tenant id, tokens, sample ids, curl examples)
# is printed once it is ready. Press Ctrl+C to stop; the database is discarded on exit.
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

echo "Starting Allocra embedded demo (no Docker needed)."
echo "First run downloads a PostgreSQL binary and project dependencies — please be patient."
echo

exec ./mvnw -q -Pdemo -pl demo/embedded -am spring-boot:run
