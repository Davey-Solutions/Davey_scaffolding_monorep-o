import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type LoginResponse = {
  accessToken: string
  refreshToken: string
}

type Job = {
  id: string
  customerName: string
  siteAddress: string
  status: string
  paid: boolean
}

type StoredSession = {
  accessToken: string
  refreshToken: string
}

const ACCESS_TOKEN_KEY = 'davey.accessToken'
const REFRESH_TOKEN_KEY = 'davey.refreshToken'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
const JOBS_ROUTE = '#/jobs'

function isJobsRoute() {
  return window.location.hash === JOBS_ROUTE
}

function getStoredSession(): StoredSession | null {
  const accessToken = window.localStorage.getItem(ACCESS_TOKEN_KEY)
  const refreshToken = window.localStorage.getItem(REFRESH_TOKEN_KEY)

  if (!accessToken || !refreshToken) {
    return null
  }

  return { accessToken, refreshToken }
}

function storeSession(session: StoredSession) {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken)
  window.localStorage.setItem(REFRESH_TOKEN_KEY, session.refreshToken)
}

function clearSession() {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY)
  window.localStorage.removeItem(REFRESH_TOKEN_KEY)
}

async function apiRequest(path: string, init: RequestInit = {}) {
  const session = getStoredSession()
  const headers = new Headers(init.headers)

  if (session?.accessToken) {
    headers.set('Authorization', ['Bearer', session.accessToken].join(' '))
  }

  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  return fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  })
}

async function login(email: string, password: string) {
  const response = await apiRequest('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })

  if (!response.ok) {
    throw new Error(response.status === 401 ? 'Invalid email or password.' : 'Login failed.')
  }

  return (await response.json()) as LoginResponse
}

async function loadJobs() {
  const response = await apiRequest('/jobs')

  if (!response.ok) {
    throw new Error(response.status === 401 ? 'Your session has expired. Please log in again.' : 'Unable to load jobs.')
  }

  return (await response.json()) as Job[]
}

function navigateTo(path: string, replace = false) {
  const target = path === '/' ? '' : path
  if (replace) {
    window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}${target}`)
    return
  }

  window.location.hash = target
}

function App() {
  const initialSession = useMemo(() => getStoredSession(), [])
  const [session, setSession] = useState<StoredSession | null>(initialSession)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [jobs, setJobs] = useState<Job[]>([])
  const [jobsError, setJobsError] = useState<string | null>(null)
  const [loginError, setLoginError] = useState<string | null>(null)
  const [isLoadingJobs, setIsLoadingJobs] = useState(false)
  const [route, setRoute] = useState(() => (isJobsRoute() && initialSession ? 'jobs' : 'login'))

  useEffect(() => {
    const handleHashChange = () => {
      setRoute(isJobsRoute() && getStoredSession() ? 'jobs' : 'login')
    }

    window.addEventListener('hashchange', handleHashChange)
    return () => window.removeEventListener('hashchange', handleHashChange)
  }, [])

  useEffect(() => {
    if (isJobsRoute() && !session) {
      navigateTo('/', true)
      setRoute('login')
    }
  }, [session])

  useEffect(() => {
    if (route !== 'jobs' || !session) {
      return
    }

    let isActive = true

    setIsLoadingJobs(true)
    setJobsError(null)

    void loadJobs()
      .then((loadedJobs) => {
        if (isActive) {
          setJobs(loadedJobs)
        }
      })
      .catch((error: unknown) => {
        if (!isActive) {
          return
        }

        const message = error instanceof Error ? error.message : 'Unable to load jobs.'
        if (message.includes('session')) {
          clearSession()
          setSession(null)
          navigateTo('/', true)
          setRoute('login')
          setLoginError(message)
          return
        }

        setJobsError(message)
      })
      .finally(() => {
        if (isActive) {
          setIsLoadingJobs(false)
        }
      })

    return () => {
      isActive = false
    }
  }, [route, session])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSubmitting(true)
    setLoginError(null)

    try {
      const nextSession = await login(email, password)
      storeSession(nextSession)
      setSession(nextSession)
      setPassword('')
      navigateTo(JOBS_ROUTE)
      setRoute('jobs')
    } catch (error: unknown) {
      setLoginError(error instanceof Error ? error.message : 'Login failed.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="app-shell">
      {route === 'jobs' && session ? (
        <section className="jobs-view">
          <header className="jobs-header">
            <div>
              <p className="eyebrow">Signed in</p>
              <h1>Jobs</h1>
            </div>
          </header>
          {isLoadingJobs ? <p className="panel">Loading jobs…</p> : null}
          {jobsError ? <p className="panel panel-error">{jobsError}</p> : null}
          {!isLoadingJobs && !jobsError ? (
            jobs.length > 0 ? (
              <ul className="job-list">
                {jobs.map((job) => (
                  <li className="job-card" key={job.id}>
                    <h2>{job.customerName}</h2>
                    <p>{job.siteAddress}</p>
                    <dl>
                      <div>
                        <dt>Status</dt>
                        <dd>{job.status}</dd>
                      </div>
                      <div>
                        <dt>Paid</dt>
                        <dd>{job.paid ? 'Yes' : 'No'}</dd>
                      </div>
                    </dl>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="panel">No jobs yet.</p>
            )
          ) : null}
        </section>
      ) : (
        <section className="login-card">
          <p className="eyebrow">Davey Scaffolding</p>
          <h1>Log in</h1>
          <p className="login-copy">Sign in with your owner account to view jobs.</p>
          <form className="login-form" onSubmit={handleSubmit}>
            <label>
              <span>Email</span>
              <input
                autoComplete="email"
                name="email"
                onChange={(event) => setEmail(event.target.value)}
                required
                type="email"
                value={email}
              />
            </label>
            <label>
              <span>Password</span>
              <input
                autoComplete="current-password"
                name="password"
                onChange={(event) => setPassword(event.target.value)}
                required
                type="password"
                value={password}
              />
            </label>
            {loginError ? <p className="panel panel-error">{loginError}</p> : null}
            <button disabled={isSubmitting} type="submit">
              {isSubmitting ? 'Logging in…' : 'Log in'}
            </button>
          </form>
        </section>
      )}
    </main>
  )
}

export default App
