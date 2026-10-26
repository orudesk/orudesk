import type { Schemas } from '@orudesk/api-client';
import { describe, expect, it } from 'vitest';
import { accessibleUnits, flatten } from './accessibleUnits';

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
