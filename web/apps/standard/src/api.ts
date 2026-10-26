import { createOrudeskClient } from '@orudesk/api-client';

/**
 * The app's one API client. Same origin (the default), so in development Vite's proxy forwards /api to Spring
 * Boot. `fetch` is looked up on every call rather than once, so tests can replace it with vi.stubGlobal.
 */
export const api = createOrudeskClient({ fetch: (request) => globalThis.fetch(request) });
