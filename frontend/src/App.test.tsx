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

  it('deletes a job after confirmation', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/auth/login')) {
        return createJsonResponse({
          accessToken: 'access-token',
          refreshToken: 'refresh-token',
        })
      }

      if (url.endsWith('/jobs/job-1') && init?.method === 'DELETE') {
        return new Response(null, { status: 204 })
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

    await screen.findByText('Alice')
    fireEvent.click(screen.getByRole('button', { name: 'Delete job for Alice' }))

    await waitFor(() => expect(screen.queryByText('Alice')).not.toBeInTheDocument())
    expect(confirmSpy).toHaveBeenCalledWith('Are you sure you want to delete this job?')
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('shows an error when deleting a job fails', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/auth/login')) {
        return createJsonResponse({
          accessToken: 'access-token',
          refreshToken: 'refresh-token',
        })
      }

      if (url.endsWith('/jobs/job-1') && init?.method === 'DELETE') {
        return new Response(null, { status: 500 })
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

    await screen.findByText('Alice')
    fireEvent.click(screen.getByRole('button', { name: 'Delete job for Alice' }))

    expect(await screen.findByText('Unable to delete job.')).toBeInTheDocument()
    expect(screen.getByText('Alice')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('returns to login when deleting a job returns 401', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/auth/login')) {
        return createJsonResponse({
          accessToken: 'access-token',
          refreshToken: 'refresh-token',
        })
      }

      if (url.endsWith('/jobs/job-1') && init?.method === 'DELETE') {
        return new Response(null, { status: 401 })
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

    await screen.findByText('Alice')
    fireEvent.click(screen.getByRole('button', { name: 'Delete job for Alice' }))

    await screen.findByRole('heading', { name: 'Log in' })
    expect(screen.getByText('Your session has expired. Please log in again.')).toBeInTheDocument()
    expect(window.localStorage.getItem('davey.accessToken')).toBeNull()
    expect(window.localStorage.getItem('davey.refreshToken')).toBeNull()
  })
})

function createJsonResponse(payload: unknown) {
  return new Response(JSON.stringify(payload), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}
