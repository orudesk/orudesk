import { buildRoutes } from '@orudesk/web-core';
import { describe, expect, it } from 'vitest';
import { modules } from './modules';

describe('the standard edition', () => {
  it('has a home page and no two modules on the same path', () => {
    expect(() => buildRoutes(modules)).not.toThrow();
    expect(modules.flatMap((module) => module.routes).some((route) => route.path === '/')).toBe(
      true,
    );
  });
});
