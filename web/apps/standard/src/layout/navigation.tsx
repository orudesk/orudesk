import { IconHome } from '@tabler/icons-react';
import type { ReactNode } from 'react';

export interface NavItem {
  label: string;
  path: string;
  icon: ReactNode;
}

/** The main menu. A new screen adds its entry here; the shell draws it. */
export const navigation: NavItem[] = [{ label: 'Home', path: '/', icon: <IconHome size={18} /> }];
