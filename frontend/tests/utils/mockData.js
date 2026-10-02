import { vi } from 'vitest'

export const MOCK_USER = {
  id: 1,
  username: 'testuser',
  email: 'test@example.com',
  fullName: 'Test User',
  role: 'USER',
  isActive: true,
  isVerified: true,
}

export const MOCK_ACCESS_TOKEN = 'mock-access-token-12345'

export const MOCK_LOGIN_CREDENTIALS = {
  identifier: 'testuser',
  password: 'Password123@',
}

export const MOCK_SIGNUP_DATA = {
  username: 'newuser',
  email: 'newuser@example.com',
  fullName: 'New User',
  password: 'SecurePass123@',
}

export const MOCK_OTP = '123456'

export const MOCK_AUTH_CONTEXT_DEFAULT = {
  user: null,
  accessToken: null,
  loading: false,
  login: vi.fn(),
  loginWithGoogle: vi.fn(),
  signup: vi.fn(),
  verifyAccount: vi.fn(),
  resendVerificationOtp: vi.fn(),
  sendResetPasswordOtp: vi.fn(),
  resetPassword: vi.fn(),
  logout: vi.fn(),
  updateUser: vi.fn(),
}
