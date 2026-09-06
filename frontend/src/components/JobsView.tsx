import { JobStatus } from '../types/Job'
import type { Job } from '../types/Job'
import { useMemo, useState } from 'react'
import { buildJobDetailRoute, JOBS_ROUTE } from '../routes'

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
  /** Optional selected job id for detail rendering. */
  selectedJobId?: string
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
  const statusOptions = useMemo(() => getStatusOptions(props.jobs), [props.jobs])
  const selectedJob = useMemo(
    () => (props.selectedJobId ? props.jobs.find((job) => job.id === props.selectedJobId) ?? null : null),
    [props.jobs, props.selectedJobId],
  )
  const filteredJobs = useMemo(() => {
    return props.jobs.filter((job) => matchesFilters(job, filters))
  }, [filters, props.jobs])

  if (props.isLoadingJobs) {
    return <p className="panel">Loading jobs…</p>
  }

  if (props.jobsError) {
    return <p className="panel panel-error">{props.jobsError}</p>
  }

  if (props.selectedJobId) {
    return selectedJob ? <JobDetailView job={selectedJob} /> : <JobNotFoundView />
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
        {props.jobs.length === 0 ? <p className="panel">No jobs yet.</p> : null}
        {props.jobs.length > 0 && filteredJobs.length === 0 ? (
          <p className="panel">No jobs match the selected filters.</p>
        ) : null}
        {filteredJobs.length > 0 ? <JobList jobs={filteredJobs} /> : null}
      </div>
    </section>
  )
}

function JobNotFoundView() {
  return (
    <section className="jobs-view">
      <p className="panel">Job not found.</p>
      <p>
        <a href={JOBS_ROUTE}>Back to jobs</a>
      </p>
    </section>
  )
}

function JobDetailView({ job }: { job: Job }) {
  return (
    <section className="jobs-view">
      <header className="jobs-header">
        <div>
          <p className="eyebrow">Signed in</p>
          <h1>Job details</h1>
        </div>
        <a href={JOBS_ROUTE}>Back to jobs</a>
      </header>
      <article className="job-card">
        <JobBadges job={job} />
        <h2>{job.customerName}</h2>
        <p>{job.siteAddress}</p>
        <dl>
          <div>
            <dt>Job ID</dt>
            <dd>{job.id}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>{job.status}</dd>
          </div>
          <div>
            <dt>Paid</dt>
            <dd>{job.paid ? 'Yes' : 'No'}</dd>
          </div>
        </dl>
      </article>
    </section>
  )
}

function JobList({ jobs }: { jobs: Job[] }) {
  return (
    <ul className="job-list">
      {jobs.map((job) => (
        <li className="job-card" key={job.id}>
          <JobBadges job={job} />
          <h2>
            <a href={buildJobDetailRoute(job.id)}>{job.customerName}</a>
          </h2>
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
