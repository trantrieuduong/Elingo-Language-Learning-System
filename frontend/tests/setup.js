import '@testing-library/jest-dom'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeAll, afterAll, vi } from 'vitest'
import { server } from './mocks/server'

// Mock translations for tests
const mockTranslations = {
  auth: {
    badge: 'Chào mừng bạn trở lại',
    title: 'Đăng nhập Elingo',
    subtitle: 'Tiếp tục bài học, ôn tập và luyện nói của bạn.',
    usernameLabel: 'Email hoặc Username',
    usernamePlaceholder: 'you@example.com',
    usernameRequired: 'Email hoặc Username là bắt buộc.',
    passwordLabel: 'Mật khẩu',
    passwordPlaceholder: 'Nhập mật khẩu',
    passwordRequired: 'Mật khẩu là bắt buộc.',
    forgotPassword: 'Quên mật khẩu?',
    submit: 'Đăng nhập',
    submitting: 'Đang đăng nhập...',
    dividerOr: 'hoặc',
    googleNotConfigured: 'Google Sign-In chưa được cấu hình.',
    noAccount: 'Chưa có tài khoản Elingo?',
    signupNow: 'Đăng ký ngay',
    googleLoadError: 'Không thể tải dịch vụ đăng nhập Google.',
    googleCredentialError: 'Không nhận được thông tin đăng nhập từ Google.',
    googleServiceError: 'Không thể tải đăng nhập Google. Vui lòng thử lại.',
    signupBadge: 'Bắt đầu cùng Elingo',
    signupTitle: 'Tạo tài khoản Elingo',
    signupSubtitle: 'Thiết lập hồ sơ học viên và xác thực email để bắt đầu.',
    fullNameLabel: 'Họ và tên',
    fullNamePlaceholder: 'Nguyễn Văn A',
    fullNameRequired: 'Họ và tên là bắt buộc.',
    usernameSignupLabel: 'Username',
    usernameSignupPlaceholder: 'ANguyen',
    usernameSignupRequired: 'Username là bắt buộc.',
    usernameSignupInvalid: 'Username gồm 3-15 chữ cái hoặc chữ số, không có khoảng trắng hay ký tự đặc biệt.',
    emailLabel: 'Email',
    emailPlaceholder: 'AN123@example.com',
    emailRequired: 'Email là bắt buộc.',
    emailInvalid: 'Email không đúng định dạng.',
    passwordSignupLabel: 'Mật khẩu',
    passwordSignupPlaceholder: 'Nhập mật khẩu',
    passwordSignupRequired: 'Mật khẩu là bắt buộc.',
    passwordSignupInvalid: 'Mật khẩu phải có ít nhất 8 ký tự và bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.',
    confirmPasswordLabel: 'Xác nhận mật khẩu',
    confirmPasswordPlaceholder: 'Nhập lại mật khẩu',
    confirmPasswordMismatch: 'Mật khẩu xác nhận không khớp.',
    submitSignup: 'Tạo tài khoản',
    submittingSignup: 'Đang tạo tài khoản...',
    hasAccount: 'Đã có tài khoản?',
    loginLink: 'Đăng nhập',
    verifyBadge: 'Xác thực email',
    verifyTitle: 'Xác thực tài khoản',
    verifySubtitle: 'Nhập mã gồm 6 chữ số đã được gửi đến email đăng ký của bạn.',
    otpLabel: 'Mã xác thực',
    otpRequired: 'Mã OTP là bắt buộc.',
    submitVerify: 'Xác thực tài khoản',
    submittingVerify: 'Đang xác thực...',
    resendCode: 'Gửi lại mã xác thực',
    resendingCode: 'Đang gửi mã...',
    resendAfter: 'Gửi lại mã sau {{time}}',
    verifyError: 'Không tìm thấy thông tin xác thực. Vui lòng đăng nhập lại.',
    resetBadge: 'Khôi phục tài khoản',
    resetTitle: 'Đặt lại mật khẩu',
    resetSubtitle: 'Nhập email đã đăng ký để nhận mã OTP và tạo mật khẩu mới.',
    resetEmailLabel: 'Email đã đăng ký',
    resetOtpLabel: 'Mã OTP',
    resetOtpPlaceholder: 'Nhập mã OTP 6 chữ số',
    resetOtpRequired: 'Mã OTP là bắt buộc.',
    resetNewPasswordLabel: 'Mật khẩu mới',
    resetNewPasswordPlaceholder: 'Nhập mật khẩu mới',
    resetNewPasswordRequired: 'Mật khẩu mới là bắt buộc.',
    resetConfirmPasswordLabel: 'Xác nhận mật khẩu mới',
    resetConfirmPasswordPlaceholder: 'Nhập lại mật khẩu mới',
    resetConfirmPasswordMismatch: 'Mật khẩu xác nhận không khớp.',
    submitReset: 'Xác nhận đặt lại mật khẩu',
    submittingReset: 'Đang xử lý...',
    backToLogin: 'Quay lại',
    loginLinkText: 'Đăng nhập',
    sendOtp: 'Gửi mã OTP',
    sendingOtp: 'Đang gửi...',
    resendOtp: 'Gửi lại mã OTP',
    resendOtpTimer: 'Gửi lại ({{time}})',
  },
  api: {
    common: {
      UNKNOWN_ERROR: 'Đã xảy ra lỗi không xác định',
    },
    error: {
      INVALID_CREDENTIALS: 'Email/Username hoặc mật khẩu không chính xác.',
      USER_NOT_FOUND: 'Không tìm thấy người dùng.',
      EMAIL_NOT_EXISTED: 'Email không tồn tại trong hệ thống.',
      OTP_INVALID: 'Mã OTP không chính xác hoặc đã hết hạn.',
      TOO_MANY_REQUESTS: 'Quá nhiều yêu cầu, vui lòng thử lại sau.',
      RATE_LIMIT_EXCEEDED: 'Quá nhiều yêu cầu, vui lòng thử lại sau.',
      USERNAME_EXISTED: 'Username đã tồn tại.',
      EMAIL_EXISTED: 'Email đã được đăng ký.',
    },
    success: {
      SIGNUP_SUCCESS: 'Tạo tài khoản thành công! Vui lòng kiểm tra email để xác thực.',
      ACCOUNT_VERIFIED: 'Tài khoản đã được xác thực thành công. Bạn có thể đăng nhập ngay.',
      OTP_SENT: 'Đã gửi mã OTP thành công. Vui lòng kiểm tra email của bạn.',
      OTP_RESENT: 'Mã xác thực mới đã được gửi đến email của bạn.',
      PASSWORD_RESET: 'Đặt lại mật khẩu thành công.',
    },
  },
}

