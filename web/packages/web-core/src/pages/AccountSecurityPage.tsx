import {
  Alert,
  Badge,
  Button,
  Code,
  Group,
  Image,
  Loader,
  Paper,
  PasswordInput,
  SimpleGrid,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import QRCode from 'qrcode';
import { useEffect, useState, type FormEvent } from 'react';
import { usePageTitle } from '../layout/usePageTitle';
import {
  useBeginTwoFactorSetup,
  useChangePassword,
  useConfirmTwoFactor,
  useDisableTwoFactor,
  useEndSessions,
  useMySessions,
  useNewRecoveryCodes,
  useTwoFactor,
} from '../queries/account';
import { ValidationError } from '../queries/formErrors';

/**
 * The user's own security (Day 70): the password, two-factor sign-in and where they're signed in. Every change
 * asks for the current password again, so a borrowed, unlocked laptop isn't enough to take the account over.
 */
export function AccountSecurityPage() {
  usePageTitle('Security');
  return (
    <Stack maw={720}>
      <Title order={1}>Security</Title>
      <PasswordSection />
      <TwoFactorSection />
      <SessionsSection />
    </Stack>
  );
}

/** The message for one field, or the general one when the error isn't about that field. */
function fieldError(error: Error | null, field: string): string | undefined {
  return error instanceof ValidationError ? error.fields[field] : undefined;
}

function generalError(error: Error | null, fields: string[]): string | null {
  if (!error) return null;
  if (error instanceof ValidationError && fields.some((f) => error.fields[f])) return null;
  return error.message;
}

function PasswordSection() {
  const change = useChangePassword();
  const [done, setDone] = useState(false);

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    setDone(false);
    change.mutate(
      { password: String(form.get('password')), newPassword: String(form.get('newPassword')) },
      {
        onSuccess: () => {
          formElement.reset();
          setDone(true);
        },
      },
    );
  }

  const general = generalError(change.error, ['password', 'newPassword']);
  return (
    <Paper withBorder p="lg" component="section" aria-labelledby="password-heading">
      <form onSubmit={onSubmit}>
        <Stack>
          <Title order={2} size="h3" id="password-heading">
            Password
          </Title>
          <PasswordInput
            label="Current password"
            name="password"
            autoComplete="current-password"
            required
            error={fieldError(change.error, 'password')}
          />
          <PasswordInput
            label="New password"
            description="At least 8 characters. Passwords known from data breaches are refused."
            name="newPassword"
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            required
            error={fieldError(change.error, 'newPassword')}
          />
          {general && (
            <Alert color="red" role="alert">
              {general}
            </Alert>
          )}
          {done && (
            <Alert color="green" role="status">
              Password changed. Every other session was signed out.
            </Alert>
          )}
          <Group>
            <Button type="submit" loading={change.isPending}>
              Change password
            </Button>
          </Group>
        </Stack>
      </form>
    </Paper>
  );
}

function TwoFactorSection() {
  const status = useTwoFactor();
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);

  return (
    <Paper withBorder p="lg" component="section" aria-labelledby="two-factor-heading">
      <Stack>
        <Group justify="space-between">
          <Title order={2} size="h3" id="two-factor-heading">
            Two-factor sign-in
          </Title>
          {status.data && (
            <Badge color={status.data.enabled ? 'teal' : 'gray'}>
              {status.data.enabled ? 'On' : 'Off'}
            </Badge>
          )}
        </Group>
        <Text size="sm" c="dimmed">
          After your password, Orudesk asks for a six-digit code from an authenticator app on your
          phone, so a stolen password alone can&apos;t open your account.
        </Text>
        {status.isPending && <Loader size="sm" aria-label="Loading" />}
        {status.isError && <Text role="alert">{status.error.message}</Text>}
        {recoveryCodes && (
          <RecoveryCodesList codes={recoveryCodes} onDone={() => setRecoveryCodes(null)} />
        )}
        {status.data && !recoveryCodes && !status.data.available && !status.data.enabled && (
          <Text>
            Two-factor sign-in isn&apos;t set up on this server yet. Ask your administrator.
          </Text>
        )}
        {status.data && !recoveryCodes && status.data.available && !status.data.enabled && (
          <TwoFactorSetup onEnabled={setRecoveryCodes} />
        )}
        {status.data && !recoveryCodes && status.data.enabled && (
          <TwoFactorOn
            recoveryCodesLeft={status.data.recoveryCodesLeft}
            onNewCodes={setRecoveryCodes}
          />
        )}
      </Stack>
    </Paper>
  );
}

