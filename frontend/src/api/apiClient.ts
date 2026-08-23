import { StoredSession } from '../auth/StoredSession'
import type { Job } from '../types/Job'
import type { LoginResponse } from '../types/LoginResponse'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

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

/**
 * Logs a user in through the gateway auth endpoint.
 *
 * @param email email entered in the login form
 * @param password password entered in the login form
 * @returns the token pair returned by the API
 */
export async function login(email: string, password: string) {
  const response = await postJson('/auth/login', { email, password })
  return parseLoginResponse(response)
}

/**
 * Loads jobs using the stored access token.
 *
 * @returns the current list of jobs
 */
export async function loadJobs() {
  const response = await sendRequest('/jobs')
  return parseJobsResponse(response)
}

async function postJson(path: string, body: unknown) {
  return sendRequest(path, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

async function sendRequest(path: string, init: RequestInit = {}) {
  return fetch(buildUrl(path), withHeaders(init))
}

function buildUrl(path: string) {
  return `${API_BASE_URL}${path}`
}

function withHeaders(init: RequestInit) {
  const headers = buildHeaders(init)
  return { ...init, headers }
}

function buildHeaders(init: RequestInit) {
  const headers = new Headers(init.headers)
  addAuthorizationHeader(headers)
  addJsonHeader(headers, init.body)
  return headers
}

function addAuthorizationHeader(headers: Headers) {
  const session = StoredSession.load()

  if (session) {
    headers.set('Authorization', ['Bearer', session.accessToken].join(' '))
  }
}

function addJsonHeader(headers: Headers, body: BodyInit | null | undefined) {
  if (body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
}

async function parseLoginResponse(response: Response) {
  if (!response.ok) {
    throw createLoginError(response.status)
  }

  const payload = (await response.json()) as LoginResponse
  return new StoredSession(payload.accessToken, payload.refreshToken)
}

function createLoginError(status: number) {
  if (status === 401) {
    return new Error('Invalid email or password.')
  }

  return new Error('Login failed.')
}

async function parseJobsResponse(response: Response) {
  if (!response.ok) {
    throw createJobsError(response.status)
  }

  return (await response.json()) as Job[]
}

function createJobsError(status: number) {
  if (status === 401) {
    return new SessionExpiredError()
  }

  return new Error('Unable to load jobs.')
}
