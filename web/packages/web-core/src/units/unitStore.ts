const KEY = 'orudesk.orgUnit';

/**
 * Which org unit (school, clinic, branch) the user is working in. The API needs it in the X-Org-Unit header for
 * anything that happens "at a unit" (Day 19). Remembered in localStorage so a reload keeps the choice: it's a
 * preference, not a secret, and the API checks the user's access to the unit on every request anyway.
 */
export class UnitStore {
  private unitId: string | null;
  private readonly listeners = new Set<() => void>();

  constructor(private readonly storage: Storage | null = safeLocalStorage()) {
    this.unitId = this.storage?.getItem(KEY) ?? null;
  }

  get = (): string | null => this.unitId;

  set = (unitId: string | null): void => {
    if (unitId === this.unitId) return;
    this.unitId = unitId;
    if (unitId) this.storage?.setItem(KEY, unitId);
    else this.storage?.removeItem(KEY);
    this.listeners.forEach((listener) => listener());
  };

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };
}

/** localStorage can be missing or throw (private windows, blocked storage): then the choice lasts until reload. */
function safeLocalStorage(): Storage | null {
  try {
    return globalThis.localStorage ?? null;
  } catch {
    return null;
  }
}
