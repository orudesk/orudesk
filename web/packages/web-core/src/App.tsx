import { MantineProvider } from '@mantine/core';
import { QueryClientProvider } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { NavigationContext } from './layout/NavigationContext';
import type { OrudeskModule } from './module';
import { buildRoutes } from './routes';
import type { Services } from './services';
import { theme } from './theme';
import { ServicesContext } from './ServicesContext';

export interface AppProps {
  services: Services;
  /** The edition's modules, the core's first: [coreModule, ngoModule] */
  modules: OrudeskModule[];
}

export function App({ services, modules }: AppProps) {
  // Built once: the modules don't change while the app runs
  const [router] = useState(() => createBrowserRouter(buildRoutes(modules)));
  const [navigation] = useState(() => modules.flatMap((module) => module.navigation ?? []));

  // Once, at start: log back in with the refresh cookie if there is one.
  useEffect(() => {
    void services.session.restore();
  }, [services.session]);

  return (
    <MantineProvider theme={theme} defaultColorScheme="auto">
      <ServicesContext value={services}>
        <QueryClientProvider client={services.queryClient}>
          <NavigationContext value={navigation}>
            <RouterProvider router={router} />
          </NavigationContext>
        </QueryClientProvider>
      </ServicesContext>
    </MantineProvider>
  );
}
