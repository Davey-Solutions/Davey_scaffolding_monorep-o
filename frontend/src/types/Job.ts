/**
 * Supported lifecycle states for jobs.
 */
const JOB_STATUS_VALUES = ['PENDING', 'IN_PROGRESS', 'COMPLETED'] as const

/**
 * Supported lifecycle states for jobs.
 */
export type JobStatus = (typeof JOB_STATUS_VALUES)[number]

/**
 * Job status constants and utility helpers for status validation.
 */
export const JobStatus = {
  PENDING: 'PENDING' as JobStatus,
  IN_PROGRESS: 'IN_PROGRESS' as JobStatus,
  COMPLETED: 'COMPLETED' as JobStatus,
  /**
   * Returns all known job statuses.
   *
   * @returns status values accepted by the UI
   */
  values(): JobStatus[] {
    return [...JOB_STATUS_VALUES]
  },
  /**
   * Returns whether the input is a supported job status.
   *
   * @param value value to validate
   * @returns {@code true} when the value maps to a known status
   */
  is(value: string): value is JobStatus {
    return JOB_STATUS_VALUES.some((status) => status === value)
  },
}

/**
 * Minimal job payload rendered by the jobs screen.
 */
export interface Job {
  /** Unique identifier for the job. */
  id: string
  /** Customer name shown in the list. */
  customerName: string
  /** Site address shown in the list. */
  siteAddress: string
  /** Current workflow status. */
  status: JobStatus
  /** Whether the job has been paid. */
  paid: boolean
}
