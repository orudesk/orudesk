// Packs the publishable packages and proves an ordinary app can use them (Day 62). See package-test/README.md.
//   pnpm test:packages
import { execFileSync } from 'node:child_process';
import { cpSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';

const web = resolve(import.meta.dirname, '..');
const run = (cmd, args, cwd) => execFileSync(cmd, args, { cwd, stdio: 'inherit' });
const readJson = (path) => JSON.parse(readFileSync(path, 'utf8'));

const work = mkdtempSync(join(tmpdir(), 'orudesk-packages-'));
const packs = join(work, 'packs');
const app = join(work, 'app');
try {
  for (const pkg of ['api-client', 'web-core']) {
    const dir = join(web, 'packages', pkg);
    run('pnpm', ['build'], dir);
    run('pnpm', ['pack', '--pack-destination', packs], dir);
  }
  const tarball = (name) =>
    join(
      packs,
      readdirSync(packs).find((f) => f.startsWith(`orudesk-${name}-`)),
    );

  cpSync(join(web, 'package-test'), app, { recursive: true });
  cpSync(join(web, 'tsconfig.base.json'), join(work, 'tsconfig.base.json'));

  // The app's own dependencies: what web-core asks its users for (its peers), at the versions the workspace uses
  const core = readJson(join(web, 'packages', 'web-core', 'package.json'));
  const dev = core.devDependencies;
  const pick = (...names) => Object.fromEntries(names.map((n) => [n, dev[n]]));
  writeFileSync(
    join(app, 'package.json'),
    JSON.stringify(
      {
        name: 'orudesk-package-test',
        private: true,
        type: 'module',
        dependencies: {
          '@orudesk/api-client': `file:${tarball('api-client')}`,
          '@orudesk/web-core': `file:${tarball('web-core')}`,
          ...pick(
            '@mantine/core',
            '@mantine/hooks',
            '@tanstack/react-query',
            'react',
            'react-dom',
            'react-router',
          ),
        },
        devDependencies: pick(
          '@testing-library/jest-dom',
          '@testing-library/react',
          '@types/react',
          '@types/react-dom',
          '@vitejs/plugin-react',
          'jsdom',
          'typescript',
          'vite',
          'vitest',
        ),
      },
      null,
      2,
    ),
  );

  run('npm', ['install', '--no-audit', '--no-fund', '--loglevel=error'], app);
  run('npx', ['tsc', '-p', 'tsconfig.json', '--noEmit'], app);
  run('npx', ['vitest', 'run'], app);
  run('npx', ['vite', 'build', '--logLevel', 'warn'], app);

  const assets = join(app, 'dist', 'assets');
  const css = readdirSync(assets)
    .filter((f) => f.endsWith('.css'))
    .map((f) => readFileSync(join(assets, f), 'utf8'))
    .join('');
  if (!css.includes('.skip-link'))
    throw new Error("web-core's styles.css is missing from the built app");
  if (!css.includes('--mantine-color'))
    throw new Error("Mantine's styles are missing from the built app");
  console.log('\nThe packed packages work in an app outside the workspace.');
} finally {
  rmSync(work, { recursive: true, force: true });
}
