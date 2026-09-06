import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { JobsView } from './JobsView'

describe('JobsView', () => {
  const onDeleteJob = vi.fn(async () => {})

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    onDeleteJob.mockClear()
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
        onDeleteJob={onDeleteJob}
      />,
    )

    expect(screen.getByText('Completed')).toBeInTheDocument()
    expect(screen.getByText('Paid', { selector: '.job-badge-paid' })).toBeInTheDocument()
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
        onDeleteJob={onDeleteJob}
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
    render(<JobsView isLoadingJobs={false} jobs={[]} jobsError={null} onDeleteJob={onDeleteJob} />)

    expect(screen.getByText('No jobs yet.')).toBeInTheDocument()
    expect(screen.queryByText('No jobs match the selected filters.')).not.toBeInTheDocument()
  })

  it('shows a loading state while jobs are being fetched', () => {
    render(<JobsView isLoadingJobs={true} jobs={[]} jobsError={null} onDeleteJob={onDeleteJob} />)

    expect(screen.getByText('Loading jobs…')).toBeInTheDocument()
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
        onDeleteJob={onDeleteJob}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Delete job for Alice' }))
    expect(confirmSpy).toHaveBeenCalledWith('Are you sure you want to delete this job?')
    expect(onDeleteJob).not.toHaveBeenCalled()
  })

  it('shows deleting state while a confirmed delete is pending', () => {
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
        ]}
        jobsError={null}
        onDeleteJob={onDeleteJob}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Delete job for Alice' }))
    expect(screen.getByRole('button', { name: 'Delete job for Alice' })).toBeDisabled()
    resolveDelete?.()
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
        onDeleteJob={onDeleteJob}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Delete job for Alice' }))
    expect(await screen.findByText('Unable to delete job.')).toBeInTheDocument()
    expect(screen.getByText('Alice')).toBeInTheDocument()
  })
})