// Helper function to get nested translation
const getNestedTranslation = (key) => {
  const parts = key.split('.')
  let result = mockTranslations
  for (const part of parts) {
    if (result && typeof result === 'object' && part in result) {
      result = result[part]
    } else {
      return key // Return key if not found
    }
  }
  return result
}

// Mock react-i18next
vi.mock('react-i18next', () => ({
  useTranslation: (namespace) => ({
    t: (key, params) => {
      // Build full key with namespace
      const fullKey = namespace ? `${namespace}.${key}` : key
      let translated = getNestedTranslation(fullKey)

      // If not found, try without namespace
      if (translated === fullKey && key.includes('.')) {
        translated = getNestedTranslation(key)
      }

      // Replace params if any
      if (params && typeof translated === 'string') {
        Object.entries(params).forEach(([k, v]) => {
          translated = translated.replace(`{{${k}}}`, v)
        })
      }

      return translated
    },
    i18n: {
      changeLanguage: vi.fn(),
      language: 'vi',
      resolvedLanguage: 'vi',
    },
  }),
  Trans: ({ children }) => children,
  initReactI18next: {
    type: '3rdParty',
    init: vi.fn(),
  },
}))

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))

afterEach(() => {
  server.resetHandlers()
  cleanup()
  localStorage.clear()
  sessionStorage.clear()
  vi.clearAllMocks()
})

afterAll(() => server.close())

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

global.IntersectionObserver = class IntersectionObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  unobserve() {}
  takeRecords() { return [] }
}

global.ResizeObserver = class ResizeObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  unobserve() {}
}

global.window.google = {
  accounts: {
    id: {
      initialize: vi.fn(),
      renderButton: vi.fn(),
      prompt: vi.fn(),
    },
  },
}
