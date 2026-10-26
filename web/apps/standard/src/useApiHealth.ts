import { useEffect, useState } from 'react';
import { api } from './api';

export type ApiHealth = 'checking' | 'up' | 'down';

/** Asks the API's public health endpoint once, so the first screen shows whether the backend is reachable. */
export function useApiHealth(): ApiHealth {
  const [health, setHealth] = useState<ApiHealth>('checking');

  useEffect(() => {
    const controller = new AbortController();
    api
      .GET('/api/health', { signal: controller.signal })
      .then(({ response }) => setHealth(response.ok ? 'up' : 'down'))
      .catch((error: unknown) => {
        // Unmounting aborts the request; that's not the API being down.
        if (!(error instanceof DOMException && error.name === 'AbortError')) setHealth('down');
      });
    return () => controller.abort();
  }, []);

  return health;
}
