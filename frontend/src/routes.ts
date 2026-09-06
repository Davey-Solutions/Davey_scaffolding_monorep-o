/**
 * Hash route used for the jobs screen.
 */
export const JOBS_ROUTE = '#/jobs'
const JOB_DETAIL_ROUTE_PREFIX = `${JOBS_ROUTE}/`

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
 * Builds a hash route for a specific job.
 *
 * @param jobId job identifier to include in the route
 * @returns hash route pointing to the job detail screen
 */
export function buildJobDetailRoute(jobId: string) {
  return `${JOB_DETAIL_ROUTE_PREFIX}${encodeURIComponent(jobId)}`
}

/**
 * Reads a job id from the current hash route.
 *
 * @param hash location hash to inspect
 * @returns the decoded job id when present, otherwise {@code null}
 */
export function getJobIdFromRoute(hash: string = window.location.hash) {
  if (!hash.startsWith(JOB_DETAIL_ROUTE_PREFIX)) {
    return null
  }

  const routeTail = hash.slice(JOB_DETAIL_ROUTE_PREFIX.length)
  const [firstSegment = ''] = routeTail.split('/')
  const [encodedId = ''] = firstSegment.split('?')

  if (!encodedId) {
    return null
  }

  try {
    return decodeURIComponent(encodedId)
  } catch {
    return null
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
