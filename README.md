# Orudesk

**An open source ERP for organisations that serve people**: schools, hospitals and clinics, NGOs.
One core (login, roles, organisation units, a people registry) and an edition per sector built on it.

> Status: 1.1.0, the MVP: the API and the web app. Try it with the demo below.

## Editions

| Repository | Edition | First feature |
|---|---|---|
| [orudesk/orudesk](https://github.com/orudesk/orudesk) | **Core** and the standard edition | Login, roles, org units, people registry |
| [orudesk/orudesk-ngo](https://github.com/orudesk/orudesk-ngo) | NGOs | Donor registration |
| [orudesk/orudesk-school](https://github.com/orudesk/orudesk-school) | Schools | Student admission |
| [orudesk/orudesk-healthcare](https://github.com/orudesk/orudesk-healthcare) | Hospitals and clinics | Patient registration |

## Try it: the demo

You need only **Docker** (Docker Desktop, Colima or Podman) and this repository. One command:

```bash
git clone https://github.com/orudesk/orudesk.git
cd orudesk
docker compose --env-file docker/demo.env -f docker/compose.yaml --profile app up -d --build --wait
```

Open <http://localhost:8090> and log in as `admin@demo.orudesk.org` with the password `demo-password`. The
demo has two schools and a clinic, a dozen people, and users with roles in one school only
(`principal@demo.orudesk.org`, same password). Remove it with
`docker compose -f docker/compose.yaml --profile app down -v`.

## Quick start: run it

You need only **Docker** (Docker Desktop, Colima or Podman) and this repository.

```bash
git clone https://github.com/orudesk/orudesk.git
cd orudesk
cp api/.env.example api/.env        # then set DB_PASSWORD, ORUDESK_JWT_SECRET and the first admin
docker compose --env-file api/.env -f docker/compose.yaml --profile app up -d --build --wait
```

The first build takes a few minutes (it compiles the API and the web app inside Docker). Then Orudesk is on
<http://localhost:8090> (the API alone on <http://localhost:8081>); `--wait` returns once both are healthy. Instead of building,
you can run a released image: set `ORUDESK_VERSION=1.1.0` in `api/.env` and leave out `--build`.

Stop it with `docker compose -f docker/compose.yaml --profile app down` (add `-v` to delete the data too).

## Quick start: develop it

You need **JDK 25** and **Docker** (Docker Desktop, Colima or Podman). Nothing else: PostgreSQL and Redis
run in containers.

```bash
git clone https://github.com/orudesk/orudesk.git
cd orudesk
cp api/.env.example api/.env        # then set DB_PASSWORD, ORUDESK_JWT_SECRET and the first admin
docker compose --env-file api/.env -f docker/compose.yaml up -d --wait
cd api
./gradlew bootRun
```

The API is on the `SERVER_PORT` from your `.env` (8080 by default). Log in as the first admin:

```bash
curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email": "<ORUDESK_ADMIN_EMAIL>", "password": "<ORUDESK_ADMIN_PASSWORD>"}'
```

Interactive API docs: <http://localhost:8080/swagger-ui.html>. The OpenAPI contract: `/v3/api-docs`.

## Running it in production

- The image runs as a non-root user and sizes its heap from the container's memory limit.
- `/actuator/health/liveness` (is the process alive?) and `/actuator/health/readiness` (may it get traffic?
  this one checks the database) need no token and say only UP or DOWN. Point your orchestrator at them.
- Back up the PostgreSQL volume. Redis holds only refresh tokens and caches: losing it logs everyone out,
  nothing more.
- HTTPS: set `ORUDESK_SITE=erp.example.org` (your domain, pointing at the server) and publish ports 80 and
  443 for the `web` service; Caddy gets and renews the certificate. Outside `localhost` the site must be
  HTTPS, because the login cookie is `Secure`.
- `web` is the only service that needs to be reachable from outside; the API's port can stay closed.
- **Logs** are JSON lines (Elastic Common Schema) in containers; `ORUDESK_LOG_FORMAT=logstash` or `gelf` if your
  log tool prefers those. Every request has an id: the web app sends `X-Request-Id`, the API logs it as
  `request_id` and returns it, and Caddy's access log has it too. Find one request with
  `docker compose -f docker/compose.yaml logs app | grep <id>`.
- **Metrics** for Prometheus at `/actuator/prometheus` on the API (port 8080 inside the network), readable with
  `ORUDESK_METRICS_TOKEN`:

  ```yaml
  scrape_configs:
    - job_name: orudesk
      metrics_path: /actuator/prometheus
      authorization: { credentials_file: /etc/prometheus/orudesk-metrics-token }
      static_configs: [{ targets: ['app:8080'] }]
  ```

  Useful series: `http_server_requests_seconds` (by `uri`, `status`), `hikaricp_connections_pending`,
  `jvm_memory_used_bytes`. Failed logins per minute:
  `sum(rate(http_server_requests_seconds_count{uri="/api/v1/auth/login",status="401"}[5m])) * 60`.

## Run the tests

```bash
cd api
./gradlew test       # needs Docker: Testcontainers starts a throwaway PostgreSQL and Redis
```

## The web app

`web/` holds the web app (React, TypeScript, Vite), as a pnpm workspace. With the API running on 8080:

```bash
cd web
corepack enable      # once: gives you the pnpm version the workspace asks for
pnpm install
pnpm dev             # http://localhost:5173
```

See [web/README.md](web/README.md).

## Build your own edition on the core

The core modules are published to Maven Central. In your Gradle build, import the BOM and then name the
modules you need, without versions:

```groovy
dependencies {
    implementation platform('com.orudesk:orudesk-bom:1.1.0')
    implementation 'com.orudesk:orudesk-platform-core'
    implementation 'com.orudesk:orudesk-identity'
    implementation 'com.orudesk:orudesk-party'
}
```

The BOM also fixes the Spring Boot version the core was built and tested with. Releases are listed in
[CHANGELOG.md](CHANGELOG.md).

## How it's built

- **Java 25, Spring Boot 4**, PostgreSQL 17, Redis 7. Gradle, one module per feature.
- A **modular monolith**: `platform/` (shared plumbing and identity), `modules/` (features such as the
  people registry), `apps/` (runnable editions). Spring Modulith checks that modules use each other only
  through their public APIs.
- **Clean Architecture** inside each module: `domain` (plain Java rules), `application` (use cases and
  ports), `adapter` (web, database, Redis).
- **Multi-tenant**: one database, every row carries its tenant, and every query is filtered by it.
  Inside a tenant, organisation units (a group, its schools, a clinic) and roles scoped to them.

## Contributing

Bug reports, ideas and pull requests are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md).
Please report security problems privately, as described in [SECURITY.md](SECURITY.md).

## Licence

Orudesk is free software under the **GNU Affero General Public License v3.0** ([LICENSE](LICENSE)).
You may use, study, change and share it. If you run a modified version for others over a network, you
must offer them its source code.
