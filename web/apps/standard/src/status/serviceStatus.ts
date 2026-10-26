export type ServiceState =
  | { kind: 'ok' }
  /** The API answered 503: Redis or the database is down (Day 12). It says when to try again. */
  | { kind: 'unavailable'; retryAfterSeconds: number | null }
  /** No answer at all: offline, DNS, the server is down. */
  | { kind: 'unreachable' };

const OK: ServiceState = { kind: 'ok' };

/**
 * Whether the API is working, as seen from the last requests. Every response updates it, so the banner appears
 * with the first failure and disappears with the first success, without polling a health endpoint.
 */
export class ServiceStatus {
  private state: ServiceState = OK;
  private readonly listeners = new Set<() => void>();

  get = (): ServiceState => this.state;

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  set = (state: ServiceState): void => {
    // Same kind and same detail: no change, no re-render (and useSyncExternalStore needs a stable snapshot)
    if (JSON.stringify(state) === JSON.stringify(this.state)) return;
    this.state = state.kind === 'ok' ? OK : state;
    this.listeners.forEach((listener) => listener());
  };
}

/** The fetch layer that reports to ServiceStatus. It changes nothing about the request or the response. */
export function withServiceStatus(
  status: ServiceStatus,
  next: (request: Request) => Promise<Response>,
): (request: Request) => Promise<Response> {
  return async (request) => {
    let response: Response;
    try {
      response = await next(request);
    } catch (error) {
      // fetch rejects only when no HTTP answer came back. An abort is the app's own doing, not an outage.
      if (!(error instanceof DOMException && error.name === 'AbortError')) {
        status.set({ kind: 'unreachable' });
      }
      throw error;
    }
    if (response.status === 503) {
      status.set({ kind: 'unavailable', retryAfterSeconds: retryAfter(response) });
    } else if (response.status < 500) {
      status.set({ kind: 'ok' }); // a 4xx is still the API working
    }
    return response;
  };
}

/** Retry-After is seconds ("30") or an HTTP date; we only need a rough number for the message. */
function retryAfter(response: Response): number | null {
  const value = response.headers.get('Retry-After');
  if (!value) return null;
  const seconds = Number(value);
  if (Number.isFinite(seconds)) return Math.max(0, Math.round(seconds));
  const date = Date.parse(value);
  return Number.isNaN(date) ? null : Math.max(0, Math.round((date - Date.now()) / 1000));
}
