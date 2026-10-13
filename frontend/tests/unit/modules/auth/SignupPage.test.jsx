import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import SignupPage from '@/modules/auth/pages/SignupPage'
import { useAuth } from '@/context/AuthContext'
import { MOCK_AUTH_CONTEXT_DEFAULT } from '../../../utils/mockData'
import { LANGS, tr, trRx, useLang } from '../../../utils/i18n'

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

  describe.each(LANGS)('Rendering [%s]', (lng) => {
    beforeEach(() => useLang(lng))

    it('should render signup form fields', () => {
      render(<SignupPage onNavigate={mockNavigate} />)

      expect(screen.getByLabelText(tr('auth:fullNameLabel'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('auth:usernameSignupLabel'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('auth:emailLabel'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('auth:passwordSignupLabel'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('auth:confirmPasswordLabel'))).toBeInTheDocument()
      expect(screen.getByRole('button', { name: trRx('auth:submitSignup') })).toBeInTheDocument()
    })
  })

  describe.each(LANGS)('Validation [%s]', (lng) => {
    beforeEach(() => useLang(lng))

    it('should show error when fields are empty', async () => {
      const user = userEvent.setup()
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      expect(screen.getByText(tr('auth:fullNameRequired'))).toBeInTheDocument()
      expect(screen.getByText(tr('auth:usernameSignupRequired'))).toBeInTheDocument()
      expect(screen.getByText(tr('auth:emailRequired'))).toBeInTheDocument()
      expect(screen.getByText(tr('auth:passwordSignupRequired'))).toBeInTheDocument()
    })

    it('should show error if passwords do not match', async () => {
      const user = userEvent.setup()
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:fullNameLabel')), 'Test User')
      await user.type(screen.getByLabelText(tr('auth:usernameSignupLabel')), 'testuser')
      await user.type(screen.getByLabelText(tr('auth:emailLabel')), 'test@example.com')
      await user.type(screen.getByLabelText(tr('auth:passwordSignupLabel')), 'Password123@')
      await user.type(screen.getByLabelText(tr('auth:confirmPasswordLabel')), 'Password999@')

      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      expect(screen.getByText(tr('auth:confirmPasswordMismatch'))).toBeInTheDocument()
    })

    it('should show error when username, email, or password formats are invalid', async () => {
      const user = userEvent.setup()
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:fullNameLabel')), 'Test User')
      await user.type(screen.getByLabelText(tr('auth:usernameSignupLabel')), 'ab')
      await user.type(screen.getByLabelText(tr('auth:emailLabel')), 'invalid-email')
      await user.type(screen.getByLabelText(tr('auth:passwordSignupLabel')), 'short')
      await user.type(screen.getByLabelText(tr('auth:confirmPasswordLabel')), 'short')

      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      expect(screen.getByText(tr('auth:usernameSignupInvalid'))).toBeInTheDocument()
      expect(screen.getByText(tr('auth:emailInvalid'))).toBeInTheDocument()
      expect(screen.getByText(tr('auth:passwordSignupInvalid'))).toBeInTheDocument()
    })
  })

  describe('Form submission', () => {
    it('should call signup and navigate to account verification on success', async () => {
      const user = userEvent.setup()
      mockSignup.mockResolvedValue({ success: true })
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:fullNameLabel')), 'Test User')
      await user.type(screen.getByLabelText(tr('auth:usernameSignupLabel')), 'testuser')
      await user.type(screen.getByLabelText(tr('auth:emailLabel')), 'test@example.com')
      await user.type(screen.getByLabelText(tr('auth:passwordSignupLabel')), 'Password123@')
      await user.type(screen.getByLabelText(tr('auth:confirmPasswordLabel')), 'Password123@')

      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

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
      mockSignup.mockResolvedValue({ success: false, code: 'USERNAME_EXISTED', message: 'Username already in use' })
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:fullNameLabel')), 'Test User')
      await user.type(screen.getByLabelText(tr('auth:usernameSignupLabel')), 'testuser')
      await user.type(screen.getByLabelText(tr('auth:emailLabel')), 'test@example.com')
      await user.type(screen.getByLabelText(tr('auth:passwordSignupLabel')), 'Password123@')
      await user.type(screen.getByLabelText(tr('auth:confirmPasswordLabel')), 'Password123@')

      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      await waitFor(() => {
        expect(screen.getByText('Username already in use')).toBeInTheDocument()
      })
    })

    it('should show error when email existed or general error occurs', async () => {
      const user = userEvent.setup()
      mockSignup.mockResolvedValue({ success: false, code: 'EMAIL_EXISTED', message: 'Email already registered' })
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:fullNameLabel')), 'Test User')
      await user.type(screen.getByLabelText(tr('auth:usernameSignupLabel')), 'testuser')
      await user.type(screen.getByLabelText(tr('auth:emailLabel')), 'exist@example.com')
      await user.type(screen.getByLabelText(tr('auth:passwordSignupLabel')), 'Password123@')
      await user.type(screen.getByLabelText(tr('auth:confirmPasswordLabel')), 'Password123@')

      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      await waitFor(() => {
        expect(screen.getByText('Email already registered')).toBeInTheDocument()
      })
    })

    it('should handle backend errors array and general error on signup', async () => {
      const user = userEvent.setup()
      mockSignup.mockResolvedValueOnce({
        success: false,
        errors: [{ field: 'email', message: 'Email already exists' }],
      })
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.type(screen.getByLabelText(tr('auth:fullNameLabel')), 'Test User')
      await user.type(screen.getByLabelText(tr('auth:usernameSignupLabel')), 'testuser')
      await user.type(screen.getByLabelText(tr('auth:emailLabel')), 'test@example.com')
      await user.type(screen.getByLabelText(tr('auth:passwordSignupLabel')), 'Password123@')
      await user.type(screen.getByLabelText(tr('auth:confirmPasswordLabel')), 'Password123@')

      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      await waitFor(() => {
        expect(screen.getByText('Email already exists')).toBeInTheDocument()
      })

      // General error branch
      mockSignup.mockResolvedValueOnce({
        success: false,
        code: 'UNKNOWN_ERROR',
        message: 'System error',
      })

      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      await waitFor(() => {
        expect(screen.getByText('System error')).toBeInTheDocument()
      })
    })

    it('should navigate to login page when clicking login link', async () => {
      const user = userEvent.setup()
      render(<SignupPage onNavigate={mockNavigate} />)

      await user.click(screen.getByText(tr('auth:loginLink')))
      expect(mockNavigate).toHaveBeenCalledWith('/login')
    })
  })
})
