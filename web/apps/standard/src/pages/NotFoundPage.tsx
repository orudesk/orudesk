import { Button, Stack, Text, Title } from '@mantine/core';
import { Link } from 'react-router';
import { usePageTitle } from '../layout/usePageTitle';

export function NotFoundPage() {
  usePageTitle('Page not found');
  return (
    <Stack align="flex-start">
      <Title order={1}>Page not found</Title>
      <Text c="dimmed">
        There is nothing at this address. It may have moved, or the link is wrong.
      </Text>
      <Button component={Link} to="/" variant="light">
        Go to the home page
      </Button>
    </Stack>
  );
}
