/**
 * Error raised when the stored access token is rejected.
 */
export class SessionExpiredError extends Error {
  /**
   * Creates the session-expired error.
   */
  public constructor() {
    super('Your session has expired. Please log in again.')
  }
}
