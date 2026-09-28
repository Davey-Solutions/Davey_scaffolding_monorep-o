import { type Dispatch, type SetStateAction, useEffect, useMemo, useState } from 'react'
import './App.css'
import {
  createJob,
  deleteJob,
  loadJobs,
  login,
  updateJob,
} from './api/apiClient'
import type { SaveJobRequest } from './api/SaveJobRequest'
import { SessionExpiredError } from './api/SessionExpiredError'
import { StoredSession } from './auth/StoredSession'
import { JobsView } from './components/JobsView'
import { LoginView } from './components/LoginView'
import {
  buildJobDetailRoute,
  getJobIdFromEditRoute,
  getJobIdFromRoute,
  isCreateJobRoute,
  JOBS_ROUTE,
  isJobsRoute,
  navigateTo,
} from './routes'
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

type SessionResetActions = {
  setSession: (session: StoredSession | null) => void
  setRouteHash: (routeHash: string) => void
  setLoginError: (message: string | null) => void
}

type JobMutationActions = SessionResetActions & {
  setJobs: Dispatch<SetStateAction<Job[]>>
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
  const isCreateRoute = isCreateJobRoute(routeHash)
  const editingJobId = getJobIdFromEditRoute(routeHash)
  const selectedJobId = getSelectedJobId(routeHash, isCreateRoute, editingJobId)
  const showJobsView = isKnownJobsRoute(routeHash)
  const jobMutationActions = createJobMutationActions(setJobs, setLoginError, setRouteHash, setSession)

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
      setLoginError(getErrorMessage(error, 'Login failed.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleDeleteJob(jobId: string) {
    try {
      await deleteJob(jobId)
      setJobs((currentJobs) => currentJobs.filter((job) => job.id !== jobId))
    } catch (error: unknown) {
      if (error instanceof SessionExpiredError) {
        resetSession(setSession, setRouteHash, setLoginError, error.message)
        return
      }

      throw error
    }
  }

  async function handleCreateJob(request: SaveJobRequest) {
    await saveJobAndShowDetails(() => createJob(request), addOrReplaceJob, jobMutationActions)
  }

  async function handleUpdateJob(jobId: string, request: SaveJobRequest) {
    await saveJobAndShowDetails(() => updateJob(jobId, request), replaceJob, jobMutationActions)
  }

  if (showJobsView && session) {
    return (
      <main className="app-shell">
        <JobsView
          editingJobId={editingJobId}
          isLoadingJobs={isLoadingJobs}
          isCreatingJob={isCreateRoute}
          jobs={jobs}
          jobsError={jobsError}
          onCreateJob={handleCreateJob}
          onDeleteJob={handleDeleteJob}
          onUpdateJob={handleUpdateJob}
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
        password={password}
        onSubmit={(event) => {
          event.preventDefault()
          void handleSubmit()
        }}
      />
    </main>
  )
}

function getSelectedJobId(
  routeHash: string,
  isCreateRoute: boolean,
  editingJobId: string | undefined,
) {
  if (isCreateRoute || editingJobId) {
    return undefined
  }

  return getJobIdFromRoute(routeHash)
}

function createJobMutationActions(
  setJobs: Dispatch<SetStateAction<Job[]>>,
  setLoginError: (message: string | null) => void,
  setRouteHash: (routeHash: string) => void,
  setSession: (session: StoredSession | null) => void,
): JobMutationActions {
  return {
    setJobs,
    setLoginError,
    setRouteHash,
    setSession,
  }
}

function getInitialRoute(session: StoredSession | null) {
  if (isKnownJobsRoute() && session) {
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
  if (!isKnownJobsRoute() || session) {
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
  const shouldLoadJobs =
    isJobsRoute(routeHash) ||
    isCreateJobRoute(routeHash) ||
    getJobIdFromRoute(routeHash) !== undefined ||
    getJobIdFromEditRoute(routeHash) !== undefined

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

function isKnownJobsRoute(hash: string = window.location.hash) {
  return (
    isJobsRoute(hash) ||
    isCreateJobRoute(hash) ||
    getJobIdFromRoute(hash) !== undefined ||
    getJobIdFromEditRoute(hash) !== undefined
  )
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

  actions.setJobsError(getErrorMessage(error, 'Unable to load jobs.'))
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

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error) {
    return error.message
  }

  return fallback
}

async function saveJobAndShowDetails(
  saveJob: () => Promise<Job>,
  mergeJob: (jobs: Job[], savedJob: Job) => Job[],
  actions: JobMutationActions,
) {
  try {
    const savedJob = await saveJob()
    showSavedJob(savedJob, mergeJob, actions)
  } catch (error: unknown) {
    handleJobMutationError(error, actions)
  }
}

function showSavedJob(
  savedJob: Job,
  mergeJob: (jobs: Job[], savedJob: Job) => Job[],
  actions: JobMutationActions,
) {
  actions.setJobs((currentJobs) => mergeJob(currentJobs, savedJob))
  navigateToJobDetails(savedJob.id, actions.setRouteHash)
}

function navigateToJobDetails(jobId: string, setRouteHash: (routeHash: string) => void) {
  const nextRoute = buildJobDetailRoute(jobId)
  navigateTo(nextRoute)
  setRouteHash(nextRoute)
}

function handleJobMutationError(error: unknown, actions: SessionResetActions) {
  if (error instanceof SessionExpiredError) {
    resetSession(actions.setSession, actions.setRouteHash, actions.setLoginError, error.message)
    return
  }

  throw error
}

function addOrReplaceJob(jobs: Job[], savedJob: Job) {
  const existingJobIndex = jobs.findIndex((job) => job.id === savedJob.id)

  if (existingJobIndex >= 0) {
    const nextJobs = [...jobs]
    nextJobs[existingJobIndex] = savedJob
    return nextJobs
  }

  return [...jobs, savedJob]
}

function replaceJob(jobs: Job[], updatedJob: Job) {
  return jobs.map((job) => {
    if (job.id === updatedJob.id) {
      return updatedJob
    }

    return job
  })
}

export default App
