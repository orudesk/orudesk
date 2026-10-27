import type { Schemas } from '@orudesk/api-client';

export interface FlatUnit {
  id: string;
  name: string;
  type: string;
  depth: number;
}

/** The tree as a list in display order (parents before children), each with its depth for indenting. */
export function flatten(node: Schemas['OrgUnitNode'], depth = 0): FlatUnit[] {
  return [
    { id: node.id, name: node.name, type: node.type, depth },
    ...node.children.flatMap((child) => flatten(child, depth + 1)),
  ];
}

/**
 * The units this user may pick: everything for a tenant-wide grant, otherwise each granted unit and everything
 * below it. The same rule as the API's OrgAccessService; the API still checks, this only keeps the list short.
 */
export function accessibleUnits(
  tree: Schemas['OrgUnitNode'],
  grants: Schemas['Grant'][],
): FlatUnit[] {
  if (grants.some((grant) => !grant.orgUnitId)) return flatten(tree);
  const granted = new Set(grants.map((grant) => grant.orgUnitId));
  const result: FlatUnit[] = [];
  const walk = (node: Schemas['OrgUnitNode'], inside: boolean, depth: number) => {
    const here = inside || granted.has(node.id);
    if (here) result.push({ id: node.id, name: node.name, type: node.type, depth });
    node.children.forEach((child) => walk(child, here, here ? depth + 1 : depth));
  };
  walk(tree, false, 0);
  return result;
}

/**
 * Where to start someone who hasn't picked a unit yet. Everyone has the tenant-wide USER role, so every unit is
 * open to everyone; but a principal of North School wants North School, not the head office where they're just
 * a USER. So: the first unit they were given a role in, else the first unit there is.
 */
export function preferredUnit(units: FlatUnit[], grants: Schemas['Grant'][]): FlatUnit | undefined {
  const scoped = new Set(grants.flatMap((grant) => (grant.orgUnitId ? [grant.orgUnitId] : [])));
  return units.find((unit) => scoped.has(unit.id)) ?? units[0];
}
