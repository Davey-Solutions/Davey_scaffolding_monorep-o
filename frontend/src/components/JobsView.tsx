import type { Job } from '../types/Job'

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

/**
 * Jobs screen shown after a successful login.
 *
 * @param props jobs state used for rendering
 * @returns the jobs view markup
 */
export function JobsView(props: JobsViewProps) {
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
      </header>
      {props.jobs.length > 0 ? <JobList jobs={props.jobs} /> : <p className="panel">No jobs yet.</p>}
    </section>
  )
}

function JobList({ jobs }: { jobs: Job[] }) {
  return (
    <ul className="job-list">
      {jobs.map((job) => (
        <li className="job-card" key={job.id}>
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
