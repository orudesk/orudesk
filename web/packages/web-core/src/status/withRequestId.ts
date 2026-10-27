export const REQUEST_ID_HEADER = 'X-Request-Id';

/**
 * Names every request, so a failure can be found in the logs: the browser, Caddy and the API all log the same id
 * (the API's RequestIdFilter keeps it). The outermost fetch layer, so a retry after a refreshed token keeps the
 * id of the user's action.
 */
export function withRequestId(
  next: (request: Request) => Promise<Response>,
  newId: () => string = randomId,
): (request: Request) => Promise<Response> {
  return (request) => {
    if (!request.headers.has(REQUEST_ID_HEADER)) request.headers.set(REQUEST_ID_HEADER, newId());
    return next(request);
  };
}

/** crypto.randomUUID exists only on HTTPS and localhost; getRandomValues everywhere. */
function randomId(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  return Array.from(crypto.getRandomValues(new Uint8Array(16)), (b) =>
    b.toString(16).padStart(2, '0'),
  ).join('');
}
