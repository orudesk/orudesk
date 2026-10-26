import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { coreModule } from '../coreModule';
import { App } from '../App';
import { createServices } from '../services';
import { fakeApi, json, tokens } from '../test/fakeApi';

const admin = {
  id: 'u1',
  tenantId: 't1',
  email: 'ck@orudesk.org',
  roles: ['ADMIN'],
  grants: [{ role: 'ADMIN' }],
  enabled: true,
};

function entry(id: string, summary: string, extra: Record<string, unknown> = {}) {
  return {
    id,
    occurredAt: '2026-10-26T10:42:00Z',
    actorId: 'u1',
    actorEmail: 'ck@orudesk.org',
    action: 'party.person.registered',
    subjectType: 'person',
    subjectId: 'p-asha',
    summary,
    requestId: null,
    ...extra,
  };
}

function openAt(path: string, handlers: Parameters<typeof fakeApi>[0]) {
  window.history.pushState({}, '', path);
  const api = fakeApi({
    'POST /api/v1/auth/refresh': () => tokens(),
    'GET /api/v1/users/me': () => json(admin),
    'GET /api/v1/org-units': () =>
      json({ id: 'g', name: 'Group', code: 'G', type: 'GROUP', children: [] }),
    ...handlers,
  });
  render(<App services={createServices()} modules={[coreModule]} />);
  return api;
}

describe('Activity', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.unstubAllGlobals());

  it('shows who did what, newest first, and loads older entries on request', async () => {
    const api = openAt('/activity', {
      'GET /api/v1/audit-entries': (request) =>
        new URL(request.url).searchParams.get('after') === 'next'
          ? json({ items: [entry('a3', 'Registered Ravi Shah')], nextCursor: null })
          : json({
              items: [
                entry('a1', 'Registered Asha Kumar', { requestId: 'req-0001-abcd' }),
                entry('a2', 'Gave PRINCIPAL to meera@orudesk.org', {
                  actorId: null,
                  actorEmail: null,
                }),
              ],
              nextCursor: 'next',
            }),
    });

    const first = (await screen.findByText('Registered Asha Kumar')).closest('tr')!;
    expect(within(first).getByText('ck@orudesk.org')).toBeInTheDocument();
    expect(within(first).getByText('Request req-0001-abcd')).toBeInTheDocument();
    const second = screen.getByText('Gave PRINCIPAL to meera@orudesk.org').closest('tr')!;
    expect(within(second).getByText('System')).toBeInTheDocument(); // nobody was logged in

    await userEvent.click(screen.getByRole('button', { name: 'Load more' }));
    expect(await screen.findByText('Registered Ravi Shah')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Load more' })).not.toBeInTheDocument();
    expect(api.calls('GET /api/v1/audit-entries')).toBe(2);
  });

  it("shows one record's history from its page", async () => {
    const api = openAt('/people/p-asha', {
      'GET /api/v1/people/p-asha': () =>
        json({
          id: 'p-asha',
          firstName: 'Asha',
          lastName: 'Kumar',
          dateOfBirth: null,
          phone: null,
          email: null,
        }),
      'GET /api/v1/audit-entries': () => json({ items: [entry('a1', 'Registered Asha Kumar')] }),
    });

    await userEvent.click(await screen.findByRole('link', { name: 'History' }));
    expect(await screen.findByText('Registered Asha Kumar')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'History' })).toBeInTheDocument();
    const request = api.requests.find((r) => new URL(r.url).pathname === '/api/v1/audit-entries');
    expect(new URL(request!.url).searchParams.get('subjectId')).toBe('p-asha');
  });
});
