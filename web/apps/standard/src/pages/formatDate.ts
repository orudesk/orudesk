/**
 * "2014-03-09" as the reader's locale writes dates ("9 Mar 2014", "Mar 9, 2014"). The API sends plain dates
 * without a time zone, so they're formatted as UTC: in local time, midnight could fall on the day before.
 */
export function formatDate(isoDate: string | null | undefined): string {
  if (!isoDate) return '—';
  return new Date(`${isoDate}T00:00:00Z`).toLocaleDateString(undefined, {
    dateStyle: 'medium',
    timeZone: 'UTC',
  });
}
