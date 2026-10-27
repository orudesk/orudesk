import type { RouteObject } from 'react-router';
import { RequireAuth } from './auth/RequireAuth';
import { AppLayout } from './layout/AppLayout';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';

/**
 * Screens most visits never open are loaded on demand: each `lazy` import becomes its own file in the build,
 * fetched the first time someone goes there. Login and home are in the main bundle, because every visit needs
 * one of them at once.
 */
export const routes: RouteObject[] = [
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />, // every route inside needs a login
    children: [
      {
        element: <AppLayout />, // ...and is drawn inside the header and menu
        children: [
          { path: '/', element: <HomePage /> },
          {
            path: '/users',
            lazy: async () => ({ Component: (await import('./pages/UsersPage')).UsersPage }),
          },
          {
            path: '/people',
            lazy: async () => ({ Component: (await import('./pages/PeoplePage')).PeoplePage }),
          },
          {
            path: '/people/new',
            lazy: async () => ({
              Component: (await import('./pages/NewPersonPage')).NewPersonPage,
            }),
          },
          {
            path: '/people/:personId',
            lazy: async () => ({ Component: (await import('./pages/PersonPage')).PersonPage }),
          },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
];
