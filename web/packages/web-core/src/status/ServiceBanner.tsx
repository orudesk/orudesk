import { Alert, Button, Group, Text } from '@mantine/core';
import { IconCloudOff } from '@tabler/icons-react';
import { useQueryClient } from '@tanstack/react-query';
import { useSyncExternalStore } from 'react';
import { useServices } from '../ServicesContext';

/**
 * A banner while the API is down. It's the honest answer to "why is nothing loading": not the user's fault, not
 * a bug in the page, and it will pass. role="status" makes screen readers announce it without interrupting.
 */
export function ServiceBanner() {
  const { serviceStatus } = useServices();
  const state = useSyncExternalStore(serviceStatus.subscribe, serviceStatus.get);
  const queryClient = useQueryClient();

  if (state.kind === 'ok') return null;

  const message =
    state.kind === 'unreachable'
      ? "Can't reach Orudesk. Check your connection."
      : `Orudesk is having trouble right now${
          state.retryAfterSeconds ? `; try again in about ${state.retryAfterSeconds} seconds` : ''
        }. Your work isn't lost.`;

  return (
    <Alert color="yellow" icon={<IconCloudOff size={18} />} role="status" mb="md">
      <Group justify="space-between" wrap="nowrap">
        <Text size="sm">{message}</Text>
        <Button
          size="xs"
          variant="white"
          color="dark"
          // Ask again for what's on screen; the first answer that comes back hides the banner
          onClick={() => void queryClient.refetchQueries({ type: 'active' })}
        >
          Try again
        </Button>
      </Group>
    </Alert>
  );
}
