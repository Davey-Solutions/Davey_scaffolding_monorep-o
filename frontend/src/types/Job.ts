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
  status: string
  /** Whether the job has been paid. */
  paid: boolean
}