function TwoFactorSetup({ onEnabled }: { onEnabled: (codes: string[]) => void }) {
  const begin = useBeginTwoFactorSetup();
  const confirm = useConfirmTwoFactor();
  const qr = useQrCode(begin.data?.otpauthUri);

  if (!begin.data) {
    return (
      <form
        onSubmit={(event) => {
          event.preventDefault();
          begin.mutate(String(new FormData(event.currentTarget).get('password')));
        }}
      >
        <Stack>
          <PasswordInput
            label="Current password"
            name="password"
            autoComplete="current-password"
            required
            error={fieldError(begin.error, 'password')}
          />
          {generalError(begin.error, ['password']) && (
            <Alert color="red" role="alert">
              {generalError(begin.error, ['password'])}
            </Alert>
          )}
          <Group>
            <Button type="submit" loading={begin.isPending}>
              Set up two-factor sign-in
            </Button>
          </Group>
        </Stack>
      </form>
    );
  }

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        confirm.mutate(String(new FormData(event.currentTarget).get('code')), {
          onSuccess: onEnabled,
        });
      }}
    >
      <Stack>
        <Text>
          1. Scan this code with an authenticator app (Google Authenticator, Microsoft
          Authenticator, 2FAS, Aegis...).
        </Text>
        {qr && <Image src={qr} alt="QR code for your authenticator app" w={200} h={200} />}
        <Text size="sm">
          Can&apos;t scan? Type this key into the app instead:{' '}
          <Code>{groupKey(begin.data.secret)}</Code>
        </Text>
        <TextInput
          label="2. Enter the six-digit code the app shows"
          name="code"
          autoComplete="one-time-code"
          inputMode="numeric"
          required
          error={fieldError(confirm.error, 'code')}
        />
        {generalError(confirm.error, ['code']) && (
          <Alert color="red" role="alert">
            {generalError(confirm.error, ['code'])}
          </Alert>
        )}
        <Group>
          <Button type="submit" loading={confirm.isPending}>
            Turn on
          </Button>
        </Group>
      </Stack>
    </form>
  );
}

function TwoFactorOn({
  recoveryCodesLeft,
  onNewCodes,
}: {
  recoveryCodesLeft: number;
  onNewCodes: (codes: string[]) => void;
}) {
  const newCodes = useNewRecoveryCodes();
  const disable = useDisableTwoFactor();

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const password = String(new FormData(event.currentTarget).get('password'));
    const action = (event.nativeEvent as SubmitEvent).submitter?.getAttribute('value');
    if (action === 'disable') disable.mutate(password);
    else newCodes.mutate(password, { onSuccess: onNewCodes });
  }

  const error = newCodes.error ?? disable.error;
  return (
    <form onSubmit={onSubmit}>
      <Stack>
        <Text>
          {recoveryCodesLeft} recovery {recoveryCodesLeft === 1 ? 'code' : 'codes'} left.
          {recoveryCodesLeft <= 3 && ' Make new ones before you run out.'}
        </Text>
        <PasswordInput
          label="Current password"
          name="password"
          autoComplete="current-password"
          required
          error={fieldError(error, 'password')}
        />
        {generalError(error, ['password']) && (
          <Alert color="red" role="alert">
            {generalError(error, ['password'])}
          </Alert>
        )}
        <Group>
          <Button
            type="submit"
            name="action"
            value="codes"
            variant="default"
            loading={newCodes.isPending}
          >
            New recovery codes
          </Button>
          <Button
            type="submit"
            name="action"
            value="disable"
            color="red"
            variant="light"
            loading={disable.isPending}
          >
            Turn off
          </Button>
        </Group>
      </Stack>
    </form>
  );
}

