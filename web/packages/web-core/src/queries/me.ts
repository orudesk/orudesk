import { useQuery } from '@tanstack/react-query';
import { useApi } from '../ServicesContext';

/** The logged-in user's profile (GET /api/v1/users/me), cached and shared by every component that asks. */
export function useMe() {
  const api = useApi();
  return useQuery({
    queryKey: ['users', 'me'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/users/me');
      if (error) throw new Error('Could not load your profile');
      return data;
    },
  });
}
