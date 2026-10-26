# Changelog

Every release of the Orudesk core, newest first. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html): before 1.0.0, a minor release may break things.

## [Unreleased]

### Added

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
