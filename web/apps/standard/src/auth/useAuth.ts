import { useSyncExternalStore } from 'react';
import { useServices } from '../ServicesContext';

/** The login state, re-rendering the component when it changes, plus the actions. */
export function useAuth() {
  const { session } = useServices();
  const status = useSyncExternalStore(session.subscribe, session.getStatus);
  return { status, login: session.login, logout: session.logout };
}
