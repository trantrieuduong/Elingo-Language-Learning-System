import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ResetPasswordPage from '@/modules/auth/pages/ResetPasswordPage'
import { useAuth } from '@/context/AuthContext'
import { MOCK_AUTH_CONTEXT_DEFAULT } from '../../../utils/mockData'

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

  describe('Rendering', () => {
    it('should render reset password fields', () => {
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      expect(screen.getByLabelText(/email đã đăng ký/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/mã otp/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/^mật khẩu mới$/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/xác nhận mật khẩu mới/i)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /xác nhận đặt lại mật khẩu/i })).toBeInTheDocument()
    })
  })

  describe('Send OTP flow', () => {
    it('should show error if email is empty on send OTP', async () => {
      const user = userEvent.setup()
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: /gửi mã otp/i }))

      expect(screen.getByText(/email là bắt buộc/i)).toBeInTheDocument()
    })

    it('should send reset password OTP successfully', async () => {
      const user = userEvent.setup()
      mockSendResetPasswordOtp.mockResolvedValue({ success: true })
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/email đã đăng ký/i), 'test@example.com')
      await user.click(screen.getByRole('button', { name: /gửi mã otp/i }))

      await waitFor(() => {
        expect(mockSendResetPasswordOtp).toHaveBeenCalledWith('test@example.com')
        expect(screen.getByText(/đã gửi mã otp/i)).toBeInTheDocument()
      })
    })
  })

  describe('Reset Password flow', () => {
    it('should submit reset password form and redirect on success', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })

      mockResetPassword.mockResolvedValue({ success: true })
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/email đã đăng ký/i), 'test@example.com')
      await user.type(screen.getByLabelText(/mã otp/i), '123456')
      await user.type(screen.getByLabelText(/^mật khẩu mới$/i), 'NewPassword123@')
      await user.type(screen.getByLabelText(/xác nhận mật khẩu mới/i), 'NewPassword123@')

      await user.click(screen.getByRole('button', { name: /xác nhận đặt lại mật khẩu/i }))

      expect(mockResetPassword).toHaveBeenCalledWith('test@example.com', '123456', 'NewPassword123@')
      expect(screen.getByText(/đặt lại mật khẩu thành công/i)).toBeInTheDocument()

      vi.advanceTimersByTime(1500)
      expect(mockNavigate).toHaveBeenCalledWith('/login')

      vi.useRealTimers()
    })

    it('should show specific error code for OTP_INVALID and EMAIL_NOT_EXISTED', async () => {
      const user = userEvent.setup()
      mockResetPassword.mockResolvedValue({ success: false, code: 'OTP_INVALID', message: 'OTP không hợp lệ' })
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/email đã đăng ký/i), 'test@example.com')
      await user.type(screen.getByLabelText(/mã otp/i), '000000')
      await user.type(screen.getByLabelText(/^mật khẩu mới$/i), 'NewPassword123@')
      await user.type(screen.getByLabelText(/xác nhận mật khẩu mới/i), 'NewPassword123@')

      await user.click(screen.getByRole('button', { name: /xác nhận đặt lại mật khẩu/i }))

      await waitFor(() => {
        expect(screen.getByText(/otp không hợp lệ/i)).toBeInTheDocument()
      })
    })

    it('should navigate to login page when clicking login link', async () => {
      const user = userEvent.setup()
      render(<ResetPasswordPage onNavigate={mockNavigate} />)

      await user.click(screen.getByText(/^đăng nhập$/i))
      expect(mockNavigate).toHaveBeenCalledWith('/login')
    })
  })
})
