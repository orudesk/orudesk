import { Anchor, Card, Group, Loader, SimpleGrid, Stack, Text, Title } from '@mantine/core';
import { Link, useParams } from 'react-router';
import { usePageTitle } from '../layout/usePageTitle';
import { usePerson } from '../queries/people';
import { formatDate } from './formatDate';

export function PersonPage() {
  const { personId = '' } = useParams();
  const person = usePerson(personId);
  const name = person.data ? `${person.data.firstName} ${person.data.lastName}` : 'Person';
  usePageTitle(person.data === null ? 'Person not found' : name);

  return (
    <Stack>
      <Anchor component={Link} to="/people" size="sm">
        ← People
      </Anchor>
      {person.isPending && <Loader size="sm" aria-label="Loading" />}
      {person.isError && <Text role="alert">{person.error.message}</Text>}
      {person.data === null && (
        <>
          <Title order={1}>Person not found</Title>
          <Text>Nobody with this id is registered here.</Text>
        </>
      )}
      {person.data && (
        <>
          <Group justify="space-between" maw={640}>
            <Title order={1}>{name}</Title>
            <Anchor component={Link} to={`/activity?subject=${person.data.id}`} size="sm">
              History
            </Anchor>
          </Group>
          <Card withBorder maw={640}>
            <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="md" component="dl" m={0}>
              <Field label="Date of birth" value={formatDate(person.data.dateOfBirth)} />
              <Field label="Phone" value={person.data.phone ?? '—'} />
              <Field label="Email" value={person.data.email ?? '—'} />
            </SimpleGrid>
          </Card>
        </>
      )}
    </Stack>
  );
}

/** A label and its value, as a description list entry: screen readers announce them as a pair. */
function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <Text component="dt" size="sm" c="dimmed">
        {label}
      </Text>
      <Text component="dd" m={0}>
        {value}
      </Text>
    </div>
  );
}
