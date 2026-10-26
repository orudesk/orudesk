import { createContext, use } from 'react';
import type { Services } from './services';

/** Provided once by <App>: `<ServicesContext value={services}>`. */
export const ServicesContext = createContext<Services | null>(null);

export function useServices(): Services {
  const services = use(ServicesContext);
  if (!services) throw new Error('useServices() needs a <ServicesContext value={...}> above it');
  return services;
}

/** The typed API client, with the access token added to every call. */
export function useApi() {
  return useServices().api;
}
