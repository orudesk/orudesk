import { describe, expect, it } from 'vitest';
import * as api from './index';
import * as testing from './testing';

/**
 * The package's public API (Day 64). Editions are built against these names, so removing or renaming one breaks
 * them: that's a major version, and this list makes it a deliberate change in a review rather than an accident.
 * Adding a name is a minor version: add it here too. (Type-only exports don't exist at run time; the package
 * test and the editions' type checks guard those.)
 */
describe('the public API of @orudesk/web-core', () => {
  it('exports exactly these names', () => {
    expect(Object.keys(api).sort()).toEqual([
      'App',
      'EditConflictAlert',
      'EditConflictError',
      'ValidationError',
      'buildRoutes',
      'coreModule',
      'createServices',
      'formatDate',
      'problemMessage',
      'saveError',
      'startOrudeskApp',
      'useApi',
      'useCurrentUnit',
      'useCurrentUnitAccess',
      'useMe',
      'usePageTitle',
      'useServices',
    ]);
  });

  it('exports these test helpers', () => {
    expect(Object.keys(testing).sort()).toEqual(['fakeApi', 'json', 'tokens']);
  });
});
