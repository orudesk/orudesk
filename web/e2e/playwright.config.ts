import { defineConfig, devices } from '@playwright/test';

/**
 * The smoke test runs against the product as a user gets it: the demo in Docker Compose (Caddy, the API,
 * PostgreSQL, Redis), started before the tests. Not part of `pnpm check`: it needs the whole stack.
 *
 *   docker compose --env-file docker/demo.env -f docker/compose.yaml --profile app up -d --build --wait
 *   pnpm --filter @orudesk/e2e e2e
 */
export default defineConfig({
  testDir: 'tests',
  // One worker: the tests share one demo database and some of them write to it
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.ORUDESK_URL ?? 'http://localhost:8090',
    trace: 'retain-on-failure', // a failed run in CI leaves a trace to replay step by step
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'phone', use: { ...devices['Pixel 7'] } },
  ],
});
