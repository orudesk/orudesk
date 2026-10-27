import { useInfiniteQuery } from '@tanstack/react-query';
import { useApi } from '../ServicesContext';

export const AUDIT_PAGE_SIZE = 50;

/**
 * The audit log, newest first, a page at a time (GET /api/v1/audit-entries); only what happened to `subjectId`
 * when one is given. The log only grows, so a page once read never changes: "Load more" just goes further back.
 */
export function useAuditEntries(subjectId: string | undefined) {
  const api = useApi();
  return useInfiniteQuery({
    queryKey: ['audit', subjectId ?? 'all'],
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam }) => {
      const { data, error } = await api.GET('/api/v1/audit-entries', {
        params: {
          query: {
            size: AUDIT_PAGE_SIZE,
            ...(subjectId && { subjectId }),
            ...(pageParam && { after: pageParam }),
          },
        },
      });
      if (error) throw new Error('Could not load the activity');
      return data;
    },
    getNextPageParam: (page) => page.nextCursor ?? undefined,
  });
}
