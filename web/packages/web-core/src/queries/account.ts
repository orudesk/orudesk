import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useApi } from '../ServicesContext';
import { saveError } from './formErrors';
import { problemMessage } from './problem';

/**
 * The logged-in user's own security settings (Days 66-69): where they're signed in, two-factor sign-in and the
 * password. Wrong passwords and codes come back as ValidationError, with the field they belong under.
 */

const SESSIONS = ['users', 'me', 'sessions'] as const;
const TWO_FACTOR = ['users', 'me', 'two-factor'] as const;

export function useMySessions() {
  const api = useApi();
  return useQuery({
    queryKey: SESSIONS,
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/users/me/sessions');
      if (error) throw new Error('Could not load where you are signed in');
      return data;
    },
  });
}

/** Signs out one session, or (with no id) every session but this one. */
export function useEndSessions() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (sessionId?: string) => {
      const { error, response } = sessionId
        ? await api.DELETE('/api/v1/users/me/sessions/{sessionId}', {
            params: { path: { sessionId } },
          })
        : await api.POST('/api/v1/users/me/sessions/end-others');
      if (!response.ok) throw new Error(problemMessage(error, 'The session was not signed out.'));
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: SESSIONS }),
  });
}

export function useTwoFactor() {
  const api = useApi();
  return useQuery({
    queryKey: TWO_FACTOR,
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/users/me/two-factor');
      if (error) throw new Error('Could not load your two-factor sign-in');
      return data;
    },
  });
}

/** Step 1: a new secret for the app. Nothing is on until step 2 proves the app has it. */
export function useBeginTwoFactorSetup() {
  const api = useApi();
  return useMutation({
    mutationFn: async (password: string) => {
      const { data, error, response } = await api.POST('/api/v1/users/me/two-factor/setup', {
        body: { password },
      });
      if (!data) throw saveError(response, error, 'Two-factor sign-in could not be set up.');
      return data;
    },
  });
}

/** Step 2: the app's first code turns it on; the answer holds the recovery codes, shown this once. */
export function useConfirmTwoFactor() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (code: string) => {
      const { data, error, response } = await api.POST('/api/v1/users/me/two-factor/confirm', {
        body: { code },
      });
      if (!data) throw saveError(response, error, 'Two-factor sign-in was not turned on.');
      return data.recoveryCodes;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TWO_FACTOR }),
  });
}

export function useNewRecoveryCodes() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (password: string) => {
      const { data, error, response } = await api.POST(
        '/api/v1/users/me/two-factor/recovery-codes',
        {
          body: { password },
        },
      );
      if (!data) throw saveError(response, error, 'No new recovery codes were made.');
      return data.recoveryCodes;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TWO_FACTOR }),
  });
}

export function useDisableTwoFactor() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (password: string) => {
      const { error, response } = await api.POST('/api/v1/users/me/two-factor/disable', {
        body: { password },
      });
      if (!response.ok) throw saveError(response, error, 'Two-factor sign-in was not turned off.');
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TWO_FACTOR }),
  });
}

/** Changes the password; the API signs out every other session, so the list is reloaded. */
export function useChangePassword() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: { password: string; newPassword: string }) => {
      const { error, response } = await api.POST('/api/v1/users/me/password', { body });
      if (!response.ok) throw saveError(response, error, 'The password was not changed.');
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: SESSIONS }),
  });
}
