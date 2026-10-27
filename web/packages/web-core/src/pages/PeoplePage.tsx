import { Anchor, Button, Group, Loader, Stack, Table, Text, TextInput, Title } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { IconPlus, IconSearch } from '@tabler/icons-react';
import { useState } from 'react';
import { Link } from 'react-router';
import { usePageTitle } from '../layout/usePageTitle';
import { usePeople } from '../queries/people';
import { formatDate } from './formatDate';

/** The person registry: find someone by name, phone or email, or add someone new. */
export function PeoplePage() {
  usePageTitle('People');
  const [search, setSearch] = useState('');
  const [debounced] = useDebouncedValue(search.trim(), 300);
  const people = usePeople(debounced);
  const rows = people.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={1}>People</Title>
        <Button component={Link} to="/people/new" leftSection={<IconPlus size={16} />}>
          Add person
        </Button>
      </Group>
      <TextInput
        label="Search"
        description="Part of a name, phone number or email"
        leftSection={<IconSearch size={16} />}
        value={search}
        onChange={(event) => setSearch(event.currentTarget.value)}
        maw={360}
      />

      {people.isPending && <Loader size="sm" aria-label="Loading" />}
      {people.isError && <Text role="alert">{people.error.message}</Text>}
      {people.isSuccess && rows.length === 0 && (
        <Text>{debounced ? `Nobody matches “${debounced}”.` : 'Nobody is registered yet.'}</Text>
      )}

      {rows.length > 0 && (
        <Table.ScrollContainer minWidth={640}>
          <Table striped highlightOnHover>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Name</Table.Th>
                <Table.Th>Date of birth</Table.Th>
                <Table.Th>Phone</Table.Th>
                <Table.Th>Email</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {rows.map((person) => (
                <Table.Tr key={person.id}>
                  <Table.Td>
                    <Anchor component={Link} to={`/people/${person.id}`}>
                      {person.lastName}, {person.firstName}
                    </Anchor>
                  </Table.Td>
                  <Table.Td>{formatDate(person.dateOfBirth)}</Table.Td>
                  <Table.Td>{person.phone ?? '—'}</Table.Td>
                  <Table.Td>{person.email ?? '—'}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      {people.hasNextPage && (
        <Group>
          <Button
            variant="default"
            onClick={() => void people.fetchNextPage()}
            loading={people.isFetchingNextPage}
          >
            Load more
          </Button>
        </Group>
      )}
    </Stack>
  );
}
