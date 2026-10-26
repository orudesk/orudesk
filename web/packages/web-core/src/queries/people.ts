import type { Schemas } from '@orudesk/api-client';
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useApi } from '../ServicesContext';
import { problemMessage } from './problem';

export const PEOPLE_PAGE_SIZE = 20;

/** The registry, a page at a time, sorted by name (GET /api/v1/people). */
export function usePeople(search: string) {
  const api = useApi();
  return useInfiniteQuery({
    queryKey: ['people', 'list', search],
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam }) => {
      const { data, error } = await api.GET('/api/v1/people', {
        params: {
          query: {
            size: PEOPLE_PAGE_SIZE,
            ...(search && { q: search }),
            ...(pageParam && { after: pageParam }),
          },
        },
      });
      if (error) throw new Error('Could not load the people');
      return data;
    },
    getNextPageParam: (page) => page.nextCursor ?? undefined,
  });
}

/** One person; null when there is no such person (404), so the page can say so. */
export function usePerson(personId: string) {
  const api = useApi();
  return useQuery({
    queryKey: ['people', 'one', personId],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/people/{personId}', {
        params: { path: { personId } },
      });
      if (response.status === 404 || response.status === 400) return null; // 400: not even a valid id
      if (error) throw new Error('Could not load this person');
      return data;
    },
  });
}

/**
 * The API refused the form: `fields` has a message per field name ("firstName": "must not be blank"), straight
 * from the server's validation, so the browser never has to repeat the rules.
 */
export class ValidationError extends Error {
  constructor(
    message: string,
    readonly fields: Record<string, string>,
  ) {
    super(message);
  }
}

export function useRegisterPerson() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (person: Schemas['RegisterPersonRequest']) => {
      const { data, error, response } = await api.POST('/api/v1/people', { body: person });
      if (data) return data.id;
      if (response.status === 400) {
        const fields = (error as { errors?: Record<string, string> } | undefined)?.errors ?? {};
        throw new ValidationError(problemMessage(error, 'Some fields are invalid.'), fields);
      }
      throw new Error(problemMessage(error, 'The person was not saved.'));
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['people', 'list'] }),
  });
}
