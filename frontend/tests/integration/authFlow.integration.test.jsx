import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuthProvider } from '@/context/AuthContext'
import LoginPage from '@/modules/auth/pages/LoginPage'

describe('Auth flow (integration)', () => {
  it('should complete full login flow and persist token', async () => {
    const user = userEvent.setup()
    render(
      <AuthProvider>
        <LoginPage onNavigate={() => {}} />
      </AuthProvider>
    )

    await user.type(screen.getByLabelText(/email hoặc username/i), 'testuser')
    await user.type(screen.getByLabelText(/^mật khẩu$/i), 'Password123@')
    await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

    await waitFor(() => {
      expect(localStorage.getItem('accessToken')).toBe('mock-access-token-12345')
    })
  })
})
