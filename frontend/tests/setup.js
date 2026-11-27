import '@testing-library/jest-dom'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeAll, afterAll, beforeEach, vi } from 'vitest'
import { server } from './mocks/server'
import i18n from '../src/i18n'

const setupGoogleMock = () => {
  global.window.google = {
    accounts: {
      id: {
        initialize: vi.fn(),
        renderButton: vi.fn(),
        prompt: vi.fn(),
      },
    },
  }
}

beforeEach(async () => {
  setupGoogleMock()
  await i18n.changeLanguage('vi')
})

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))

afterEach(() => {
  server.resetHandlers()
  cleanup()
  localStorage.clear()
  sessionStorage.clear()
  vi.clearAllMocks()
  document.getElementById('google-gsi-script')?.remove()
  setupGoogleMock()
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
  disconnect() {} // NOSONAR - intentional stub for jsdom test environment
  observe() {}    // NOSONAR - intentional stub for jsdom test environment
  unobserve() {}  // NOSONAR - intentional stub for jsdom test environment
  takeRecords() { return [] }
}

global.ResizeObserver = class ResizeObserver {
  constructor() {}
  disconnect() {} // NOSONAR - intentional stub for jsdom test environment
  observe() {}    // NOSONAR - intentional stub for jsdom test environment
  unobserve() {}  // NOSONAR - intentional stub for jsdom test environment
}
