import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1',
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
  (response) => response,
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
          `${import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'}/auth/refresh`,
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
