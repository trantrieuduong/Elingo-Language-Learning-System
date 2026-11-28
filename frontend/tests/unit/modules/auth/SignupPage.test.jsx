import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
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
    it('should show passwordTooLong error when password exceeds 72 bytes and not call signup', async () => {
      const longPassword = 'Aa1!' + 'a'.repeat(100)
      render(<SignupPage onNavigate={mockNavigate} />)

      fireEvent.change(screen.getByLabelText(tr('auth:fullNameLabel')), { target: { value: 'Test User' } })
      fireEvent.change(screen.getByLabelText(tr('auth:usernameSignupLabel')), { target: { value: 'testuser' } })
      fireEvent.change(screen.getByLabelText(tr('auth:emailLabel')), { target: { value: 'test@example.com' } })
      fireEvent.change(screen.getByLabelText(tr('auth:passwordSignupLabel')), { target: { value: longPassword } })
      fireEvent.change(screen.getByLabelText(tr('auth:confirmPasswordLabel')), { target: { value: longPassword } })

      const user = userEvent.setup()
      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      await waitFor(() => {
        expect(screen.getByText(tr('auth:passwordTooLong'))).toBeInTheDocument()
      })
      expect(mockSignup).not.toHaveBeenCalled()
    })

    it('should NOT show passwordTooLong error when password is exactly 72 bytes', async () => {
      // 'Aa1!' (4 byte) + 68*'a' (68 byte) = 72 byte
      const validPassword = 'Aa1!' + 'a'.repeat(68)
      mockSignup.mockResolvedValue({ success: true })
      render(<SignupPage onNavigate={mockNavigate} />)

      fireEvent.change(screen.getByLabelText(tr('auth:fullNameLabel')), { target: { value: 'Test User' } })
      fireEvent.change(screen.getByLabelText(tr('auth:usernameSignupLabel')), { target: { value: 'testuser' } })
      fireEvent.change(screen.getByLabelText(tr('auth:emailLabel')), { target: { value: 'test@example.com' } })
      fireEvent.change(screen.getByLabelText(tr('auth:passwordSignupLabel')), { target: { value: validPassword } })
      fireEvent.change(screen.getByLabelText(tr('auth:confirmPasswordLabel')), { target: { value: validPassword } })

      const user = userEvent.setup()
      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      await waitFor(() => {
        expect(mockSignup).toHaveBeenCalledWith(expect.objectContaining({ password: validPassword }))
      })
    })

    it('should show passwordTooLong error when password is 73 bytes', async () => {
      // 'Aa1!' (4 byte) + 69*'a' (69 byte) = 73 byte
      const longPassword = 'Aa1!' + 'a'.repeat(69)
      render(<SignupPage onNavigate={mockNavigate} />)

      fireEvent.change(screen.getByLabelText(tr('auth:passwordSignupLabel')), { target: { value: longPassword } })

      const user = userEvent.setup()
      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      await waitFor(() => {
        expect(screen.getByText(tr('auth:passwordTooLong'))).toBeInTheDocument()
      })
    })

    it('should show passwordTooLong for accented password exceeding 72 bytes', async () => {
      // 'Aa1!' (4 byte) + 23*'â' (23*3=69 byte) = 73 byte
      const longPassword = 'Aa1!' + '\u1edb'.repeat(23)
      render(<SignupPage onNavigate={mockNavigate} />)

      fireEvent.change(screen.getByLabelText(tr('auth:passwordSignupLabel')), { target: { value: longPassword } })

      const user = userEvent.setup()
      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      await waitFor(() => {
        expect(screen.getByText(tr('auth:passwordTooLong'))).toBeInTheDocument()
      })
    })

    it('should show length requirement as unmet when password is 73 bytes', async () => {
      // 'Aa1!' (4 byte) + 69*'a' (69 byte) = 73 byte - validation should show length requirement as unmet
      const longPassword = 'Aa1!' + 'a'.repeat(69)
      render(<SignupPage onNavigate={mockNavigate} />)

      fireEvent.change(screen.getByLabelText(tr('auth:passwordSignupLabel')), { target: { value: longPassword } })

      await waitFor(() => {
        const lengthItem = screen.getByText(tr('auth:passwordRequirements.length')).closest('li')
        expect(lengthItem).toHaveClass('auth-password-requirements__item--unmet')
      })
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

    it('should show PASSWORD_INVALID error at password field when BE returns it', async () => {
      const user = userEvent.setup()
      mockSignup.mockResolvedValue({ success: false, code: 'PASSWORD_INVALID', message: 'Password must be 8-72 characters' })
      render(<SignupPage onNavigate={mockNavigate} />)

      fireEvent.change(screen.getByLabelText(tr('auth:fullNameLabel')), { target: { value: 'Test User' } })
      fireEvent.change(screen.getByLabelText(tr('auth:usernameSignupLabel')), { target: { value: 'testuser' } })
      fireEvent.change(screen.getByLabelText(tr('auth:emailLabel')), { target: { value: 'test@example.com' } })
      fireEvent.change(screen.getByLabelText(tr('auth:passwordSignupLabel')), { target: { value: 'Password123@' } })
      fireEvent.change(screen.getByLabelText(tr('auth:confirmPasswordLabel')), { target: { value: 'Password123@' } })

      await user.click(screen.getByRole('button', { name: trRx('auth:submitSignup') }))

      await waitFor(() => {
        expect(screen.getByText('Password must be 8-72 characters')).toBeInTheDocument()
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
