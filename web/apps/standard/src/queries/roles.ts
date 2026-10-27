import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useApi } from '../ServicesContext';
import { problemMessage } from './problem';

/** The roles there are (GET /api/v1/roles), for pickers. */
export function useRoles() {
  const api = useApi();
  return useQuery({
    queryKey: ['roles'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/roles');
      if (error) throw new Error('Could not load the roles');
      return data;
    },
    staleTime: 5 * 60_000, // roles are declared by the modules, they change with a release
  });
}

export interface GrantChange {
  userId: string;
  role: string;
  /** null: the whole tenant */
  orgUnitId: string | null;
}

/**
 * Give or take away one grant. Afterwards every cached "users" query is out of date (the list, and "me" if you
 * changed your own roles), so they're all refetched.
 */
export function useChangeGrant() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ change, add }: { change: GrantChange; add: boolean }) => {
      const { error } = add
        ? await api.POST('/api/v1/users/{userId}/roles', {
            params: { path: { userId: change.userId } },
            body: { role: change.role, orgUnitId: change.orgUnitId },
          })
        : await api.DELETE('/api/v1/users/{userId}/roles/{role}', {
            params: {
              path: { userId: change.userId, role: change.role },
              query: change.orgUnitId ? { orgUnitId: change.orgUnitId } : {},
            },
          });
      if (error !== undefined) throw new Error(problemMessage(error, 'The change was not saved.'));
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  });
}
