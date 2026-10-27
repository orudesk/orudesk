import { Badge, Card, Group, Loader, Stack, Text, Title } from '@mantine/core';
import { usePageTitle } from '../layout/usePageTitle';
import { useMe } from '../queries/me';
import { useCurrentUnitAccess } from '../queries/orgUnits';
import { useCurrentUnit } from '../units/useCurrentUnit';

export function HomePage() {
  usePageTitle('Home');
  const me = useMe();
  const { unitId } = useCurrentUnit();
  const here = useCurrentUnitAccess(unitId);

  return (
    <Stack>
      <Title order={1}>Home</Title>
      {me.isPending && <Loader size="sm" aria-label="Loading" />}
      {me.isError && <Text role="alert">{me.error.message}</Text>}
      {me.data && (
        <Text>
          Signed in as <strong>{me.data.email}</strong>
        </Text>
      )}
      {here.data && (
        <Card withBorder maw={480}>
          <Text size="sm" c="dimmed">
            Working in
          </Text>
          <Text fw={600} size="lg">
            {here.data.name}
          </Text>
          <Group gap="xs" mt="sm" aria-label="Your roles here">
            {here.data.roles.length === 0 ? (
              <Text size="sm">You have no roles here.</Text>
            ) : (
              here.data.roles.map((role) => (
                <Badge key={role} variant="light">
                  {role}
                </Badge>
              ))
            )}
          </Group>
        </Card>
      )}
    </Stack>
  );
}
