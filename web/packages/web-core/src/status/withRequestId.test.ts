import { describe, expect, it, vi } from 'vitest';
import { withRequestId } from './withRequestId';

describe('withRequestId', () => {
  const ok = () =>
    vi.fn<(request: Request) => Promise<Response>>(async () => new Response(null, { status: 204 }));

  it('gives each request its own id', async () => {
    const next = ok();
    const send = withRequestId(next);

    await send(new Request('http://api.test/a'));
    await send(new Request('http://api.test/b'));

    const [first, second] = next.mock.calls.map(([request]) => request.headers.get('X-Request-Id'));
    expect(first).toMatch(/^[0-9a-f-]{36}$/);
    expect(second).not.toBe(first);
  });

  it('keeps an id the caller set', async () => {
    const next = ok();

    await withRequestId(next)(
      new Request('http://api.test/a', { headers: { 'X-Request-Id': 'chosen-1' } }),
    );

    expect(next.mock.calls[0]?.[0].headers.get('X-Request-Id')).toBe('chosen-1');
  });
});
