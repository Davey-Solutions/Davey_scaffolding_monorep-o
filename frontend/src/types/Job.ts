import { JobStatus, type JobStatus as JobStatusValue } from './JobStatus'

/**
 * Primitive job data copied into {@link Job} instances.
 */
export interface JobData {
  /** Unique identifier for the job. */
  id: string
  /** Customer name shown in the list. */
  customerName: string
  /** Site address shown in the list. */
  siteAddress: string
  /** Current workflow status. */
  status: JobStatusValue
  /** Whether the job has been paid. */
  paid: boolean
}

/**
 * Minimal job payload rendered by the jobs screen.
 */
export class Job {
  /** Unique identifier for the job. */
  public readonly id: string
  /** Customer name shown in the list. */
  public readonly customerName: string
  /** Site address shown in the list. */
  public readonly siteAddress: string
  /** Current workflow status. */
  public readonly status: JobStatusValue
  /** Whether the job has been paid. */
  public readonly paid: boolean

  /**
   * Creates a new job by copying the supplied values.
   *
   * @param values source values used to populate this instance
   */
  public constructor(values: JobData | Job) {
    this.id = values.id
    this.customerName = values.customerName
    this.siteAddress = values.siteAddress
    this.status = values.status
    this.paid = values.paid
  }

  /**
   * Creates a new pending unpaid job instance.
   *
   * @param id job identifier
   * @param customerName customer name
   * @param siteAddress site address
   * @returns pending unpaid job instance
   */
  public static createPending(id: string, customerName: string, siteAddress: string) {
    return new Job({
      id,
      customerName,
      siteAddress,
      status: JobStatus.PENDING,
      paid: false,
    })
  }
}
