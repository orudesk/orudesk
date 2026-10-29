import { Anchor, Button, Group, List, Loader, Stack, Table, Text, Title } from '@mantine/core';
import type { Schemas } from '@orudesk/api-client';
import { Link, useSearchParams } from 'react-router';
import { usePageTitle } from '../layout/usePageTitle';
import { useAuditEntries } from '../queries/audit';

/**
 * Who changed what, and when: the audit log (Day 42). `/activity?subject=<id>` shows one record's history,
 * which is where a person's page links to.
 *
 * The request id is shown small: it's what an operator searches the logs for when someone asks "why did this
 * happen?".
 */
export function ActivityPage() {
  const [params] = useSearchParams();
  const subject = params.get('subject') ?? undefined;
  usePageTitle(subject ? 'History' : 'Activity');
  const entries = useAuditEntries(subject);
  const rows = entries.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <Stack>
      <Title order={1}>{subject ? 'History' : 'Activity'}</Title>
      {subject && (
        <Anchor component={Link} to="/activity" size="sm">
          Show everything
        </Anchor>
      )}

      {entries.isPending && <Loader size="sm" aria-label="Loading" />}
      {entries.isError && <Text role="alert">{entries.error.message}</Text>}
      {entries.isSuccess && rows.length === 0 && <Text>Nothing has been recorded yet.</Text>}

      {rows.length > 0 && (
        <Table.ScrollContainer minWidth={640}>
          <Table striped>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>When</Table.Th>
                <Table.Th>Who</Table.Th>
                <Table.Th>What</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {rows.map((entry) => (
                <Table.Tr key={entry.id}>
                  <Table.Td>
                    <time dateTime={entry.occurredAt}>{formatTime(entry.occurredAt)}</time>
                  </Table.Td>
                  <Table.Td>{entry.actorEmail ?? 'System'}</Table.Td>
                  <Table.Td>
                    <Text size="sm">{entry.summary}</Text>
                    {entry.changes.length > 0 && <Changes changes={entry.changes} />}
                    {entry.requestId && (
                      <Text size="xs" c="dimmed">
                        Request {entry.requestId}
                      </Text>
                    )}
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      {entries.hasNextPage && (
        <Group>
          <Button
            variant="default"
            onClick={() => void entries.fetchNextPage()}
            loading={entries.isFetchingNextPage}
          >
            Load more
          </Button>
        </Group>
      )}
    </Stack>
  );
}

/**
 * What an edit changed, field by field (Day 47): "phone: +91 98450 12345 → +91 98450 99999". A hidden value
 * (a PAN) shows only that it changed: the log never had the value to show.
 */
function Changes({ changes }: { changes: Schemas['FieldChange'][] }) {
  return (
    <List size="xs" c="dimmed" spacing={2} mt={4} aria-label="Changes">
      {changes.map((change) => (
        <List.Item key={change.field}>
          {change.valuesHidden
            ? `${change.field}: changed (value not recorded)`
            : `${change.field}: ${change.before ?? '—'} → ${change.after ?? '—'}`}
        </List.Item>
      ))}
    </List>
  );
}

/** An instant in the reader's own time zone and locale ("26 Oct 2026, 16:23"). */
function formatTime(instant: string): string {
  return new Date(instant).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}
