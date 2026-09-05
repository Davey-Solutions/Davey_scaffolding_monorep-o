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
}

type StatusFilter = 'ALL' | Job['status']
type PaidFilter = 'ALL' | 'PAID' | 'UNPAID'

/**
 * Jobs screen shown after a successful login.
 *
 * @param props jobs state used for rendering
 * @returns the jobs view markup
 */
export function JobsView(props: JobsViewProps) {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')
  const [paidFilter, setPaidFilter] = useState<PaidFilter>('ALL')
  const statusOptions = useMemo(() => {
    return Array.from(new Set(props.jobs.map((job) => job.status)))
  }, [props.jobs])
  const filteredJobs = useMemo(() => {
    return props.jobs.filter((job) => {
      const matchesStatus = statusFilter === 'ALL' || job.status === statusFilter
      const matchesPaid =
        paidFilter === 'ALL' || (paidFilter === 'PAID' ? job.paid : !job.paid)
      return matchesStatus && matchesPaid
    })
  }, [paidFilter, props.jobs, statusFilter])

  if (props.isLoadingJobs) {
    return <p className="panel">Loading jobs…</p>
  }

  if (props.jobsError) {
    return <p className="panel panel-error">{props.jobsError}</p>
  }

  return (
    <section className="jobs-view">
      <header className="jobs-header">
        <div>
          <p className="eyebrow">Signed in</p>
          <h1>Jobs</h1>
        </div>
        <div className="jobs-filters">
          <label>
            Status
            <select
              onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
              value={statusFilter}
            >
              <option value="ALL">All statuses</option>
              {statusOptions.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>
          <label>
            Paid
            <select
              onChange={(event) => setPaidFilter(event.target.value as PaidFilter)}
              value={paidFilter}
            >
              <option value="ALL">All payments</option>
              <option value="PAID">Paid</option>
              <option value="UNPAID">Unpaid</option>
            </select>
          </label>
        </div>
      </header>
      {props.jobs.length === 0 ? <p className="panel">No jobs yet.</p> : null}
      {props.jobs.length > 0 && filteredJobs.length === 0 ? (
        <p className="panel">No jobs match the selected filters.</p>
      ) : null}
      {filteredJobs.length > 0 ? <JobList jobs={filteredJobs} /> : null}
    </section>
  )
}

function JobList({ jobs }: { jobs: Job[] }) {
  return (
    <ul className="job-list">
      {jobs.map((job) => (
        <li className="job-card" key={job.id}>
          <div className="job-badges">
            {job.status === 'COMPLETED' ? <span className="job-badge">Completed</span> : null}
            {job.paid ? (
              <span className="job-badge job-badge-paid">Paid</span>
            ) : null}
          </div>
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
        </li>
      ))}
    </ul>
  )
}
