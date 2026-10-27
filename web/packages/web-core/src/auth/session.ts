import type { OrudeskClient, Schemas } from '@orudesk/api-client';

export type SessionStatus = 'restoring' | 'signedIn' | 'signedOut';

/** Why the last session ended, for the login page to explain. null: never started, or the user logged out. */
export type EndReason = 'expired' | 'unreachable' | null;

export type LoginResult =
  | { ok: true }
  | { ok: false; reason: 'credentials' | 'unavailable' | 'network' | 'other'; message?: string }
  | { ok: false; reason: 'throttled'; retryAfterSeconds: number; message?: string };

/** Asks the API for the refresh token as an HttpOnly cookie (Day 29), never in JSON. */
const COOKIE = { header: { 'X-Refresh-Token-Transport': 'cookie' } } as const;

/** Refresh this long before the access token expires, so a request never goes out with a dying token. */
const REFRESH_EARLY_MS = 30_000;

/**
 * Who is logged in, kept in memory only.
 *
 * The access token lives in this object, never in localStorage: a script injected into the page (XSS) can read
 * localStorage, but can't reach a variable inside this module's closure from outside. The refresh token is an
 * HttpOnly cookie that no script can read at all. Reloading the page loses the access token, so restore() asks
 * for a new one with the cookie: the "silent refresh".
 *
 * Plain TypeScript, no React: the React parts (AuthProvider, useAuth) only subscribe to it.
 */
export class AuthSession {
  private accessToken: string | null = null;
  private expiresAt = 0;
  private status: SessionStatus = 'restoring';
  private endReason: EndReason = null;
  private refreshing: Promise<boolean> | null = null;
  private readonly listeners = new Set<() => void>();
  private readonly channel: BroadcastChannel | null;

  /**
   * @param authApi a client WITHOUT the auth middleware (it is the auth), used for login, refresh, logout
   * @param now the clock, replaceable in tests
   */
  constructor(
    private readonly authApi: OrudeskClient,
    private readonly now: () => number = Date.now,
  ) {
    // Logging out in one tab logs out the others: the cookie is gone for all of them anyway.
    this.channel =
      typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel('orudesk-auth');
    this.channel?.addEventListener('message', (event: MessageEvent) => {
      if (event.data === 'logout') this.clear(null);
    });
  }

  // Arrow functions: React calls these without `this` (useSyncExternalStore, onClick handlers).

  getStatus = (): SessionStatus => this.status;

  getEndReason = (): EndReason => this.endReason;

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  login = async (email: string, password: string): Promise<LoginResult> => {
    try {
      const { data, error, response } = await this.authApi.POST('/api/v1/auth/login', {
        params: COOKIE,
        body: { email, password },
      });
      if (data) {
        this.accept(data);
        return { ok: true };
      }
      if (response.status === 401) return { ok: false, reason: 'credentials' };
      if (response.status === 429) {
        return { ok: false, reason: 'throttled', retryAfterSeconds: retryAfter(response) };
      }
      if (response.status === 503) return { ok: false, reason: 'unavailable' };
      const message = (error as { detail?: string } | undefined)?.detail;
      return message ? { ok: false, reason: 'other', message } : { ok: false, reason: 'other' };
    } catch {
      return { ok: false, reason: 'network' };
    }
  };

  /** On page load: is there still a refresh cookie? Then we're logged in without asking for the password. */
  restore = async (): Promise<void> => {
    await this.refresh();
  };

  /**
   * Gets new tokens with the refresh cookie. Ten requests that need a token at once share one refresh, and two
   * tabs never refresh at the same time (see withTabLock), because each refresh token works only once.
   */
  refresh = (): Promise<boolean> => {
    this.refreshing ??= withTabLock(() => this.refreshNow()).finally(() => {
      this.refreshing = null;
    });
    return this.refreshing;
  };

  /** A token that is valid for at least 30 more seconds, refreshing first if needed; null when logged out. */
  validAccessToken = async (): Promise<string | null> => {
    if (this.accessToken && this.now() < this.expiresAt - REFRESH_EARLY_MS) return this.accessToken;
    if (this.status === 'signedOut') return null;
    return (await this.refresh()) ? this.accessToken : null;
  };

  logout = async (): Promise<void> => {
    try {
      await this.authApi.POST('/api/v1/auth/logout', { params: COOKIE });
    } catch {
      // Offline: forget the token locally anyway. The cookie expires by itself.
    } finally {
      this.clear(null);
      this.channel?.postMessage('logout');
    }
  };

  /**
   * Two kinds of failure, handled differently. The API said no (401, 400): the session is over, log out. The
   * API didn't answer properly (network error, 503, 500): nothing is known about the session, so a logged-in
   * user stays logged in and the requests fail until the API is back (the banner says so). Logging people out
   * because Redis restarted would lose their unsaved work for nothing.
   */
  private async refreshNow(): Promise<boolean> {
    let status: number | null = null;
    try {
      const { data, response } = await this.authApi.POST('/api/v1/auth/refresh', {
        params: COOKIE,
      });
      if (data) {
        this.accept(data);
        return true;
      }
      status = response.status;
    } catch {
      // no answer: status stays null
    }
    const refused = status !== null && status < 500;
    if (refused) {
      this.clear(this.status === 'signedIn' ? 'expired' : null);
    } else if (this.status === 'restoring') {
      this.clear('unreachable'); // at page load there's no session to keep, so show the login page and why
    }
    return false;
  }

  private accept(tokens: Schemas['TokenResponse']): void {
    this.accessToken = tokens.accessToken;
    this.expiresAt = this.now() + tokens.expiresIn * 1000;
    this.endReason = null;
    this.setStatus('signedIn');
  }

  private clear(reason: EndReason): void {
    this.accessToken = null;
    this.expiresAt = 0;
    this.endReason = reason; // set before the listeners run, so the login page reads it in the same render
    this.setStatus('signedOut');
  }

  private setStatus(status: SessionStatus): void {
    if (this.status === status) return;
    this.status = status;
    this.listeners.forEach((listener) => listener());
  }
}

/**
 * Runs `work` while holding a lock shared by every tab of this site (the Web Locks API). Two tabs opened at once
 * would otherwise both send the same refresh cookie; the first rotates it, and the second gets 401 and logs out.
 * With the lock, the second waits, and its request carries the cookie the first one just received.
 */
function withTabLock<T>(work: () => Promise<T>): Promise<T> {
  const locks = typeof navigator === 'undefined' ? undefined : navigator.locks;
  return locks ? locks.request('orudesk-refresh', work) : work();
}

/** Retry-After in seconds (the API sends a number; a proxy could send a date). Unknown: 15 minutes. */
function retryAfter(response: Response): number {
  const header = response.headers.get('Retry-After');
  const seconds = header === null ? NaN : Number(header);
  if (Number.isFinite(seconds) && seconds > 0) return seconds;
  const date = header === null ? NaN : Date.parse(header);
  return Number.isFinite(date) ? Math.max(1, Math.ceil((date - Date.now()) / 1000)) : 15 * 60;
}
