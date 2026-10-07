import { createContext, useContext, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  getMeApi,
  loginApi,
  loginWithGoogleApi,
  logoutApi,
  refreshTokenApi,
  resendVerificationOtpApi,
  resetPasswordApi,
  sendResetPasswordOtpApi,
  signupApi,
  verifyAccountApi,
} from '../modules/auth/authApi'

const AuthContext = createContext(null)
let initialRefreshPromise = null

const refreshInitialSession = () => {
  if (!initialRefreshPromise) {
    initialRefreshPromise = refreshTokenApi().finally(() => {
      initialRefreshPromise = null
    })
  }

  return initialRefreshPromise
}

const extractApiError = (error, fallbackMessage) => {
  const responseData = error?.response?.data

  // Backend trả errors array, lấy code/message từ error đầu tiên hoặc root level
  const code = responseData?.code || null
  const message = responseData?.message || error?.message || fallbackMessage
  const notVerified = code === 'ACCOUNT_NOT_VERIFIED' || code === 'USER_NOT_VERIFIED'
  const errors = responseData?.errors || null

  return {
    success: false,
    message,
    code,
    errors,
    notVerified,
  }
}

export const AuthProvider = ({ children }) => {
  const { t } = useTranslation('api')
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('user')
    return savedUser ? JSON.parse(savedUser) : null
  })
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem('accessToken'))
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    initializeAuth()

    const handleGlobalLogout = () => {
      setUser(null)
      setAccessToken(null)
    }

    window.addEventListener('auth:logout', handleGlobalLogout)
    return () => window.removeEventListener('auth:logout', handleGlobalLogout)
  }, [])

  const initializeAuth = async () => {
    try {
      const response = await refreshInitialSession()
      if (response.success && response.data?.accessToken) {
        const token = response.data.accessToken
        localStorage.setItem('accessToken', token)
        setAccessToken(token)
        await loadCurrentUser()
      } else {
        clearAuthState()
      }
    } catch (error) {
      clearAuthState()
    } finally {
      setLoading(false)
    }
  }

  const loadCurrentUser = async () => {
    const response = await getMeApi()
    if (response.success && response.data) {
      localStorage.setItem('user', JSON.stringify(response.data))
      setUser(response.data)
      return response.data
    }
    return null
  }

  const clearAuthState = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('user')
    setAccessToken(null)
    setUser(null)
  }

  const login = async (identifier, password) => {
    try {
      const response = await loginApi(identifier, password)
      if (response.success && response.data?.accessToken) {
        const token = response.data.accessToken
        localStorage.setItem('accessToken', token)
        setAccessToken(token)
        const userData = await loadCurrentUser()
        return { success: true, user: userData }
      }
      return { success: false, message: response.message || t('common.UNKNOWN_ERROR') }
    } catch (error) {
      clearAuthState()
      return extractApiError(error, t('common.UNKNOWN_ERROR'))
    }
  }

  const loginWithGoogle = async (idToken) => {
    try {
      const response = await loginWithGoogleApi(idToken)
      if (response.success && response.data?.accessToken) {
        const token = response.data.accessToken
        localStorage.setItem('accessToken', token)
        setAccessToken(token)
        const userData = await loadCurrentUser()
        return { success: true, user: userData }
      }
      return { success: false, message: response.message || t('common.UNKNOWN_ERROR') }
    } catch (error) {
      clearAuthState()
      return extractApiError(error, t('common.UNKNOWN_ERROR'))
    }
  }

  const signup = async (form) => {
    try {
      const response = await signupApi(form)
      if (response.success) {
        return { success: true, message: response.message || t('success.SIGNUP_SUCCESS') }
      }
      return { success: false, message: response.message || t('common.UNKNOWN_ERROR') }
    } catch (error) {
      return extractApiError(error, t('common.UNKNOWN_ERROR'))
    }
  }

  const verifyAccount = async (email, otp) => {
    try {
      const response = await verifyAccountApi(email, otp)
      if (response.success) {
        return { success: true, message: response.message || t('success.ACCOUNT_VERIFIED') }
      }
      return { success: false, message: response.message || t('common.UNKNOWN_ERROR') }
    } catch (error) {
      return extractApiError(error, t('common.UNKNOWN_ERROR'))
    }
  }

  const resendVerificationOtp = async (email) => {
    try {
      const response = await resendVerificationOtpApi(email)
      if (response.success) {
        return { success: true, message: response.message || t('success.OTP_RESENT') }
      }
      return { success: false, message: response.message || t('common.UNKNOWN_ERROR') }
    } catch (error) {
      return extractApiError(error, t('common.UNKNOWN_ERROR'))
    }
  }

  const sendResetPasswordOtp = async (email) => {
    try {
      const response = await sendResetPasswordOtpApi(email)
      if (response.success) {
        return { success: true, message: response.message || t('success.OTP_SENT') }
      }
      return { success: false, message: response.message || t('common.UNKNOWN_ERROR') }
    } catch (error) {
      return extractApiError(error, t('common.UNKNOWN_ERROR'))
    }
  }

  const resetPassword = async (email, otp, newPassword) => {
    try {
      const response = await resetPasswordApi(email, otp, newPassword)
      if (response.success) {
        return { success: true, message: response.message || t('success.PASSWORD_RESET') }
      }
      return { success: false, message: response.message || t('common.UNKNOWN_ERROR') }
    } catch (error) {
      return extractApiError(error, t('common.UNKNOWN_ERROR'))
    }
  }

  const logout = async () => {
    try {
      await logoutApi()
    } catch (error) {
      console.error('Lỗi khi gọi API đăng xuất:', error)
    } finally {
      clearAuthState()
    }
  }

  const updateUser = (updatedFields) => {
    setUser((prev) => {
      if (!prev) return null
      const updated = { ...prev, ...updatedFields }
      localStorage.setItem('user', JSON.stringify(updated))
      return updated
    })
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        accessToken,
        loading,
        login,
        loginWithGoogle,
        signup,
        verifyAccount,
        resendVerificationOtp,
        sendResetPasswordOtp,
        resetPassword,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth phai duoc su dung trong AuthProvider')
  }
  return context
}
