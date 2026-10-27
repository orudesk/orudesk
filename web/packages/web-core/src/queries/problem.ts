/**
 * The API's errors are RFC 9457 problem details: { title, detail, status }. The detail is written for people
 * ("a user must have at least one role"), so a screen can show it; anything else gets the fallback.
 */
export function problemMessage(error: unknown, fallback: string): string {
  if (typeof error === 'object' && error !== null && 'detail' in error) {
    const { detail } = error as { detail: unknown };
    if (typeof detail === 'string' && detail)
      return detail.charAt(0).toUpperCase() + detail.slice(1) + '.';
  }
  return fallback;
}
