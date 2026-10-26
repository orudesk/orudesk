import { describe, expect, it } from 'vitest';
import { coreModule } from './coreModule';
import type { OrudeskModule } from './module';
import { buildRoutes } from './routes';

describe('buildRoutes', () => {
  it("puts every module's pages inside the shell", () => {
    const donors: OrudeskModule = { id: 'ngo', routes: [{ path: '/donors', element: null }] };

    const json = JSON.stringify(buildRoutes([coreModule, donors]), (key, value) =>
      key === 'element' || key === 'errorElement' || key === 'lazy' ? undefined : value,
    );

    expect(json).toContain('"/donors"');
    expect(json).toContain('"/people"');
  });

  it('refuses two modules with a page at the same path', () => {
    const clash: OrudeskModule = { id: 'school', routes: [{ path: '/people', element: null }] };

    expect(() => buildRoutes([coreModule, clash])).toThrow(
      'Modules "core" and "school" both have a page at /people',
    );
  });
});
