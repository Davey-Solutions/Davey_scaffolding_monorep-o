import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  const originalFetch = global.fetch

  beforeEach(() => {
    window.localStorage.clear()
    window.history.replaceState(null, '', '/')
  })

  afterEach(() => {
    cleanup()
    global.fetch = originalFetch
    vi.restoreAllMocks()
  })

  it('logs in, stores tokens, loads jobs, and sends the bearer token', async () => {
    let jobRequestHeaders: Headers | null = null
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/auth/login')) {
        return createJsonResponse({
          accessToken: 'access-token',
          refreshToken: 'refresh-token',
        })
      }

      jobRequestHeaders = new Headers(init?.headers)

      return createJsonResponse([
        {
          id: 'job-1',
          customerName: 'Alice',
          siteAddress: '1 Scaffold Street',
          status: 'PENDING',
          paid: false,
        },
      ])
    })

    global.fetch = fetchMock

    render(<App />)
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'owner@example.com' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password-123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }))

    await screen.findByRole('heading', { name: 'Jobs' })
    await screen.findByText('Alice')
    await waitFor(() => expect(window.location.hash).toBe('#/jobs'))
    expect(window.localStorage.getItem('davey.accessToken')).toBe('access-token')
    expect(window.localStorage.getItem('davey.refreshToken')).toBe('refresh-token')
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(jobRequestHeaders).toBeInstanceOf(Headers)
    expect(jobRequestHeaders?.get('Authorization')).toBe(['Bearer', 'access-token'].join(' '))
  })

  it('shows an invalid credentials error', async () => {
    global.fetch = vi.fn<typeof fetch>().mockResolvedValue(new Response(null, { status: 401 }))

    render(<App />)
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'owner@example.com' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong-password' } })
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }))

    await screen.findByText('Invalid email or password.')
    expect(window.location.hash).toBe('')
  })

  it('navigates from the jobs list to a job detail view', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input) => {
      const url = String(input)

      if (url.endsWith('/auth/login')) {
        return createJsonResponse({
          accessToken: 'access-token',
          refreshToken: 'refresh-token',
        })
      }

      return createJsonResponse([
        {
          id: 'job-1',
          customerName: 'Alice',
          siteAddress: '1 Scaffold Street',
          status: 'PENDING',
          paid: false,
        },
      ])
    })

    global.fetch = fetchMock

    render(<App />)
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'owner@example.com' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password-123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }))

    const detailsLink = await screen.findByRole('link', { name: 'Alice' })
    fireEvent.click(detailsLink)

    await screen.findByRole('heading', { name: 'Job details' })
    await waitFor(() => expect(window.location.hash).toBe('#/jobs/job-1'))
    expect(screen.getByText('1 Scaffold Street')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('link', { name: 'Back to jobs' }))
    await screen.findByRole('heading', { name: 'Jobs' })
    await waitFor(() => expect(window.location.hash).toBe('#/jobs'))
  })

  it('shows a fallback when the selected job id does not exist', async () => {
    window.localStorage.setItem('davey.accessToken', 'access-token')
    window.localStorage.setItem('davey.refreshToken', 'refresh-token')
    window.history.replaceState(null, '', '/#/jobs/missing-job')

    global.fetch = vi.fn<typeof fetch>().mockResolvedValue(
      createJsonResponse([
        {
          id: 'job-1',
          customerName: 'Alice',
          siteAddress: '1 Scaffold Street',
          status: 'PENDING',
          paid: false,
        },
      ]),
    )

    render(<App />)

    await screen.findByText('Job not found.')
    expect(screen.getByRole('link', { name: 'Back to jobs' })).toHaveAttribute('href', '#/jobs')
  })
})

function createJsonResponse(payload: unknown) {
  return new Response(JSON.stringify(payload), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}
