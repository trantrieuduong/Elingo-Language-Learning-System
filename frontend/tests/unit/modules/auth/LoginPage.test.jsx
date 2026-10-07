import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import LoginPage from '@/modules/auth/pages/LoginPage'
import { useAuth } from '@/context/AuthContext'
import { MOCK_AUTH_CONTEXT_DEFAULT } from '../../../utils/mockData'

vi.mock('@/context/AuthContext')

describe('LoginPage', () => {
  const mockLogin = vi.fn()
  const mockLoginWithGoogle = vi.fn()
  const mockNavigate = vi.fn()

  beforeEach(() => {
    useAuth.mockReturnValue({
      ...MOCK_AUTH_CONTEXT_DEFAULT,
      login: mockLogin,
      loginWithGoogle: mockLoginWithGoogle,
    })
  })

  describe('Rendering', () => {
    it('should render login form with username and password fields', () => {
      render(<LoginPage onNavigate={mockNavigate} />)

      expect(screen.getByLabelText(/email hoặc username/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/^mật khẩu$/i)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /đăng nhập/i })).toBeInTheDocument()
    })
  })

  describe('Form validation', () => {
    it('should show error when username or password is empty', async () => {
      const user = userEvent.setup()
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

      expect(screen.getByText(/email hoặc username là bắt buộc/i)).toBeInTheDocument()
      expect(screen.getByText(/mật khẩu là bắt buộc/i)).toBeInTheDocument()
    })
  })

  describe('Form submission', () => {
    it('should call login with correct credentials on submit and navigate to dashboard for USER role', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValue({ success: true, user: { role: 'USER' } })
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/email hoặc username/i), 'testuser')
      await user.type(screen.getByLabelText(/^mật khẩu$/i), 'Password123@')
      await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

      await waitFor(() => {
        expect(mockLogin).toHaveBeenCalledWith('testuser', 'Password123@')
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
      })
    })

    it('should navigate to account-verification if user is not verified', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValue({ success: false, notVerified: true })
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/email hoặc username/i), 'unverified@example.com')
      await user.type(screen.getByLabelText(/^mật khẩu$/i), 'Password123@')
      await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/account-verification', { email: 'unverified@example.com' })
      })
    })

    it('should show error message when login fails', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValue({ success: false, message: 'Sai tài khoản hoặc mật khẩu' })
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/email hoặc username/i), 'testuser')
      await user.type(screen.getByLabelText(/^mật khẩu$/i), 'wrong')
      await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

      await waitFor(() => {
        expect(screen.getByText(/sai tài khoản hoặc mật khẩu/i)).toBeInTheDocument()
      })
    })
  })

  describe('Navigation links', () => {
    it('should navigate to signup page on clicking register link', async () => {
      const user = userEvent.setup()
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.click(screen.getByText(/đăng ký ngay/i))
      expect(mockNavigate).toHaveBeenCalledWith('/signup')
    })

    it('should navigate to /admin for ADMIN role', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValue({ success: true, user: { role: 'ADMIN' } })
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/email hoặc username/i), 'adminuser')
      await user.type(screen.getByLabelText(/^mật khẩu$/i), 'Admin123@')
      await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/admin')
      })
    })

    it('should navigate to reset password page on clicking forgot password link', async () => {
      const user = userEvent.setup()
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.click(screen.getByText(/quên mật khẩu\?/i))
      expect(mockNavigate).toHaveBeenCalledWith('/reset-password')
    })

    it('should handle Google login credential callback successfully', async () => {
      mockLoginWithGoogle.mockResolvedValue({ success: true, user: { role: 'USER' } })
      render(<LoginPage onNavigate={mockNavigate} />)

      const callback = window.google.accounts.id.initialize.mock.calls[0]?.[0]?.callback
      if (callback) {
        await callback({ credential: 'mock-google-credential' })
        expect(mockLoginWithGoogle).toHaveBeenCalledWith('mock-google-credential')
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
      }
    })

    it('should handle Google login credential callback with error', async () => {
      mockLoginWithGoogle.mockResolvedValue({ success: false, message: 'Google login failed' })
      render(<LoginPage onNavigate={mockNavigate} />)

      const callback = window.google.accounts.id.initialize.mock.calls[0]?.[0]?.callback
      if (callback) {
        await callback({ credential: 'mock-google-credential' })
        expect(screen.getByText(/google login failed/i)).toBeInTheDocument()
      }
    })

    it('should handle Google login without credential and navigate to /admin on ADMIN role', async () => {
      mockLoginWithGoogle.mockResolvedValue({ success: true, user: { role: 'ADMIN' } })
      render(<LoginPage onNavigate={mockNavigate} />)

      const callback = window.google.accounts.id.initialize.mock.calls[0]?.[0]?.callback
      if (callback) {
        await callback({})
        expect(screen.getByText(/không nhận được thông tin đăng nhập từ google/i)).toBeInTheDocument()

        await callback({ credential: 'admin-credential' })
        expect(mockNavigate).toHaveBeenCalledWith('/admin')
      }
    })

    it('should set field error when login fails with USER_NOT_FOUND or array errors', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValueOnce({
        success: false,
        code: 'USER_NOT_FOUND',
        message: 'Không tìm thấy người dùng',
      })

      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/email hoặc username/i), 'notfound')
      await user.type(screen.getByLabelText(/^mật khẩu$/i), 'Password123@')
      await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

      await waitFor(() => {
        expect(screen.getByText(/không tìm thấy người dùng/i)).toBeInTheDocument()
      })
    })

    it('should set field errors when backend returns errors array', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValueOnce({
        success: false,
        errors: [{ field: 'username', message: 'Username không tồn tại' }],
      })

      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(/email hoặc username/i), 'baduser')
      await user.type(screen.getByLabelText(/^mật khẩu$/i), 'Password123@')
      await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

      await waitFor(() => {
        expect(screen.getByText(/username không tồn tại/i)).toBeInTheDocument()
      })
    })

    it('should clear errors when typing in username and password inputs', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValue({ success: false, message: 'Lỗi chung' })
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: /đăng nhập/i }))
      expect(screen.getByText(/email hoặc username là bắt buộc/i)).toBeInTheDocument()

      await user.type(screen.getByLabelText(/email hoặc username/i), 'a')
      expect(screen.queryByText(/email hoặc username là bắt buộc/i)).not.toBeInTheDocument()

      await user.type(screen.getByLabelText(/^mật khẩu$/i), 'b')
      expect(screen.queryByText(/mật khẩu là bắt buộc/i)).not.toBeInTheDocument()
    })
  })
})
