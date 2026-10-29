import { problemMessage } from './problem';

/**
 * The API refused the form: `fields` has a message per field name ("firstName": "must not be blank"), straight
 * from the server's validation, so the browser never has to repeat the rules.
 */
export class ValidationError extends Error {
  constructor(
    message: string,
    readonly fields: Record<string, string>,
  ) {
    super(message);
  }
}

/**
 * Someone else saved the record after this form read it (409, Day 46). Saving anyway would silently undo their
 * change, so the screen offers to reload instead.
 */
export class EditConflictError extends Error {
  constructor() {
    super('Someone else changed this while you were editing it.');
  }
}

/** The error to throw for a refused save: 400 per field, 409 a conflict, anything else the API's own words. */
export function saveError(response: Response, error: unknown, fallback: string): Error {
  if (response.status === 400) {
    const fields = (error as { errors?: Record<string, string> } | undefined)?.errors ?? {};
    return new ValidationError(problemMessage(error, 'Some fields are invalid.'), fields);
  }
  if (response.status === 409 && isEditConflict(error)) return new EditConflictError();
  return new Error(problemMessage(error, fallback));
}

// A 409 can also mean "PAN already registered": the version checks (in the service, or the database's when two
// saves race) both answer with the title "Edit conflict"
function isEditConflict(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    'title' in error &&
    error.title === 'Edit conflict'
  );
}
