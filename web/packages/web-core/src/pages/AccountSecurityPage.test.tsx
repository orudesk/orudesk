import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '../App';
import { coreModule } from '../coreModule';
import { createServices } from '../services';
import { fakeApi, json, tokens } from '../test/fakeApi';

const me = {
  id: 'u1',
  tenantId: 't1',
  email: 'asha@orudesk.org',
  roles: ['USER'],
  grants: [{ role: 'USER' }],
  enabled: true,
};
const sessions = [
  {
    id: 's1',
    device: 'Firefox on Windows',
    ipAddress: '10.0.0.5',
    startedAt: '2026-10-29T08:00:00Z',
    lastUsedAt: '2026-10-29T10:00:00Z',
    current: true,
  },
  {
    id: 's2',
    device: 'Chrome on Android',
    ipAddress: '10.0.0.9',
    startedAt: '2026-10-28T08:00:00Z',
    lastUsedAt: '2026-10-29T09:00:00Z',
    current: false,
  },
];
const off = { available: true, enabled: false, recoveryCodesLeft: 0 };

function openSecurityPage(handlers: Parameters<typeof fakeApi>[0]) {
  window.history.pushState({}, '', '/account/security');
  const api = fakeApi({
    'POST /api/v1/auth/refresh': () => tokens(),
    'GET /api/v1/users/me': () => json(me),
    'GET /api/v1/org-units': () =>
      json({ id: 'g', name: 'G', code: 'G', type: 'GROUP', children: [] }),
    'GET /api/v1/users/me/sessions': () => json(sessions),
    'GET /api/v1/users/me/two-factor': () => json(off),
    ...handlers,
  });
  render(<App services={createServices()} modules={[coreModule]} />);
  return api;
}

describe('Security page', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.unstubAllGlobals());

  it('lists where the user is signed in and signs out another device', async () => {
    const api = openSecurityPage({
      'DELETE /api/v1/users/me/sessions/s2': () => new Response(null, { status: 204 }),
    });

    const table = await screen.findByRole('table');
    expect(within(table).getByText('This browser')).toBeInTheDocument();
    await userEvent.click(
      within(table).getByRole('button', { name: 'Sign out Chrome on Android' }),
    );

    expect(api.calls('DELETE /api/v1/users/me/sessions/s2')).toBe(1);
  });

  it('sets up two-factor sign-in with the app’s first code and shows the recovery codes once', async () => {
    let enabled = false;
    const api = openSecurityPage({
      'GET /api/v1/users/me/two-factor': () =>
        json(enabled ? { ...off, enabled: true, recoveryCodesLeft: 10 } : off),
      'POST /api/v1/users/me/two-factor/setup': () =>
        json({
          secret: 'JBSWY3DPEHPK3PXP',
          otpauthUri:
            'otpauth://totp/Orudesk%3Aasha%40orudesk.org?secret=JBSWY3DPEHPK3PXP&issuer=Orudesk',
        }),
      'POST /api/v1/users/me/two-factor/confirm': () => {
        enabled = true;
        return json({ recoveryCodes: ['AAAA-BBBB-CCCC', 'DDDD-EEEE-FFFF'] });
      },
    });
    const section = await screen.findByRole('region', { name: 'Two-factor sign-in' });

    await userEvent.type(
      await within(section).findByLabelText(/Current password/),
      'asha-password',
    );
    await userEvent.click(
      within(section).getByRole('button', { name: 'Set up two-factor sign-in' }),
    );
    expect(await within(section).findByText('JBSW Y3DP EHPK 3PXP')).toBeInTheDocument();
    expect(await within(section).findByAltText(/QR code/)).toHaveAttribute(
      'src',
      expect.stringMatching(/^data:image\/svg\+xml/),
    );
    await userEvent.type(within(section).getByLabelText(/six-digit code/), '123456');
    await userEvent.click(within(section).getByRole('button', { name: 'Turn on' }));

    expect(await within(section).findByText('AAAA-BBBB-CCCC')).toBeInTheDocument();
    const confirm = api.requests.find((r) => r.url.endsWith('/two-factor/confirm'));
    expect(await confirm?.json()).toEqual({ code: '123456' });
    await userEvent.click(within(section).getByRole('button', { name: "I've saved them" }));
    expect(await within(section).findByText('10 recovery codes left.')).toBeInTheDocument();
  });

  it('shows a breached new password under its field', async () => {
    openSecurityPage({
      'POST /api/v1/users/me/password': () =>
        json(
          {
            title: 'Validation failed',
            detail: 'some fields are invalid',
            errors: { newPassword: 'This password has appeared in a data breach. Choose another' },
          },
          400,
        ),
    });
    const section = await screen.findByRole('region', { name: 'Password' });

    await userEvent.type(within(section).getByLabelText(/Current password/), 'old-password');
    await userEvent.type(within(section).getByLabelText(/New password/), 'password123');
    await userEvent.click(within(section).getByRole('button', { name: 'Change password' }));

    expect(await within(section).findByText(/appeared in a data breach/)).toBeInTheDocument();
  });
});
