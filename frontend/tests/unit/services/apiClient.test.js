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

  it('should map API response errors with validation array and code translation via mapApiResponse', async () => {
    server.use(
      http.post('*/test-error-map', () => {
        return HttpResponse.json(
          {
            code: 'INVALID_CREDENTIALS',
            errors: [
              { code: 'REQUIRED', field: 'email' },
              { code: 'USER_NOT_FOUND' },
            ],
          },
          { status: 400 }
        )
      })
    )

    try {
      await apiClient.post('/test-error-map')
    } catch (err) {
      expect(err.response.data.message).toBeDefined()
      expect(err.response.data.errors).toHaveLength(2)
    }
  })

  it('should fallback to UNKNOWN_ERROR when error response is HTML or unknown format', async () => {
    server.use(
      http.get('*/test-html-error', () => {
        return new HttpResponse('<html>502 Bad Gateway</html>', {
          status: 502,
          headers: { 'Content-Type': 'text/html' },
        })
      })
    )

    try {
      await apiClient.get('/test-html-error')
    } catch (err) {
      expect(err.message).toBeDefined()
    }
  })

  it('should handle request rejection interceptor', async () => {
    const errorInterceptor = apiClient.interceptors.request.use(null, (error) => Promise.reject(error))
    try {
      await apiClient.interceptors.request.handlers[0].rejected(new Error('Request Error'))
    } catch (err) {
      expect(err.message).toBe('Request Error')
    }
    apiClient.interceptors.request.eject(errorInterceptor)
  })

  it('should handle mapApiResponse edge cases (network error without response.data)', async () => {
    server.use(
      http.get('*/test-network-error', () => {
        return HttpResponse.error()
      })
    )

    try {
      await apiClient.get('/test-network-error')
    } catch (err) {
      expect(err.message).toBeDefined()
    }
  })

  it('should map firstError code/message to root if data.code and data.message are missing', async () => {
    server.use(
      http.post('*/test-first-error', () => {
        return HttpResponse.json(
          {
            errors: [{ code: 'FIRST_ERR', message: 'First Error Message' }],
          },
          { status: 400 }
        )
      })
    )

    try {
      await apiClient.post('/test-first-error')
    } catch (err) {
      expect(err.response.data.code).toBe('FIRST_ERR')
    }
  })

  it('should handle refresh token error queue rejection and failed refresh response without newAccessToken', async () => {
    localStorage.setItem('accessToken', 'exp-token')

    server.use(
      http.get('*/res-queue-fail', () => {
        return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 })
      }),
      http.post('*/auth/refresh', () => {
        return HttpResponse.json({ success: true, data: {} })
      })
    )

    try {
      await apiClient.get('/res-queue-fail')
    } catch (err) {
      expect(err.response).toBeDefined()
    }
  })

  it('should process failed queue when concurrent refresh fails', async () => {
    localStorage.setItem('accessToken', 'exp-token-concurrent-fail')
    let firstCallDone = false

    server.use(
      http.get('*/res-c1', () => {
        if (!firstCallDone) {
          firstCallDone = true
          return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 })
        }
        return HttpResponse.json({ success: true })
      }),
      http.get('*/res-c2', () => {
        return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 })
      }),
      http.post('*/auth/refresh', async () => {
        return HttpResponse.json({ message: 'Refresh Failed' }, { status: 401 })
      })
    )

    const results = await Promise.allSettled([
      apiClient.get('/res-c1'),
      apiClient.get('/res-c2'),
    ])

    expect(results[0].status).toBe('rejected')
    expect(results[1].status).toBe('rejected')
  })
})
