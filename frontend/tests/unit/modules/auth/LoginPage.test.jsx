import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import LoginPage from '@/modules/auth/pages/LoginPage'
import { useAuth } from '@/context/AuthContext'
import { MOCK_AUTH_CONTEXT_DEFAULT } from '../../../utils/mockData'
import { LANGS, tr, trRx, useLang } from '../../../utils/i18n'

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

  describe.each(LANGS)('Rendering [%s]', (lng) => {
    beforeEach(() => useLang(lng))

    it('should render login form with username and password fields', () => {
      render(<LoginPage onNavigate={mockNavigate} />)

      expect(screen.getByLabelText(tr('auth:usernameLabel'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('auth:passwordLabel'))).toBeInTheDocument()
      expect(screen.getByRole('button', { name: trRx('auth:submit') })).toBeInTheDocument()
    })
  })

  describe.each(LANGS)('Form validation [%s]', (lng) => {
    beforeEach(() => useLang(lng))

    it('should show error when username or password is empty', async () => {
      const user = userEvent.setup()
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: trRx('auth:submit') }))

      expect(screen.getByText(tr('auth:usernameRequired'))).toBeInTheDocument()
      expect(screen.getByText(tr('auth:passwordRequired'))).toBeInTheDocument()
    })
  })

  describe('Form submission', () => {
    it('should call login with correct credentials on submit and navigate to dashboard for USER role', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValue({ success: true, user: { role: 'USER' } })
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:usernameLabel')), 'testuser')
      await user.type(screen.getByLabelText(tr('auth:passwordLabel')), 'Password123@')
      await user.click(screen.getByRole('button', { name: trRx('auth:submit') }))

      await waitFor(() => {
        expect(mockLogin).toHaveBeenCalledWith('testuser', 'Password123@')
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
      })
    })

    it('should navigate to account-verification if user is not verified', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValue({ success: false, notVerified: true })
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:usernameLabel')), 'unverified@example.com')
      await user.type(screen.getByLabelText(tr('auth:passwordLabel')), 'Password123@')
      await user.click(screen.getByRole('button', { name: trRx('auth:submit') }))

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/account-verification', { email: 'unverified@example.com' })
      })
    })

    it('should show error message when login fails', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValue({ success: false, message: 'Invalid credentials' })
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:usernameLabel')), 'testuser')
      await user.type(screen.getByLabelText(tr('auth:passwordLabel')), 'wrong')
      await user.click(screen.getByRole('button', { name: trRx('auth:submit') }))

      await waitFor(() => {
        expect(screen.getByText('Invalid credentials')).toBeInTheDocument()
      })
    })
  })

  describe('Navigation links', () => {
    it('should navigate to signup page on clicking register link', async () => {
      const user = userEvent.setup()
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.click(screen.getByText(tr('auth:signupNow')))
      expect(mockNavigate).toHaveBeenCalledWith('/signup')
    })

    it('should navigate to /admin for ADMIN role', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValue({ success: true, user: { role: 'ADMIN' } })
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:usernameLabel')), 'adminuser')
      await user.type(screen.getByLabelText(tr('auth:passwordLabel')), 'Admin123@')
      await user.click(screen.getByRole('button', { name: trRx('auth:submit') }))

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/admin')
      })
    })

    it('should navigate to reset password page on clicking forgot password link', async () => {
      const user = userEvent.setup()
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.click(screen.getByText(tr('auth:forgotPassword')))
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
        expect(screen.getByText(tr('auth:googleCredentialError'))).toBeInTheDocument()

        await callback({ credential: 'admin-credential' })
        expect(mockNavigate).toHaveBeenCalledWith('/admin')
      }
    })

    it('should set field error when login fails with USER_NOT_FOUND or array errors', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValueOnce({
        success: false,
        code: 'USER_NOT_FOUND',
        message: 'User not found in system',
      })

      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:usernameLabel')), 'notfound')
      await user.type(screen.getByLabelText(tr('auth:passwordLabel')), 'Password123@')
      await user.click(screen.getByRole('button', { name: trRx('auth:submit') }))

      await waitFor(() => {
        expect(screen.getByText('User not found in system')).toBeInTheDocument()
      })
    })

    it('should set field errors when backend returns errors array', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValueOnce({
        success: false,
        errors: [{ field: 'username', message: 'Username does not exist' }],
      })

      render(<LoginPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:usernameLabel')), 'baduser')
      await user.type(screen.getByLabelText(tr('auth:passwordLabel')), 'Password123@')
      await user.click(screen.getByRole('button', { name: trRx('auth:submit') }))

      await waitFor(() => {
        expect(screen.getByText('Username does not exist')).toBeInTheDocument()
      })
    })

    it('should clear errors when typing in username and password inputs', async () => {
      const user = userEvent.setup()
      mockLogin.mockResolvedValue({ success: false, message: 'General error' })
      render(<LoginPage onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: trRx('auth:submit') }))
      expect(screen.getByText(tr('auth:usernameRequired'))).toBeInTheDocument()

      await user.type(screen.getByLabelText(tr('auth:usernameLabel')), 'a')
      expect(screen.queryByText(tr('auth:usernameRequired'))).not.toBeInTheDocument()

      await user.type(screen.getByLabelText(tr('auth:passwordLabel')), 'b')
      expect(screen.queryByText(tr('auth:passwordRequired'))).not.toBeInTheDocument()
    })
  })
})
