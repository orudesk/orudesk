import { Alert, Anchor, Button, Center, Paper, PasswordInput, Stack, Title } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router';
import type { ResetResult } from '../auth/session';
import { useAuth } from '../auth/useAuth';
import { usePageTitle } from '../layout/usePageTitle';

const failures: Record<Exclude<ResetResult, { ok: true }>['reason'], string> = {
  invalidLink: 'This link has expired or was already used.',
  invalidPassword: 'Choose a password of 8 to 72 characters.',
  network: "Can't reach the server. Check your connection.",
  other: "Couldn't change the password.",
};

/** The page the reset email links to: /reset-password?token=... (Day 55). */
export function ResetPasswordPage() {
  usePageTitle('Choose a new password');
  const { resetPassword } = useAuth();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [result, setResult] = useState<ResetResult | null>(null);
  const [mismatch, setMismatch] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) return;
    const form = new FormData(event.currentTarget);
    const password = String(form.get('password'));
    // Checked here only: the API never sees the second copy. A typo in a password you can't see is easy to make.
    if (password !== String(form.get('confirm'))) {
      setMismatch(true);
      return;
    }
    setMismatch(false);
    setSubmitting(true);
    setResult(await resetPassword(token, password));
    setSubmitting(false);
  }

  const linkDead = !token || (result && !result.ok && result.reason === 'invalidLink');

  return (
    <Center component="main" mih="100vh" p="md">
      <Paper withBorder shadow="sm" p="xl" w="100%" maw={400}>
        <Stack>
          <Title order={1} size="h2">
            Choose a new password
          </Title>
          {result?.ok ? (
            <>
              <Alert color="green" role="status" title="Password changed">
                You&apos;ve been logged out everywhere. Log in with your new password.
              </Alert>
              <Button component={Link} to="/login" fullWidth>
                Log in
              </Button>
            </>
          ) : linkDead ? (
            <>
              <Alert color="red" icon={<IconAlertCircle size={18} />} role="alert">
                {token ? failures.invalidLink : 'This link is incomplete.'} Ask for a new one.
              </Alert>
              <Button component={Link} to="/forgot-password" fullWidth>
                Email me a new link
              </Button>
            </>
          ) : (
            <form onSubmit={onSubmit}>
              <Stack>
                <PasswordInput
                  label="New password"
                  name="password"
                  autoComplete="new-password"
                  description="8 to 72 characters"
                  minLength={8}
                  maxLength={72}
                  required
                  autoFocus
                />
                <PasswordInput
                  label="New password again"
                  name="confirm"
                  autoComplete="new-password"
                  required
                />
                {mismatch && (
                  <Alert color="red" icon={<IconAlertCircle size={18} />} role="alert">
                    The two passwords are different.
                  </Alert>
                )}
                {result && !result.ok && (
                  <Alert color="red" icon={<IconAlertCircle size={18} />} role="alert">
                    {result.message ?? failures[result.reason]}
                  </Alert>
                )}
                <Button type="submit" loading={submitting} fullWidth>
                  Change password
                </Button>
              </Stack>
            </form>
          )}
          <Anchor component={Link} to="/login" size="sm" ta="center">
            Back to log in
          </Anchor>
        </Stack>
      </Paper>
    </Center>
  );
}
