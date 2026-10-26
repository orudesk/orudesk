import { Loader, Stack, Text, Title } from '@mantine/core';
import { usePageTitle } from '../layout/usePageTitle';
import { useMe } from '../queries/me';

export function HomePage() {
  usePageTitle('Home');
  const me = useMe();

  return (
    <Stack>
      <Title order={1}>Home</Title>
      {me.isPending && <Loader size="sm" aria-label="Loading" />}
      {me.isError && <Text role="alert">{me.error.message}</Text>}
      {me.data && (
        <Text>
          Signed in as <strong>{me.data.email}</strong> ({me.data.roles.join(', ') || 'no roles'})
        </Text>
      )}
    </Stack>
  );
}
