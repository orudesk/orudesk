import { useSyncExternalStore } from 'react';
import { useServices } from '../ServicesContext';

/** The unit the user is working in, and a way to switch. */
export function useCurrentUnit() {
  const { units } = useServices();
  const unitId = useSyncExternalStore(units.subscribe, units.get);
  return { unitId, setUnitId: units.set };
}
