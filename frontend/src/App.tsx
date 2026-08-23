import { useEffect, useMemo, useState } from 'react'
import './App.css'
import { loadJobs, login } from './api/apiClient'
import { StoredSession } from './auth/StoredSession'
import { JobsView } from './components/JobsView'
import { LoginView } from './components/LoginView'
import { JOBS_ROUTE, isJobsRoute, navigateTo } from './routes'
import type { Job } from './types/Job'

/**
 * In-app route names used by the single-screen frontend.
 */
type RouteName = 'jobs' | 'login'

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
  const [route, setRoute] = useState<RouteName>(() => getInitialRoute(initialSession))

  useEffect(() => registerHashChangeHandler(setRoute), [])
  useEffect(() => syncRouteWithSession(session, setRoute), [session])
  useEffect(() => {
    return loadJobsForRoute(route, session, {
      setIsLoadingJobs,
      setJobsError,
      setJobs,
      onSessionExpired: (message) => resetSession(setSession, setRoute, setLoginError, message),
    })
  }, [route, session])

  async function handleSubmit() {
    setIsSubmitting(true)
    setLoginError(null)

    try {
      await completeLogin(email, password, setSession, setPassword, setRoute)
    } catch (error: unknown) {
      setLoginError(error instanceof Error ? error.message : 'Login failed.')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (route === 'jobs' && session) {
    return (
      <main className="app-shell">
        <JobsView isLoadingJobs={isLoadingJobs} jobs={jobs} jobsError={jobsError} />
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

function getInitialRoute(session: StoredSession | null): RouteName {
  if (isJobsRoute() && session) {
    return 'jobs'
  }

  return 'login'
}

function registerHashChangeHandler(setRoute: (route: RouteName) => void) {
  const handleHashChange = () => setRoute(getInitialRoute(StoredSession.load()))
  window.addEventListener('hashchange', handleHashChange)
  return () => window.removeEventListener('hashchange', handleHashChange)
}

function syncRouteWithSession(
  session: StoredSession | null,
  setRoute: (route: RouteName) => void,
) {
  if (!isJobsRoute() || session) {
    return
  }

  navigateTo('/', true)
  setRoute('login')
}

function loadJobsForRoute(
  route: RouteName,
  session: StoredSession | null,
  actions: JobsLoaderActions,
) {
  if (route !== 'jobs' || !session) {
    return
  }

  let isActive = true
  actions.setIsLoadingJobs(true)
  actions.setJobsError(null)

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
    isActive = false
  }
}

async function completeLogin(
  email: string,
  password: string,
  setSession: (session: StoredSession | null) => void,
  setPassword: (password: string) => void,
  setRoute: (route: RouteName) => void,
) {
  const nextSession = await login(email, password)
  nextSession.save()
  setSession(nextSession)
  setPassword('')
  navigateTo(JOBS_ROUTE)
  setRoute('jobs')
}

function handleJobsError(error: unknown, actions: JobsLoaderActions) {
  const message = error instanceof Error ? error.message : 'Unable to load jobs.'

  if (message.includes('session')) {
    actions.onSessionExpired(message)
    return
  }

  actions.setJobsError(message)
}

function resetSession(
  setSession: (session: StoredSession | null) => void,
  setRoute: (route: RouteName) => void,
  setLoginError: (message: string | null) => void,
  message: string,
) {
  StoredSession.clear()
  setSession(null)
  navigateTo('/', true)
  setRoute('login')
  setLoginError(message)
}

export default App
