import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AccountVerificationPage from '@/modules/auth/pages/AccountVerificationPage'
import { useAuth } from '@/context/AuthContext'
import { MOCK_AUTH_CONTEXT_DEFAULT } from '../../../utils/mockData'

vi.mock('@/context/AuthContext')

describe('AccountVerificationPage', () => {
  const mockVerifyAccount = vi.fn()
  const mockResendVerificationOtp = vi.fn()
  const mockNavigate = vi.fn()

  beforeEach(() => {
    useAuth.mockReturnValue({
      ...MOCK_AUTH_CONTEXT_DEFAULT,
      verifyAccount: mockVerifyAccount,
      resendVerificationOtp: mockResendVerificationOtp,
    })
  })

  describe('Rendering', () => {
    it('should render OTP input and buttons', () => {
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      expect(screen.getByLabelText(/mã xác thực/i)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /xác thực tài khoản/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /gửi lại mã xác thực/i })).toBeInTheDocument()
    })
  })

  describe('Form submission', () => {
    it('should show error if OTP is empty', async () => {
      const user = userEvent.setup()
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: /xác thực tài khoản/i }))
      expect(screen.getByText(/mã otp là bắt buộc/i)).toBeInTheDocument()
    })

    it('should submit OTP successfully and redirect to login', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })

      mockVerifyAccount.mockResolvedValue({ success: true })
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/mã xác thực/i), '123456')
      await user.click(screen.getByRole('button', { name: /xác thực tài khoản/i }))

      expect(mockVerifyAccount).toHaveBeenCalledWith('test@example.com', '123456')
      expect(screen.getByText(/tài khoản đã được xác thực/i)).toBeInTheDocument()

      vi.advanceTimersByTime(1000)
      expect(mockNavigate).toHaveBeenCalledWith('/login')

      vi.useRealTimers()
    })

    it('should show error message when OTP is invalid', async () => {
      const user = userEvent.setup()
      mockVerifyAccount.mockResolvedValue({ success: false, message: 'Mã OTP không đúng' })
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/mã xác thực/i), '000000')
      await user.click(screen.getByRole('button', { name: /xác thực tài khoản/i }))

      await waitFor(() => {
        expect(screen.getByText(/mã otp không đúng/i)).toBeInTheDocument()
      })
    })
  })

  describe('Resend OTP', () => {
    it('should call resendVerificationOtp when resend button is clicked', async () => {
      const user = userEvent.setup()
      mockResendVerificationOtp.mockResolvedValue({ success: true })
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: /gửi lại mã xác thực/i }))

      await waitFor(() => {
        expect(mockResendVerificationOtp).toHaveBeenCalledWith('test@example.com')
        expect(screen.getByText(/mã xác thực mới đã được gửi/i)).toBeInTheDocument()
      })
    })
  })
})
