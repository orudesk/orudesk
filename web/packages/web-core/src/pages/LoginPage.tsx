import {
  Alert,
  Anchor,
  Button,
  Center,
  Paper,
  PasswordInput,
  Stack,
  TextInput,
  Title,
} from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { useState, type FormEvent } from 'react';
import { Link, Navigate, useSearchParams } from 'react-router';
import type { LoginResult } from '../auth/session';
import { safeRedirect } from '../auth/safeRedirect';
import { useAuth } from '../auth/useAuth';
import { usePageTitle } from '../layout/usePageTitle';

type Failure = Exclude<LoginResult, { ok: true }>;

const failures: Record<Failure['reason'], string> = {
  credentials: 'Wrong email or password.',
  secondFactor: 'Enter the code from your authenticator app.',
  wrongCode: "That code isn't right, or was already used. Try the newest one.",
  expired: 'That took too long. Please enter your email and password again.',
  unavailable: 'Login is unavailable right now. Please try again in a minute.',
  network: "Can't reach the server. Check your connection.",
  other: 'Login failed.',
  throttled: 'Too many failed attempts.',
};

/** "Try again in 13 minutes": rounded up, so nobody is told 0 minutes. */
function failureMessage(result: Failure): string {
  if (result.message) return result.message;
  if (result.reason !== 'throttled') return failures[result.reason];
  const minutes = Math.ceil(result.retryAfterSeconds / 60);
  return `Too many failed attempts. Try again in ${minutes} ${minutes === 1 ? 'minute' : 'minutes'}.`;
}

export function LoginPage() {
  usePageTitle('Log in');
  const { status, endReason, login, loginSecondFactor } = useAuth();
  const [searchParams] = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  // Set once the password was right for a user with two-factor sign-in: the form asks for the code instead
  const [loginToken, setLoginToken] = useState<string | null>(null);

  if (status === 'signedIn') {
    return <Navigate to={safeRedirect(searchParams.get('from'))} replace />;
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSubmitting(true);
    setError(null);
    const result = loginToken
      ? await loginSecondFactor(loginToken, String(form.get('code')))
      : await login(String(form.get('email')), String(form.get('password')));
    setSubmitting(false);
    if (result.ok) return; // the status becomes signedIn, and the <Navigate> above takes over
    if (result.reason === 'secondFactor') {
      setLoginToken(result.loginToken);
      return;
    }
    if (result.reason === 'expired') setLoginToken(null);
    setError(failureMessage(result));
  }

  return (
    <Center component="main" mih="100vh" p="md">
      <Paper withBorder shadow="sm" p="xl" w="100%" maw={400}>
        <form onSubmit={onSubmit}>
          <Stack>
            <Title order={1} size="h2">
              Log in to Orudesk
            </Title>
            {loginToken ? (
              <TextInput
                key="code"
                label="Code from your authenticator app"
                description="Lost your phone? Enter one of your recovery codes instead."
                name="code"
                autoComplete="one-time-code"
                inputMode="text"
                required
                autoFocus
              />
            ) : (
              <>
                <TextInput
                  label="Email"
                  name="email"
                  type="email"
                  autoComplete="username"
                  required
                  autoFocus
                />
                <PasswordInput
                  label="Password"
                  name="password"
                  autoComplete="current-password"
                  required
                />
              </>
            )}
            {!error && endReason && (
              <Alert color="blue" role="status">
                {endReason === 'expired'
                  ? 'Your session has ended. Please log in again.'
                  : "Can't reach Orudesk right now, so you're logged out until it's back."}
              </Alert>
            )}
            {error && (
              <Alert color="red" icon={<IconAlertCircle size={18} />} role="alert">
                {error}
              </Alert>
            )}
            <Button type="submit" loading={submitting} fullWidth>
              {loginToken ? 'Continue' : 'Log in'}
            </Button>
            {loginToken ? (
              <Anchor
                component="button"
                type="button"
                size="sm"
                onClick={() => setLoginToken(null)}
              >
                Start again
              </Anchor>
            ) : (
              <Anchor component={Link} to="/forgot-password" size="sm" ta="center">
                Forgot your password?
              </Anchor>
            )}
          </Stack>
        </form>
      </Paper>
    </Center>
  );
}
