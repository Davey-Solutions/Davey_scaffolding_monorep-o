import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { JobsView } from './JobsView'

describe('JobsView', () => {
  const onCreateJob = vi.fn(async () => {})
  const onDeleteJob = vi.fn(async () => {})
  const onUpdateJob = vi.fn(async () => {})

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    onCreateJob.mockClear()
    onDeleteJob.mockClear()
    onUpdateJob.mockClear()
  })

  it('shows completed and paid badges', () => {
    render(
      <JobsView
        isLoadingJobs={false}
        jobs={[
          {
            id: 'job-1',
            customerName: 'Alice',
            siteAddress: '1 Scaffold Street',
            status: 'COMPLETED',
            paid: true,
          },
        ]}
        jobsError={null}
        onCreateJob={onCreateJob}
        onDeleteJob={onDeleteJob}
        onUpdateJob={onUpdateJob}
      />,
    )

    expect(screen.getByText('Completed')).toBeInTheDocument()
    expect(screen.getByText('Paid', { selector: '.job-badge-paid' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Alice' })).toHaveAttribute('href', '#/jobs/job-1')
    expect(screen.getByRole('link', { name: 'Create job' })).toHaveAttribute('href', '#/jobs/new')
  })

  it('filters jobs by status and paid values', () => {
    render(
      <JobsView
        isLoadingJobs={false}
        jobs={[
          {
            id: 'job-1',
            customerName: 'Alice',
            siteAddress: '1 Scaffold Street',
            status: 'PENDING',
            paid: false,
          },
          {
            id: 'job-2',
            customerName: 'Bob',
            siteAddress: '2 Scaffold Street',
            status: 'COMPLETED',
            paid: true,
          },
          {
            id: 'job-3',
            customerName: 'Cara',
            siteAddress: '3 Scaffold Street',
            status: 'COMPLETED',
            paid: false,
          },
        ]}
        jobsError={null}
        onCreateJob={onCreateJob}
        onDeleteJob={onDeleteJob}
        onUpdateJob={onUpdateJob}
      />,
    )

    fireEvent.change(screen.getByRole('combobox', { name: 'Status' }), {
      target: { value: 'COMPLETED' },
    })
    expect(screen.queryByText('Alice')).not.toBeInTheDocument()
    expect(screen.getByText('Bob')).toBeInTheDocument()
    expect(screen.getByText('Cara')).toBeInTheDocument()

    fireEvent.change(screen.getByRole('combobox', { name: 'Paid' }), {
      target: { value: 'PAID' },
    })
    expect(screen.getByText('Bob')).toBeInTheDocument()
    expect(screen.queryByText('Cara')).not.toBeInTheDocument()

    fireEvent.change(screen.getByRole('combobox', { name: 'Status' }), {
      target: { value: 'PENDING' },
    })
    expect(screen.getByText('No jobs match the selected filters.')).toBeInTheDocument()
    expect(screen.queryByText('Bob')).not.toBeInTheDocument()
  })

  it('shows base empty state when no jobs are loaded', () => {
    render(
      <JobsView
        isLoadingJobs={false}
        jobs={[]}
        jobsError={null}
        onCreateJob={onCreateJob}
        onDeleteJob={onDeleteJob}
        onUpdateJob={onUpdateJob}
      />,
    )

    expect(screen.getByText('No jobs yet.')).toBeInTheDocument()
    expect(screen.queryByText('No jobs match the selected filters.')).not.toBeInTheDocument()
  })

  it('shows a loading state while jobs are being fetched', () => {
    render(
      <JobsView
        isLoadingJobs={true}
        jobs={[]}
        jobsError={null}
        onCreateJob={onCreateJob}
        onDeleteJob={onDeleteJob}
        onUpdateJob={onUpdateJob}
      />,
    )

    expect(screen.getByText('Loading jobs…')).toBeInTheDocument()
  })

  it('renders details for a selected job', () => {
    render(
      <JobsView
        isLoadingJobs={false}
        jobs={[
          {
            id: 'job-1',
            customerName: 'Alice',
            siteAddress: '1 Scaffold Street',
            status: 'IN_PROGRESS',
            paid: false,
          },
        ]}
        jobsError={null}
        onCreateJob={onCreateJob}
        onDeleteJob={onDeleteJob}
        onUpdateJob={onUpdateJob}
        selectedJobId="job-1"
      />,
    )

    expect(screen.getByRole('heading', { name: 'Job details' })).toBeInTheDocument()
    expect(screen.getByText('1 Scaffold Street')).toBeInTheDocument()
    expect(screen.getByText('job-1')).toBeInTheDocument()
    expect(screen.queryByRole('combobox', { name: 'Status' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to jobs' })).toHaveAttribute('href', '#/jobs')
    expect(screen.getByRole('link', { name: 'Edit job' })).toHaveAttribute('href', '#/jobs/job-1/edit')
  })

  it('renders not found state for an unknown selected job', () => {
    render(
      <JobsView
        isLoadingJobs={false}
        jobs={[]}
        jobsError={null}
        onCreateJob={onCreateJob}
        onDeleteJob={onDeleteJob}
        onUpdateJob={onUpdateJob}
        selectedJobId="missing-job"
      />,
    )

    expect(screen.getByText('Job not found.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to jobs' })).toHaveAttribute('href', '#/jobs')
  })

  it('validates the create form before submitting', () => {
    render(
      <JobsView
        isCreatingJob={true}
        isLoadingJobs={false}
        jobs={[]}
        jobsError={null}
        onCreateJob={onCreateJob}
        onDeleteJob={onDeleteJob}
        onUpdateJob={onUpdateJob}
      />,
    )

    fireEvent.change(screen.getByLabelText('Customer name'), { target: { value: '   ' } })
    fireEvent.change(screen.getByLabelText('Site address'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create job' }))

    expect(screen.getByText('Customer name is required.')).toBeInTheDocument()
    expect(screen.getByText('Site address is required.')).toBeInTheDocument()
    expect(onCreateJob).not.toHaveBeenCalled()
  })

  it('submits edited job values including status and paid state', async () => {
    render(
      <JobsView
        editingJobId="job-1"
        isLoadingJobs={false}
        jobs={[
          {
            id: 'job-1',
            customerName: 'Alice',
            siteAddress: '1 Scaffold Street',
            status: 'IN_PROGRESS',
            paid: false,
          },
        ]}
        jobsError={null}
        onCreateJob={onCreateJob}
        onDeleteJob={onDeleteJob}
        onUpdateJob={onUpdateJob}
      />,
    )

    fireEvent.change(screen.getByLabelText('Customer name'), { target: { value: '  Alice Smith  ' } })
    fireEvent.change(screen.getByLabelText('Site address'), { target: { value: '  2 Scaffold Street  ' } })
    fireEvent.change(screen.getByRole('combobox', { name: 'Status' }), {
      target: { value: 'COMPLETED' },
    })
    fireEvent.click(screen.getByRole('checkbox', { name: 'Paid' }))
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() =>
      expect(onUpdateJob).toHaveBeenCalledWith('job-1', {
        customerName: 'Alice Smith',
        siteAddress: '2 Scaffold Street',
        status: 'COMPLETED',
        paid: true,
      }),
    )
  })

  it('deletes a job only after confirmation', () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false)

    render(
      <JobsView
        isLoadingJobs={false}
        jobs={[
          {
            id: 'job-1',
            customerName: 'Alice',
            siteAddress: '1 Scaffold Street',
            status: 'PENDING',
            paid: false,
          },
        ]}
        jobsError={null}
        onCreateJob={onCreateJob}
        onDeleteJob={onDeleteJob}
        onUpdateJob={onUpdateJob}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Delete job for Alice' }))
    expect(confirmSpy).toHaveBeenCalledWith('Are you sure you want to delete this job?')
    expect(onDeleteJob).not.toHaveBeenCalled()
  })

  it('shows deleting state while a confirmed delete is pending', async () => {
    let resolveDelete: (() => void) | null = null
    onDeleteJob.mockImplementationOnce(
      () =>
        new Promise<void>((resolve) => {
          resolveDelete = resolve
        }),
    )
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    render(
      <JobsView
        isLoadingJobs={false}
        jobs={[
          {
            id: 'job-1',
            customerName: 'Alice',
            siteAddress: '1 Scaffold Street',
            status: 'PENDING',
            paid: false,
          },
          {
            id: 'job-2',
            customerName: 'Bob',
            siteAddress: '2 Scaffold Street',
            status: 'PENDING',
            paid: false,
          },
        ]}
        jobsError={null}
        onCreateJob={onCreateJob}
        onDeleteJob={onDeleteJob}
        onUpdateJob={onUpdateJob}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Delete job for Alice' }))
    expect(screen.getByRole('button', { name: 'Delete job for Alice' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Delete job for Bob' })).toBeEnabled()
    resolveDelete?.()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Delete job for Alice' })).toBeEnabled())
  })

  it('shows delete errors after a confirmed delete fails', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    onDeleteJob.mockRejectedValueOnce(new Error('Unable to delete job.'))

    render(
      <JobsView
        isLoadingJobs={false}
        jobs={[
          {
            id: 'job-1',
            customerName: 'Alice',
            siteAddress: '1 Scaffold Street',
            status: 'PENDING',
            paid: false,
          },
        ]}
        jobsError={null}
        onCreateJob={onCreateJob}
        onDeleteJob={onDeleteJob}
        onUpdateJob={onUpdateJob}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Delete job for Alice' }))
    expect(await screen.findByText('Unable to delete job.')).toBeInTheDocument()
    expect(screen.getByText('Alice')).toBeInTheDocument()
  })
})
