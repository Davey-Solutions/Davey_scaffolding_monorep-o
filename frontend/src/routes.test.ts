import { describe, expect, it } from 'vitest'
import { buildJobDetailRoute, getJobIdFromRoute } from './routes'

describe('routes', () => {
  it('builds and parses a job detail route', () => {
    const route = buildJobDetailRoute('job-1')
    expect(route).toBe('#/jobs/job-1')
    expect(getJobIdFromRoute(route)).toBe('job-1')
  })

  it('ignores query text when parsing detail routes', () => {
    expect(getJobIdFromRoute('#/jobs/job-1?tab=notes')).toBe('job-1')
  })

  it('supports ids containing slashes', () => {
    const route = buildJobDetailRoute('group/job-1')
    expect(getJobIdFromRoute(route)).toBe('group/job-1')
  })

  it('rejects routes with additional path segments', () => {
    expect(getJobIdFromRoute('#/jobs/job-1/extra')).toBeUndefined()
  })

  it('returns undefined for malformed encoded job ids', () => {
    expect(getJobIdFromRoute('#/jobs/%E0%A4%A')).toBeUndefined()
  })
})
