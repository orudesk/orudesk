import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from './useAuth';

/** Wraps every page that needs a login. Logged out: off to /login, which comes back here afterwards. */
export function RequireAuth() {
  const { status } = useAuth();
  const location = useLocation();

  if (status === 'restoring') {
    return <p role="status">Loading...</p>;
  }
  if (status === 'signedOut') {
    const from = location.pathname + location.search;
    return <Navigate to={`/login?from=${encodeURIComponent(from)}`} replace />;
  }
  return <Outlet />;
}
