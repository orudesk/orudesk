import { Button, Code, Stack, Text, Title } from '@mantine/core';
import { isRouteErrorResponse, useRouteError } from 'react-router';
import { usePageTitle } from '../layout/usePageTitle';

/**
 * What a page shows when it crashed (a render error, a failed lazy import). The router catches the error here,
 * an "error boundary", so one broken page doesn't blank the whole app: the menu still works.
 */
export function RouteError() {
  const error = useRouteError();
  usePageTitle('Something went wrong');

  // After a new release, a tab opened earlier asks for page files that no longer exist (their names have
  // hashes). Reloading fetches the new version.
  if (isStaleBuild(error)) {
    return (
      <Stack align="flex-start">
        <Title order={1}>Orudesk has been updated</Title>
        <Text>This page belongs to a newer version. Reload to get it.</Text>
        <Button onClick={() => window.location.reload()}>Reload</Button>
      </Stack>
    );
  }

  return (
    <Stack align="flex-start">
      <Title order={1}>Something went wrong</Title>
      <Text>
        This page ran into a problem. Reloading usually helps; if it keeps happening, tell your
        admin.
      </Text>
      <Button onClick={() => window.location.reload()}>Reload</Button>
      {/* Details only while developing: in production they mean nothing to users and may reveal internals */}
      {import.meta.env.DEV && <Code block>{describe(error)}</Code>}
    </Stack>
  );
}

function isStaleBuild(error: unknown): boolean {
  return (
    error instanceof Error &&
    /dynamically imported module|Importing a module script failed|Failed to fetch dynamically/i.test(
      error.message,
    )
  );
}

function describe(error: unknown): string {
  if (isRouteErrorResponse(error)) return `${error.status} ${error.statusText}`;
  if (error instanceof Error) return error.stack ?? error.message;
  return String(error);
}
