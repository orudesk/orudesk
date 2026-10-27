// The Orudesk API client. Types come from src/generated/schema.ts, which openapi-typescript writes from the
// API's OpenAPI contract (openapi/orudesk-standard.json). Never edit the generated file: change the Java
// code, then run `pnpm contract:sync`.
import createClient, { type Client, type ClientOptions, type Middleware } from 'openapi-fetch';
import type { components, operations, paths } from './generated/schema';

export type { components, operations, paths, Middleware };

/** The JSON shapes the API sends and receives, by name: `Schemas['PersonDetails']`. */
export type Schemas = components['schemas'];

export type OrudeskClient = Client<paths>;

/**
 * A client for one API. Every call is typed from the contract:
 *
 * ```ts
 * const { data, error } = await client.GET('/api/v1/people/{personId}', {
 *   params: { path: { personId } },
 * });
 * ```
 *
 * `baseUrl` defaults to the page's own origin: in development Vite proxies /api to the API, in production
 * Caddy does. (A full URL rather than '', because `new Request('/api/...')` only works in a browser.)
 */
export function createOrudeskClient(options: ClientOptions = {}): OrudeskClient {
  return createClient<paths>({ baseUrl: globalThis.location?.origin ?? '', ...options });
}
