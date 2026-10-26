# Changelog

Every release of the Orudesk core, newest first. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html): before 1.0.0, a minor release may break things.

## [Unreleased]

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
- Web app: sign-in, the app shell, the org unit switcher, and a Users screen for admins.

### Changed

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

[Unreleased]: https://github.com/orudesk/orudesk/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/orudesk/orudesk/releases/tag/v0.1.0
