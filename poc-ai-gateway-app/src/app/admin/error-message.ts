import { HttpErrorResponse } from '@angular/common/http';

/**
 * @param error error of an admin API call
 * @returns short message for the user
 */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'AI gateway not reachable. Is it running on :8080?';
    }
    const detail = (error.error as { detail?: unknown } | null)?.detail;
    return typeof detail === 'string' ? detail : `Request failed: ${error.status} ${error.statusText}`;
  }
  return 'Unexpected error';
}
