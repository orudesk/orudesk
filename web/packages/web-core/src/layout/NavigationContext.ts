import { createContext, use } from 'react';
import type { NavItem } from '../module';

/** The menu: every module's entries, in module order. Provided by <App>. */
export const NavigationContext = createContext<NavItem[]>([]);

export const useNavigation = (): NavItem[] => use(NavigationContext);
