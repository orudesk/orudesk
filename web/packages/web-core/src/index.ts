/**
 * @orudesk/web-core: the shell, sign-in, the unit switcher and the core's screens, for every edition's web app.
 * An edition (apps/standard, orudesk-ngo/web) is a main.tsx that lists its modules. Everything a module may use
 * is exported here; anything else is internal and may change without notice.
 */

// Starting an app
export { App, type AppProps } from './App';
export { startOrudeskApp } from './start';
export { buildRoutes } from './routes';
export type { NavItem, OrudeskModule } from './module';
export { coreModule } from './coreModule';

// For a module's pages
export { useApi, useServices } from './ServicesContext';
export type { Services } from './services';
export { createServices } from './services';
export { useMe } from './queries/me';
export { problemMessage } from './queries/problem';
export { usePageTitle } from './layout/usePageTitle';
export { useCurrentUnit } from './units/useCurrentUnit';
export { formatDate } from './pages/formatDate';
