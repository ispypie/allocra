# Allocra demo environments

Self-contained ways to run Allocra locally and explore it. Each environment lives in its own
folder with its own startup script and README, so they're independent and easy to add to.

| Environment | Docker? | Persistent? | Start | Status |
|-------------|---------|-------------|-------|--------|
| [`embedded/`](embedded) | No | No (fresh each run) | `./demo/embedded/run.sh` | ✅ Available |
| [`compose/`](compose) | Yes | Yes (idempotent seed) | `./demo/compose/run.sh` | ✅ Available |

Both run the **same** backend and are explored the same way — via **Swagger UI**
(`/swagger-ui`) using a `Bearer` token (the user id, in demo mode) and an `X-Tenant-Id`
header. Start with [`embedded/`](embedded) — it needs nothing but a JDK.
