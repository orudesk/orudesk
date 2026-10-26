import type { UnitStore } from './unitStore';

export const ORG_UNIT_HEADER = 'X-Org-Unit';

/** Adds the current unit to every API request, unless the caller already chose one for this request. */
export function withOrgUnit(
  units: UnitStore,
  next: (request: Request) => Promise<Response>,
): (request: Request) => Promise<Response> {
  return (request) => {
    const unitId = units.get();
    if (unitId && !request.headers.has(ORG_UNIT_HEADER)) {
      request.headers.set(ORG_UNIT_HEADER, unitId);
    }
    return next(request);
  };
}
