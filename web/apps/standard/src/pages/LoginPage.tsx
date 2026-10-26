import { useState, type FormEvent } from 'react';
import { Navigate, useSearchParams } from 'react-router';
import type { LoginResult } from '../auth/session';
import { safeRedirect } from '../auth/safeRedirect';
import { useAuth } from '../auth/useAuth';

const failures: Record<Exclude<LoginResult, { ok: true }>['reason'], string> = {
  credentials: 'Wrong email or password.',
  unavailable: 'Login is unavailable right now. Please try again in a minute.',
  network: "Can't reach the server. Check your connection.",
  other: 'Login failed.',
};

export function LoginPage() {
  const { status, login } = useAuth();
  const [searchParams] = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (status === 'signedIn') {
    return <Navigate to={safeRedirect(searchParams.get('from'))} replace />;
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSubmitting(true);
    setError(null);
    const result = await login(String(form.get('email')), String(form.get('password')));
    setSubmitting(false);
    if (!result.ok) setError(result.message ?? failures[result.reason]);
    // On success the status becomes signedIn, and the <Navigate> above takes over.
  }

  return (
    <main>
      <h1>Log in to Orudesk</h1>
      <form onSubmit={onSubmit} noValidate={false}>
        <label>
          Email
          <input name="email" type="email" autoComplete="username" required />
        </label>
        <label>
          Password
          <input name="password" type="password" autoComplete="current-password" required />
        </label>
        {error && <p role="alert">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Logging in...' : 'Log in'}
        </button>
      </form>
    </main>
  );
}