function RecoveryCodesList({ codes, onDone }: { codes: string[]; onDone: () => void }) {
  return (
    <Alert color="yellow" title="Save your recovery codes" role="status">
      <Stack>
        <Text size="sm">
          If you lose your phone, each of these codes signs you in once instead of the app&apos;s
          code. They won&apos;t be shown again: print them or keep them in a password manager.
        </Text>
        <SimpleGrid cols={2} component="ul" style={{ listStyle: 'none', padding: 0 }}>
          {codes.map((code) => (
            <li key={code}>
              <Code>{code}</Code>
            </li>
          ))}
        </SimpleGrid>
        <Group>
          <Button
            variant="default"
            onClick={() => void navigator.clipboard?.writeText(codes.join('\n'))}
          >
            Copy
          </Button>
          <Button onClick={onDone}>I&apos;ve saved them</Button>
        </Group>
      </Stack>
    </Alert>
  );
}

function SessionsSection() {
  const sessions = useMySessions();
  const end = useEndSessions();
  const others = sessions.data?.filter((s) => !s.current).length ?? 0;

  return (
    <Paper withBorder p="lg" component="section" aria-labelledby="sessions-heading">
      <Stack>
        <Group justify="space-between">
          <Title order={2} size="h3" id="sessions-heading">
            Where you&apos;re signed in
          </Title>
          {others > 0 && (
            <Button
              variant="light"
              color="red"
              loading={end.isPending}
              onClick={() => end.mutate(undefined)}
            >
              Sign out everywhere else
            </Button>
          )}
        </Group>
        {sessions.isPending && <Loader size="sm" aria-label="Loading" />}
        {sessions.isError && <Text role="alert">{sessions.error.message}</Text>}
        {end.isError && (
          <Alert color="red" role="alert">
            {end.error.message}
          </Alert>
        )}
        {sessions.data && (
          <Table.ScrollContainer minWidth={520}>
            <Table>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Device</Table.Th>
                  <Table.Th>Address</Table.Th>
                  <Table.Th>Last used</Table.Th>
                  <Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {sessions.data.map((session) => (
                  <Table.Tr key={session.id}>
                    <Table.Td>{session.device}</Table.Td>
                    <Table.Td>{session.ipAddress}</Table.Td>
                    <Table.Td>
                      <time dateTime={session.lastUsedAt}>{formatTime(session.lastUsedAt)}</time>
                    </Table.Td>
                    <Table.Td>
                      {session.current ? (
                        <Badge variant="light">This browser</Badge>
                      ) : (
                        <Button
                          size="xs"
                          variant="subtle"
                          color="red"
                          aria-label={`Sign out ${session.device}`}
                          onClick={() => end.mutate(session.id)}
                        >
                          Sign out
                        </Button>
                      )}
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        )}
      </Stack>
    </Paper>
  );
}

function formatTime(instant: string): string {
  return new Date(instant).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

/** "JBSW Y3DP ..." is easier to type than one 32-letter run. */
function groupKey(secret: string): string {
  return secret.match(/.{1,4}/g)?.join(' ') ?? secret;
}

/** The QR code as an SVG data URL, drawn in the browser: the secret never goes to a QR service. */
function useQrCode(text: string | undefined): string | null {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    if (!text) return;
    let current = true;
    void QRCode.toString(text, { type: 'svg', margin: 1 }).then((svg) => {
      if (current) setUrl(`data:image/svg+xml;utf8,${encodeURIComponent(svg)}`);
    });
    return () => {
      current = false;
    };
  }, [text]);
  return text ? url : null;
}
