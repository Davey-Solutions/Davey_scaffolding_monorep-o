import { type Dispatch, type FormEvent, type SetStateAction, useEffect, useMemo, useRef, useState } from 'react'
import type { SaveJobRequest } from '../api/SaveJobRequest'
import {
  buildCreateJobRoute,
  buildEditJobRoute,
  buildJobDetailRoute,
  JOBS_ROUTE,
} from '../routes'
import { Job } from '../types/Job'
import { JobStatus } from '../types/JobStatus'
import type { JobStatus as JobStatusValue } from '../types/JobStatus'
import type {
  JobDetailViewProps,
  JobFiltersState,
  JobFormFieldErrors,
  JobFormMode,
  JobFormValues,
  JobFormViewProps,
  JobListProps,
  JobsFiltersProps,
  JobsViewProps,
  PaidFilter,
  StatusFilter,
} from './JobsViewModels'

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
  const selectedJob = useMemo(() => findJobById(props.jobs, props.selectedJobId), [props.jobs, props.selectedJobId])
  const editingJob = useMemo(() => findJobById(props.jobs, props.editingJobId), [props.editingJobId, props.jobs])
  const filteredJobs = useMemo(() => props.jobs.filter((job) => matchesFilters(job, filters)), [filters, props.jobs])

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
    return renderCreateJobView(props.onCreateJob)
  }

  if (props.editingJobId) {
    return renderEditJobView(editingJob, props.onUpdateJob)
  }

  if (props.selectedJobId) {
    return renderSelectedJobView(selectedJob)
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
            onStatusFilterChange={(value) => setFilters((current) => ({ ...current, status: value }))}
            statusOptions={statusOptions}
          />
        </div>
      </header>
      <div aria-live="polite">
        {renderDeleteError(deleteError)}
        {renderJobListState(props.jobs, filteredJobs, deletingJobIds, handleDeleteJob)}
      </div>
    </section>
  )
}

function renderCreateJobView(onCreateJob: (request: SaveJobRequest) => Promise<void>) {
  return (
    <JobFormView
      key="create-job"
      cancelHref={JOBS_ROUTE}
      mode="create"
      onSubmit={onCreateJob}
    />
  )
}

function renderEditJobView(
  editingJob: Job | null,
  onUpdateJob: (jobId: string, request: SaveJobRequest) => Promise<void>,
) {
  if (!editingJob) {
    return <JobNotFoundView />
  }

  return (
    <JobFormView
      key={editingJob.id}
      cancelHref={buildJobDetailRoute(editingJob.id)}
      job={editingJob}
      mode="edit"
      onSubmit={(request) => onUpdateJob(editingJob.id, request)}
    />
  )
}

function renderSelectedJobView(selectedJob: Job | null) {
  if (!selectedJob) {
    return <JobNotFoundView />
  }

  return <JobDetailView job={selectedJob} />
}

function renderDeleteError(deleteError: string | null) {
  if (!deleteError) {
    return null
  }

  return <p className="panel panel-error">{deleteError}</p>
}

