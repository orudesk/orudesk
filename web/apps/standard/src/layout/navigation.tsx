import { IconAddressBook, IconHome, IconUsers } from '@tabler/icons-react';
import type { ReactNode } from 'react';

export interface NavItem {
  label: string;
  path: string;
  icon: ReactNode;
  /** Shown only to users with this role. Hiding is a courtesy: the API refuses them anyway. */
  role?: string;
}

/** The main menu. A new screen adds its entry here; the shell draws it. */
export const navigation: NavItem[] = [
  { label: 'Home', path: '/', icon: <IconHome size={18} /> },
  { label: 'People', path: '/people', icon: <IconAddressBook size={18} />, role: 'ADMIN' },
  { label: 'Users', path: '/users', icon: <IconUsers size={18} />, role: 'ADMIN' },
];
