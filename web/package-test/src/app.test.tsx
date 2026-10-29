import { render, screen } from '@testing-library/react';
import { App, coreModule, createServices } from '@orudesk/web-core';
import { fakeApi, json } from '@orudesk/web-core/testing';
import { describe, expect, it } from 'vitest';

describe('the published @orudesk/web-core', () => {
  it('starts, with its test helpers, and shows the login page', async () => {
    fakeApi({ 'POST /api/v1/auth/refresh': () => json({ title: 'Invalid refresh token' }, 401) });
    render(<App services={createServices()} modules={[coreModule]} />);

    expect(await screen.findByRole('heading', { name: 'Log in to Orudesk' })).toBeInTheDocument();
  });
});
