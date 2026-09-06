import { describe, expect, it } from 'vitest'
import { buildJobDetailRoute, getJobIdFromRoute } from './routes'

describe('routes', () => {
  it('builds and parses a job detail route', () => {
    const route = buildJobDetailRoute('job-1')
    expect(route).toBe('#/jobs/job-1')
    expect(getJobIdFromRoute(route)).toBe('job-1')
  })

  it('parses only the first path segment and ignores query text', () => {
    expect(getJobIdFromRoute('#/jobs/job-1?tab=notes')).toBe('job-1')
    expect(getJobIdFromRoute('#/jobs/job-1/extra')).toBe('job-1')
  })
})
