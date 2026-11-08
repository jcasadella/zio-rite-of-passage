# CLAUDE.md

Review Board app: Scala 3 + ZIO backend, Scala.js + Laminar frontend, PostgreSQL. See `README.md` for full setup and deployment.

## Modules

- `modules/common`: cross-compiled (JVM + JS) domain types and Tapir endpoint definitions shared by server and app.
- `modules/server`: ZIO HTTP backend (port 4041). Layers: `http/controllers` → `services` → `repositories` (Quill). Flyway migrations in `src/main/resources/db/migration`.
- `modules/app`: Scala.js frontend, bundled with Parcel.
- `modules/foundations`: course exercises, not part of the app.
- `build/staging`, `ansible/`, `terraform/`: staging deployment (run by GitHub Actions on `main`).

## Commands

```bash
sbt test                                                       # all tests (needs Docker: repository tests use Testcontainers)
sbt "server / runMain com.rockthejvm.reviewboard.Application"  # backend; start DB first with `docker-compose up`
sbt "app / Compile / fastLinkJS" && (cd modules/app && npm run dev)  # frontend dev server
sbt scalafmtAll                                                # format
```

The backend and tests require `STRIPE_SECRET_KEY`, `STRIPE_INVITES_WEBHOOK_SECRET` and `OPENAI_SECRET_KEY` in the environment (any dummy value works for tests).

## Conventions

- New endpoints: define in `common/.../http/endpoints`, implement in a server controller, register it in `HttpApi.makeControllers`.
- Schema changes: add a new `V<n>__*.sql` Flyway migration; never edit existing ones.
- Formatting follows `.scalafmt.conf` (max 100 columns). Markdown is linted by Vale (Google style) in CI.
- Never commit secrets; `.env*` files are local only.
