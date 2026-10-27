import { MantineProvider } from '@mantine/core';
import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RouteError } from './RouteError';

function renderCrashing(error: Error) {
  const Crash = () => {
    throw error;
  };
  const router = createMemoryRouter([
    { path: '/', element: <Crash />, errorElement: <RouteError /> },
  ]);
  render(
    <MantineProvider>
      <RouterProvider router={router} />
    </MantineProvider>,
  );
}

describe('RouteError', () => {
  // React logs caught render errors to the console; expected here, so keep the test output clean
  afterEach(() => vi.restoreAllMocks());
  const quiet = () => vi.spyOn(console, 'error').mockImplementation(() => {});

  it('shows a way out instead of a blank page', async () => {
    quiet();
    renderCrashing(new Error('Cannot read properties of undefined'));

    expect(
      await screen.findByRole('heading', { name: 'Something went wrong' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reload' })).toBeInTheDocument();
  });

  it('recognises a page file from an older release', async () => {
    quiet();
    renderCrashing(
      new TypeError('Failed to fetch dynamically imported module: /assets/PeoplePage-old.js'),
    );

    expect(
      await screen.findByRole('heading', { name: 'Orudesk has been updated' }),
    ).toBeInTheDocument();
  });
});
