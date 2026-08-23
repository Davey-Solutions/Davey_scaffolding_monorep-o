/**
 * Hash route used for the jobs screen.
 */
export const JOBS_ROUTE = '#/jobs'

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
