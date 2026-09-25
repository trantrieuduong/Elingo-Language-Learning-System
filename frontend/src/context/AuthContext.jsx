import { createContext, useContext, useEffect, useState } from 'react'
import {
  getMeApi,
  loginApi,
  loginWithGoogleApi,
  logoutApi,
  refreshTokenApi,
  resendVerificationOtpApi,
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

export const AuthProvider = ({ children }) => {
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
      return { success: false, message: response.message || 'Đăng nhập không thành công' }
    } catch (error) {
      clearAuthState()
      const message = error.response?.data?.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.'
      const code = error.response?.data?.code
      return {
        success: false,
        message,
        notVerified: code === 'ACCOUNT_NOT_VERIFIED' || code === 'USER_NOT_VERIFIED',
      }
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
      return { success: false, message: response.message || 'Đăng nhập Google không thành công.' }
    } catch (error) {
      clearAuthState()
      const message = error.response?.data?.errors?.[0]?.message
        || error.response?.data?.message
        || 'Không thể xác thực tài khoản Google. Vui lòng thử lại.'
      return { success: false, message }
    }
  }

  const signup = async (form) => {
    try {
      const response = await signupApi(form)
      if (response.success) {
        return { success: true, message: response.message || 'Đăng ký thành công' }
      }
      return { success: false, message: response.message || 'Đăng ký thất bại' }
    } catch (error) {
      const message = error.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại sau.'
      const errors = error.response?.data?.errors || null
      return { success: false, message, errors }
    }
  }

  const verifyAccount = async (email, otp) => {
    try {
      const response = await verifyAccountApi(email, otp)
      if (response.success) {
        return { success: true, message: response.message || 'Xác thực tài khoản thành công' }
      }
      return { success: false, message: response.message || 'Mã xác thực không hợp lệ' }
    } catch (error) {
      const message = error.response?.data?.message || 'Mã xác thực không hợp lệ hoặc đã hết hạn.'
      return { success: false, message }
    }
  }

  const resendVerificationOtp = async (email) => {
    try {
      const response = await resendVerificationOtpApi(email)
      if (response.success) {
        return { success: true, message: response.message || 'Đã gửi lại mã xác thực' }
      }
      return { success: false, message: response.message || 'Gửi lại mã thất bại' }
    } catch (error) {
      const message = error.response?.data?.message || 'Gửi lại mã thất bại. Vui lòng thử lại sau.'
      return { success: false, message }
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
