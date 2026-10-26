/**
 * Where to go after login, from ?from=... in the URL. Only paths inside this app: otherwise a link like
 * /login?from=https://evil.example would send people to another site right after they typed their password
 * (an "open redirect", a favourite of phishing).
 */
export function safeRedirect(from: string | null): string {
  if (!from || !from.startsWith('/') || from.startsWith('//') || from.startsWith('/\\')) return '/';
  return from;
}
