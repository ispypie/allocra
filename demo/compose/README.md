# Docker Compose demo (planned)

🚧 **Not built yet.** This folder is a placeholder for a Docker-based demo:

- `postgres` + the Allocra app as containers, started with `docker compose up`.
- **Persistent** data (a named volume) and easy to **share** on a network.
- Same exploration surface as the embedded demo: Swagger UI at `/swagger-ui`, `Bearer`
  token + `X-Tenant-Id` header.

Until this lands, use the no-Docker [`../embedded`](../embedded) demo, which needs only a JDK.

Planned contents: `docker-compose.yml`, `run.sh`, and a README mirroring the embedded one
(seeded data + cheat sheet). Requires Docker installed.
