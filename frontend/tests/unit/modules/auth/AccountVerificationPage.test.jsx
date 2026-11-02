import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AccountVerificationPage from '@/modules/auth/pages/AccountVerificationPage'
import { useAuth } from '@/context/AuthContext'
import { MOCK_AUTH_CONTEXT_DEFAULT } from '../../../utils/mockData'
import { LANGS, tr, trRx, useLang } from '../../../utils/i18n'

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

  describe.each(LANGS)('Rendering [%s]', (lng) => {
    beforeEach(() => useLang(lng))

    it('should render OTP input and buttons', () => {
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      expect(screen.getByLabelText(tr('auth:otpLabel'))).toBeInTheDocument()
      expect(screen.getByRole('button', { name: trRx('auth:submitVerify') })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: trRx('auth:resendCode') })).toBeInTheDocument()
    })
  })

  describe.each(LANGS)('Form validation [%s]', (lng) => {
    beforeEach(() => useLang(lng))

    it('should show error if OTP is empty', async () => {
      const user = userEvent.setup()
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: trRx('auth:submitVerify') }))
      expect(screen.getByText(tr('auth:otpRequired'))).toBeInTheDocument()
    })
  })

  describe('Form submission', () => {
    it('should submit OTP successfully and redirect to login', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })

      mockVerifyAccount.mockResolvedValue({ success: true, message: 'Account verified successfully' })
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:otpLabel')), '123456')
      await user.click(screen.getByRole('button', { name: trRx('auth:submitVerify') }))

      await waitFor(() => {
        expect(mockVerifyAccount).toHaveBeenCalledWith('test@example.com', '123456')
        expect(screen.getByText('Account verified successfully')).toBeInTheDocument()
      })

      vi.advanceTimersByTime(1000)
      expect(mockNavigate).toHaveBeenCalledWith('/login')

      vi.useRealTimers()
    })

    it('should show error message when OTP is invalid', async () => {
      const user = userEvent.setup()
      mockVerifyAccount.mockResolvedValue({ success: false, message: 'Invalid OTP' })
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:otpLabel')), '000000')
      await user.click(screen.getByRole('button', { name: trRx('auth:submitVerify') }))

      await waitFor(() => {
        expect(screen.getByText('Invalid OTP')).toBeInTheDocument()
      })
    })

    it('should disable submit button if initialEmail is missing', () => {
      render(<AccountVerificationPage email="" onNavigate={mockNavigate} />)
      expect(screen.getByRole('button', { name: trRx('auth:submitVerify') })).toBeDisabled()
    })

    it('should show general error when OTP verify fails with non-OTP_INVALID code', async () => {
      const user = userEvent.setup()
      mockVerifyAccount.mockResolvedValue({ success: false, code: 'SERVER_ERROR', message: 'Server error' })
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:otpLabel')), '123456')
      await user.click(screen.getByRole('button', { name: trRx('auth:submitVerify') }))

      await waitFor(() => {
        expect(screen.getByText('Server error')).toBeInTheDocument()
      })
    })

    it('should call resendVerificationOtp successfully', async () => {
      const user = userEvent.setup()
      mockResendVerificationOtp.mockResolvedValue({ success: true, message: 'OTP resent successfully' })
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: trRx('auth:resendCode') }))

      await waitFor(() => {
        expect(mockResendVerificationOtp).toHaveBeenCalledWith('test@example.com')
        expect(screen.getByText('OTP resent successfully')).toBeInTheDocument()
      })
    })

    it('should show error when resend OTP fails', async () => {
      const user = userEvent.setup()
      mockResendVerificationOtp.mockResolvedValue({ success: false, message: 'Resend failed' })
      render(<AccountVerificationPage email="test@example.com" onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: trRx('auth:resendCode') }))

      await waitFor(() => {
        expect(screen.getByText('Resend failed')).toBeInTheDocument()
      })
    })
  })
})
