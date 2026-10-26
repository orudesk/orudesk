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
const tree = {
  id: 'group',
  name: 'Sunrise Group',
  code: 'SUNRISE',
  type: 'GROUP',
  children: [{ id: 'north', name: 'North School', code: 'N', type: 'SCHOOL', children: [] }],
};
const user = (email: string, grants: unknown[] = [{ role: 'USER' }], enabled = true) => ({
  id: email.split('@')[0]!,
  email,
  enabled,
  grants,
});

function openUsersPage(handlers: Parameters<typeof fakeApi>[0]) {
  window.history.pushState({}, '', '/users');
  const api = fakeApi({
    'POST /api/v1/auth/refresh': () => tokens(),
    'GET /api/v1/users/me': () => json(admin),
    'GET /api/v1/org-units': () => json(tree),
    ...handlers,
  });
  render(<App services={createServices()} modules={[coreModule]} />);
  return api;
}

describe('Users page', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.unstubAllGlobals());

  it('lists users with their roles, naming the unit of a scoped role', async () => {
    openUsersPage({
      'GET /api/v1/users': () =>
        json({
          items: [
            user('anna@orudesk.org', [{ role: 'TEACHER', orgUnitId: 'north' }, { role: 'USER' }]),
            user('old@orudesk.org', [{ role: 'USER' }], false),
          ],
          nextCursor: null,
        }),
    });

    const anna = (await screen.findByText('anna@orudesk.org')).closest('tr')!;
    expect(await within(anna).findByText('TEACHER · North School')).toBeInTheDocument();
    expect(within(anna).getByText('Active')).toBeInTheDocument();
    const old = screen.getByText('old@orudesk.org').closest('tr')!;
    expect(within(old).getByText('Disabled')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Load more' })).not.toBeInTheDocument();
    expect(document.title).toBe('Users · Orudesk');
  });

  it('loads the next page with the cursor of the last one', async () => {
    const api = openUsersPage({
      'GET /api/v1/users': (request) =>
        new URL(request.url).searchParams.get('after') === 'c1'
          ? json({ items: [user('zoe@orudesk.org')], nextCursor: null })
          : json({ items: [user('anna@orudesk.org')], nextCursor: 'c1' }),
    });

    await userEvent.click(await screen.findByRole('button', { name: 'Load more' }));

    expect(await screen.findByText('zoe@orudesk.org')).toBeInTheDocument();
    expect(screen.getByText('anna@orudesk.org')).toBeInTheDocument(); // added to, not replaced
    expect(api.calls('GET /api/v1/users')).toBe(2);
  });

  it('searches once typing pauses', async () => {
    const api = openUsersPage({
      'GET /api/v1/users': (request) => {
        const q = new URL(request.url).searchParams.get('q');
        return json({ items: q ? [] : [user('anna@orudesk.org')], nextCursor: null });
      },
    });
    await screen.findByText('anna@orudesk.org');

    await userEvent.type(screen.getByLabelText('Search by email'), 'nobody');

    expect(await screen.findByText('No users match “nobody”.')).toBeInTheDocument();
    const searched = api.requests.filter((r) => new URL(r.url).searchParams.has('q'));
    expect(searched.map((r) => new URL(r.url).searchParams.get('q'))).toEqual(['nobody']);
  });

  it('tells a user who is not an admin that the page is not for them', async () => {
    openUsersPage({
      'GET /api/v1/users/me': () => json({ ...admin, roles: ['USER'], grants: [{ role: 'USER' }] }),
      'GET /api/v1/users': () => json({ title: 'Forbidden' }, 403),
    });

    expect(await screen.findByRole('alert')).toHaveTextContent(
      "You don't have access to this page.",
    );
    expect(screen.queryByRole('link', { name: 'Users' })).not.toBeInTheDocument();
  });

  describe('the roles dialog', () => {
    const roles = [
      { code: 'ADMIN', name: 'Administrator', description: null },
      { code: 'USER', name: 'User', description: null },
    ];

    // A tiny server: anna's grants change when the dialog posts or deletes
    function server() {
      let grants: { role: string; orgUnitId?: string | null }[] = [
        { role: 'ADMIN', orgUnitId: 'north' },
        { role: 'USER' },
      ];
      return openUsersPage({
        'GET /api/v1/roles': () => json(roles),
        'GET /api/v1/users': () =>
          json({ items: [user('anna@orudesk.org', grants)], nextCursor: null }),
        'POST /api/v1/users/anna/roles': async (request) => {
          grants = [...grants, (await request.json()) as (typeof grants)[number]];
          return new Response(null, { status: 204 });
        },
        'DELETE /api/v1/users/anna/roles/ADMIN': (request) => {
          const unit = new URL(request.url).searchParams.get('orgUnitId');
          grants = grants.filter((g) => !(g.role === 'ADMIN' && (g.orgUnitId ?? null) === unit));
          return new Response(null, { status: 204 });
        },
        'DELETE /api/v1/users/anna/roles/USER': () =>
          json(
            { title: 'Role change not allowed', detail: 'a user must have at least one role' },
            409,
          ),
      });
    }

    async function openDialog() {
      await userEvent.click(
        await screen.findByRole('button', { name: 'Manage roles of anna@orudesk.org' }),
      );
      return screen.findByRole('dialog', { name: 'Roles of anna@orudesk.org' });
    }

    it('removes a scoped role, and the list shows it gone', async () => {
      const api = server();
      const dialog = await openDialog();

      await userEvent.click(
        await within(dialog).findByRole('button', { name: 'Remove Administrator in North School' }),
      );

      const anna = screen.getByText('anna@orudesk.org').closest('tr')!;
      await vi.waitFor(() =>
        expect(within(anna).queryByText('ADMIN · North School')).not.toBeInTheDocument(),
      );
      const sent = api.requests.find((r) => r.method === 'DELETE')!;
      expect(new URL(sent.url).searchParams.get('orgUnitId')).toBe('north');
    });

    it('adds a role for the whole organisation', async () => {
      const api = server();
      const dialog = await openDialog();

      await userEvent.click(within(dialog).getByRole('combobox', { name: 'Role' }));
      await userEvent.click(
        await screen.findByRole('option', { name: 'Administrator', hidden: true }),
      );
      await userEvent.click(within(dialog).getByRole('button', { name: 'Add role' }));

      expect(
        await within(dialog).findByRole('button', {
          name: 'Remove Administrator in Whole organisation',
        }),
      ).toBeInTheDocument();
      const sent = api.requests.find((r) => r.method === 'POST' && r.url.endsWith('/roles'))!;
      expect(await sent.json()).toEqual({ role: 'ADMIN', orgUnitId: null });
    });

    it('explains when the API says no', async () => {
      server();
      const dialog = await openDialog();

      await userEvent.click(
        await within(dialog).findByRole('button', { name: 'Remove User in Whole organisation' }),
      );

      expect(await within(dialog).findByRole('alert')).toHaveTextContent(
        'A user must have at least one role.',
      );
    });
  });
});
