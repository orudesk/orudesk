import { Alert, Button, Stack, Text } from '@mantine/core';

/**
 * Shown when a save was refused because someone else saved first (409). The choice is the user's: reload
 * (their typing is lost, but they see what the other person did) or leave the page. Never "save anyway": that
 * would quietly undo a colleague's correction, which is what optimistic locking is there to prevent.
 */
export function EditConflictAlert({
  onReload,
  reloading,
}: {
  onReload: () => void;
  reloading: boolean;
}) {
  return (
    <Alert color="orange" role="alert" title="Changed by someone else">
      <Stack gap="xs" align="flex-start">
        <Text size="sm">
          Someone saved this record after you opened it. Reload to see their changes, then make
          yours again.
        </Text>
        <Button size="xs" variant="white" onClick={onReload} loading={reloading}>
          Reload
        </Button>
      </Stack>
    </Alert>
  );
}
