import { HttpErrorResponse } from '@angular/common/http';

/**
 * @param error error of an admin API call
 * @returns short message for the user
 */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    return error.status === 0
      ? 'AI gateway not reachable. Is it running on :8080?'
      : `Request failed: ${error.status} ${error.statusText}`;
  }
  return 'Unexpected error';
}
