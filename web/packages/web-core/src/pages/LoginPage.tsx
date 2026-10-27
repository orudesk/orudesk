import {
  Alert,
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
import { Navigate, useSearchParams } from 'react-router';
import type { LoginResult } from '../auth/session';
import { safeRedirect } from '../auth/safeRedirect';
import { useAuth } from '../auth/useAuth';
import { usePageTitle } from '../layout/usePageTitle';

const failures: Record<Exclude<LoginResult, { ok: true }>['reason'], string> = {
  credentials: 'Wrong email or password.',
  unavailable: 'Login is unavailable right now. Please try again in a minute.',
  network: "Can't reach the server. Check your connection.",
  other: 'Login failed.',
};

export function LoginPage() {
  usePageTitle('Log in');
  const { status, endReason, login } = useAuth();
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
    <Center component="main" mih="100vh" p="md">
      <Paper withBorder shadow="sm" p="xl" w="100%" maw={400}>
        <form onSubmit={onSubmit}>
          <Stack>
            <Title order={1} size="h2">
              Log in to Orudesk
            </Title>
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
              Log in
            </Button>
          </Stack>
        </form>
      </Paper>
    </Center>
  );
}
