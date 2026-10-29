# Changelog

Every release of the Orudesk core, newest first. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html): before 1.0.0, a minor release may break things.

## [Unreleased]

### Added

- Sessions: each login is a session that lives through every token refresh. `GET /api/v1/users/me/sessions` lists
  where the user is signed in (device, address, when), `DELETE /api/v1/users/me/sessions/{id}` signs one out and
  `POST /api/v1/users/me/sessions/end-others` signs out everywhere else; both are audited. Access tokens name their
  session (`sid`), and a token whose session has ended (logout, sign-out from another device, password reset) is
  refused at once instead of when it expires. Behind a proxy on a private network the API now reads the client's
  address from `X-Forwarded-For` (`server.forward-headers-strategy: native`).
- The web packages are published to npm with each release, with the release's version:
  `@orudesk/web-core` and `@orudesk/api-client`. An edition's web app can depend on them instead of checking
  out the core next to itself.
- Request ids: every response has an `X-Request-Id` (the caller's, if it sent a plain one of 8-64 characters,
  else a new UUID), and every log line written for the request carries it (`request_id`). The web app sends
  one with each request, and Caddy logs each request as JSON.
- Prometheus metrics at `/actuator/prometheus`, for whoever sends `Authorization: Bearer <ORUDESK_METRICS_TOKEN>`
  (unset: nobody). Every metric has the tag `application`.
- An audit log (new module `orudesk-audit`): registering a user, giving or taking away a role, creating an
  organisation unit and registering a person are recorded with who did it, when, and the request id.
  `GET /api/v1/audit-entries` (admins; `subjectId` for one record's history, newest first, paged with
  `after`). The database refuses to change or delete an entry. Editions add their own events by publishing an
  `AuditableEvent`.
- The web app has an Activity page (admins), and a person's page links to their history.
- Login limit: after 5 failed logins for an email (known or not) within 15 minutes, that email gets `429 Too
  Many Requests` with `Retry-After` until the 15 minutes are up, even with the right password. Counted in Redis,
  so every API instance shares it. `ORUDESK_LOGIN_MAX_FAILURES` and `ORUDESK_LOGIN_WINDOW` change the policy.
  The login page says how long to wait.
- API responses under `/api` carry `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` and
  `Referrer-Policy: no-referrer`. Caddy adds `Strict-Transport-Security` (browsers apply it on HTTPS only) and
  `Cross-Origin-Opener-Policy`, and no longer replaces the API's own headers.
- web-core, for modules: a menu entry can ask for `rolesAnywhere` (a role tenant-wide or at any unit, like an
  admissions officer at one school), and `useCurrentUnitAccess` says what the current unit is and the user's
  roles there.
- CI: CodeQL (Java, TypeScript and the workflows) on every pull request and weekly, and a dependency review that
  fails a pull request adding a dependency with a known high or critical vulnerability. Dependabot also watches
  the Docker base images.

### Changed

- **web-core no longer imports CSS.** An app's `main.tsx` imports `@mantine/core/styles.css` and
  `@orudesk/web-core/styles.css` itself (the editions are updated).
- In Docker Compose the API logs JSON lines (Elastic Common Schema); `ORUDESK_LOG_FORMAT` picks another format.

## [1.1.0] - 2026-10-26

The MVP: the web app. An admin signs in, picks the unit they work in, manages users' roles and the people
registry, from a browser, and `docker/demo.env` runs it all with sample data in one command.

### Added

- `web/`: the web app's workspace (React, TypeScript, Vite), and `@orudesk/api-client`, a typed client
  generated from the OpenAPI contract.
- Refresh token as a cookie: a client that sends `X-Refresh-Token-Transport: cookie` gets the refresh token
  as an HttpOnly, Secure, SameSite=Strict cookie (path `/api/v1/auth`) instead of in the JSON body, and
  `/refresh` and `/logout` read it from there. Clients that don't send the header work as before.
- CORS, off by default: `ORUDESK_WEB_ALLOWED_ORIGINS` lists other origins whose pages may call the API.
- `GET /api/v1/users` (admins): the tenant's users with their grants, sorted by email, searchable by part of
  the address (`q`), paged with a cursor (`after`, `size` up to 100).
