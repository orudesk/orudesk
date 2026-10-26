# Orudesk web

The browser app for Orudesk, a [pnpm workspace](https://pnpm.io/workspaces):

```
web/
├── apps/
│   └── standard/        ← the standard edition's app (Vite + React + TypeScript)
└── packages/            ← code shared by the apps (the API client from Day 28, more later)
```

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
```
