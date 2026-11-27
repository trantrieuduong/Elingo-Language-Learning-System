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

export const MOCK_NOTIFICATION_CONTEXT_DEFAULT = {
  notifications: [],
  unreadCount: 0,
  isLoadingFirstPage: false,
  isLoadingMore: false,
  isLast: true,
  hasError: false,
  fetchUnreadCount: vi.fn(),
  loadInitialNotifications: vi.fn(),
  loadMoreNotifications: vi.fn(),
  markAsRead: vi.fn(),
  markAllAsRead: vi.fn(),
}

export const MOCK_DECK = {
  id: 'deck-1',
  slug: 'mock-deck',
  title: 'Mock Deck',
  description: 'Mô tả bộ từ',
  status: 'DRAFT',
  isPremium: false,
  topicCount: 2,
  cardCount: 10,
  learnedCardCount: 3,
  cefrLevels: [{ id: 'a1', label: 'A1' }],
  tags: [{ id: 't1', label: 'Du lịch' }],
  coverImageUrl: null,
  ownerId: 1,
}

export const MOCK_PUBLISHED_DECK = {
  ...MOCK_DECK,
  id: 'deck-2',
  slug: 'published-deck',
  title: 'Published Deck',
  status: 'PUBLISHED',
  ownerId: 99,
}

export const MOCK_TOPIC = {
  id: 1,
  name: 'Animals',
  slug: 'animals',
  order: 1,
  cardCount: 5,
  learnedCardCount: 2,
}

export const MOCK_CARD = {
  id: 'card-1',
  term: 'hello',
  pos: 'interjection',
  translation: 'xin chào',
  explanationVi: null,
  explanationEn: null,
  examplesVi: null,
  examplesEn: null,
  state: { starred: false },
}

export const MOCK_STARRED_CARD = {
  ...MOCK_CARD,
  id: 'card-2',
  term: 'book',
  translation: 'sách',
  state: { starred: true },
}

export const MOCK_AUTH_CONTEXT_LOGGED_IN = {
  user: MOCK_USER,
  accessToken: 'mock-token',
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

