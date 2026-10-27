import { useQuery } from '@tanstack/react-query';
import { useApi } from '../ServicesContext';

/** The tenant's org units as a tree (GET /api/v1/org-units). */
export function useOrgTree() {
  const api = useApi();
  return useQuery({
    queryKey: ['org-units', 'tree'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/org-units');
      if (error) throw new Error('Could not load the organisation');
      return data;
    },
    staleTime: 5 * 60_000, // units change rarely
  });
}

/** The current unit and the user's roles there (GET /api/v1/org-units/current, with X-Org-Unit). */
export function useCurrentUnitAccess(unitId: string | null) {
  const api = useApi();
  return useQuery({
    queryKey: ['org-units', 'current', unitId],
    enabled: unitId !== null,
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/org-units/current', {
        params: { header: { 'X-Org-Unit': unitId! } },
      });
      if (error) throw new Error('Could not load this unit');
      return data;
    },
  });
}
