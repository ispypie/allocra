#!/usr/bin/env bash
#
# Starts the Allocra Compose demo: a PostgreSQL container + the app container. Data persists
# in a named volume across restarts; the seeder is idempotent so it only seeds once. The app
# is on http://localhost:8080. Stop with Ctrl+C (containers keep the volume); see below to wipe.
#
set -euo pipefail

cd "$(dirname "$0")"

echo "Starting Allocra Compose demo (Docker required). Building the app image on first run…"
echo "Once up, open http://localhost:8080/swagger-ui  —  cheat sheet: docker compose logs app"
echo

exec docker compose up --build
