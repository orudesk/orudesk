import { MantineProvider } from '@mantine/core';
import '@mantine/core/styles.css';
import { QueryClientProvider } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { routes } from './routes';
import type { Services } from './services';
import './styles.css';
import { theme } from './theme';
import { ServicesContext } from './ServicesContext';

export function App({ services }: { services: Services }) {
  const [router] = useState(() => createBrowserRouter(routes));

  // Once, at start: log back in with the refresh cookie if there is one.
  useEffect(() => {
    void services.session.restore();
  }, [services.session]);

  return (
    <MantineProvider theme={theme} defaultColorScheme="auto">
      <ServicesContext value={services}>
        <QueryClientProvider client={services.queryClient}>
          <RouterProvider router={router} />
        </QueryClientProvider>
      </ServicesContext>
    </MantineProvider>
  );
}
