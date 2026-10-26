import type { RouteObject } from 'react-router';
import { RequireAuth } from './auth/RequireAuth';
import { RouteError } from './errors/RouteError';
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
  { path: '/login', element: <LoginPage />, errorElement: <RouteError /> },
  {
    element: <RequireAuth />,
    errorElement: <RouteError />, // the last line of defence: the shell itself failed // every route inside needs a login
    children: [
      {
        element: <AppLayout />, // ...and is drawn inside the header and menu
        children: [
          {
            // A pathless route with only an errorElement: a crashing page is replaced by RouteError *inside*
            // the shell, so the menu still works and the user can go elsewhere.
            errorElement: <RouteError />,
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
    ],
  },
];
