import { Alert, Anchor, Button, Center, Paper, Stack, Text, TextInput, Title } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { useAuth } from '../auth/useAuth';
import { usePageTitle } from '../layout/usePageTitle';

/**
 * "Email me a reset link" (Day 55). The answer never says whether the email has an account, the same as the
 * API: a page that said "no such account" would let anyone check who works at a hospital.
 */
export function ForgotPasswordPage() {
  usePageTitle('Forgot your password');
  const { requestPasswordReset } = useAuth();
  const [sentTo, setSentTo] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const email = String(new FormData(event.currentTarget).get('email')).trim();
    setSubmitting(true);
    setFailed(false);
    const accepted = await requestPasswordReset(email);
    setSubmitting(false);
    if (accepted) setSentTo(email);
    else setFailed(true);
  }

  return (
    <Center component="main" mih="100vh" p="md">
      <Paper withBorder shadow="sm" p="xl" w="100%" maw={400}>
        <Stack>
          <Title order={1} size="h2">
            Forgot your password?
          </Title>
          {sentTo ? (
            <Alert color="green" role="status" title="Check your email">
              If {sentTo} has an Orudesk account, we&apos;ve sent it a link to choose a new
              password. The link works once, for 30 minutes. Nothing there? Check the spam folder,
              or ask again.
            </Alert>
          ) : (
            <form onSubmit={onSubmit}>
              <Stack>
                <Text size="sm">
                  Type the email you log in with, and we&apos;ll send you a link to choose a new
                  password.
                </Text>
                <TextInput
                  label="Email"
                  name="email"
                  type="email"
                  autoComplete="username"
                  required
                  autoFocus
                />
                {failed && (
                  <Alert color="red" icon={<IconAlertCircle size={18} />} role="alert">
                    Couldn&apos;t send the request. Check your connection and try again.
                  </Alert>
                )}
                <Button type="submit" loading={submitting} fullWidth>
                  Email me a link
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
