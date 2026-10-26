import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
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
const asha = {
  id: 'p-asha',
  firstName: 'Asha',
  lastName: 'Kumar',
  dateOfBirth: '2014-03-09',
  phone: '+91 98765 43210',
  email: null,
};

function openAt(path: string, handlers: Parameters<typeof fakeApi>[0]) {
  window.history.pushState({}, '', path);
  const api = fakeApi({
    'POST /api/v1/auth/refresh': () => tokens(),
    'GET /api/v1/users/me': () => json(admin),
    'GET /api/v1/org-units': () =>
      json({ id: 'g', name: 'Group', code: 'G', type: 'GROUP', children: [] }),
    ...handlers,
  });
  render(<App services={createServices()} />);
  return api;
}

describe('People', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.unstubAllGlobals());

  it('lists people by name, each a link to their page', async () => {
    openAt('/people', {
      'GET /api/v1/people': () => json({ items: [asha], nextCursor: null }),
    });

    const link = await screen.findByRole('link', { name: 'Kumar, Asha' });
    expect(link).toHaveAttribute('href', '/people/p-asha');
    const row = link.closest('tr')!;
    expect(within(row).getByText('+91 98765 43210')).toBeInTheDocument();
    expect(within(row).getByText('—')).toBeInTheDocument(); // no email
  });

  it('shows one person, and says so when there is no such person', async () => {
    openAt('/people/p-asha', {
      'GET /api/v1/people/p-asha': () => json(asha),
      'GET /api/v1/people/nobody': () => json({ title: 'Person not found' }, 404),
    });

    expect(await screen.findByRole('heading', { name: 'Asha Kumar' })).toBeInTheDocument();
    expect(document.title).toBe('Asha Kumar · Orudesk');

    window.history.pushState({}, '', '/people/nobody');
    window.dispatchEvent(new PopStateEvent('popstate'));
    expect(await screen.findByRole('heading', { name: 'Person not found' })).toBeInTheDocument();
  });

  it('adds a person and opens their page', async () => {
    const api = openAt('/people/new', {
      'POST /api/v1/people': () => json({ id: 'p-new' }, 201),
      'GET /api/v1/people/p-new': () =>
        json({
          id: 'p-new',
          firstName: 'Ravi',
          lastName: 'Shah',
          dateOfBirth: null,
          phone: null,
          email: null,
        }),
    });

    await userEvent.type(await screen.findByLabelText(/First name/), 'Ravi');
    await userEvent.type(screen.getByLabelText(/Last name/), ' Shah ');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByRole('heading', { name: 'Ravi Shah' })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/people/p-new');
    const sent = api.requests.find((r) => r.method === 'POST' && r.url.endsWith('/people'))!;
    // trimmed, and empty optional fields sent as null
    expect(await sent.json()).toEqual({
      firstName: 'Ravi',
      lastName: 'Shah',
      dateOfBirth: null,
      phone: null,
      email: null,
    });
  });

  it("shows the server's message under each field it refused", async () => {
    openAt('/people/new', {
      'POST /api/v1/people': () =>
        json(
          {
            title: 'Validation failed',
            detail: 'some fields are invalid',
            errors: {
              firstName: 'must not be blank',
              email: 'must be a well-formed email address',
            },
          },
          400,
        ),
    });

    await userEvent.type(await screen.findByLabelText(/Email/), 'not-an-email');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('must not be blank')).toBeInTheDocument();
    expect(screen.getByLabelText(/First name/)).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText(/Email/)).toHaveAccessibleDescription(
      'must be a well-formed email address',
    );
    expect(window.location.pathname).toBe('/people/new');
  });
});
