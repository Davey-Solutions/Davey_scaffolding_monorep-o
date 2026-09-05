import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { JobsView } from './JobsView'

describe('JobsView', () => {
  afterEach(() => {
    cleanup()
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
  })
})
