# Local Docker stack

From repo root [`compose.yml`](../compose.yml):

- **PostgreSQL 16**: default host port **`5432`** (override with `POSTGRES_PUBLISH_PORT` if occupied, e.g. `POSTGRES_PUBLISH_PORT=5433`).
- **pgAdmin**: <http://localhost:5050> — email `admin@localhost.localdomain`, password `admin`. A server `openfinova_local` is pre-registered (`postgres`, user/password `openfinova`).
- **banking-app** image (Jib → local Docker daemon): built automatically when you run **`./mvnw -pl banking-app -am package`** or **`install`** (the `docker` profile is **`activeByDefault`**). Produces **`openfinova/banking-app:local`** (`compose.yml` uses **`pull_policy: never`**). To skip building the image (no Docker, or faster builds): **`-Djib.skip=true`**.
- **Maven**: If you activate any profile with **`-Pname`** on the command line, **`activeByDefault`** profiles are turned off unless you add them explicitly, e.g. **`-Pdocker,myProfile`**.
- **dashboard**: built from sibling repo `../minab-banking-dashboard` with `NEXT_PUBLIC_*` URLs pointing at `http://localhost:8080` (browser-visible URLs).

Minimal DB-only startup:

```bash
docker compose up -d postgres pgadmin
```

Full stack (rebuild banking image when code changes):

```bash
./mvnw -pl banking-app -am package -DskipTests
docker compose up -d postgres pgadmin banking-app dashboard
```

Or only recreate services after a fresh Jib build:

```bash
docker compose up -d banking-app dashboard
```

If Jib fails with **401 Unauthorized** pulling `eclipse-temurin` from Docker Hub, your Docker config may be sending bad Hub credentials (anonymous pulls then fail). Run `docker logout` or fix `~/.docker/config.json`, then retry `./mvnw -pl banking-app -am package`.

Liquibase installs the baseline schema and **local-profile seed data** (`context=local`). Stage/prod use `liquibase.contexts` without `local`; tests use context `test` (no demo seed SQL).
