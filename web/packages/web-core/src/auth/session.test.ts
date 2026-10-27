import { createOrudeskClient } from '@orudesk/api-client';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { fakeApi, json, tokens } from '../test/fakeApi';
import { AuthSession } from './session';

const newSession = (now: () => number = Date.now) =>
  new AuthSession(createOrudeskClient({ fetch: (r) => globalThis.fetch(r) }), now);

describe('AuthSession', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('logs in, asking for the refresh token as a cookie', async () => {
    const api = fakeApi({ 'POST /api/v1/auth/login': () => tokens('the-access-token') });
    const session = newSession();

    expect(await session.login('ck@orudesk.org', 'secret-password')).toEqual({ ok: true });

    expect(session.getStatus()).toBe('signedIn');
    expect(await session.validAccessToken()).toBe('the-access-token');
    expect(api.requests[0]?.headers.get('X-Refresh-Token-Transport')).toBe('cookie');
  });

  it('says why a login failed', async () => {
    fakeApi({ 'POST /api/v1/auth/login': () => json({ title: 'Login failed' }, 401) });
    const session = newSession();

    expect(await session.login('ck@orudesk.org', 'wrong')).toEqual({
      ok: false,
      reason: 'credentials',
    });
  });

  it('says how long to wait after too many failed logins', async () => {
    fakeApi({
      'POST /api/v1/auth/login': () =>
        json({ title: 'Too many login attempts' }, 429, { 'Retry-After': '754' }),
    });
    const session = newSession();

    expect(await session.login('ck@orudesk.org', 'guess')).toEqual({
      ok: false,
      reason: 'throttled',
      retryAfterSeconds: 754,
    });
  });

  it('restores the session from the refresh cookie after a page reload', async () => {
    fakeApi({ 'POST /api/v1/auth/refresh': () => tokens('restored') });
    const session = newSession();
    expect(session.getStatus()).toBe('restoring');

    await session.restore();

    expect(session.getStatus()).toBe('signedIn');
    expect(await session.validAccessToken()).toBe('restored');
  });

  it('is logged out when there is no valid refresh cookie', async () => {
    fakeApi({ 'POST /api/v1/auth/refresh': () => json({ title: 'Invalid refresh token' }, 401) });
    const session = newSession();

    await session.restore();

    expect(session.getStatus()).toBe('signedOut');
    expect(await session.validAccessToken()).toBeNull();
  });

  it('sends one refresh when many requests need a token at once', async () => {
    const api = fakeApi({ 'POST /api/v1/auth/refresh': () => tokens('fresh') });
    const session = newSession();

    const results = await Promise.all([1, 2, 3, 4, 5].map(() => session.validAccessToken()));

    expect(results).toEqual(['fresh', 'fresh', 'fresh', 'fresh', 'fresh']);
    expect(api.calls('POST /api/v1/auth/refresh')).toBe(1);
  });

  it('refreshes 30 seconds before the access token expires', async () => {
    let now = 0;
    const api = fakeApi({
      'POST /api/v1/auth/login': () => tokens('first', 900),
      'POST /api/v1/auth/refresh': () => tokens('second', 900),
    });
    const session = newSession(() => now);
    await session.login('ck@orudesk.org', 'secret-password');

    now = 869_000; // 31 s left
    expect(await session.validAccessToken()).toBe('first');
    now = 871_000; // 29 s left
    expect(await session.validAccessToken()).toBe('second');
    expect(api.calls('POST /api/v1/auth/refresh')).toBe(1);
  });

  it('forgets the token on logout, even when the API is unreachable', async () => {
    fakeApi({
      'POST /api/v1/auth/login': () => tokens(),
      'POST /api/v1/auth/logout': () => Promise.reject(new TypeError('Failed to fetch')),
    });
    const session = newSession();
    await session.login('ck@orudesk.org', 'secret-password');

    await session.logout();

    expect(session.getStatus()).toBe('signedOut');
    expect(await session.validAccessToken()).toBeNull();
  });

  describe('when a refresh fails', () => {
    // Logged in with a token that has already expired, so the next request must refresh
    async function loggedInThenExpired() {
      let time = 0;
      const session = newSession(() => time);
      fakeApi({ 'POST /api/v1/auth/login': () => tokens('first', 60) });
      await session.login('ck@orudesk.org', 'secret-password');
      time = 61_000;
      return session;
    }

    it('logs out and says the session expired when the API refuses the cookie', async () => {
      const session = await loggedInThenExpired();
      fakeApi({ 'POST /api/v1/auth/refresh': () => json({ title: 'Invalid refresh token' }, 401) });

      expect(await session.validAccessToken()).toBeNull();

      expect(session.getStatus()).toBe('signedOut');
      expect(session.getEndReason()).toBe('expired');
    });

    it('stays logged in while the API is down, and carries on when it is back', async () => {
      const session = await loggedInThenExpired();
      fakeApi({ 'POST /api/v1/auth/refresh': () => json({ title: 'Service unavailable' }, 503) });

      expect(await session.validAccessToken()).toBeNull();
      expect(session.getStatus()).toBe('signedIn');

      fakeApi({ 'POST /api/v1/auth/refresh': () => tokens('after-outage') });
      expect(await session.validAccessToken()).toBe('after-outage');
    });

    it('shows the login page with the reason when the API is unreachable at page load', async () => {
      vi.stubGlobal('fetch', () => Promise.reject(new TypeError('Failed to fetch')));
      const session = newSession();

      await session.restore();

      expect(session.getStatus()).toBe('signedOut');
      expect(session.getEndReason()).toBe('unreachable');
    });

    it('gives no reason after a plain logout', async () => {
      const session = await loggedInThenExpired();
      fakeApi({ 'POST /api/v1/auth/logout': () => new Response(null, { status: 204 }) });

      await session.logout();

      expect(session.getEndReason()).toBeNull();
    });
  });
});
