import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';

describe('App', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('says the API is up when /api/health answers 200', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"status":"UP"}')));
    render(<App />);
    expect(screen.getByRole('status')).toHaveTextContent('Checking the API...');
    expect(await screen.findByText('The API is up.')).toBeInTheDocument();
  });

  it('says the API is not reachable when the request fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    render(<App />);
    expect(await screen.findByText(/not reachable/)).toBeInTheDocument();
  });
});
