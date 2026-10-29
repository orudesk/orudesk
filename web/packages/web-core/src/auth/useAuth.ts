import { useSyncExternalStore } from 'react';
import { useServices } from '../ServicesContext';

/** The login state, re-rendering the component when it changes, plus the actions. */
export function useAuth() {
  const { session } = useServices();
  const status = useSyncExternalStore(session.subscribe, session.getStatus);
  // Read during render: it changes only together with the status, which is what triggers the re-render.
  return {
    status,
    endReason: session.getEndReason(),
    login: session.login,
    loginSecondFactor: session.loginSecondFactor,
    logout: session.logout,
    requestPasswordReset: session.requestPasswordReset,
    resetPassword: session.resetPassword,
  };
}
