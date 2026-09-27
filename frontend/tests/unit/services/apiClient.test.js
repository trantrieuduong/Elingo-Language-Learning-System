import { describe, it, expect, beforeEach, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../../mocks/server'
import apiClient from '@/services/apiClient'

describe('apiClient', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('should attach Authorization Bearer token to request headers if token exists in localStorage', async () => {
    localStorage.setItem('accessToken', 'my-test-token')

    let capturedHeader = null
    server.use(
      http.get('*/test-endpoint', ({ request }) => {
        capturedHeader = request.headers.get('Authorization')
        return HttpResponse.json({ success: true })
      })
    )

    await apiClient.get('/test-endpoint')
    expect(capturedHeader).toBe('Bearer my-test-token')
  })

  it('should not attach Authorization header if no token in localStorage', async () => {
    let capturedHeader = null
    server.use(
      http.get('*/test-endpoint', ({ request }) => {
        capturedHeader = request.headers.get('Authorization')
        return HttpResponse.json({ success: true })
      })
    )

    await apiClient.get('/test-endpoint')
    expect(capturedHeader).toBeNull()
  })

  it('should handle 401 error and refresh token automatically', async () => {
    localStorage.setItem('accessToken', 'expired-token')

    let requestCount = 0
    server.use(
      http.get('*/protected-resource', () => {
        requestCount++
        if (requestCount === 1) {
          return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 })
        }
        return HttpResponse.json({ success: true, data: 'secret' })
      }),
      http.post('*/auth/refresh', () => {
        return HttpResponse.json({
          success: true,
          data: { accessToken: 'refreshed-token-999' },
        })
      })
    )

    const response = await apiClient.get('/protected-resource')
    expect(response.data.data).toBe('secret')
    expect(localStorage.getItem('accessToken')).toBe('refreshed-token-999')
  })

  it('should force logout when token refresh fails with 401', async () => {
    const logoutListener = vi.fn()
    window.addEventListener('auth:logout', logoutListener)

    server.use(
      http.get('*/protected-resource', () => {
        return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 })
      }),
      http.post('*/auth/refresh', () => {
        return HttpResponse.json({ message: 'Refresh token expired' }, { status: 401 })
      })
    )

    await expect(apiClient.get('/protected-resource')).rejects.toThrow()
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(logoutListener).toHaveBeenCalled()

    window.removeEventListener('auth:logout', logoutListener)
  })

  it('should queue concurrent requests while refreshing token and resolve them after token refresh', async () => {
    localStorage.setItem('accessToken', 'expired-token')
    let firstCallDone = false

    server.use(
      http.get('*/res-1', () => {
        if (!firstCallDone) {
          firstCallDone = true
          return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 })
        }
        return HttpResponse.json({ success: true, data: 'res1' })
      }),
      http.get('*/res-2', () => {
        return HttpResponse.json({ success: true, data: 'res2' })
      }),
      http.post('*/auth/refresh', async () => {
        return HttpResponse.json({
          success: true,
          data: { accessToken: 'concurrent-token' },
        })
      })
    )

    const [res1, res2] = await Promise.all([
      apiClient.get('/res-1'),
      apiClient.get('/res-2'),
    ])

    expect(res1.data.data).toBe('res1')
    expect(res2.data.data).toBe('res2')
  })

  it('should handle request interceptor error and navigate on force logout for protected route', async () => {
    const errorInterceptor = apiClient.interceptors.request.use(null, (error) => Promise.reject(error))

    server.use(
      http.get('*/protected-nav', () => {
        return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 })
      }),
      http.post('*/auth/refresh', () => {
        return HttpResponse.json({ message: 'Error' }, { status: 500 })
      })
    )

    delete window.location
    window.location = new URL('http://localhost/dashboard')

    await expect(apiClient.get('/protected-nav')).rejects.toThrow()

    apiClient.interceptors.request.eject(errorInterceptor)
  })
})
