import { createOrudeskClient } from '@orudesk/api-client';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { fakeApi, json, tokens } from '../test/fakeApi';
import { authFetch } from './authFetch';
import { AuthSession } from './session';

async function loggedIn() {
  const session = new AuthSession(createOrudeskClient({ fetch: (r) => globalThis.fetch(r) }));
  await session.login('ck@orudesk.org', 'secret-password');
  return { session, api: createOrudeskClient({ fetch: authFetch(session) }) };
}

describe('authFetch', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('adds the access token to every request', async () => {
    const fake = fakeApi({
      'POST /api/v1/auth/login': () => tokens('the-token'),
      'GET /api/v1/users/me': () => json({ email: 'ck@orudesk.org' }),
    });
    const { api } = await loggedIn();

    await api.GET('/api/v1/users/me');

    expect(fake.requests[1]?.headers.get('Authorization')).toBe('Bearer the-token');
  });

  it('refreshes once and retries when the API says 401', async () => {
    let meCalls = 0;
    fakeApi({
      'POST /api/v1/auth/login': () => tokens('revoked'),
      'POST /api/v1/auth/refresh': () => tokens('fresh'),
      'GET /api/v1/users/me': (request) => {
        meCalls++;
        return request.headers.get('Authorization') === 'Bearer fresh'
          ? json({ email: 'ck@orudesk.org' })
          : json({ title: 'Unauthorized' }, 401);
      },
    });
    const { api } = await loggedIn();

    const { data } = await api.GET('/api/v1/users/me');

    expect(data?.email).toBe('ck@orudesk.org');
    expect(meCalls).toBe(2);
  });

  it('gives up after one retry, logging out', async () => {
    fakeApi({
      'POST /api/v1/auth/login': () => tokens(),
      'POST /api/v1/auth/refresh': () => json({ title: 'Invalid refresh token' }, 401),
      'GET /api/v1/users/me': () => json({ title: 'Unauthorized' }, 401),
    });
    const { api, session } = await loggedIn();

    const { response } = await api.GET('/api/v1/users/me');

    expect(response.status).toBe(401);
    expect(session.getStatus()).toBe('signedOut');
  });
});
