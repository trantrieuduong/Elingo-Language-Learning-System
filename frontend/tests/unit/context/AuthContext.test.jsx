import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { server } from '../../mocks/server'
import { AuthProvider, useAuth } from '@/context/AuthContext'

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  describe('login', () => {
    it('should login successfully and update user state', async () => {
      server.use(
        http.post('*/auth/refresh', () => {
          return HttpResponse.json({ success: false }, { status: 401 })
        }),
        http.post('*/auth/login', () => {
          return HttpResponse.json({
            success: true,
            data: { accessToken: 'valid-login-token' },
          })
        }),
        http.get('*/users/me', () => {
          return HttpResponse.json({
            success: true,
            data: { id: 1, username: 'testuser', email: 'testuser@example.com', role: 'USER' },
          })
        })
      )

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })

      await waitFor(() => {
        expect(result.current.loading).toBe(false)
      })

      let loginRes
      await act(async () => {
        loginRes = await result.current.login('testuser', 'Password123@')
      })

      expect(loginRes.success).toBe(true)
      expect(result.current.user).toMatchObject({ id: 1, username: 'testuser' })
      expect(localStorage.getItem('accessToken')).toBe('valid-login-token')
    })

    it('should handle login failure and clear auth state', async () => {
      server.use(
        http.post('*/auth/refresh', () => {
          return HttpResponse.json({ success: false }, { status: 401 })
        }),
        http.post('*/auth/login', () => {
          return HttpResponse.json({ success: false, message: 'Invalid credentials' }, { status: 401 })
        })
      )

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })

      await waitFor(() => {
        expect(result.current.loading).toBe(false)
      })

      let loginRes
      await act(async () => {
        loginRes = await result.current.login('wronguser', 'wrongpass')
      })

      expect(loginRes.success).toBe(false)
      expect(result.current.user).toBeNull()
      expect(localStorage.getItem('accessToken')).toBeNull()
    })
  })

  describe('signup & verifyAccount', () => {
    it('should execute signup workflow', async () => {
      server.use(
        http.post('*/auth/refresh', () => {
          return HttpResponse.json({ success: false }, { status: 401 })
        }),
        http.post('*/auth/signup', () => {
          return HttpResponse.json({ success: true, message: 'Đăng ký thành công' })
        })
      )

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
      await waitFor(() => expect(result.current.loading).toBe(false))

      let res
      await act(async () => {
        res = await result.current.signup({
          username: 'newuser',
          email: 'newuser@example.com',
          fullName: 'New User',
          password: 'Password123@',
        })
      })

      expect(res.success).toBe(true)
    })

    it('should verify account with OTP', async () => {
      server.use(
        http.post('*/auth/refresh', () => HttpResponse.json({ success: false }, { status: 401 })),
        http.post('*/auth/account-verifications', () => {
          return HttpResponse.json({ success: true, message: 'Xác thực tài khoản thành công' })
        })
      )

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
      await waitFor(() => expect(result.current.loading).toBe(false))

      let res
      await act(async () => {
        res = await result.current.verifyAccount('testuser@example.com', '123456')
      })

      expect(res.success).toBe(true)
    })
  })

  describe('logout & updateUser', () => {
    it('should logout and reset user state', async () => {
      server.use(
        http.post('*/auth/refresh', () => HttpResponse.json({ success: false }, { status: 401 })),
        http.post('*/auth/logout', () => HttpResponse.json({ success: true }))
      )

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
      await waitFor(() => expect(result.current.loading).toBe(false))

      await act(async () => {
        await result.current.logout()
      })

      expect(result.current.user).toBeNull()
      expect(result.current.accessToken).toBeNull()
    })

    it('should update user fields locally', async () => {
      server.use(
        http.post('*/auth/refresh', () => {
          return HttpResponse.json({
            success: true,
            data: { accessToken: 'valid-token' },
          })
        }),
        http.get('*/users/me', () => {
          return HttpResponse.json({
            success: true,
            data: { id: 1, fullName: 'Old Name' },
          })
        })
      )

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
      await waitFor(() => expect(result.current.loading).toBe(false))

      act(() => {
        result.current.updateUser({ fullName: 'New Name' })
      })

      expect(result.current.user.fullName).toBe('New Name')
      expect(JSON.parse(localStorage.getItem('user')).fullName).toBe('New Name')
    })
  })

  describe('loginWithGoogle, resetPassword, sendResetPasswordOtp', () => {
    it('should login with Google', async () => {
      server.use(
        http.post('*/auth/refresh', () => HttpResponse.json({ success: false }, { status: 401 })),
        http.post('*/auth/google', () => HttpResponse.json({ success: true, data: { accessToken: 'google-token' } })),
        http.get('*/users/me', () => HttpResponse.json({ success: true, data: { id: 2, username: 'googleuser' } }))
      )

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
      await waitFor(() => expect(result.current.loading).toBe(false))

      let res
      await act(async () => {
        res = await result.current.loginWithGoogle('token123')
      })

      expect(res.success).toBe(true)
      expect(result.current.user.username).toBe('googleuser')
    })

    it('should send reset password OTP and reset password', async () => {
      server.use(
        http.post('*/auth/refresh', () => HttpResponse.json({ success: false }, { status: 401 })),
        http.post('*/auth/password-reset/otp', () => HttpResponse.json({ success: true, message: 'OTP sent' })),
        http.post('*/auth/password-reset', () => HttpResponse.json({ success: true, message: 'Reset ok' }))
      )

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
      await waitFor(() => expect(result.current.loading).toBe(false))

      let sendRes, resetRes
      await act(async () => {
        sendRes = await result.current.sendResetPasswordOtp('test@example.com')
        resetRes = await result.current.resetPassword('test@example.com', '123456', 'NewPass123@')
      })

      expect(sendRes.success).toBe(true)
      expect(resetRes.success).toBe(true)
    })

    it('should handle resendVerificationOtp and failure cases for reset password', async () => {
      server.use(
        http.post('*/auth/refresh', () => HttpResponse.json({ success: false }, { status: 401 })),
        http.post('*/auth/account-verifications/otp', () => HttpResponse.json({ success: true, message: 'Resent' })),
        http.post('*/auth/password-reset/otp', () => HttpResponse.json({ success: false, message: 'Failed OTP' }, { status: 400 })),
        http.post('*/auth/password-reset', () => HttpResponse.json({ success: false, message: 'Failed Reset' }, { status: 400 }))
      )

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
      await waitFor(() => expect(result.current.loading).toBe(false))

      let resendRes, sendFailRes, resetFailRes
      await act(async () => {
        resendRes = await result.current.resendVerificationOtp('test@example.com')
        sendFailRes = await result.current.sendResetPasswordOtp('fail@example.com')
        resetFailRes = await result.current.resetPassword('fail@example.com', '000', '123')
      })

      expect(resendRes.success).toBe(true)
      expect(sendFailRes.success).toBe(false)
      expect(resetFailRes.success).toBe(false)
    })

    it('should handle window auth:logout event', async () => {
      server.use(
        http.post('*/auth/refresh', () => HttpResponse.json({ success: false }, { status: 401 }))
      )

      localStorage.setItem('user', JSON.stringify({ id: 1, name: 'Stored' }))
      localStorage.setItem('accessToken', 'stored-token')

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
      await waitFor(() => expect(result.current.loading).toBe(false))

      act(() => {
        window.dispatchEvent(new Event('auth:logout'))
      })

      expect(result.current.user).toBeNull()
      expect(result.current.accessToken).toBeNull()
    })

    it('should initialize auth with valid session refresh on mount', async () => {
      server.use(
        http.post('*/auth/refresh', () => HttpResponse.json({ success: true, data: { accessToken: 'init-token' } })),
        http.get('*/users/me', () => HttpResponse.json({ success: true, data: { id: 99, username: 'inituser' } }))
      )

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
      await waitFor(() => expect(result.current.loading).toBe(false))

      expect(result.current.user).toMatchObject({ id: 99, username: 'inituser' })
      expect(result.current.accessToken).toBe('init-token')
    })

    it('should handle non-success responses and errors for login, signup, verifyAccount', async () => {
      server.use(
        http.post('*/auth/refresh', () => HttpResponse.json({ success: false }, { status: 401 })),
        http.post('*/auth/login', () => HttpResponse.json({ success: false, message: '' })),
        http.post('*/auth/google', () => HttpResponse.json({ success: false, message: '' })),
        http.post('*/auth/signup', () => HttpResponse.json({ success: false, message: '' })),
        http.post('*/auth/account-verifications', () => HttpResponse.json({ success: false, message: '' })),
        http.post('*/auth/account-verifications/otp', () => HttpResponse.json({ success: false, message: '' })),
        http.post('*/auth/password-reset/otp', () => HttpResponse.json({ success: false, message: '' })),
        http.post('*/auth/password-reset', () => HttpResponse.json({ success: false, message: '' }))
      )

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
      await waitFor(() => expect(result.current.loading).toBe(false))

      let loginRes, googleRes, signupRes, verifyRes, resendRes, sendOtpRes, resetRes
      await act(async () => {
        loginRes = await result.current.login('u', 'p')
        googleRes = await result.current.loginWithGoogle('gt')
        signupRes = await result.current.signup({})
        verifyRes = await result.current.verifyAccount('e', 'o')
        resendRes = await result.current.resendVerificationOtp('e')
        sendOtpRes = await result.current.sendResetPasswordOtp('e')
        resetRes = await result.current.resetPassword('e', 'o', 'p')
      })

      expect(loginRes.success).toBe(false)
      expect(googleRes.success).toBe(false)
      expect(signupRes.success).toBe(false)
      expect(verifyRes.success).toBe(false)
      expect(resendRes.success).toBe(false)
      expect(sendOtpRes.success).toBe(false)
      expect(resetRes.success).toBe(false)
    })

    it('should throw error when useAuth is used outside AuthProvider', () => {
      expect(() => renderHook(() => useAuth())).toThrow('useAuth phai duoc su dung trong AuthProvider')
    })
  })
})
