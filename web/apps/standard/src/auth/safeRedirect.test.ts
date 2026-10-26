import { describe, expect, it } from 'vitest';
import { safeRedirect } from './safeRedirect';

describe('safeRedirect', () => {
  it('keeps paths inside the app', () => {
    expect(safeRedirect('/people?q=asha')).toBe('/people?q=asha');
  });

  it.each([
    null,
    '',
    'https://evil.example',
    '//evil.example',
    '/\\evil.example',
    'javascript:alert(1)',
  ])('sends %s to the home page instead', (from) => {
    expect(safeRedirect(from)).toBe('/');
  });
});
