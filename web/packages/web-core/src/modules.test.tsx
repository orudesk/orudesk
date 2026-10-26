import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { coreModule } from './coreModule';
import type { OrudeskModule } from './module';
import { createServices } from './services';
import { useServices } from './ServicesContext';
import { fakeApi, json, tokens } from './test/fakeApi';

// A vertical's page, the way one would write it: its own path, the shared fetch (token and unit included)
function DonorsPage() {
  const { fetch } = useServices();
  void fetch(new Request(`${window.location.origin}/api/v1/donors`));
  return <h1>Donors</h1>;
}
const ngo: OrudeskModule = {
  id: 'ngo',
  routes: [{ path: '/donors', element: <DonorsPage /> }],
  navigation: [{ label: 'Donors', path: '/donors', icon: null }],
};

describe('a module plugged into the app', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('gets a menu entry, a page inside the shell, and authenticated requests', async () => {
    window.history.pushState({}, '', '/donors');
    localStorage.setItem('orudesk.orgUnit', 'north');
    const api = fakeApi({
      'POST /api/v1/auth/refresh': () => tokens('the-token'),
      'GET /api/v1/users/me': () =>
        json({
          id: 'u1',
          tenantId: 't1',
          email: 'ck@orudesk.org',
          roles: [],
          grants: [],
          enabled: true,
        }),
      'GET /api/v1/donors': () => json([]),
    });
    render(<App services={createServices()} modules={[coreModule, ngo]} />);

    expect(await screen.findByRole('heading', { name: 'Donors' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Donors' })).toHaveAttribute('href', '/donors');
    expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument(); // the core's menu is still there
    const donors = api.requests.find((r) => r.url.endsWith('/donors'))!;
    expect(donors.headers.get('Authorization')).toBe('Bearer the-token');
    expect(donors.headers.get('X-Org-Unit')).toBe('north');
    localStorage.clear();
  });
});
