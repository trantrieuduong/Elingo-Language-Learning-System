import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { ThemeProvider, useTheme } from '@/context/ThemeContext'

describe('ThemeContext', () => {
  beforeEach(() => {
    document.documentElement.removeAttribute('data-theme')
  })

  it('mặc định là light, set data-theme trên <html> và lưu vào localStorage', () => {
    const { result } = renderHook(() => useTheme(), { wrapper: ThemeProvider })

    expect(result.current.theme).toBe('light')
    expect(result.current.isDark).toBe(false)
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
    expect(localStorage.getItem('theme')).toBe('light')
  })

  it('khởi tạo từ theme đã lưu trong localStorage', () => {
    localStorage.setItem('theme', 'dark')

    const { result } = renderHook(() => useTheme(), { wrapper: ThemeProvider })

    expect(result.current.theme).toBe('dark')
    expect(result.current.isDark).toBe(true)
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('toggleTheme đổi light -> dark -> light và đồng bộ <html> + localStorage', () => {
    const { result } = renderHook(() => useTheme(), { wrapper: ThemeProvider })

    act(() => result.current.toggleTheme())
    expect(result.current.isDark).toBe(true)
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    expect(localStorage.getItem('theme')).toBe('dark')

    act(() => result.current.toggleTheme())
    expect(result.current.isDark).toBe(false)
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
    expect(localStorage.getItem('theme')).toBe('light')
  })

  it('throw lỗi khi useTheme được dùng ngoài ThemeProvider', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})

    expect(() => renderHook(() => useTheme())).toThrow(
      'useTheme phải được sử dụng trong ThemeProvider'
    )

    consoleError.mockRestore()
  })
})
