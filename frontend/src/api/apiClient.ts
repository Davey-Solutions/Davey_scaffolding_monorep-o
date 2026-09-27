import { StoredSession } from '../auth/StoredSession'
import { Job } from '../types/Job'
import type { LoginResponse } from '../types/LoginResponse'
import { SessionExpiredError } from './SessionExpiredError'
import { HttpStatus } from './httpStatus'
import type { SaveJobRequest } from './SaveJobRequest'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

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

/**
 * Creates a job.
 *
 * @param request validated job payload
 * @returns the created job returned by the API
 */
export async function createJob(request: SaveJobRequest) {
  const response = await postJson('/jobs', request)
  return parseJobResponse(response, createSaveJobError)
}

/**
 * Updates a job by id.
 *
 * @param jobId id of the job to update
 * @param request validated job payload
 * @returns the updated job returned by the API
 */
export async function updateJob(jobId: string, request: SaveJobRequest) {
  const response = await putJson(`/jobs/${encodeURIComponent(jobId)}`, request)
  return parseJobResponse(response, createUpdateJobError)
}

/**
 * Deletes a job by id.
 *
 * @param jobId id of the job to delete
 */
export async function deleteJob(jobId: string) {
  const response = await sendRequest(`/jobs/${encodeURIComponent(jobId)}`, {
    method: 'DELETE',
  })

  if (!response.ok) {
    throw createDeleteJobError(response.status)
  }
}

async function postJson(path: string, body: unknown) {
  return sendRequest(path, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

async function putJson(path: string, body: unknown) {
  return sendRequest(path, {
    method: 'PUT',
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
  if (status === HttpStatus.UNAUTHORIZED) {
    return new Error('Invalid email or password.')
  }

  return new Error('Login failed.')
}

async function parseJobsResponse(response: Response) {
  if (!response.ok) {
    throw createJobsError(response.status)
  }

  const payload = (await response.json()) as Job[]
  return payload.map((job) => new Job(job))
}

async function parseJobResponse(
  response: Response,
  createError: (status: number) => Error,
) {
  if (!response.ok) {
    throw createError(response.status)
  }

  const payload = (await response.json()) as Job
  return new Job(payload)
}

function createJobsError(status: number) {
  if (status === HttpStatus.UNAUTHORIZED) {
    return new SessionExpiredError()
  }

  return new Error('Unable to load jobs.')
}

function createDeleteJobError(status: number) {
  if (status === HttpStatus.UNAUTHORIZED) {
    return new SessionExpiredError()
  }

  if (status === HttpStatus.NOT_FOUND) {
    return new Error('Job no longer exists.')
  }

  return new Error('Unable to delete job.')
}

function createSaveJobError(status: number) {
  if (status === HttpStatus.UNAUTHORIZED) {
    return new SessionExpiredError()
  }

  return new Error('Unable to create job.')
}

function createUpdateJobError(status: number) {
  if (status === HttpStatus.UNAUTHORIZED) {
    return new SessionExpiredError()
  }

  if (status === HttpStatus.NOT_FOUND) {
    return new Error('Job no longer exists.')
  }

  return new Error('Unable to save job changes.')
}
