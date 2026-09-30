import '@testing-library/jest-dom'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeAll, afterAll, vi } from 'vitest'
import { server } from './mocks/server'

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
