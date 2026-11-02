import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../../../mocks/server'
import {
  loginApi,
  loginWithGoogleApi,
  signupApi,
  verifyAccountApi,
  resendVerificationOtpApi,
  refreshTokenApi,
  logoutApi,
  sendResetPasswordOtpApi,
  resetPasswordApi,
  getMeApi,
} from '@/modules/auth/authApi'

describe('authApi', () => {
  describe('loginApi', () => {
    it('should login successfully with valid credentials', async () => {
      const result = await loginApi('testuser', 'Password123@')
      expect(result.success).toBe(true)
      expect(result.data.accessToken).toBe('mock-access-token-12345')
    })

    it('should fail login with invalid credentials', async () => {
      server.use(
        http.post('*/auth/login', () => {
          return HttpResponse.json(
            { success: false, message: 'Sai tài khoản hoặc mật khẩu' },
            { status: 401 }
          )
        })
      )

      await expect(loginApi('wrong', 'wrong')).rejects.toThrow()
    })
  })

  describe('loginWithGoogleApi', () => {
    it('should login with Google idToken successfully', async () => {
      server.use(
        http.post('*/auth/google', () => {
          return HttpResponse.json({
            success: true,
            data: { accessToken: 'google-access-token' },
          })
        })
      )

      const result = await loginWithGoogleApi('fake-id-token')
      expect(result.success).toBe(true)
      expect(result.data.accessToken).toBe('google-access-token')
    })

    it('should handle Google login failure', async () => {
      server.use(
        http.post('*/auth/google', () => {
          return HttpResponse.json({ success: false, message: 'Google token invalid' }, { status: 400 })
        })
      )

      await expect(loginWithGoogleApi('invalid-token')).rejects.toThrow()
    })
  })

  describe('signupApi', () => {
    it('should signup successfully', async () => {
      const signupData = {
        username: 'newuser',
        email: 'newuser@example.com',
        fullName: 'New User',
        password: 'Password123@',
      }
      const result = await signupApi(signupData)
      expect(result.success).toBe(true)
      expect(result.message).toBe('Đăng ký thành công')
    })

    it('should fail signup on validation/conflict error', async () => {
      server.use(
        http.post('*/auth/signup', () => {
          return HttpResponse.json({ success: false, message: 'Email đã tồn tại' }, { status: 409 })
        })
      )

      await expect(
        signupApi({ username: 'user', email: 'exist@example.com', fullName: 'User', password: '123' })
      ).rejects.toThrow()
    })
  })

  describe('verifyAccountApi', () => {
    it('should verify account successfully with correct OTP', async () => {
      server.use(
        http.post('*/auth/account-verifications', () => {
          return HttpResponse.json({ success: true, message: 'Xác thực thành công' })
        })
      )

      const result = await verifyAccountApi('test@example.com', '123456')
      expect(result.success).toBe(true)
    })

    it('should fail verification with invalid OTP', async () => {
      server.use(
        http.post('*/auth/account-verifications', () => {
          return HttpResponse.json({ success: false, message: 'OTP sai' }, { status: 400 })
        })
      )

      await expect(verifyAccountApi('test@example.com', '000000')).rejects.toThrow()
    })
  })

  describe('resendVerificationOtpApi', () => {
    it('should resend OTP successfully', async () => {
      server.use(
        http.post('*/auth/account-verifications/otp', () => {
          return HttpResponse.json({ success: true, message: 'Gửi lại OTP thành công' })
        })
      )

      const result = await resendVerificationOtpApi('test@example.com')
      expect(result.success).toBe(true)
    })

    it('should handle error when resending OTP', async () => {
      server.use(
        http.post('*/auth/account-verifications/otp', () => {
          return HttpResponse.json({ success: false, message: 'Lỗi gửi mail' }, { status: 500 })
        })
      )

      await expect(resendVerificationOtpApi('test@example.com')).rejects.toThrow()
    })
  })

  describe('refreshTokenApi', () => {
    it('should refresh token successfully', async () => {
      server.use(
        http.post('*/auth/refresh', () => {
          return HttpResponse.json({
            success: true,
            data: { accessToken: 'new-refreshed-token' },
          })
        })
      )

      const result = await refreshTokenApi()
      expect(result.success).toBe(true)
      expect(result.data.accessToken).toBe('new-refreshed-token')
    })

    it('should fail refreshing token when unauthenticated', async () => {
      server.use(
        http.post('*/auth/refresh', () => {
          return HttpResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 })
        })
      )

      await expect(refreshTokenApi()).rejects.toThrow()
    })
  })

  describe('logoutApi', () => {
    it('should logout successfully', async () => {
      server.use(
        http.post('*/auth/logout', () => {
          return HttpResponse.json({ success: true, message: 'Đăng xuất thành công' })
        })
      )

      const result = await logoutApi()
      expect(result.success).toBe(true)
    })

    it('should handle logout error', async () => {
      server.use(
        http.post('*/auth/logout', () => {
          return HttpResponse.json({ success: false, message: 'Server error' }, { status: 500 })
        })
      )

      await expect(logoutApi()).rejects.toThrow()
    })
  })

  describe('sendResetPasswordOtpApi', () => {
    it('should send reset password OTP successfully', async () => {
      server.use(
        http.post('*/auth/password-reset/otp', () => {
          return HttpResponse.json({ success: true, message: 'Đã gửi mã reset password' })
        })
      )

      const result = await sendResetPasswordOtpApi('test@example.com')
      expect(result.success).toBe(true)
    })

    it('should handle error when sending reset password OTP', async () => {
      server.use(
        http.post('*/auth/password-reset/otp', () => {
          return HttpResponse.json({ success: false, message: 'Email không tồn tại' }, { status: 404 })
        })
      )

      await expect(sendResetPasswordOtpApi('notfound@example.com')).rejects.toThrow()
    })
  })

  describe('resetPasswordApi', () => {
    it('should reset password successfully', async () => {
      server.use(
        http.post('*/auth/password-reset', () => {
          return HttpResponse.json({ success: true, message: 'Đổi mật khẩu thành công' })
        })
      )

      const result = await resetPasswordApi('test@example.com', '123456', 'NewPass123@')
      expect(result.success).toBe(true)
    })

    it('should handle invalid OTP or reset password error', async () => {
      server.use(
        http.post('*/auth/password-reset', () => {
          return HttpResponse.json({ success: false, message: 'Mã OTP không hợp lệ' }, { status: 400 })
        })
      )

      await expect(resetPasswordApi('test@example.com', '000000', 'NewPass123@')).rejects.toThrow()
    })
  })

  describe('getMeApi', () => {
    it('should fetch user info successfully', async () => {
      const result = await getMeApi()
      expect(result.success).toBe(true)
      expect(result.data.username).toBe('testuser')
    })

    it('should fail getting user info when not authenticated', async () => {
      server.use(
        http.get('*/users/me', () => {
          return HttpResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 })
        })
      )

      await expect(getMeApi()).rejects.toThrow()
    })
  })
})
