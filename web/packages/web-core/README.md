# @orudesk/web-core

The shell of every [Orudesk](https://github.com/orudesk/orudesk) web app: sign-in, the menu, the
organisation unit switcher, the core's screens (people, users, activity), and the hooks an edition's own
screens use. An edition is a `main.tsx` that lists its modules.

```bash
npm install @orudesk/web-core @mantine/core @mantine/hooks @tanstack/react-query react react-dom react-router
```

```tsx
// main.tsx
import '@mantine/core/styles.css';
import '@orudesk/web-core/styles.css';
import { coreModule, startOrudeskApp } from '@orudesk/web-core';
import { myModule } from './myModule';

startOrudeskApp([coreModule, myModule]);
```

React, Mantine, TanStack Query and React Router are **peer dependencies**: your app installs them, and the
package uses your copy, so there is only one React.

## For a module's pages

`useServices().fetch` calls the API with the access token and the current unit; `saveError`,
`ValidationError`, `EditConflictError` and `EditConflictAlert` turn the API's problem responses into form
errors; `usePageTitle`, `useMe`, `useCurrentUnit`, `formatDate` and the rest are listed in
[`src/index.ts`](src/index.ts). Anything not exported there is internal.

## Tests

```ts
// vite.config.ts
test: { environment: 'jsdom', setupFiles: ['@orudesk/web-core/test-setup'] }
```

```ts
import { fakeApi, json, tokens } from '@orudesk/web-core/testing';
```

`test-setup` adds the DOM matchers and the browser APIs Mantine needs in jsdom; `testing` fakes the API.
Both need `vitest` and `@testing-library/react` (and `@testing-library/jest-dom`), which are optional peers.

## Versions

Released with the Orudesk core, with the same version number: `@orudesk/web-core@1.2.0` goes with the API
of Orudesk 1.2.0. Removing or renaming anything exported from `src/index.ts`, or raising a peer dependency's
major version, is a breaking change and waits for a major release; `src/publicApi.test.ts` lists the exported
names so such a change is never an accident. See the [changelog](https://github.com/orudesk/orudesk/blob/main/CHANGELOG.md).

Licence: AGPL-3.0-only.