- `GET /api/v1/roles`: the roles there are, with names and descriptions.
- `DELETE /api/v1/users/{userId}/roles/{role}` (optional `orgUnitId`): takes one grant away, with the same
  rule as assigning about who may do it. 409 if the user would be left without a role, or if an admin tries to
  remove their own tenant-wide ADMIN role.
- Web app: sign-in, the app shell, the org unit switcher, a Users screen for admins where they give and
  take away roles, and People screens (search, add with the server's field errors, one person's page), a banner while the API is down, error pages
  that keep the menu, and a notice when the session has ended.
- A web image (`ghcr.io/orudesk/orudesk-standard-web`): the web app served by Caddy, which also passes `/api`
  to the API, sets security headers (CSP, no framing) and caching, and gets an HTTPS certificate when
  `ORUDESK_SITE` is a domain. Compose runs it on port 8090 with the `app` profile.
- A one-command demo with sample data: `docker/demo.env` and the `demo` Spring profile (two schools, a clinic,
  users with scoped roles, twelve people), loaded through the use cases by each module.
- `SystemActor.runAs(tenant, work)`: work no user started runs inside one tenant, through the same checks as
  a request.
- End-to-end smoke tests (`web/e2e`, Playwright) against the demo stack, at desktop and phone sizes, in CI.
- `web/packages/web-core`: the web app's shell and core screens as a package, and a module contract
  (`OrudeskModule`: routes and menu entries) through which editions add their own screens.

### Changed

- The unit switcher starts a user without a remembered unit in the first unit they have a role in (a school
  principal in their school), not at the top of the tree.
- A database that goes away mid-request, or a pool that can't get a connection, now answers 503 with
  `Retry-After` (it was a 500 without a problem body). The standard app waits at most 5 s for a database
  connection (`spring.datasource.hikari.connection-timeout`), not 30 s.
- The contract says 201 Created (not 200) for `POST /api/v1/auth/register`, `/api/v1/org-units` and
  `/api/v1/people`, as the API has always answered.
- Assigning a role to a disabled user answers 409 "Role change not allowed" instead of 500.
- The OpenAPI contract says which fields are always present: record components are required unless they
  are `@Nullable` (JSpecify), and nullable ones have type `[..., "null"]`. The contract is sorted and
  indented, so it diffs cleanly.
- `TokenResponse.refreshToken` is optional in the contract (absent for cookie clients), and the bodies of
  `/refresh` and `/logout` are optional (cookie clients send none).

## [1.0.0] - 2026-10-26

Backend release 1. From this version on, the public API (the HTTP endpoints, and the published modules'
public Java packages) changes incompatibly only in a new major version.

### Added

- **Docker image** for the standard edition (`ghcr.io/orudesk/orudesk-standard`), built from source by
  `api/Dockerfile`, running as a non-root user. `docker compose --profile app` runs the whole product.
- **Health probes**: `/actuator/health/liveness` and `/actuator/health/readiness` for Docker and Kubernetes.
- `RoleDefinition`: a module or vertical declares the roles it needs as beans, and identity adds them at
  startup.

### Fixed

- The people registry's tables are now always created before a vertical's tables, which point at them.

## [0.1.0] - 2026-10-26

The first release: the shared core that the Orudesk verticals (NGO, school, hospital) build on.

### Added

- **Login**: register, log in with email and password, short-lived access tokens (JWT) and refresh tokens
  that rotate and can be revoked.
- **Tenants**: every organisation's data is kept apart in one database (`tenant_id` on every row).
- **Roles**: users have one or more roles, for the whole tenant or for one org unit and everything below it.
- **Org units**: each tenant is a tree of units (group, schools, branches, departments).
- **People registry**: register, view and search people (by part of a name, phone or email), with paging.
- **API contract**: OpenAPI at `/v3/api-docs` with stable operation names, for generated clients.
- `orudesk-bom`, so a vertical writes one version number for all core modules.

[Unreleased]: https://github.com/orudesk/orudesk/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/orudesk/orudesk/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/orudesk/orudesk/compare/v0.1.0...v1.0.0
[0.1.0]: https://github.com/orudesk/orudesk/releases/tag/v0.1.0
