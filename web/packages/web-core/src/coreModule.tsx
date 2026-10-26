import { IconAddressBook, IconHome, IconUsers } from '@tabler/icons-react';
import type { OrudeskModule } from './module';
import { HomePage } from './pages/HomePage';

/**
 * The core's screens: home, people (the party module) and users (identity). Every edition includes it.
 *
 * Pages most visits never open are loaded on demand (Day 35): each `lazy` import becomes its own file in the
 * build, fetched the first time someone goes there. Home is in the main bundle, because every visit needs it.
 */
export const coreModule: OrudeskModule = {
  id: 'core',
  routes: [
    { path: '/', element: <HomePage /> },
    {
      path: '/people',
      lazy: async () => ({ Component: (await import('./pages/PeoplePage')).PeoplePage }),
    },
    {
      path: '/people/new',
      lazy: async () => ({ Component: (await import('./pages/NewPersonPage')).NewPersonPage }),
    },
    {
      path: '/people/:personId',
      lazy: async () => ({ Component: (await import('./pages/PersonPage')).PersonPage }),
    },
    {
      path: '/users',
      lazy: async () => ({ Component: (await import('./pages/UsersPage')).UsersPage }),
    },
  ],
  navigation: [
    { label: 'Home', path: '/', icon: <IconHome size={18} /> },
    { label: 'People', path: '/people', icon: <IconAddressBook size={18} />, role: 'ADMIN' },
    { label: 'Users', path: '/users', icon: <IconUsers size={18} />, role: 'ADMIN' },
  ],
};
