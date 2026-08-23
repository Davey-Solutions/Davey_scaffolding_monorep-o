/**
 * Persisted login session for the frontend.
 */
export class StoredSession {
  private static readonly accessTokenKey = 'davey.accessToken'
  private static readonly refreshTokenKey = 'davey.refreshToken'
  public readonly accessToken: string
  public readonly refreshToken: string

  /**
   * Creates a session value from the received JWT pair.
   *
   * @param accessToken signed JWT access token
   * @param refreshToken signed JWT refresh token
   */
  public constructor(
    accessToken: string,
    refreshToken: string,
  ) {
    this.accessToken = accessToken
    this.refreshToken = refreshToken
  }

  /**
   * Loads the persisted session when both tokens are available.
   *
   * @param storage storage backing the saved session
   * @returns the stored session, or {@code null} when incomplete
   */
  public static load(storage: Storage = window.localStorage): StoredSession | null {
    const accessToken = storage.getItem(StoredSession.accessTokenKey)
    const refreshToken = storage.getItem(StoredSession.refreshTokenKey)

    if (!accessToken || !refreshToken) {
      return null
    }

    return new StoredSession(accessToken, refreshToken)
  }

  /**
   * Clears any persisted session tokens.
   *
   * @param storage storage backing the saved session
   */
  public static clear(storage: Storage = window.localStorage): void {
    storage.removeItem(StoredSession.accessTokenKey)
    storage.removeItem(StoredSession.refreshTokenKey)
  }

  /**
   * Saves the session tokens for later requests.
   *
   * @param storage storage backing the saved session
   */
  public save(storage: Storage = window.localStorage): void {
    storage.setItem(StoredSession.accessTokenKey, this.accessToken)
    storage.setItem(StoredSession.refreshTokenKey, this.refreshToken)
  }
}
