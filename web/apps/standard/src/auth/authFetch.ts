import type { AuthSession } from './session';

/**
 * The fetch the app's API client uses: adds the access token to every request, and when the API still answers
 * 401 (the token was revoked, or the clock is off), refreshes once and tries again.
 */
export function authFetch(session: AuthSession): (request: Request) => Promise<Response> {
  async function send(request: Request): Promise<Response> {
    const token = await session.validAccessToken();
    if (token) request.headers.set('Authorization', `Bearer ${token}`);
    return globalThis.fetch(request);
  }

  return async (request) => {
    const retry = request.clone(); // a body can be read only once, so keep a copy for the second try
    const response = await send(request);
    if (response.status !== 401 || session.getStatus() !== 'signedIn') return response;
    return (await session.refresh()) ? send(retry) : response;
  };
}
