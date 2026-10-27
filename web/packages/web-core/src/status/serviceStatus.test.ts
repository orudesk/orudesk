import { describe, expect, it } from 'vitest';
import { ServiceStatus, withServiceStatus } from './serviceStatus';

const answer =
  (status: number, headers: Record<string, string> = {}) =>
  () =>
    Promise.resolve(new Response(null, { status, headers }));
const request = () => new Request('http://localhost/api/v1/people');

describe('withServiceStatus', () => {
  it('notes a 503 with its Retry-After, and clears it with the next answer', async () => {
    const status = new ServiceStatus();

    await withServiceStatus(status, answer(503, { 'Retry-After': '30' }))(request());
    expect(status.get()).toEqual({ kind: 'unavailable', retryAfterSeconds: 30 });

    await withServiceStatus(status, answer(404))(request()); // a 404 is the API working
    expect(status.get()).toEqual({ kind: 'ok' });
  });

  it('notes when nothing answers, and still lets the caller see the error', async () => {
    const status = new ServiceStatus();
    const offline = () => Promise.reject(new TypeError('Failed to fetch'));

    await expect(withServiceStatus(status, offline)(request())).rejects.toThrow('Failed to fetch');
    expect(status.get()).toEqual({ kind: 'unreachable' });
  });

  it('ignores requests the app cancelled itself', async () => {
    const status = new ServiceStatus();
    const aborted = () => Promise.reject(new DOMException('aborted', 'AbortError'));

    await expect(withServiceStatus(status, aborted)(request())).rejects.toThrow();
    expect(status.get()).toEqual({ kind: 'ok' });
  });

  it('tells listeners only about real changes', async () => {
    const status = new ServiceStatus();
    let calls = 0;
    status.subscribe(() => calls++);

    await withServiceStatus(status, answer(200))(request());
    await withServiceStatus(status, answer(503))(request());
    await withServiceStatus(status, answer(503))(request());

    expect(calls).toBe(1);
  });
});
