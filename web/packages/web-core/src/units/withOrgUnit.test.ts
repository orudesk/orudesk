import { describe, expect, it, vi } from 'vitest';
import { UnitStore } from './unitStore';
import { withOrgUnit } from './withOrgUnit';

describe('withOrgUnit', () => {
  const ok = () =>
    vi.fn<(request: Request) => Promise<Response>>(async () => new Response(null, { status: 204 }));

  it('adds the current unit to the request', async () => {
    const units = new UnitStore(null);
    units.set('unit-1');
    const next = ok();

    await withOrgUnit(units, next)(new Request('http://api.test/api/v1/people'));

    expect(next.mock.calls[0]?.[0].headers.get('X-Org-Unit')).toBe('unit-1');
  });

  it('leaves a request alone that already names its unit, or when no unit is chosen', async () => {
    const units = new UnitStore(null);
    const next = ok();

    await withOrgUnit(units, next)(new Request('http://api.test/a'));
    units.set('unit-1');
    await withOrgUnit(
      units,
      next,
    )(new Request('http://api.test/b', { headers: { 'X-Org-Unit': 'unit-2' } }));

    expect(next.mock.calls[0]?.[0].headers.has('X-Org-Unit')).toBe(false);
    expect(next.mock.calls[1]?.[0].headers.get('X-Org-Unit')).toBe('unit-2');
  });

  it('remembers the choice across reloads', () => {
    const storage = new Map<string, string>();
    const fakeStorage = {
      getItem: (k: string) => storage.get(k) ?? null,
      setItem: (k: string, v: string) => void storage.set(k, v),
      removeItem: (k: string) => void storage.delete(k),
    } as Storage;

    new UnitStore(fakeStorage).set('unit-9');

    expect(new UnitStore(fakeStorage).get()).toBe('unit-9');
  });
});
