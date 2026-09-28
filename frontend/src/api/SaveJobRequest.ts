import type { JobStatus } from '../types/JobStatus'

/**
 * Payload accepted by the create and update job endpoints.
 */
export interface SaveJobRequest {
  /** Customer name required by the API. */
  customerName: string
  /** Site address required by the API. */
  siteAddress: string
  /** Optional lifecycle status update. */
  status?: JobStatus
  /** Optional payment-state update. */
  paid?: boolean
}
