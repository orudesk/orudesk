import { createTheme } from '@mantine/core';

/** Orudesk's look, in one place. Components take colours and spacing from here, never as hard-coded values. */
export const theme = createTheme({
  primaryColor: 'teal',
  defaultRadius: 'md',
  fontFamily:
    'system-ui, -apple-system, "Segoe UI", Roboto, "Noto Sans", "Helvetica Neue", Arial, sans-serif',
  headings: { fontWeight: '650' },
});
