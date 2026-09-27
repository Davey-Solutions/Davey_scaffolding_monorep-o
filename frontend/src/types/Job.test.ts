import { describe, expect, it } from 'vitest'
import { Job } from './Job'
import { JobStatus } from './JobStatus'

describe('Job', () => {
  it('copies job values through the constructor', () => {
    const originalJob = new Job({
      id: 'job-1',
      customerName: 'Alice',
      siteAddress: '1 Scaffold Street',
      status: JobStatus.IN_PROGRESS,
      paid: true,
    })

    const copiedJob = new Job(originalJob)

    expect(copiedJob).not.toBe(originalJob)
    expect(copiedJob).toEqual(originalJob)
  })

  it('creates pending unpaid jobs through the factory', () => {
    const pendingJob = Job.createPending('job-2', 'Bob', '2 Scaffold Street')

    expect(pendingJob).toEqual(
      new Job({
        id: 'job-2',
        customerName: 'Bob',
        siteAddress: '2 Scaffold Street',
        status: JobStatus.PENDING,
        paid: false,
      }),
    )
  })
})
