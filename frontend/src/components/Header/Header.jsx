import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../../context/AuthContext'
import { useTheme } from '../../context/ThemeContext'
import './Header.css'

/**
 * Header component — glassmorphism navbar, fixed floating style.
 * Nhận onNavigate prop thay vì tự navigate trực tiếp.
 *
 * @param {Function} onNavigate - navigate function từ App.jsx
 * @param {string} currentPath - currentPath state từ App.jsx
 */
function Header({ onNavigate, currentPath }) {
  const { t, i18n } = useTranslation('nav')
  const { user, logout } = useAuth()
  const { isDark, toggleTheme } = useTheme()
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [isUserDropdownOpen, setIsUserDropdownOpen] = useState(false)
  const [activeLandingSection, setActiveLandingSection] = useState('hero')
  const dropdownRef = useRef(null)
  const activeLandingSectionRef = useRef('hero')

  const LANDING_NAV_ITEMS = [
    { key: 'home', label: t('home'), path: '#hero', sectionId: 'hero' },
    { key: 'features', label: t('features'), path: '#features', sectionId: 'features' },
    { key: 'learning', label: t('learning'), path: '#learning', sectionId: 'learning' },
    { key: 'community', label: t('community'), path: '#community', sectionId: 'community' },
    { key: 'pricing', label: t('pricing'), path: '#pricing', sectionId: 'pricing' },
  ]

  const NAV_ITEMS = [
    { key: 'learning', label: t('learning'), path: '/learning' },
    { key: 'battle', label: t('battle'), path: '/battle' },
    { key: 'community', label: t('community'), path: '/community' },
    { key: 'gamification', label: t('gamification'), path: '/gamification' },
  ]

  // ── Đóng dropdown khi click outside ──
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsUserDropdownOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // ── Đóng mobile menu khi navigate ──
  useEffect(() => {
    setIsMenuOpen(false)
  }, [currentPath])

  useEffect(() => {
    if (user || currentPath !== '/') return undefined

    let animationFrameId = null

    const updateActiveSection = () => {
      animationFrameId = null
      const headerOffset = 130
      const activeItem = [...LANDING_NAV_ITEMS].reverse().find((item) => {
        const section = document.getElementById(item.sectionId)
        return section && section.getBoundingClientRect().top <= headerOffset
      })
      const nextSection = activeItem?.sectionId ?? 'hero'

      if (nextSection !== activeLandingSectionRef.current) {
        activeLandingSectionRef.current = nextSection
        setActiveLandingSection(nextSection)
      }
    }

    const handleScroll = () => {
      if (animationFrameId === null) {
        animationFrameId = window.requestAnimationFrame(updateActiveSection)
      }
    }

    updateActiveSection()
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => {
      window.removeEventListener('scroll', handleScroll)
      if (animationFrameId !== null) window.cancelAnimationFrame(animationFrameId)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPath, user])

  const handleLogout = async () => {
    setIsUserDropdownOpen(false)
    await logout()
    if (onNavigate) onNavigate('/login')
  }

  const handleNav = (path) => {
    setIsMenuOpen(false)
    if (onNavigate) onNavigate(path)
  }

  const handleLandingNav = (e, item) => {
    e.preventDefault()
    if (activeLandingSectionRef.current !== item.sectionId) {
      activeLandingSectionRef.current = item.sectionId
      setActiveLandingSection(item.sectionId)
    }
    document.getElementById(item.sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const isActive = (path) => currentPath === path || currentPath.startsWith(path + '/')

  const changeLanguage = (lng) => {
    i18n.changeLanguage(lng)
  }

  const currentLanguage = i18n.language || 'vi'

  return (
    <header className={`header ${currentPath !== '/' ? 'header--auth' : ''} glass-capsule glass-sheen`}>
      <div className="header__inner container">
        {/* Brand */}
        <a
          href="/"
          className="header__brand"
          onClick={(e) => { e.preventDefault(); handleNav('/') }}
        >
          <span className="header__brand-name">Elingo</span>
        </a>

        {/* Desktop Navigation */}
        <nav className="header__nav" aria-label="Main navigation">
          {(user ? NAV_ITEMS : currentPath === '/' ? LANDING_NAV_ITEMS : []).map((item) => (
              <a
                key={item.key}
                href={item.path}
                className={`header__nav-link ${(user && isActive(item.path)) || (!user && item.sectionId === activeLandingSection) ? 'header__nav-link--active' : ''}`}
                onClick={user ? (e) => { e.preventDefault(); handleNav(item.path) } : (e) => handleLandingNav(e, item)}
              >
                {item.label}
              </a>
            ))}
        </nav>

        {/* Right side */}
        <div className="header__right">
          {/* Language switch button */}
          <button
            type="button"
            className="btn-icon header__lang-btn"
            onClick={() => changeLanguage(currentLanguage.startsWith('vi') ? 'en' : 'vi')}
            aria-label="Switch language"
            title={currentLanguage.startsWith('vi') ? 'Switch to English' : 'Chuyển sang Tiếng Việt'}
          >
            <span className="header__lang-code">
              {currentLanguage.startsWith('vi') ? 'VI' : 'EN'}
            </span>
          </button>

          {/* Theme toggle */}
          <button
            type="button"
            className="btn-icon"
            onClick={toggleTheme}
            aria-label={isDark ? t('lightMode') : t('darkMode')}
          >
            <span className="material-symbols-outlined">
              {isDark ? 'light_mode' : 'dark_mode'}
            </span>
          </button>

          {user ? (
            <>
              {/* Notification bell */}
              <button
                type="button"
                className="btn-icon"
                onClick={() => handleNav('/notification')}
                aria-label={t('notifications')}
              >
                <span className="material-symbols-outlined">notifications</span>
              </button>

              {/* User dropdown */}
              <div className="header__user-dropdown" ref={dropdownRef}>
                <button
                  type="button"
                  className="header__avatar btn-icon"
                  onClick={() => setIsUserDropdownOpen((prev) => !prev)}
                  aria-label={t('userMenu')}
                  aria-expanded={isUserDropdownOpen}
                >
                  <span className="material-symbols-outlined">account_circle</span>
                </button>

                {isUserDropdownOpen && (
                  <div className="header__dropdown-menu glass-card">
                    <div className="header__dropdown-user">
                      <span className="text-label-lg">{user.fullName ?? user.email}</span>
                      <span className="text-body-sm" style={{ color: 'var(--color-text-disabled)' }}>
                        {user.email}
                      </span>
                    </div>
                    <hr className="header__dropdown-divider" />
                    <a
                      href="/profile"
                      className="header__dropdown-item"
                      onClick={(e) => { e.preventDefault(); setIsUserDropdownOpen(false); handleNav('/profile') }}
                    >
                      <span className="material-symbols-outlined">person</span>
                      {t('profile')}
                    </a>
                    <a
                      href="/progress"
                      className="header__dropdown-item"
                      onClick={(e) => { e.preventDefault(); setIsUserDropdownOpen(false); handleNav('/progress') }}
                    >
                      <span className="material-symbols-outlined">insights</span>
                      {t('progress')}
                    </a>
                    <a
                      href="/premium"
                      className="header__dropdown-item"
                      onClick={(e) => { e.preventDefault(); setIsUserDropdownOpen(false); handleNav('/premium') }}
                    >
                      <span className="material-symbols-outlined">star</span>
                      {t('premium')}
                    </a>
                    <hr className="header__dropdown-divider" />
                    <button
                      type="button"
                      className="header__dropdown-item header__dropdown-item--danger"
                      onClick={handleLogout}
                    >
                      <span className="material-symbols-outlined">logout</span>
                      {t('logout')}
                    </button>
                  </div>
                )}
              </div>

              <button
                type="button"
                className="header__logout-button btn-icon"
                onClick={handleLogout}
                aria-label={t('logout')}
                title={t('logout')}
              >
                <span className="material-symbols-outlined">logout</span>
              </button>
            </>
          ) : (
            /* Chưa đăng nhập */
            <div className="header__auth-btns">
              <button
                type="button"
                className={currentPath === '/login' ? 'btn-primary header__auth-button--active' : 'btn-secondary'}
                onClick={() => handleNav('/login')}
              >
                {t('login')}
              </button>
              <button
                type="button"
                className={currentPath === '/signup' ? 'btn-primary header__auth-button--active' : 'btn-secondary'}
                onClick={() => handleNav('/signup')}
              >
                {t('signup')}
              </button>
            </div>
          )}

          {/* Mobile hamburger */}
          {user && (
            <button
              type="button"
              className="header__hamburger btn-icon"
              onClick={() => setIsMenuOpen((prev) => !prev)}
              aria-label={isMenuOpen ? t('closeMenu') : t('openMenu')}
              aria-expanded={isMenuOpen}
            >
              <span className="material-symbols-outlined">
                {isMenuOpen ? 'close' : 'menu'}
              </span>
            </button>
          )}
        </div>
      </div>

      {/* Mobile Navigation */}
      {user && isMenuOpen && (
        <nav className="header__mobile-nav glass-capsule" aria-label="Mobile navigation">
          {NAV_ITEMS.map((item) => (
            <a
              key={item.key}
              href={item.path}
              className={`header__mobile-nav-link ${isActive(item.path) ? 'header__mobile-nav-link--active' : ''}`}
              onClick={(e) => { e.preventDefault(); handleNav(item.path) }}
            >
              {item.label}
            </a>
          ))}
        </nav>
      )}
    </header>
  )
}

export default Header
