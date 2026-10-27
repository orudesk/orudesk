import { useEffect } from 'react';

/** The browser tab's title, and what screen readers announce when the page changes. */
export function usePageTitle(title: string) {
  useEffect(() => {
    document.title = `${title} · Orudesk`;
  }, [title]);
}
