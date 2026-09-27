import { type FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import type { SaveJobRequest } from '../api/apiClient'
import {
  buildCreateJobRoute,
  buildEditJobRoute,
  buildJobDetailRoute,
  JOBS_ROUTE,
} from '../routes'
import { JobStatus } from '../types/Job'
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

type StatusFilter = 'ALL' | JobStatus
type PaidFilter = 'ALL' | 'PAID' | 'UNPAID'
type JobFiltersState = {
  status: StatusFilter
  paid: PaidFilter
}
type JobFormMode = 'create' | 'edit'
type JobFormValues = {
  customerName: string
  siteAddress: string
  status: JobStatus
  paid: boolean
}
type JobFormFieldErrors = Partial<Record<'customerName' | 'siteAddress', string>>

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
  const selectedJob = useMemo(
    () => (props.selectedJobId ? props.jobs.find((job) => job.id === props.selectedJobId) ?? null : null),
    [props.jobs, props.selectedJobId],
  )
  const editingJob = useMemo(
    () => (props.editingJobId ? props.jobs.find((job) => job.id === props.editingJobId) ?? null : null),
    [props.editingJobId, props.jobs],
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

  if (props.isCreatingJob) {
    return (
      <JobFormView
        key="create-job"
        cancelHref={JOBS_ROUTE}
        mode="create"
        onSubmit={props.onCreateJob}
      />
    )
  }

  if (props.editingJobId) {
    return editingJob ? (
      <JobFormView
        key={editingJob.id}
        cancelHref={buildJobDetailRoute(editingJob.id)}
        job={editingJob}
        mode="edit"
        onSubmit={(request) => props.onUpdateJob(editingJob.id, request)}
      />
    ) : (
      <JobNotFoundView />
    )
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
        <div className="jobs-header-actions">
          <a className="job-primary-action" href={buildCreateJobRoute()}>
            Create job
          </a>
          <JobsFilters
            filters={filters}
            onPaidFilterChange={(value) => setFilters((current) => ({ ...current, paid: value }))}
            onStatusFilterChange={(value) =>
              setFilters((current) => ({ ...current, status: value }))
            }
            statusOptions={statusOptions}
          />
        </div>
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
        <div className="jobs-header-actions">
          <a className="job-primary-action" href={buildEditJobRoute(job.id)}>
            Edit job
          </a>
          <a href={JOBS_ROUTE}>Back to jobs</a>
        </div>
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

function JobFormView(props: {
  mode: JobFormMode
  cancelHref: string
  onSubmit: (request: SaveJobRequest) => Promise<void>
  job?: Job
}) {
  const [values, setValues] = useState<JobFormValues>(() => getInitialJobFormValues(props.job))
  const [fieldErrors, setFieldErrors] = useState<JobFormFieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const heading = props.mode === 'create' ? 'Create job' : 'Edit job'
  const submitLabel = props.mode === 'create' ? 'Create job' : 'Save changes'
  const pendingLabel = props.mode === 'create' ? 'Creating…' : 'Saving…'
  const customerNameErrorId = 'job-form-customer-name-error'
  const siteAddressErrorId = 'job-form-site-address-error'
  const resetKeyRef = useRef<string | null>(null)
  const formResetKey = JSON.stringify({
    mode: props.mode,
    id: props.job?.id ?? null,
    customerName: props.job?.customerName ?? null,
    siteAddress: props.job?.siteAddress ?? null,
    status: props.job?.status ?? null,
    paid: props.job?.paid ?? null,
  })

  useEffect(() => {
    if (resetKeyRef.current === formResetKey) {
      return
    }

    resetKeyRef.current = formResetKey
    setValues(getInitialJobFormValues(props.job))
    setFieldErrors({})
    setFormError(null)
    setIsSubmitting(false)
  }, [formResetKey, props.job])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError(null)

    const nextFieldErrors = validateJobForm(values)
    setFieldErrors(nextFieldErrors)

    if (hasFieldErrors(nextFieldErrors)) {
      return
    }

    setIsSubmitting(true)

    try {
      await props.onSubmit(buildSaveJobRequest(values, props.mode))
    } catch (error: unknown) {
      setFormError(
        error instanceof Error
          ? error.message
          : props.mode === 'create'
            ? 'Unable to create job.'
            : 'Unable to save job changes.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="jobs-view">
      <header className="jobs-header">
        <div>
          <p className="eyebrow">Signed in</p>
          <h1>{heading}</h1>
        </div>
        <a href={props.cancelHref}>Cancel</a>
      </header>
      <form className="job-form" noValidate onSubmit={handleSubmit}>
        <label>
          <span>Customer name</span>
          <input
            aria-describedby={fieldErrors.customerName ? customerNameErrorId : undefined}
            aria-invalid={Boolean(fieldErrors.customerName)}
            name="customerName"
            onChange={(event) => {
              setValues((current) => ({ ...current, customerName: event.target.value }))
              setFieldErrors((current) => ({ ...current, customerName: undefined }))
            }}
            required
            type="text"
            value={values.customerName}
          />
        </label>
        {fieldErrors.customerName ? (
          <p className="field-error" id={customerNameErrorId}>
            {fieldErrors.customerName}
          </p>
        ) : null}
        <label>
          <span>Site address</span>
          <input
            aria-describedby={fieldErrors.siteAddress ? siteAddressErrorId : undefined}
            aria-invalid={Boolean(fieldErrors.siteAddress)}
            name="siteAddress"
            onChange={(event) => {
              setValues((current) => ({ ...current, siteAddress: event.target.value }))
              setFieldErrors((current) => ({ ...current, siteAddress: undefined }))
            }}
            required
            type="text"
            value={values.siteAddress}
          />
        </label>
        {fieldErrors.siteAddress ? (
          <p className="field-error" id={siteAddressErrorId}>
            {fieldErrors.siteAddress}
          </p>
        ) : null}
        {props.mode === 'edit' ? (
          <>
            <label>
              <span>Status</span>
              <select
                name="status"
                onChange={(event) =>
                  setValues((current) => ({
                    ...current,
                    status: parseJobStatus(event.target.value, current.status),
                  }))
                }
                value={values.status}
              >
                {JobStatus.values().map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </label>
            <label className="job-checkbox">
              <input
                checked={values.paid}
                name="paid"
                onChange={(event) =>
                  setValues((current) => ({ ...current, paid: event.target.checked }))
                }
                type="checkbox"
              />
              <span>Paid</span>
            </label>
          </>
        ) : null}
        {formError ? (
          <p className="panel panel-error" role="alert">
            {formError}
          </p>
        ) : null}
        <div className="job-form-actions">
          <button className="job-primary-button" disabled={isSubmitting} type="submit">
            {isSubmitting ? pendingLabel : submitLabel}
          </button>
          <a href={props.cancelHref}>Cancel</a>
        </div>
      </form>
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

function getInitialJobFormValues(job?: Job): JobFormValues {
  if (!job) {
    return {
      customerName: '',
      siteAddress: '',
      status: JobStatus.PENDING,
      paid: false,
    }
  }

  return {
    customerName: job.customerName,
    siteAddress: job.siteAddress,
    status: job.status,
    paid: job.paid,
  }
}

function validateJobForm(values: JobFormValues): JobFormFieldErrors {
  const customerName = values.customerName.trim()
  const siteAddress = values.siteAddress.trim()

  return {
    customerName: customerName ? undefined : 'Customer name is required.',
    siteAddress: siteAddress ? undefined : 'Site address is required.',
  }
}

function hasFieldErrors(fieldErrors: JobFormFieldErrors) {
  return Boolean(fieldErrors.customerName || fieldErrors.siteAddress)
}

function buildSaveJobRequest(values: JobFormValues, mode: JobFormMode): SaveJobRequest {
  const request: SaveJobRequest = {
    customerName: values.customerName.trim(),
    siteAddress: values.siteAddress.trim(),
  }

  if (mode === 'edit') {
    request.status = values.status
    request.paid = values.paid
  }

  return request
}

function parseJobStatus(value: string, fallback: JobStatus) {
  return JobStatus.is(value) ? value : fallback
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
