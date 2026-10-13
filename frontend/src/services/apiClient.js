import axios from 'axios'
import i18n from '../i18n'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
})

let isRefreshing = false
let failedQueue = []

const processQueue = (error, token = null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error)
    } else {
      promise.resolve(token)
    }
  })
  failedQueue = []
}

/**
 * Dịch API response message dựa theo code và params.
 * Guard an toàn khi data là chuỗi HTML (502/504 Nginx) hoặc Blob (file download).
 */
const mapApiResponse = (data, isError = false) => {
  if (!data || typeof data !== 'object' || data instanceof Blob) {
    return data
  }

  const params = data.params || {}

  if (isError) {
    // Backend trả errors array, cần promote lỗi đầu tiên lên root level
    if (Array.isArray(data.errors) && data.errors.length > 0) {
      const firstError = data.errors[0]

      // Nếu chưa có code/message ở root, lấy từ error đầu tiên
      if (!data.code && firstError.code) {
        data.code = firstError.code
      }
      if (!data.message && firstError.message) {
        data.message = firstError.message
      }

      // Dịch từng lỗi validation (có field) hoặc lỗi business (không có field)
      data.errors = data.errors.map((err) => {
        if (err && typeof err === 'object' && err.code) {
          let translatedMessage = err.message

          // Validation error (có field): validation.{field}.{code}
          if (err.field) {
            const fieldKey = `validation.${err.field}.${err.code}`
            const fieldParams = err.params || {}
            if (i18n.exists(fieldKey, { ns: 'api' })) {
              translatedMessage = i18n.t(fieldKey, { ...fieldParams, ns: 'api' })
            }
          } else {
            // Business error (không có field): error.{code}
            const errorKey = `error.${err.code}`
            if (i18n.exists(errorKey, { ns: 'api' })) {
              translatedMessage = i18n.t(errorKey, { ...params, ns: 'api' })
            }
          }

          return {
            ...err,
            message: translatedMessage || i18n.t('common.UNKNOWN_ERROR', { ns: 'api' }),
          }
        }
        return err
      })
    }

    // Dịch message ở root level theo code
    if (data.code) {
      const key = `error.${data.code}`
      if (i18n.exists(key, { ns: 'api' })) {
        data.message = i18n.t(key, { ...params, ns: 'api' })
      } else if (!data.message) {
        data.message = i18n.t('common.UNKNOWN_ERROR', { ns: 'api' })
      }
    } else if (!data.message) {
      data.message = i18n.t('common.UNKNOWN_ERROR', { ns: 'api' })
    }
  } else {
    // 3. Thông báo thành công theo code
    if (data.code) {
      const key = `success.${data.code}`
      if (i18n.exists(key, { ns: 'api' })) {
        data.message = i18n.t(key, { ...params, ns: 'api', defaultValue: data.message })
      }
    }
  }

  return data
}

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

apiClient.interceptors.response.use(
  (response) => {
    if (response.data) {
      response.data = mapApiResponse(response.data, false)
    }
    return response
  },
  async (error) => {
    const originalRequest = error.config

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/refresh')
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            return apiClient(originalRequest)
          })
          .catch((err) => Promise.reject(err))
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const response = await axios.post(
          `${import.meta.env.VITE_API_URL}/auth/refresh`,
          {},
          { withCredentials: true }
        )

        const newAccessToken = response.data?.data?.accessToken
        if (newAccessToken) {
          localStorage.setItem('accessToken', newAccessToken)
          apiClient.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`

          processQueue(null, newAccessToken)
          return apiClient(originalRequest)
        }
      } catch (refreshError) {
        processQueue(refreshError, null)
        handleForceLogout()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // Map thông báo lỗi qua i18n khi response.data là object hợp lệ
    if (error.response && error.response.data) {
      if (
        typeof error.response.data === 'object' &&
        !(error.response.data instanceof Blob)
      ) {
        error.response.data = mapApiResponse(error.response.data, true)
      } else {
        // Nginx/Gateway trả HTML string hoặc Blob — gán thông báo chung
        error.message = i18n.t('api:common.UNKNOWN_ERROR')
      }
    } else {
      error.message = i18n.t('api:common.UNKNOWN_ERROR')
    }

    return Promise.reject(error)
  }
)

function handleForceLogout() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('user')
  window.dispatchEvent(new Event('auth:logout'))

  const publicPaths = ['/', '/login', '/signup', '/account-verification']
  if (!publicPaths.includes(window.location.pathname)) {
    window.history.pushState({}, '', '/login')
    window.dispatchEvent(new PopStateEvent('popstate'))
  }
}

export default apiClient
