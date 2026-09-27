/**
 * Hash route used for the jobs screen.
 */
export const JOBS_ROUTE = '#/jobs'
const CREATE_JOB_ROUTE = `${JOBS_ROUTE}/new`
const JOB_DETAIL_ROUTE_PREFIX = `${JOBS_ROUTE}/`
const EDIT_JOB_ROUTE_SUFFIX = '/edit'

/**
 * Returns whether the current hash targets the jobs screen.
 *
 * @param hash location hash to inspect
 * @returns {@code true} when the jobs screen should be shown
 */
export function isJobsRoute(hash: string = window.location.hash) {
  return hash === JOBS_ROUTE
}

/**
 * Returns whether the current hash targets the create-job screen.
 *
 * @param hash location hash to inspect
 * @returns {@code true} when the create-job screen should be shown
 */
export function isCreateJobRoute(hash: string = window.location.hash) {
  return hash === CREATE_JOB_ROUTE
}

/**
 * Builds a hash route for creating a new job.
 *
 * @returns hash route pointing to the create-job screen
 */
export function buildCreateJobRoute() {
  return CREATE_JOB_ROUTE
}

/**
 * Builds a hash route for a specific job.
 *
 * @param jobId job identifier to include in the route
 * @returns hash route pointing to the job detail screen
 */
export function buildJobDetailRoute(jobId: string) {
  return `${JOB_DETAIL_ROUTE_PREFIX}${encodeURIComponent(jobId)}`
}

/**
 * Builds a hash route for editing a specific job.
 *
 * @param jobId job identifier to include in the route
 * @returns hash route pointing to the edit-job screen
 */
export function buildEditJobRoute(jobId: string) {
  return `${buildJobDetailRoute(jobId)}${EDIT_JOB_ROUTE_SUFFIX}`
}

/**
 * Reads a job id from the current hash route.
 *
 * @param hash location hash to inspect
 * @returns the decoded job id when present, otherwise {@code undefined}
 */
export function getJobIdFromRoute(hash: string = window.location.hash) {
  const routeTail = getRouteTail(hash)

  if (!routeTail) {
    return undefined
  }

  const encodedId = getEncodedJobId(routeTail)

  if (!isSinglePathSegment(encodedId) || encodedId === 'new') {
    return undefined
  }

  return decodeJobId(encodedId)
}

/**
 * Reads a job id from the current edit hash route.
 *
 * @param hash location hash to inspect
 * @returns the decoded job id when present, otherwise {@code undefined}
 */
export function getJobIdFromEditRoute(hash: string = window.location.hash) {
  const routeTail = getRouteTail(hash)

  if (!routeTail) {
    return undefined
  }

  const routeSegments = getRoutePath(routeTail).split('/')

  if (routeSegments.length !== 2) {
    return undefined
  }

  const [encodedId = '', action = ''] = routeSegments

  if (action !== 'edit' || !isSinglePathSegment(encodedId) || encodedId === 'new') {
    return undefined
  }

  return decodeJobId(encodedId)
}

function getRouteTail(hash: string) {
  if (!hash.startsWith(JOB_DETAIL_ROUTE_PREFIX)) {
    return undefined
  }

  return hash.slice(JOB_DETAIL_ROUTE_PREFIX.length)
}

function getEncodedJobId(routeTail: string) {
  const [encodedId = ''] = routeTail.split('?')
  return encodedId
}

function getRoutePath(routeTail: string) {
  const [path = ''] = routeTail.split('?')
  return path
}

function isSinglePathSegment(encodedId: string) {
  return Boolean(encodedId) && !encodedId.includes('/')
}

function decodeJobId(encodedId: string) {
  try {
    return decodeURIComponent(encodedId)
  } catch {
    return undefined
  }
}

/**
 * Navigates within the single-page app hash routes.
 *
 * @param path hash route to navigate to
 * @param replace whether to replace the current history entry
 */
export function navigateTo(path: string, replace = false) {
  if (replace) {
    replaceLocation(path)
    return
  }

  window.location.hash = normalizePath(path)
}

function replaceLocation(path: string) {
  const target = normalizePath(path)
  const url = `${window.location.pathname}${window.location.search}${target}`
  window.history.replaceState(null, '', url)
}

function normalizePath(path: string) {
  return path === '/' ? '' : path
}
