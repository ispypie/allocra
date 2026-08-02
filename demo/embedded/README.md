# Embedded demo (no Docker)

Run the whole Allocra backend on your machine with **one command** — no Docker, no Firebase,
no separate PostgreSQL install. A real PostgreSQL is downloaded and run in-process, the schema
is migrated, a sample clinic is seeded, and the API starts on `http://localhost:8080`.

## Prerequisites
- JDK 21+ (a newer JDK is fine; the build targets Java 21).
- Internet access on first run (to download a PostgreSQL binary + dependencies).

## Run
```bash
./demo/embedded/run.sh
```
(or, equivalently, from the repo root: `./mvnw -Pdemo -pl demo/embedded -am spring-boot:run`)

When it's ready it prints a **cheat sheet** with the tenant id, tokens, sample ids and
ready-to-paste `curl` commands. Press **Ctrl+C** to stop — the database is discarded on exit
(every run starts clean).

## Explore
Open the interactive API docs:

- **Swagger UI:** http://localhost:8080/swagger-ui
- OpenAPI spec: http://localhost:8080/v3/api-docs

Every call needs two headers (shown in the cheat sheet):
- `Authorization: Bearer <token>` — in the demo the **token is the user id**. Use `admin`,
  `scheduler`, or `viewer`. (This is a local stub verifier; production uses Firebase.)
- `X-Tenant-Id: <tenant id>` — printed on startup.

In Swagger UI, click **Authorize** / use the "Try it out" buttons and add those two headers.

### A typical tour
1. `POST /v1/services/{serviceTypeId}/availability/search` — find bookable options.
2. `POST /v1/bookings` — confirm one (atomic; a clashing slot returns 409).
3. `GET /v1/bookings` and `GET /v1/bookings/{id}` — see what you created.
4. `POST /v1/bookings/{id}/reschedule`, `/cancel`, `/complete`, `/no-show` — lifecycle.
5. As `admin`, try the config endpoints (`/v1/resources`, `/v1/services`, …) to build your own.
6. Try `viewer` on a write endpoint to see a `403`, or a wrong `X-Tenant-Id` to see isolation.

## What's seeded
"Demo Physio Clinic" with: `admin` / `scheduler` / `viewer` users; two physios (one also
Sports-qualified), two rooms (one private), an ultrasound unit; each available every day
08:00–20:00; and a **Physio Session** service requiring a qualified physio + a room, with
optional ultrasound.

## Notes
- The embedded database is **ephemeral** (fresh each run). For a persistent/shareable instance,
  see [`../compose`](../compose) (Docker Compose).
- Port `8080` and allowed CORS origins (`localhost:3000`, `localhost:5173`) are set in
  `DemoEmbeddedApplication`.
