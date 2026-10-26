import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../auth/useAuth';
import { useApi } from '../ServicesContext';

export function HomePage() {
  const api = useApi();
  const { logout } = useAuth();
  const me = useQuery({
    queryKey: ['users', 'me'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/users/me');
      if (error) throw new Error('Could not load your profile');
      return data;
    },
  });

  return (
    <main>
      <h1>Orudesk</h1>
      {me.isPending && <p role="status">Loading...</p>}
      {me.isError && <p role="alert">{me.error.message}</p>}
      {me.data && (
        <p>
          Signed in as <strong>{me.data.email}</strong> ({me.data.roles.join(', ') || 'no roles'})
        </p>
      )}
      <button type="button" onClick={() => void logout()}>
        Log out
      </button>
    </main>
  );
}
