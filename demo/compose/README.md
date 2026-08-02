# Docker Compose demo

Runs Allocra as two containers — PostgreSQL + the app — with **persistent** data, so it's a
good fit for a longer-lived or shareable demo. Same exploration surface as the embedded demo.

## Prerequisites
- Docker (with the Compose plugin: `docker compose`).

## Run
```bash
./demo/compose/run.sh
```
(or, from this folder: `docker compose up --build`)

The app comes up on `http://localhost:8080`. The **cheat sheet** (tenant id, tokens, sample
ids, curl examples) is printed in the app logs:
```bash
docker compose -f demo/compose/docker-compose.yml logs app
```

## Explore
- **Swagger UI:** http://localhost:8080/swagger-ui
- Headers on every call: `Authorization: Bearer <admin|scheduler|viewer>` and
  `X-Tenant-Id: <tenant id>` (from the cheat sheet). Same seeded "Demo Physio Clinic" as the
  embedded demo. See [`../embedded/README.md`](../embedded/README.md) for a suggested tour.

## Persistence & lifecycle
- Data is stored in the `allocra_pgdata` volume and **survives restarts**. The seeder is
  **idempotent** (keyed on the demo tenant), so restarting reuses the existing data.
- Stop: `Ctrl+C`, or `docker compose down` (keeps the volume).
- Wipe everything (fresh seed next time): `docker compose down -v`.

## How it works
The app container is built from the repo-root `Dockerfile` and started with
`SPRING_PROFILES_ACTIVE=demo` pointing at the `postgres` service; Flyway migrates on startup
and the `demo`-profile seeder populates + prints the cheat sheet. CORS is preset for
`localhost:3000` and `localhost:5173`.

For a zero-Docker option, use [`../embedded`](../embedded) instead.
