import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { createServices } from './services';
import { fakeApi, json, tokens } from './test/fakeApi';

const me = {
  id: 'u1',
  tenantId: 't1',
  email: 'ck@orudesk.org',
  roles: ['ADMIN'],
  grants: [{ role: 'ADMIN' }],
  enabled: true,
};

describe('App', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/');
    localStorage.clear(); // the colour scheme and unit choices
  });
  afterEach(() => vi.unstubAllGlobals());

  it('sends a logged-out visitor to the login page, and back home after logging in', async () => {
    fakeApi({
      'POST /api/v1/auth/refresh': () => json({ title: 'Invalid refresh token' }, 401),
      'POST /api/v1/auth/login': () => tokens(),
      'GET /api/v1/users/me': () => json(me),
    });
    render(<App services={createServices()} />);

    expect(await screen.findByRole('heading', { name: 'Log in to Orudesk' })).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText(/Email/), 'ck@orudesk.org');
    await userEvent.type(screen.getByLabelText(/Password/), 'secret-password');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByText('ck@orudesk.org')).toBeInTheDocument();
    expect(window.location.pathname).toBe('/');
  });

  it('skips the login page when the refresh cookie is still valid', async () => {
    const api = fakeApi({
      'POST /api/v1/auth/refresh': () => tokens(),
      'GET /api/v1/users/me': () => json(me),
    });
    render(<App services={createServices()} />);

    expect(await screen.findByText('ck@orudesk.org')).toBeInTheDocument();
    expect(api.calls('POST /api/v1/auth/login')).toBe(0);
  });

  it('shows why the login failed', async () => {
    fakeApi({
      'POST /api/v1/auth/refresh': () => json({}, 401),
      'POST /api/v1/auth/login': () => json({ title: 'Login failed' }, 401),
    });
    render(<App services={createServices()} />);

    await userEvent.type(await screen.findByLabelText(/Email/), 'ck@orudesk.org');
    await userEvent.type(screen.getByLabelText(/Password/), 'wrong-password');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Wrong email or password.');
  });

  it('logs out and forgets the cached profile', async () => {
    fakeApi({
      'POST /api/v1/auth/refresh': () => tokens(),
      'GET /api/v1/users/me': () => json(me),
      'POST /api/v1/auth/logout': () => new Response(null, { status: 204 }),
    });
    const services = createServices();
    render(<App services={services} />);

    await userEvent.click(await screen.findByRole('button', { name: 'Account' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Log out' }));

    expect(await screen.findByRole('heading', { name: 'Log in to Orudesk' })).toBeInTheDocument();
    expect(services.queryClient.getQueryData(['users', 'me'])).toBeUndefined();
  });

  it('draws the shell around pages, and a 404 page for unknown addresses', async () => {
    window.history.pushState({}, '', '/no-such-page');
    fakeApi({
      'POST /api/v1/auth/refresh': () => tokens(),
      'GET /api/v1/users/me': () => json(me),
    });
    render(<App services={createServices()} />);

    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/');
    expect(document.title).toBe('Page not found · Orudesk');
  });

  it('switches between light and dark', async () => {
    fakeApi({
      'POST /api/v1/auth/refresh': () => tokens(),
      'GET /api/v1/users/me': () => json(me),
    });
    render(<App services={createServices()} />);

    await userEvent.click(await screen.findByRole('button', { name: 'Switch to dark theme' }));

    expect(document.documentElement).toHaveAttribute('data-mantine-color-scheme', 'dark');
    expect(screen.getByRole('button', { name: 'Switch to light theme' })).toBeInTheDocument();
  });

  it('works in the first unit the user may use, and switches units', async () => {
    const tree = {
      id: 'group',
      name: 'Sunrise Group',
      code: 'SUNRISE',
      type: 'GROUP',
      children: [
        { id: 'north', name: 'North School', code: 'N', type: 'SCHOOL', children: [] },
        { id: 'south', name: 'South School', code: 'S', type: 'SCHOOL', children: [] },
      ],
    };
    const api = fakeApi({
      'POST /api/v1/auth/refresh': () => tokens(),
      'GET /api/v1/users/me': () => json(me),
      'GET /api/v1/org-units': () => json(tree),
      'GET /api/v1/org-units/current': (request) => {
        const id = request.headers.get('X-Org-Unit');
        const unit = id === 'group' ? tree : tree.children.find((c) => c.id === id)!;
        return json({ ...unit, roles: id === 'south' ? ['ADMIN', 'TEACHER'] : ['ADMIN'] });
      },
    });
    render(<App services={createServices()} />);

    expect(await screen.findByText('Sunrise Group', { selector: 'p' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('combobox', { name: 'Org unit' }));
    // hidden: the dropdown is still fading in (a CSS transition jsdom never finishes)
    await userEvent.click(
      await screen.findByRole('option', { name: /South School/, hidden: true }),
    );

    expect(await screen.findByText('South School', { selector: 'p' })).toBeInTheDocument();
    expect(await screen.findByText('TEACHER')).toBeInTheDocument();
    const last = api.requests.filter((r) => r.url.endsWith('/org-units/current')).at(-1);
    expect(last?.headers.get('X-Org-Unit')).toBe('south');
    expect(localStorage.getItem('orudesk.orgUnit')).toBe('south');
  });
});
