import { JobStatus } from '../types/Job'
import type { Job } from '../types/Job'
import { useMemo, useState } from 'react'

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
  /** Deletes a job by id. */
  onDeleteJob: (jobId: string) => Promise<void>
}

type StatusFilter = 'ALL' | JobStatus
type PaidFilter = 'ALL' | 'PAID' | 'UNPAID'
type JobFiltersState = {
  status: StatusFilter
  paid: PaidFilter
}

/**
 * Jobs screen shown after a successful login.
 *
 * @param props jobs state used for rendering
 * @returns the jobs view markup
 */
export function JobsView(props: JobsViewProps) {
  const [filters, setFilters] = useState<JobFiltersState>({ status: 'ALL', paid: 'ALL' })
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [deletingJobIds, setDeletingJobIds] = useState<string[]>([])
  const statusOptions = useMemo(() => getStatusOptions(props.jobs), [props.jobs])
  const filteredJobs = useMemo(() => {
    return props.jobs.filter((job) => matchesFilters(job, filters))
  }, [filters, props.jobs])

  if (props.isLoadingJobs) {
    return <p className="panel">Loading jobs…</p>
  }

  if (props.jobsError) {
    return <p className="panel panel-error">{props.jobsError}</p>
  }

  async function handleDeleteJob(job: Job) {
    const confirmed = window.confirm('Are you sure you want to delete this job?')
    if (!confirmed) {
      return
    }

    setDeleteError(null)
    setDeletingJobIds((current) => addDeletingJobId(current, job.id))

    try {
      await tryDeleteJob(job.id, props.onDeleteJob, setDeleteError)
    } finally {
      setDeletingJobIds((current) => removeDeletingJobId(current, job.id))
    }
  }

  return (
    <section className="jobs-view">
      <header className="jobs-header">
        <div>
          <p className="eyebrow">Signed in</p>
          <h1>Jobs</h1>
        </div>
        <JobsFilters
          filters={filters}
          onPaidFilterChange={(value) => setFilters((current) => ({ ...current, paid: value }))}
          onStatusFilterChange={(value) =>
            setFilters((current) => ({ ...current, status: value }))
          }
          statusOptions={statusOptions}
        />
      </header>
      <div aria-live="polite">
        {deleteError ? <p className="panel panel-error">{deleteError}</p> : null}
        {props.jobs.length === 0 ? <p className="panel">No jobs yet.</p> : null}
        {props.jobs.length > 0 && filteredJobs.length === 0 ? (
          <p className="panel">No jobs match the selected filters.</p>
        ) : null}
        {filteredJobs.length > 0 ? (
          <JobList
            deletingJobIds={deletingJobIds}
            jobs={filteredJobs}
            onDeleteJob={(job) => void handleDeleteJob(job)}
          />
        ) : null}
      </div>
    </section>
  )
}

function JobList(props: {
  jobs: Job[]
  deletingJobIds: string[]
  onDeleteJob: (job: Job) => void
}) {
  return (
    <ul className="job-list">
      {props.jobs.map((job) => (
        <li className="job-card" key={job.id}>
          <JobBadges job={job} />
          <h2>{job.customerName}</h2>
          <p>{job.siteAddress}</p>
          <dl>
            <div>
              <dt>Status</dt>
              <dd>{job.status}</dd>
            </div>
            <div>
              <dt>Paid</dt>
              <dd>{job.paid ? 'Yes' : 'No'}</dd>
            </div>
          </dl>
          <button
            aria-label={`Delete job for ${job.customerName}`}
            className="job-delete-button"
            disabled={props.deletingJobIds.includes(job.id)}
            onClick={() => props.onDeleteJob(job)}
            type="button"
          >
            {props.deletingJobIds.includes(job.id) ? 'Deleting…' : 'Delete'}
          </button>
        </li>
      ))}
    </ul>
  )
}

function JobsFilters(props: {
  filters: JobFiltersState
  statusOptions: JobStatus[]
  onStatusFilterChange: (value: StatusFilter) => void
  onPaidFilterChange: (value: PaidFilter) => void
}) {
  return (
    <div className="jobs-filters">
      <label>
        Status
        <select
          onChange={(event) =>
            props.onStatusFilterChange(parseStatusFilter(event.target.value, props.statusOptions))
          }
          value={props.filters.status}
        >
          <option value="ALL">All statuses</option>
          {props.statusOptions.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </label>
      <label>
        Paid
        <select
          onChange={(event) => props.onPaidFilterChange(parsePaidFilter(event.target.value))}
          value={props.filters.paid}
        >
          <option value="ALL">All payments</option>
          <option value="PAID">Paid</option>
          <option value="UNPAID">Unpaid</option>
        </select>
      </label>
    </div>
  )
}

function JobBadges({ job }: { job: Job }) {
  return (
    <div className="job-badges">
      {job.status === JobStatus.COMPLETED ? <span className="job-badge">Completed</span> : null}
      {job.paid ? <span className="job-badge job-badge-paid">Paid</span> : null}
    </div>
  )
}

function getStatusOptions(jobs: Job[]): JobStatus[] {
  const presentStatuses = new Set(jobs.map((job) => job.status))
  return JobStatus.values().filter((status) => presentStatuses.has(status))
}

function matchesFilters(job: Job, filters: JobFiltersState) {
  const matchesStatus = filters.status === 'ALL' || job.status === filters.status
  const matchesPaid = filters.paid === 'ALL' || (filters.paid === 'PAID' ? job.paid : !job.paid)
  return matchesStatus && matchesPaid
}

function parseStatusFilter(value: string, statusOptions: JobStatus[]): StatusFilter {
  if (value === 'ALL') {
    return value
  }

  if (JobStatus.is(value) && statusOptions.includes(value)) {
    return value
  }

  return 'ALL'
}

function parsePaidFilter(value: string): PaidFilter {
  if (value === 'PAID' || value === 'UNPAID') {
    return value
  }

  return 'ALL'
}

function addDeletingJobId(currentDeletingJobIds: string[], jobId: string) {
  if (currentDeletingJobIds.includes(jobId)) {
    return currentDeletingJobIds
  }

  return [...currentDeletingJobIds, jobId]
}

function removeDeletingJobId(currentDeletingJobIds: string[], jobId: string) {
  return currentDeletingJobIds.filter((id) => id !== jobId)
}

async function tryDeleteJob(
  jobId: string,
  onDeleteJob: (jobId: string) => Promise<void>,
  setDeleteError: (message: string | null) => void,
) {
  try {
    await onDeleteJob(jobId)
  } catch (error: unknown) {
    setDeleteError(error instanceof Error ? error.message : 'Unable to delete job.')
  }
}
