import { IconAddressBook, IconHistory, IconHome, IconUsers } from '@tabler/icons-react';
import type { OrudeskModule } from './module';
import { HomePage } from './pages/HomePage';

/**
 * The core's screens: home, people (the party module), users (identity) and the activity log (audit). Every edition includes it.
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
      path: '/people/:personId/edit',
      lazy: async () => ({ Component: (await import('./pages/EditPersonPage')).EditPersonPage }),
    },
    {
      path: '/users',
      lazy: async () => ({ Component: (await import('./pages/UsersPage')).UsersPage }),
    },
    {
      path: '/account/security',
      lazy: async () => ({
        Component: (await import('./pages/AccountSecurityPage')).AccountSecurityPage,
      }),
    },
    {
      path: '/activity',
      lazy: async () => ({ Component: (await import('./pages/ActivityPage')).ActivityPage }),
    },
  ],
  navigation: [
    { label: 'Home', path: '/', icon: <IconHome size={18} /> },
    { label: 'People', path: '/people', icon: <IconAddressBook size={18} />, roles: ['ADMIN'] },
    { label: 'Users', path: '/users', icon: <IconUsers size={18} />, roles: ['ADMIN'] },
    { label: 'Activity', path: '/activity', icon: <IconHistory size={18} />, roles: ['ADMIN'] },
  ],
};
