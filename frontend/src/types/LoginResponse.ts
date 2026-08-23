/**
 * Token pair returned by the login endpoint.
 */
export interface LoginResponse {
  /** Signed JWT access token used for authenticated API calls. */
  accessToken: string
  /** Signed JWT refresh token reserved for future token refresh calls. */
  refreshToken: string
}
