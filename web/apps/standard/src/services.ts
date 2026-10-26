import { createOrudeskClient, type OrudeskClient } from '@orudesk/api-client';
import { QueryClient } from '@tanstack/react-query';
import { authFetch } from './auth/authFetch';
import { AuthSession } from './auth/session';
import { ServiceStatus, withServiceStatus } from './status/serviceStatus';
import { UnitStore } from './units/unitStore';
import { withOrgUnit } from './units/withOrgUnit';

/**
 * Everything the app shares, created once in main.tsx and handed down through React context. The same idea as
 * Spring's dependency injection: components ask for an `api`, they don't build one, so a test can hand them a
 * different one.
 */
export interface Services {
  session: AuthSession;
  units: UnitStore;
  serviceStatus: ServiceStatus;
  api: OrudeskClient;
  queryClient: QueryClient;
}

export function createServices(): Services {
  // `fetch` is looked up on every call rather than once, so tests can replace it with vi.stubGlobal.
  const serviceStatus = new ServiceStatus();
  const plainFetch = withServiceStatus(serviceStatus, (request) => globalThis.fetch(request));
  const session = new AuthSession(createOrudeskClient({ fetch: plainFetch }));
  const units = new UnitStore();
  // Each request passes through: add X-Org-Unit, then add the token (and retry once on 401), then note
  // whether the API answered, then fetch.
  const api = createOrudeskClient({
    fetch: withOrgUnit(units, authFetch(session, plainFetch)),
  });
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
  });
  // The next person to log in on this computer must not see the last one's cached data.
  session.subscribe(() => {
    if (session.getStatus() === 'signedOut') queryClient.clear();
  });
  return { session, units, serviceStatus, api, queryClient };
}
