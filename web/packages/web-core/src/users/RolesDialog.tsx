import {
  ActionIcon,
  Alert,
  Button,
  Group,
  Modal,
  Select,
  Stack,
  Table,
  Text,
  Tooltip,
  VisuallyHidden,
} from '@mantine/core';
import { IconTrash } from '@tabler/icons-react';
import type { Schemas } from '@orudesk/api-client';
import { useMemo, useState, type FormEvent } from 'react';
import { useOrgTree } from '../queries/orgUnits';
import { useChangeGrant, useRoles } from '../queries/roles';
import { flatten } from '../units/accessibleUnits';

const WHOLE_TENANT = 'tenant';

interface Props {
  user: Schemas['UserSummary'] | null;
  onClose: () => void;
}

/** One user's grants: each can be removed, and a new one added for the whole organisation or one unit. */
export function RolesDialog({ user, onClose }: Props) {
  const roles = useRoles();
  const tree = useOrgTree();
  const change = useChangeGrant();
  const [role, setRole] = useState<string | null>(null);
  const [where, setWhere] = useState<string>(WHOLE_TENANT);

  const units = useMemo(() => (tree.data ? flatten(tree.data) : []), [tree.data]);
  const unitName = (id: string) => units.find((unit) => unit.id === id)?.name ?? 'unknown unit';
  const depthOf = new Map(units.map((unit) => [unit.id, unit.depth]));
  const roleName = (code: string) => roles.data?.find((r) => r.code === code)?.name ?? code;

  const close = () => {
    change.reset(); // don't show the last error next time
    setRole(null);
    setWhere(WHOLE_TENANT);
    onClose();
  };

  const add = (event: FormEvent) => {
    event.preventDefault();
    if (!user || !role) return;
    change.mutate(
      {
        add: true,
        change: { userId: user.id, role, orgUnitId: where === WHOLE_TENANT ? null : where },
      },
      { onSuccess: () => setRole(null) },
    );
  };

  return (
    <Modal
      opened={user !== null}
      onClose={close}
      title={user && `Roles of ${user.email}`}
      size="lg"
    >
      {user && (
        <Stack>
          {change.isError && (
            <Alert color="red" role="alert" title="Not saved">
              {change.error.message}
            </Alert>
          )}

          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Role</Table.Th>
                <Table.Th>Where</Table.Th>
                <Table.Th>
                  <VisuallyHidden>Remove</VisuallyHidden>
                </Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {user.grants.map((grant) => {
                const place = grant.orgUnitId ? unitName(grant.orgUnitId) : 'Whole organisation';
                const label = `Remove ${roleName(grant.role)} in ${place}`;
                return (
                  <Table.Tr key={`${grant.role}@${grant.orgUnitId ?? WHOLE_TENANT}`}>
                    <Table.Td>{roleName(grant.role)}</Table.Td>
                    <Table.Td>{place}</Table.Td>
                    <Table.Td ta="right">
                      <Tooltip label={label}>
                        <ActionIcon
                          variant="subtle"
                          color="red"
                          aria-label={label}
                          disabled={change.isPending}
                          onClick={() =>
                            change.mutate({
                              add: false,
                              change: {
                                userId: user.id,
                                role: grant.role,
                                orgUnitId: grant.orgUnitId ?? null,
                              },
                            })
                          }
                        >
                          <IconTrash size={16} />
                        </ActionIcon>
                      </Tooltip>
                    </Table.Td>
                  </Table.Tr>
                );
              })}
            </Table.Tbody>
          </Table>

          <form onSubmit={add}>
            <Text fw={600} mb="xs">
              Add a role
            </Text>
            <Group align="flex-end">
              <Select
                label="Role"
                placeholder="Pick a role"
                data={(roles.data ?? []).map((r) => ({ value: r.code, label: r.name }))}
                value={role}
                onChange={setRole}
                w={180}
              />
              <Select
                label="Where"
                data={[
                  { value: WHOLE_TENANT, label: 'Whole organisation' },
                  ...units.map((unit) => ({ value: unit.id, label: unit.name })),
                ]}
                renderOption={({ option }) => (
                  <Text component="span" size="sm" pl={(depthOf.get(option.value) ?? 0) * 16}>
                    {option.label}
                  </Text>
                )}
                value={where}
                onChange={(value) => setWhere(value ?? WHOLE_TENANT)}
                allowDeselect={false}
                w={220}
              />
              <Button type="submit" disabled={!role} loading={change.isPending}>
                Add role
              </Button>
            </Group>
          </form>
        </Stack>
      )}
    </Modal>
  );
}
