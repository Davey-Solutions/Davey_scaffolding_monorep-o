import { useEffect, useMemo, useState } from 'react'
import './App.css'
import { loadJobs, login, SessionExpiredError } from './api/apiClient'
import { StoredSession } from './auth/StoredSession'
import { JobsView } from './components/JobsView'
import { LoginView } from './components/LoginView'
import { getJobIdFromRoute, JOBS_ROUTE, isJobsRoute, navigateTo } from './routes'
import type { Job } from './types/Job'

/**
 * State update callbacks used while loading jobs.
 */
type JobsLoaderActions = {
  setIsLoadingJobs: (loading: boolean) => void
  setJobsError: (message: string | null) => void
  setJobs: (jobs: Job[]) => void
  onSessionExpired: (message: string) => void
}

/**
 * Root application component for the frontend login flow.
 *
 * @returns the login screen or the authenticated jobs screen
 */
function App() {
  const initialSession = useMemo(() => StoredSession.load(), [])
  const [session, setSession] = useState<StoredSession | null>(initialSession)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [jobs, setJobs] = useState<Job[]>([])
  const [jobsError, setJobsError] = useState<string | null>(null)
  const [loginError, setLoginError] = useState<string | null>(null)
  const [isLoadingJobs, setIsLoadingJobs] = useState(false)
  const [routeHash, setRouteHash] = useState(() => getInitialRoute(initialSession))
  const selectedJobId = getJobIdFromRoute(routeHash)
  const showJobsView = isJobsRoute(routeHash) || selectedJobId !== null

  useEffect(() => registerHashChangeHandler(setRouteHash), [])
  useEffect(() => syncRouteWithSession(session, setRouteHash), [session])
  useEffect(() => {
    return loadJobsForRoute(routeHash, session, {
      setIsLoadingJobs,
      setJobsError,
      setJobs,
      onSessionExpired: (message) => resetSession(setSession, setRouteHash, setLoginError, message),
    })
  }, [routeHash, session])

  async function handleSubmit() {
    setIsSubmitting(true)
    setLoginError(null)

    try {
      await completeLogin(email, password, setSession, setPassword, setRouteHash)
    } catch (error: unknown) {
      setLoginError(error instanceof Error ? error.message : 'Login failed.')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (showJobsView && session) {
    return (
      <main className="app-shell">
        <JobsView
          isLoadingJobs={isLoadingJobs}
          jobs={jobs}
          jobsError={jobsError}
          selectedJobId={selectedJobId}
        />
      </main>
    )
  }

  return (
    <main className="app-shell">
      <LoginView
        email={email}
        isSubmitting={isSubmitting}
        loginError={loginError}
        onEmailChange={setEmail}
        onPasswordChange={setPassword}
        onSubmit={(event) => {
          event.preventDefault()
          void handleSubmit()
        }}
        password={password}
      />
    </main>
  )
}

function getInitialRoute(session: StoredSession | null) {
  const isKnownJobsRoute = isJobsRoute() || getJobIdFromRoute() !== null

  if (isKnownJobsRoute && session) {
    return window.location.hash
  }

  return ''
}

function registerHashChangeHandler(setRouteHash: (routeHash: string) => void) {
  const handleHashChange = () => setRouteHash(getInitialRoute(StoredSession.load()))
  window.addEventListener('hashchange', handleHashChange)
  return () => window.removeEventListener('hashchange', handleHashChange)
}

function syncRouteWithSession(
  session: StoredSession | null,
  setRouteHash: (routeHash: string) => void,
) {
  const isKnownJobsRoute = isJobsRoute() || getJobIdFromRoute() !== null

  if (!isKnownJobsRoute || session) {
    return
  }

  navigateTo('/', true)
  setRouteHash('')
}

function loadJobsForRoute(
  routeHash: string,
  session: StoredSession | null,
  actions: JobsLoaderActions,
) {
  const shouldLoadJobs = isJobsRoute(routeHash) || getJobIdFromRoute(routeHash) !== null

  if (!shouldLoadJobs || !session) {
    return
  }

  let isActive = true
  actions.setIsLoadingJobs(true)
  actions.setJobsError(null)
  actions.setJobs([])

  void loadJobs()
    .then((loadedJobs) => {
      if (isActive) {
        actions.setJobs(loadedJobs)
      }
    })
    .catch((error: unknown) => {
      if (isActive) {
        handleJobsError(error, actions)
      }
    })
    .finally(() => {
      if (isActive) {
        actions.setIsLoadingJobs(false)
      }
    })

  return () => {
    if (!isActive) {
      return
    }

    isActive = false
    actions.setIsLoadingJobs(false)
  }
}

async function completeLogin(
  email: string,
  password: string,
  setSession: (session: StoredSession | null) => void,
  setPassword: (password: string) => void,
  setRouteHash: (routeHash: string) => void,
) {
  const nextSession = await login(email, password)
  nextSession.save()
  setSession(nextSession)
  setPassword('')
  navigateTo(JOBS_ROUTE)
  setRouteHash(JOBS_ROUTE)
}

function handleJobsError(error: unknown, actions: JobsLoaderActions) {
  if (error instanceof SessionExpiredError) {
    actions.onSessionExpired(error.message)
    return
  }

  actions.setJobsError(error instanceof Error ? error.message : 'Unable to load jobs.')
}

function resetSession(
  setSession: (session: StoredSession | null) => void,
  setRouteHash: (routeHash: string) => void,
  setLoginError: (message: string | null) => void,
  message: string,
) {
  StoredSession.clear()
  setSession(null)
  navigateTo('/', true)
  setRouteHash('')
  setLoginError(message)
}

export default App
