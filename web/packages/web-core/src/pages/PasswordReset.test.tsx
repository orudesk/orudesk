import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '../App';
import { coreModule } from '../coreModule';
import { createServices } from '../services';
import { fakeApi, json } from '../test/fakeApi';

const loggedOut = {
  'POST /api/v1/auth/refresh': () => json({ title: 'Invalid refresh token' }, 401),
};

describe('password reset', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.unstubAllGlobals());

  it('asks for a link from the login page, without saying whether the account exists', async () => {
    window.history.pushState({}, '', '/login');
    const api = fakeApi({
      ...loggedOut,
      'POST /api/v1/auth/password-reset': () => new Response(null, { status: 202 }),
    });
    render(<App services={createServices()} modules={[coreModule]} />);

    await userEvent.click(await screen.findByRole('link', { name: 'Forgot your password?' }));
    await userEvent.type(await screen.findByLabelText(/Email/), 'asha@orudesk.org');
    await userEvent.click(screen.getByRole('button', { name: 'Email me a link' }));

    expect(
      await screen.findByText(/If asha@orudesk.org has an Orudesk account/),
    ).toBeInTheDocument();
    expect(await api.requests.at(-1)?.json()).toEqual({ email: 'asha@orudesk.org' });
  });

  it('sets the new password with the token from the link', async () => {
    window.history.pushState({}, '', '/reset-password?token=abc123');
    const api = fakeApi({
      ...loggedOut,
      'POST /api/v1/auth/password-reset/confirm': () => new Response(null, { status: 204 }),
    });
    render(<App services={createServices()} modules={[coreModule]} />);

    await userEvent.type(await screen.findByLabelText(/^New password(?! again)/), 'new-password');
    await userEvent.type(screen.getByLabelText(/again/), 'new-password');
    await userEvent.click(screen.getByRole('button', { name: 'Change password' }));

    expect(await screen.findByText('Password changed')).toBeInTheDocument();
    const confirm = api.requests.find((r) => r.url.endsWith('/password-reset/confirm'));
    expect(await confirm?.json()).toEqual({ token: 'abc123', newPassword: 'new-password' });
  });

  it('catches two different passwords before asking the API', async () => {
    window.history.pushState({}, '', '/reset-password?token=abc123');
    const api = fakeApi(loggedOut);
    render(<App services={createServices()} modules={[coreModule]} />);

    await userEvent.type(await screen.findByLabelText(/^New password(?! again)/), 'new-password');
    await userEvent.type(screen.getByLabelText(/again/), 'new-passwort');
    await userEvent.click(screen.getByRole('button', { name: 'Change password' }));

    expect(await screen.findByText('The two passwords are different.')).toBeInTheDocument();
    expect(api.calls('POST /api/v1/auth/password-reset/confirm')).toBe(0);
  });

  it('offers a new link when this one has expired', async () => {
    window.history.pushState({}, '', '/reset-password?token=old');
    fakeApi({
      ...loggedOut,
      'POST /api/v1/auth/password-reset/confirm': () => json({ title: 'Invalid reset link' }, 400),
    });
    render(<App services={createServices()} modules={[coreModule]} />);

    await userEvent.type(await screen.findByLabelText(/^New password(?! again)/), 'new-password');
    await userEvent.type(screen.getByLabelText(/again/), 'new-password');
    await userEvent.click(screen.getByRole('button', { name: 'Change password' }));

    expect(
      await screen.findByText(/This link has expired or was already used/),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Email me a new link' })).toHaveAttribute(
      'href',
      '/forgot-password',
    );
  });
});
