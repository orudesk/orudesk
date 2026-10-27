import type { ReactNode } from 'react';
import type { RouteObject } from 'react-router';

export interface NavItem {
  label: string;
  path: string;
  icon: ReactNode;
  /** Shown only to users with one of these roles. Hiding is a courtesy: the API refuses them anyway. */
  roles?: string[];
}

/**
 * What a feature brings to the web app: its pages and its menu entries. The core's own screens are one module
 * (coreModule); each vertical (NGO, school, hospital) adds its own. The app knows modules only through this
 * interface, so adding one never means editing the shell: the Open/Closed principle, and the web twin of the
 * backend's Spring Modulith modules.
 */
export interface OrudeskModule {
  /** A short unique name, used in error messages: "ngo". */
  id: string;
  /** Pages drawn inside the shell, for logged-in users. Paths are absolute ("/donors"). */
  routes: RouteObject[];
  /** Menu entries, in the order they appear. */
  navigation?: NavItem[];
}
