import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ResetPasswordPage from '@/modules/auth/pages/ResetPasswordPage'
import { useAuth } from '@/context/AuthContext'
import { MOCK_AUTH_CONTEXT_DEFAULT } from '../../../utils/mockData'
import { LANGS, tr, trRx, useLang } from '../../../utils/i18n'

vi.mock('@/context/AuthContext')

describe('ResetPasswordPage', () => {
  const mockSendResetPasswordOtp = vi.fn()
  const mockResetPassword = vi.fn()
  const mockNavigate = vi.fn()

  beforeEach(() => {
    useAuth.mockReturnValue({
      ...MOCK_AUTH_CONTEXT_DEFAULT,
      sendResetPasswordOtp: mockSendResetPasswordOtp,
      resetPassword: mockResetPassword,
    })
  })

  describe.each(LANGS)('Rendering [%s]', (lng) => {
    beforeEach(() => useLang(lng))

    it('should render reset password fields', () => {
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      expect(screen.getByLabelText(tr('auth:resetEmailLabel'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('auth:resetOtpLabel'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('auth:resetNewPasswordLabel'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('auth:resetConfirmPasswordLabel'))).toBeInTheDocument()
      expect(screen.getByRole('button', { name: trRx('auth:submitReset') })).toBeInTheDocument()
    })
  })

  describe.each(LANGS)('Send OTP flow [%s]', (lng) => {
    beforeEach(() => useLang(lng))

    it('should show error if email is empty on send OTP', async () => {
      const user = userEvent.setup()
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: trRx('auth:sendOtp') }))

      expect(screen.getByText(tr('auth:emailRequired'))).toBeInTheDocument()
    })

    it('should show error when email format is invalid on send OTP', async () => {
      const user = userEvent.setup()
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:resetEmailLabel')), 'invalid-email')
      await user.click(screen.getByRole('button', { name: trRx('auth:sendOtp') }))

      expect(screen.getByText(tr('auth:emailInvalid'))).toBeInTheDocument()
    })
  })

  describe('Reset Password flow', () => {
    it('should send reset password OTP successfully', async () => {
      const user = userEvent.setup()
      mockSendResetPasswordOtp.mockResolvedValue({ success: true, message: 'OTP sent' })
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:resetEmailLabel')), 'test@example.com')
      await user.click(screen.getByRole('button', { name: trRx('auth:sendOtp') }))

      await waitFor(() => {
        expect(mockSendResetPasswordOtp).toHaveBeenCalledWith('test@example.com')
        expect(screen.getByText('OTP sent')).toBeInTheDocument()
      })
    })

    it('should submit reset password form and redirect on success', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })

      mockResetPassword.mockResolvedValue({ success: true })
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:resetEmailLabel')), 'test@example.com')
      await user.type(screen.getByLabelText(tr('auth:resetOtpLabel')), '123456')
      await user.type(screen.getByLabelText(tr('auth:resetNewPasswordLabel')), 'NewPassword123@')
      await user.type(screen.getByLabelText(tr('auth:resetConfirmPasswordLabel')), 'NewPassword123@')

      await user.click(screen.getByRole('button', { name: trRx('auth:submitReset') }))

      await waitFor(() => {
        expect(mockResetPassword).toHaveBeenCalledWith('test@example.com', '123456', 'NewPassword123@')
      })

      vi.advanceTimersByTime(1500)
      expect(mockNavigate).toHaveBeenCalledWith('/login')

      vi.useRealTimers()
    })

    it('should show specific error code for OTP_INVALID and EMAIL_NOT_EXISTED', async () => {
      const user = userEvent.setup()
      mockResetPassword.mockResolvedValue({ success: false, code: 'OTP_INVALID', message: 'Invalid OTP' })
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:resetEmailLabel')), 'test@example.com')
      await user.type(screen.getByLabelText(tr('auth:resetOtpLabel')), '000000')
      await user.type(screen.getByLabelText(tr('auth:resetNewPasswordLabel')), 'NewPassword123@')
      await user.type(screen.getByLabelText(tr('auth:resetConfirmPasswordLabel')), 'NewPassword123@')

      await user.click(screen.getByRole('button', { name: trRx('auth:submitReset') }))

      await waitFor(() => {
        expect(screen.getByText('Invalid OTP')).toBeInTheDocument()
      })
    })

    it('should show field error when send OTP fails with EMAIL_NOT_EXISTED', async () => {
      const user = userEvent.setup()
      mockSendResetPasswordOtp.mockResolvedValue({
        success: false,
        code: 'EMAIL_NOT_EXISTED',
        message: 'Email does not exist in system',
      })
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:resetEmailLabel')), 'notfound@example.com')
      await user.click(screen.getByRole('button', { name: trRx('auth:sendOtp') }))

      await waitFor(() => {
        expect(screen.getByText('Email does not exist in system')).toBeInTheDocument()
      })
    })

    it('should show general error when send OTP fails with non-field error code', async () => {
      const user = userEvent.setup()
      mockSendResetPasswordOtp.mockResolvedValue({
        success: false,
        code: 'TOO_MANY_REQUESTS',
        message: 'Too many requests',
      })
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:resetEmailLabel')), 'test@example.com')
      await user.click(screen.getByRole('button', { name: trRx('auth:sendOtp') }))

      await waitFor(() => {
        expect(screen.getByText('Too many requests')).toBeInTheDocument()
      })
    })

    it('should handle reset password with backend validation errors array and field error maps', async () => {
      const user = userEvent.setup()
      mockResetPassword.mockResolvedValueOnce({
        success: false,
        errors: [{ field: 'newPassword', message: 'Password error from server' }],
      })
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:resetEmailLabel')), 'test@example.com')
      await user.type(screen.getByLabelText(tr('auth:resetOtpLabel')), '123456')
      await user.type(screen.getByLabelText(tr('auth:resetNewPasswordLabel')), 'NewPassword123@')
      await user.type(screen.getByLabelText(tr('auth:resetConfirmPasswordLabel')), 'NewPassword123@')

      await user.click(screen.getByRole('button', { name: trRx('auth:submitReset') }))

      await waitFor(() => {
        expect(screen.getByText('Password error from server')).toBeInTheDocument()
      })
    })

    it('should navigate to login page when clicking login link', async () => {
      const user = userEvent.setup()
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.click(screen.getByText(tr('auth:loginLinkText')))
      expect(mockNavigate).toHaveBeenCalledWith('/login')
    })
  })
})
