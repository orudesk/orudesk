import type { Schemas } from '@orudesk/api-client';
import { describe, expect, it } from 'vitest';
import { accessibleUnits, flatten, preferredUnit } from './accessibleUnits';

const unit = (id: string, children: Schemas['OrgUnitNode'][] = []): Schemas['OrgUnitNode'] => ({
  id,
  name: id,
  code: id,
  type: 'SCHOOL',
  children,
});

//      group
//      ├── north
//      │   └── north-a
//      └── south
const tree = unit('group', [unit('north', [unit('north-a')]), unit('south')]);

describe('accessibleUnits', () => {
  it('lists the tree parents first, with depths', () => {
    expect(flatten(tree).map((u) => `${u.depth}:${u.id}`)).toEqual([
      '0:group',
      '1:north',
      '2:north-a',
      '1:south',
    ]);
  });

  it('gives a tenant-wide grant every unit', () => {
    expect(accessibleUnits(tree, [{ role: 'ADMIN' }]).map((u) => u.id)).toEqual([
      'group',
      'north',
      'north-a',
      'south',
    ]);
  });

  it('gives a unit grant that unit and everything below it, indented from there', () => {
    const units = accessibleUnits(tree, [{ role: 'ADMISSIONS_OFFICER', orgUnitId: 'north' }]);
    expect(units.map((u) => `${u.depth}:${u.id}`)).toEqual(['0:north', '1:north-a']);
  });

  it('gives nothing without grants', () => {
    expect(accessibleUnits(tree, [])).toEqual([]);
  });
});

describe('preferredUnit', () => {
  const units = [
    { id: 'group', name: 'Group', type: 'GROUP', depth: 0 },
    { id: 'north', name: 'North', type: 'SCHOOL', depth: 1 },
    { id: 'south', name: 'South', type: 'SCHOOL', depth: 1 },
  ];

  it('starts a school admin in their school, not at the top where they are only a USER', () => {
    const grants = [{ role: 'USER' }, { role: 'ADMIN', orgUnitId: 'south' }];
    expect(preferredUnit(units, grants)?.id).toBe('south');
  });

  it('starts someone with only tenant-wide roles at the top', () => {
    expect(preferredUnit(units, [{ role: 'ADMIN' }])?.id).toBe('group');
  });
});
