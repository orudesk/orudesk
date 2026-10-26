import type { RouteObject } from 'react-router';
import { RequireAuth } from './auth/RequireAuth';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';

export const routes: RouteObject[] = [
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />, // every route inside needs a login
    children: [{ path: '/', element: <HomePage /> }],
  },
];
