import { vi } from 'vitest';

type Handler = (request: Request) => Response | Promise<Response>;

/** A JSON response, the way the API sends them. */
export function json(body: unknown, status = 200, headers: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  });
}

export const tokens = (accessToken = 'access-1', expiresIn = 900) =>
  json({ accessToken, tokenType: 'Bearer', expiresIn, refreshToken: null });

/**
 * Replaces the browser's fetch with a fake API: handlers by "METHOD /path". Every request is recorded, and an
 * unexpected one fails the test with a 599, so a missing handler is easy to spot.
 */
export function fakeApi(handlers: Record<string, Handler>) {
  const requests: Request[] = [];
  const fetch = vi.fn(async (input: Request) => {
    requests.push(input.clone());
    const url = new URL(input.url);
    const handler = handlers[`${input.method} ${url.pathname}`];
    return handler
      ? handler(input)
      : json({ title: `No fake for ${input.method} ${url.pathname}` }, 599);
  });
  vi.stubGlobal('fetch', fetch);
  return {
    requests,
    calls: (key: string) =>
      requests.filter((r) => `${r.method} ${new URL(r.url).pathname}` === key).length,
  };
}
