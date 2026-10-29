# Orudesk web

The browser app for Orudesk, a [pnpm workspace](https://pnpm.io/workspaces):

```
web/
├── apps/
│   └── standard/        ← the standard edition: a main.tsx that lists its modules
└── packages/
    ├── api-client/      ← the typed API client, generated from the OpenAPI contract
    └── web-core/        ← the shell, sign-in, the unit switcher and the core's screens
e2e/                     ← end-to-end smoke tests (Playwright) against the demo in Docker
```

## Adding screens: modules

An edition's app is a list of **modules** (`OrudeskModule` in `packages/web-core/src/module.ts`): each
brings its pages and its menu entries, and the shell draws them.

```tsx
// orudesk-ngo/web/src/main.tsx
import { coreModule, startOrudeskApp } from '@orudesk/web-core';
import { ngoModule } from './ngoModule';

startOrudeskApp([coreModule, ngoModule]);
```

A module's pages use `useServices().fetch` for API calls: the access token, the current org unit and the
outage banner come with it. Two modules with a page at the same path stop the app at start.

The backend owns every business rule and validation; the web app shows data and forms, and displays
the API's errors. It never decides on its own whether something is allowed.

## Run it

You need Node 22.12+ (24 recommended, see `.nvmrc`) and the API on port 8080
(`./gradlew bootRun` in `api/`).

```bash
corepack enable          # once: installs the pnpm version named in package.json
pnpm install
pnpm dev                 # http://localhost:5173, with /api proxied to the API
```

Point the proxy somewhere else with `ORUDESK_API_URL=http://localhost:8081 pnpm dev` (for example, the
API running in Docker Compose).

## Check it

```bash
pnpm check               # Prettier, ESLint, TypeScript and the Vitest tests: what CI runs
pnpm build               # the production build, in apps/standard/dist
pnpm format              # fix formatting
pnpm test:packages       # pack web-core and api-client, and use them from an app outside the workspace
```

## End-to-end tests

`e2e/` drives a real browser through the whole product: the demo (Caddy, the API, PostgreSQL, Redis), at
desktop and phone sizes. They need the stack running, so they're not part of `pnpm check`:

```bash
docker compose --env-file docker/demo.env -f docker/compose.yaml --profile app up -d --build --wait   # repo root
pnpm --filter @orudesk/e2e exec playwright install chromium   # once
pnpm --filter @orudesk/e2e e2e
```
