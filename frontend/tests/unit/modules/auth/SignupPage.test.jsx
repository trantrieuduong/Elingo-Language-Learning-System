import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import SignupPage from '@/modules/auth/pages/SignupPage'
import { useAuth } from '@/context/AuthContext'
import { MOCK_AUTH_CONTEXT_DEFAULT } from '../../../utils/mockData'

vi.mock('@/context/AuthContext')

describe('SignupPage', () => {
  const mockSignup = vi.fn()
  const mockNavigate = vi.fn()

  beforeEach(() => {
    useAuth.mockReturnValue({
      ...MOCK_AUTH_CONTEXT_DEFAULT,
      signup: mockSignup,
    })
  })

  describe('Rendering', () => {
    it('should render signup form fields', () => {
      render(<SignupPage onNavigate={mockNavigate} />)

      expect(screen.getByLabelText(/họ và tên/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/^username$/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/^mật khẩu$/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/xác nhận mật khẩu/i)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /tạo tài khoản/i })).toBeInTheDocument()
    })
  })

  describe('Validation', () => {
    it('should show error when fields are empty', async () => {
      const user = userEvent.setup()
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: /tạo tài khoản/i }))

      expect(screen.getByText(/họ và tên là bắt buộc/i)).toBeInTheDocument()
      expect(screen.getByText(/username là bắt buộc/i)).toBeInTheDocument()
      expect(screen.getByText(/email là bắt buộc/i)).toBeInTheDocument()
      expect(screen.getByText(/mật khẩu là bắt buộc/i)).toBeInTheDocument()
    })

    it('should show error if passwords do not match', async () => {
      const user = userEvent.setup()
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/họ và tên/i), 'Test User')
      await user.type(screen.getByLabelText(/^username$/i), 'testuser')
      await user.type(screen.getByLabelText(/email/i), 'test@example.com')
      await user.type(screen.getByLabelText(/^mật khẩu$/i), 'Password123@')
      await user.type(screen.getByLabelText(/xác nhận mật khẩu/i), 'Password999@')

      await user.click(screen.getByRole('button', { name: /tạo tài khoản/i }))

      expect(screen.getByText(/mật khẩu xác nhận không khớp/i)).toBeInTheDocument()
    })
  })

  describe('Form submission', () => {
    it('should call signup and navigate to account verification on success', async () => {
      const user = userEvent.setup()
      mockSignup.mockResolvedValue({ success: true })
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/họ và tên/i), 'Test User')
      await user.type(screen.getByLabelText(/^username$/i), 'testuser')
      await user.type(screen.getByLabelText(/email/i), 'test@example.com')
      await user.type(screen.getByLabelText(/^mật khẩu$/i), 'Password123@')
      await user.type(screen.getByLabelText(/xác nhận mật khẩu/i), 'Password123@')

      await user.click(screen.getByRole('button', { name: /tạo tài khoản/i }))

      await waitFor(() => {
        expect(mockSignup).toHaveBeenCalledWith({
          fullName: 'Test User',
          username: 'testuser',
          email: 'test@example.com',
          password: 'Password123@',
        })
        expect(mockNavigate).toHaveBeenCalledWith('/account-verification', { email: 'test@example.com' })
      })
    })

    it('should show specific field error if backend returns username or email existed', async () => {
      const user = userEvent.setup()
      mockSignup.mockResolvedValue({ success: false, code: 'USERNAME_EXISTED', message: 'Username đã được dùng' })
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/họ và tên/i), 'Test User')
      await user.type(screen.getByLabelText(/^username$/i), 'testuser')
      await user.type(screen.getByLabelText(/email/i), 'test@example.com')
      await user.type(screen.getByLabelText(/^mật khẩu$/i), 'Password123@')
      await user.type(screen.getByLabelText(/xác nhận mật khẩu/i), 'Password123@')

      await user.click(screen.getByRole('button', { name: /tạo tài khoản/i }))

      await waitFor(() => {
        expect(screen.getByText(/username đã được dùng/i)).toBeInTheDocument()
      })
    })

    it('should show error when email existed or general error occurs', async () => {
      const user = userEvent.setup()
      mockSignup.mockResolvedValue({ success: false, code: 'EMAIL_EXISTED', message: 'Email đã được dùng' })
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/họ và tên/i), 'Test User')
      await user.type(screen.getByLabelText(/^username$/i), 'testuser')
      await user.type(screen.getByLabelText(/email/i), 'exist@example.com')
      await user.type(screen.getByLabelText(/^mật khẩu$/i), 'Password123@')
      await user.type(screen.getByLabelText(/xác nhận mật khẩu/i), 'Password123@')

      await user.click(screen.getByRole('button', { name: /tạo tài khoản/i }))

      await waitFor(() => {
        expect(screen.getByText(/email đã được dùng/i)).toBeInTheDocument()
      })
    })

    it('should navigate to login page when clicking login link', async () => {
      const user = userEvent.setup()
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.click(screen.getByText(/^đăng nhập$/i))
      expect(mockNavigate).toHaveBeenCalledWith('/login')
    })
  })
})
