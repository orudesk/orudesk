import type { RouteObject } from 'react-router';
import { RequireAuth } from './auth/RequireAuth';
import { RouteError } from './errors/RouteError';
import { AppLayout } from './layout/AppLayout';
import type { OrudeskModule } from './module';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';

/**
 * The app's routes: the login and password reset pages, then every module's pages inside the login check and the shell. Two
 * modules claiming the same path is a mistake in the edition's setup, so it fails at start, loudly, rather than
 * one page silently hiding the other.
 */
export function buildRoutes(modules: OrudeskModule[]): RouteObject[] {
  const owner = new Map<string, string>();
  for (const module of modules) {
    for (const route of module.routes) {
      if (!route.path) continue;
      const other = owner.get(route.path);
      if (other) {
        throw new Error(`Modules "${other}" and "${module.id}" both have a page at ${route.path}`);
      }
      owner.set(route.path, module.id);
    }
  }

  return [
    { path: '/login', element: <LoginPage />, errorElement: <RouteError /> },
    // Outside RequireAuth: the people who need these pages are exactly the ones who can't log in (Day 55)
    { path: '/forgot-password', element: <ForgotPasswordPage />, errorElement: <RouteError /> },
    { path: '/reset-password', element: <ResetPasswordPage />, errorElement: <RouteError /> },
    {
      element: <RequireAuth />, // every route inside needs a login
      errorElement: <RouteError />, // the last line of defence: the shell itself failed
      children: [
        {
          element: <AppLayout />, // ...and is drawn inside the header and menu
          children: [
            {
              // A pathless route with only an errorElement: a crashing page is replaced by RouteError *inside*
              // the shell, so the menu still works and the user can go elsewhere.
              errorElement: <RouteError />,
              children: [
                ...modules.flatMap((module) => module.routes),
                { path: '*', element: <NotFoundPage /> },
              ],
            },
          ],
        },
      ],
    },
  ];
}