function renderJobListState(
  jobs: Job[],
  filteredJobs: Job[],
  deletingJobIds: string[],
  onDeleteJob: (job: Job) => void,
) {
  if (jobs.length === 0) {
    return <p className="panel">No jobs yet.</p>
  }

  if (filteredJobs.length === 0) {
    return <p className="panel">No jobs match the selected filters.</p>
  }

  return <JobList deletingJobIds={deletingJobIds} jobs={filteredJobs} onDeleteJob={onDeleteJob} />
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

function JobDetailView(props: JobDetailViewProps) {
  return (
    <section className="jobs-view">
      <header className="jobs-header">
        <div>
          <p className="eyebrow">Signed in</p>
          <h1>Job details</h1>
        </div>
        <div className="jobs-header-actions">
          <a className="job-primary-action" href={buildEditJobRoute(props.job.id)}>
            Edit job
          </a>
          <a href={JOBS_ROUTE}>Back to jobs</a>
        </div>
      </header>
      <article className="job-card">
        <JobBadges job={props.job} />
        <h2>{props.job.customerName}</h2>
        <p>{props.job.siteAddress}</p>
        <dl>
          <div>
            <dt>Job ID</dt>
            <dd>{props.job.id}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>{props.job.status}</dd>
          </div>
          <div>
            <dt>Paid</dt>
            <dd>{getPaidText(props.job.paid)}</dd>
          </div>
        </dl>
      </article>
    </section>
  )
}

function JobFormView(props: JobFormViewProps) {
  const [values, setValues] = useState<JobFormValues>(() => getInitialJobFormValues(props.job))
  const [fieldErrors, setFieldErrors] = useState<JobFormFieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const customerNameErrorId = 'job-form-customer-name-error'
  const siteAddressErrorId = 'job-form-site-address-error'
  const resetKeyRef = useRef<string | null>(null)
  const formResetKey = buildFormResetKey(props)

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
      setFormError(getJobFormErrorMessage(error, props.mode))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="jobs-view">
      <header className="jobs-header">
        <div>
          <p className="eyebrow">Signed in</p>
          <h1>{getJobFormHeading(props.mode)}</h1>
        </div>
        <a href={props.cancelHref}>Cancel</a>
      </header>
      <form className="job-form" noValidate onSubmit={handleSubmit}>
        <label>
          <span>Customer name</span>
          <input
            aria-describedby={getFieldErrorId(fieldErrors.customerName, customerNameErrorId)}
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
        {renderFieldError(fieldErrors.customerName, customerNameErrorId)}
        <label>
          <span>Site address</span>
          <input
            aria-describedby={getFieldErrorId(fieldErrors.siteAddress, siteAddressErrorId)}
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
        {renderFieldError(fieldErrors.siteAddress, siteAddressErrorId)}
        {renderEditOnlyFields(props.mode, values, setValues)}
        {renderFormError(formError)}
        <div className="job-form-actions">
          <button className="job-primary-button" disabled={isSubmitting} type="submit">
            {getJobFormButtonLabel(isSubmitting, props.mode)}
          </button>
          <a href={props.cancelHref}>Cancel</a>
        </div>
      </form>
    </section>
  )
}

function renderEditOnlyFields(
  mode: JobFormMode,
  values: JobFormValues,
  setValues: Dispatch<SetStateAction<JobFormValues>>,
) {
  if (mode !== 'edit') {
    return null
  }

  return (
    <>
      <label>
        <span>Status</span>
        <select
          name="status"
          onChange={(event) => {
            const nextStatus = parseJobStatus(event.target.value, values.status)
            setValues((current) => ({ ...current, status: nextStatus }))
          }}
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
          onChange={(event) => setValues((current) => ({ ...current, paid: event.target.checked }))}
          type="checkbox"
        />
        <span>Paid</span>
      </label>
    </>
  )
}

function renderFieldError(message: string | undefined, errorId: string) {
  if (!message) {
    return null
  }

  return (
    <p className="field-error" id={errorId}>
      {message}
    </p>
  )
}

function renderFormError(formError: string | null) {
  if (!formError) {
    return null
  }

  return (
    <p className="panel panel-error" role="alert">
      {formError}
    </p>
  )
}

function JobList(props: JobListProps) {
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
              <dd>{getPaidText(job.paid)}</dd>
            </div>
          </dl>
          <button
            aria-label={`Delete job for ${job.customerName}`}
            className="job-delete-button"
            disabled={props.deletingJobIds.includes(job.id)}
            onClick={() => props.onDeleteJob(job)}
            type="button"
          >
            {getDeleteButtonLabel(props.deletingJobIds, job.id)}
          </button>
        </li>
      ))}
    </ul>
  )
}

function JobsFilters(props: JobsFiltersProps) {
  return (
    <div className="jobs-filters">
      <label>
        Status
        <select
          onChange={(event) => props.onStatusFilterChange(parseStatusFilter(event.target.value, props.statusOptions))}
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
      {renderCompletedBadge(job)}
      {renderPaidBadge(job)}
    </div>
  )
}

