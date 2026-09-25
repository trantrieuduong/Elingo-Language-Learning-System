import { createContext, useContext, useEffect, useState } from 'react'

const ThemeContext = createContext(null)

export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('theme') ?? 'light'
  })

  // ── Áp dụng theme lên <html> element ──
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('theme', theme)
  }, [theme])

  /**
   * Toggle giữa light và dark.
   * Dark mode được implement qua CSS selector [data-theme='dark'].
   * KHÔNG dùng prefers-color-scheme trong CSS.
   */
  const toggleTheme = () => {
    setTheme((prev) => (prev === 'light' ? 'dark' : 'light'))
  }

  const isDark = theme === 'dark'

  const value = { theme, isDark, toggleTheme }

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

/**
 * Hook để truy cập ThemeContext.
 * Phải dùng bên trong <ThemeProvider />.
 */
export const useTheme = () => {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useTheme phải được sử dụng trong ThemeProvider')
  }
  return context
}
