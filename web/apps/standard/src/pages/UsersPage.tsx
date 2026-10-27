import {
  Badge,
  Button,
  Group,
  Loader,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
  VisuallyHidden,
} from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { IconSearch } from '@tabler/icons-react';
import type { Schemas } from '@orudesk/api-client';
import { useMemo, useState } from 'react';
import { usePageTitle } from '../layout/usePageTitle';
import { useOrgTree } from '../queries/orgUnits';
import { useUsers } from '../queries/users';
import { RolesDialog } from '../users/RolesDialog';
import { flatten } from '../units/accessibleUnits';

/** Who can log in to this tenant, and what each person may do where. Admins only. */
export function UsersPage() {
  usePageTitle('Users');
  const [search, setSearch] = useState('');
  // Wait until typing pauses: one request for "anna", not four for "a", "an", "ann", "anna"
  const [debounced] = useDebouncedValue(search.trim(), 300);
  const users = useUsers(debounced);
  const unitName = useUnitNames();
  // The id, not the user: after a change the list is refetched, and the dialog shows the fresh grants
  const [editing, setEditing] = useState<string | null>(null);

  const rows = users.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <Stack>
      <Title order={1}>Users</Title>
      <TextInput
        label="Search by email"
        leftSection={<IconSearch size={16} />}
        value={search}
        onChange={(event) => setSearch(event.currentTarget.value)}
        maw={360}
      />

      {users.isPending && <Loader size="sm" aria-label="Loading" />}
      {users.isError && <Text role="alert">{users.error.message}</Text>}

      {users.isSuccess && rows.length === 0 && <Text>No users match “{debounced}”.</Text>}

      {/* Narrower than minWidth (a phone), the table scrolls sideways rather than squeezing the badges */}
      {rows.length > 0 && (
        <Table.ScrollContainer minWidth={760}>
          <Table striped highlightOnHover>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Email</Table.Th>
                <Table.Th>Status</Table.Th>
                <Table.Th>Roles</Table.Th>
                <Table.Th>
                  <VisuallyHidden>Actions</VisuallyHidden>
                </Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {rows.map((user) => (
                <Table.Tr key={user.id}>
                  <Table.Td>{user.email}</Table.Td>
                  <Table.Td>
                    {user.enabled ? (
                      <Badge color="teal" variant="dot">
                        Active
                      </Badge>
                    ) : (
                      <Badge color="gray" variant="dot">
                        Disabled
                      </Badge>
                    )}
                  </Table.Td>
                  <Table.Td>
                    <Group gap={6}>
                      {user.grants.map((grant) => (
                        <Badge key={grantKey(grant)} variant="light" tt="none">
                          {grant.role}
                          {grant.orgUnitId ? ` · ${unitName(grant.orgUnitId)}` : ''}
                        </Badge>
                      ))}
                    </Group>
                  </Table.Td>
                  <Table.Td ta="right">
                    <Button
                      variant="subtle"
                      size="xs"
                      aria-label={`Manage roles of ${user.email}`}
                      onClick={() => setEditing(user.id)}
                    >
                      Manage roles
                    </Button>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      {users.hasNextPage && (
        <Group>
          <Button
            variant="default"
            onClick={() => void users.fetchNextPage()}
            loading={users.isFetchingNextPage}
          >
            Load more
          </Button>
        </Group>
      )}

      <RolesDialog
        user={rows.find((user) => user.id === editing) ?? null}
        onClose={() => setEditing(null)}
      />
    </Stack>
  );
}

const grantKey = (grant: Schemas['Grant']) => `${grant.role}@${grant.orgUnitId ?? 'tenant'}`;

/** Grants name units by id; the tree (already cached by the unit switcher) gives their names. */
function useUnitNames() {
  const tree = useOrgTree();
  const names = useMemo(
    () => new Map(tree.data ? flatten(tree.data).map((unit) => [unit.id, unit.name]) : []),
    [tree.data],
  );
  return (id: string) => names.get(id) ?? 'unknown unit';
}
