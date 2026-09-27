import type { SaveJobRequest } from '../api/SaveJobRequest'
import type { Job } from '../types/Job'
import type { JobStatus } from '../types/JobStatus'

/**
 * Props required to render the jobs screen.
 */
export interface JobsViewProps {
  /** Jobs loaded for the signed-in user. */
  jobs: Job[]
  /** Whether the jobs request is still loading. */
  isLoadingJobs: boolean
  /** Optional error shown when jobs fail to load. */
  jobsError: string | null
  /** Creates a job from validated form input. */
  onCreateJob: (request: SaveJobRequest) => Promise<void>
  /** Deletes a job by id. */
  onDeleteJob: (jobId: string) => Promise<void>
  /** Updates a job from validated form input. */
  onUpdateJob: (jobId: string, request: SaveJobRequest) => Promise<void>
  /** Whether the create-job form should be shown. */
  isCreatingJob?: boolean
  /** Optional selected job id for detail rendering. */
  selectedJobId?: string
  /** Optional selected job id for edit rendering. */
  editingJobId?: string
}

export type StatusFilter = 'ALL' | JobStatus
export type PaidFilter = 'ALL' | 'PAID' | 'UNPAID'

export interface JobFiltersState {
  status: StatusFilter
  paid: PaidFilter
}

export type JobFormMode = 'create' | 'edit'

export interface JobFormValues {
  customerName: string
  siteAddress: string
  status: JobStatus
  paid: boolean
}

export type JobFormFieldErrors = Partial<Record<'customerName' | 'siteAddress', string>>

export interface JobDetailViewProps {
  job: Job
}

export interface JobFormViewProps {
  mode: JobFormMode
  cancelHref: string
  onSubmit: (request: SaveJobRequest) => Promise<void>
  job?: Job
}

export interface JobListProps {
  jobs: Job[]
  deletingJobIds: string[]
  onDeleteJob: (job: Job) => void
}

export interface JobsFiltersProps {
  filters: JobFiltersState
  statusOptions: JobStatus[]
  onStatusFilterChange: (value: StatusFilter) => void
  onPaidFilterChange: (value: PaidFilter) => void
}
