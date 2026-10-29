import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '../App';
import { coreModule } from '../coreModule';
import { createServices } from '../services';
import { fakeApi, json, tokens } from '../test/fakeApi';

const secondFactorRequired = () =>
  json(
    {
      title: 'Second factor required',
      detail: 'enter the code from your authenticator app, or a recovery code',
      loginToken: 'step-two',
    },
    401,
  );

async function enterPassword() {
  await userEvent.type(await screen.findByLabelText(/Email/), 'asha@orudesk.org');
  await userEvent.type(screen.getByLabelText(/Password/), 'asha-password');
  await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
}

describe('Login with two-factor sign-in', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.pushState({}, '', '/login');
  });
  afterEach(() => vi.unstubAllGlobals());

  it('asks for the code after the password, then signs in', async () => {
    const api = fakeApi({
      'POST /api/v1/auth/refresh': () => json({ title: 'Invalid refresh token' }, 401),
      'POST /api/v1/auth/login': secondFactorRequired,
      'POST /api/v1/auth/login/two-factor': () => tokens(),
      'GET /api/v1/users/me': () =>
        json({
          id: 'u1',
          tenantId: 't1',
          email: 'asha@orudesk.org',
          roles: [],
          grants: [],
          enabled: true,
        }),
      'GET /api/v1/org-units': () =>
        json({ id: 'g', name: 'G', code: 'G', type: 'GROUP', children: [] }),
    });
    render(<App services={createServices()} modules={[coreModule]} />);

    await enterPassword();
    await userEvent.type(
      await screen.findByLabelText(/Code from your authenticator app/),
      '123456',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Continue' }));

    expect(await screen.findByRole('button', { name: 'Account' })).toBeInTheDocument();
    const second = api.requests.find((r) => r.url.endsWith('/login/two-factor'));
    expect(await second?.json()).toEqual({ loginToken: 'step-two', code: '123456' });
  });

  it('says so when the code is wrong, and starts over when the step has expired', async () => {
    let answer = () => json({ title: 'Validation failed', errors: { code: 'wrong' } }, 400);
    fakeApi({
      'POST /api/v1/auth/refresh': () => json({ title: 'Invalid refresh token' }, 401),
      'POST /api/v1/auth/login': secondFactorRequired,
      'POST /api/v1/auth/login/two-factor': () => answer(),
    });
    render(<App services={createServices()} modules={[coreModule]} />);

    await enterPassword();
    await userEvent.type(
      await screen.findByLabelText(/Code from your authenticator app/),
      '000000',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Continue' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/isn't right/);

    answer = () => json({ title: 'Login expired' }, 401);
    await userEvent.click(screen.getByRole('button', { name: 'Continue' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/took too long/);
    expect(screen.getByLabelText(/Email/)).toBeInTheDocument();
  });
});