function renderCompletedBadge(job: Job) {
  if (job.status !== JobStatus.COMPLETED) {
    return null
  }

  return <span className="job-badge">Completed</span>
}

function renderPaidBadge(job: Job) {
  if (!job.paid) {
    return null
  }

  return <span className="job-badge job-badge-paid">Paid</span>
}

function findJobById(jobs: Job[], jobId: string | undefined) {
  if (!jobId) {
    return null
  }

  for (const job of jobs) {
    if (job.id === jobId) {
      return job
    }
  }

  return null
}

function buildFormResetKey(props: JobFormViewProps) {
  return JSON.stringify({
    mode: props.mode,
    id: props.job?.id ?? null,
    customerName: props.job?.customerName ?? null,
    siteAddress: props.job?.siteAddress ?? null,
    status: props.job?.status ?? null,
    paid: props.job?.paid ?? null,
  })
}

function getFieldErrorId(message: string | undefined, errorId: string) {
  if (!message) {
    return undefined
  }

  return errorId
}

function getJobFormHeading(mode: JobFormMode) {
  if (mode === 'create') {
    return 'Create job'
  }

  return 'Edit job'
}

function getJobFormButtonLabel(isSubmitting: boolean, mode: JobFormMode) {
  if (isSubmitting) {
    return getJobFormPendingLabel(mode)
  }

  return getJobFormSubmitLabel(mode)
}

function getJobFormPendingLabel(mode: JobFormMode) {
  if (mode === 'create') {
    return 'Creating…'
  }

  return 'Saving…'
}

function getJobFormSubmitLabel(mode: JobFormMode) {
  if (mode === 'create') {
    return 'Create job'
  }

  return 'Save changes'
}

function getJobFormErrorMessage(error: unknown, mode: JobFormMode) {
  if (error instanceof Error) {
    return error.message
  }

  if (mode === 'create') {
    return 'Unable to create job.'
  }

  return 'Unable to save job changes.'
}

function getDeleteButtonLabel(deletingJobIds: string[], jobId: string) {
  if (deletingJobIds.includes(jobId)) {
    return 'Deleting…'
  }

  return 'Delete'
}

function getPaidText(paid: boolean) {
  if (paid) {
    return 'Yes'
  }

  return 'No'
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

  const copiedJob = new Job(job)
  return {
    customerName: copiedJob.customerName,
    siteAddress: copiedJob.siteAddress,
    status: copiedJob.status,
    paid: copiedJob.paid,
  }
}

function validateJobForm(values: JobFormValues): JobFormFieldErrors {
  return {
    customerName: getRequiredFieldError(values.customerName, 'Customer name is required.'),
    siteAddress: getRequiredFieldError(values.siteAddress, 'Site address is required.'),
  }
}

function getRequiredFieldError(value: string, message: string) {
  if (value.trim()) {
    return undefined
  }

  return message
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

function parseJobStatus(value: string, fallback: JobStatusValue) {
  if (JobStatus.is(value)) {
    return value
  }

  return fallback
}

function getStatusOptions(jobs: Job[]) {
  const presentStatuses = new Set(jobs.map((job) => job.status))
  return JobStatus.values().filter((status) => presentStatuses.has(status))
}

function matchesFilters(job: Job, filters: JobFiltersState) {
  return matchesStatusFilter(job, filters.status) && matchesPaidFilter(job, filters.paid)
}

function matchesStatusFilter(job: Job, statusFilter: StatusFilter) {
  if (statusFilter === 'ALL') {
    return true
  }

  return job.status === statusFilter
}

function matchesPaidFilter(job: Job, paidFilter: PaidFilter) {
  if (paidFilter === 'ALL') {
    return true
  }

  if (paidFilter === 'PAID') {
    return job.paid
  }

  return !job.paid
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
  if (value === 'PAID') {
    return value
  }

  if (value === 'UNPAID') {
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
    setDeleteError(getDeleteErrorMessage(error))
  }
}

function getDeleteErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message
  }

  return 'Unable to delete job.'
}
