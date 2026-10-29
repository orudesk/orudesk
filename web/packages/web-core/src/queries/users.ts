import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useApi } from '../ServicesContext';
import { problemMessage } from './problem';

export const USERS_PAGE_SIZE = 25;

/** Thrown when the API answers 403, so a page can say "not for you" rather than "something broke". */
export class ForbiddenError extends Error {
  constructor() {
    super("You don't have access to this page.");
  }
}

/**
 * The tenant's users, a page at a time (GET /api/v1/users). Each page carries the cursor of the next one, so
 * "Load more" asks for exactly what follows, even if users were added in the meantime.
 */
export function useUsers(search: string) {
  const api = useApi();
  return useInfiniteQuery({
    queryKey: ['users', 'list', search],
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam }) => {
      const { data, error, response } = await api.GET('/api/v1/users', {
        params: {
          // Leave out what isn't set (the tsconfig's exactOptionalPropertyTypes refuses "q: undefined")
          query: {
            size: USERS_PAGE_SIZE,
            ...(search && { q: search }),
            ...(pageParam && { after: pageParam }),
          },
        },
      });
      if (response.status === 403) throw new ForbiddenError();
      if (error) throw new Error('Could not load the users');
      return data;
    },
    getNextPageParam: (page) => page.nextCursor ?? undefined,
    retry: (count, error) => !(error instanceof ForbiddenError) && count < 1,
  });
}

/**
 * Disables or enables a user (Day 48). Disabling ends their sessions within seconds; nothing is deleted, so
 * enabling them again restores everything. The API refuses to disable your own account (409), and says why.
 */
export function useSetUserEnabled() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ userId, enabled }: { userId: string; enabled: boolean }) => {
      const params = { params: { path: { userId } } };
      const { error, response } = enabled
        ? await api.POST('/api/v1/users/{userId}/enable', params)
        : await api.POST('/api/v1/users/{userId}/disable', params);
      if (!response.ok) {
        throw new Error(
          problemMessage(
            error,
            enabled ? 'The user was not enabled.' : 'The user was not disabled.',
          ),
        );
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users', 'list'] });
      void queryClient.invalidateQueries({ queryKey: ['audit'] });
    },
  });
}
