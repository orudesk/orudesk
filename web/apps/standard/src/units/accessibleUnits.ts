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
