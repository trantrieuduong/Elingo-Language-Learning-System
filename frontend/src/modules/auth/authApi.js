import apiClient from '../../services/apiClient'

export const loginApi = async (identifier, password) => {
  const response = await apiClient.post(
    '/auth/login',
    { username: identifier, password },
    { withCredentials: true }
  )
  return response.data
}

export const loginWithGoogleApi = async (idToken) => {
  const response = await apiClient.post('/auth/google', { idToken }, { withCredentials: true })
  return response.data
}

export const signupApi = async ({ username, email, fullName, password }) => {
  const response = await apiClient.post('/auth/signup', {
    username,
    email,
    fullName,
    password,
  })
  return response.data
}

export const verifyAccountApi = async (email, otp) => {
  const response = await apiClient.post('/auth/account-verifications', { email, otp })
  return response.data
}

export const resendVerificationOtpApi = async (email) => {
  const response = await apiClient.post('/auth/account-verifications/otp', { email })
  return response.data
}

export const refreshTokenApi = async () => {
  const response = await apiClient.post('/auth/refresh', {}, { withCredentials: true })
  return response.data
}

export const logoutApi = async () => {
  const response = await apiClient.post('/auth/logout', {}, { withCredentials: true })
  return response.data
}

export const getMeApi = async () => {
  const response = await apiClient.get('/users/me')
  return response.data
}
