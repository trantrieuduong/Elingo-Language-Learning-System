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
  const [isLearningDropdownOpen, setIsLearningDropdownOpen] = useState(false)
  const [isMobileLearningOpen, setIsMobileLearningOpen] = useState(false)
  const [activeLandingSection, setActiveLandingSection] = useState('hero')
  const dropdownRef = useRef(null)
  const learningDropdownRef = useRef(null)
  const activeLandingSectionRef = useRef('hero')

  const LANDING_NAV_ITEMS = [
    { key: 'home', label: t('home'), path: '#hero', sectionId: 'hero' },
    { key: 'features', label: t('features'), path: '#features', sectionId: 'features' },
    { key: 'learning', label: t('learning'), path: '#learning', sectionId: 'learning' },
    { key: 'community', label: t('community'), path: '#community', sectionId: 'community' },
    { key: 'pricing', label: t('pricing'), path: '#pricing', sectionId: 'pricing' },
  ]

  const NAV_ITEMS = [
    {
      key: 'learning',
      label: t('learning'),
      isDropdown: true,
      children: [
        { key: 'vocabulary', label: t('vocabulary'), path: '/decks'},
        { key: 'lessons', label: t('lessons'), path: '/learning'},
      ],
    },
    { key: 'battle', label: t('battle'), path: '/battle' },
    { key: 'community', label: t('community'), path: '/community' },
    { key: 'gamification', label: t('gamification'), path: '/gamification' },
  ]

  const isPathActive = (path) => currentPath === path || currentPath.startsWith(path + '/')

  const isLearningActive =
    isPathActive('/learning') ||
    isPathActive('/decks') ||
    isPathActive('/review') ||
    isPathActive('/flashcard')

  const isItemActive = (item) => {
    if (item.isDropdown) {
      return item.children?.some((child) => isSubItemActive(child)) || isLearningActive
    }
    return isPathActive(item.path)
  }

  const isSubItemActive = (subItem) => {
    if (subItem.key === 'vocabulary') {
      return isPathActive('/decks') || isPathActive('/review') || isPathActive('/flashcard')
    }
    return isPathActive(subItem.path)
  }

  // ── Đóng dropdown khi click outside hoặc ấn Escape ──
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsUserDropdownOpen(false)
      }
      if (learningDropdownRef.current && !learningDropdownRef.current.contains(event.target)) {
        setIsLearningDropdownOpen(false)
      }
    }

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setIsUserDropdownOpen(false)
        setIsLearningDropdownOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [])

  // ── Đóng mobile menu & dropdown khi navigate ──
  useEffect(() => {
    setIsMenuOpen(false)
    setIsLearningDropdownOpen(false)
    setIsUserDropdownOpen(false)
  }, [currentPath])

  // ── Tự động mở submenu Học tập trên mobile khi đang ở trang học tập ──
  useEffect(() => {
    if (isMenuOpen && isLearningActive) {
      setIsMobileLearningOpen(true)
    }
  }, [isMenuOpen, isLearningActive])

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
          {(user ? NAV_ITEMS : currentPath === '/' ? LANDING_NAV_ITEMS : []).map((item) => {
            if (item.isDropdown) {
              const active = isItemActive(item)
              return (
                <div
                  key={item.key}
                  className="header__nav-item header__nav-item--dropdown"
                  ref={learningDropdownRef}
                >
                  <button
                    type="button"
                    className={`header__nav-link header__nav-dropdown-trigger ${active ? 'header__nav-link--active' : ''}`}
                    onClick={() => setIsLearningDropdownOpen((prev) => !prev)}
                    aria-expanded={isLearningDropdownOpen}
                    aria-haspopup="true"
                  >
                    <span>{item.label}</span>
                    <span
                      className={`material-symbols-outlined header__nav-chevron ${isLearningDropdownOpen ? 'header__nav-chevron--open' : ''}`}
                      aria-hidden="true"
                    >
                      keyboard_arrow_down
                    </span>
                  </button>

                  {isLearningDropdownOpen && (
                    <div className="header__dropdown-menu header__nav-dropdown glass-card">
                      {item.children.map((subItem) => (
                        <a
                          key={subItem.key}
                          href={subItem.path}
                          className={`header__dropdown-item ${isSubItemActive(subItem) ? 'header__dropdown-item--active' : ''}`}
                          onClick={(e) => {
                            e.preventDefault()
                            setIsLearningDropdownOpen(false)
                            handleNav(subItem.path)
                          }}
                        >
                          <span className="material-symbols-outlined" aria-hidden="true">{subItem.icon}</span>
                          <span>{subItem.label}</span>
                        </a>
                      ))}
                    </div>
                  )}
                </div>
              )
            }

            return (
              <a
                key={item.key}
                href={item.path}
                className={`header__nav-link ${(user && isPathActive(item.path)) || (!user && item.sectionId === activeLandingSection) ? 'header__nav-link--active' : ''}`}
                onClick={user ? (e) => { e.preventDefault(); handleNav(item.path) } : (e) => handleLandingNav(e, item)}
              >
                {item.label}
              </a>
            )
          })}
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
          {NAV_ITEMS.map((item) => {
            if (item.isDropdown) {
              const active = isItemActive(item)
              return (
                <div key={item.key} className="header__mobile-group">
                  <button
                    type="button"
                    className={`header__mobile-nav-link header__mobile-dropdown-btn ${active ? 'header__mobile-nav-link--active' : ''}`}
                    onClick={() => setIsMobileLearningOpen((prev) => !prev)}
                    aria-expanded={isMobileLearningOpen}
                  >
                    <span>{item.label}</span>
                    <span
                      className={`material-symbols-outlined header__mobile-chevron ${isMobileLearningOpen ? 'header__mobile-chevron--open' : ''}`}
                      aria-hidden="true"
                    >
                      keyboard_arrow_down
                    </span>
                  </button>
                  {isMobileLearningOpen && (
                    <div className="header__mobile-submenu">
                      {item.children.map((subItem) => (
                        <a
                          key={subItem.key}
                          href={subItem.path}
                          className={`header__mobile-submenu-link ${isSubItemActive(subItem) ? 'header__mobile-submenu-link--active' : ''}`}
                          onClick={(e) => {
                            e.preventDefault()
                            handleNav(subItem.path)
                          }}
                        >
                          <span className="material-symbols-outlined" aria-hidden="true">{subItem.icon}</span>
                          <span>{subItem.label}</span>
                        </a>
                      ))}
                    </div>
                  )}
                </div>
              )
            }

            return (
              <a
                key={item.key}
                href={item.path}
                className={`header__mobile-nav-link ${isPathActive(item.path) ? 'header__mobile-nav-link--active' : ''}`}
                onClick={(e) => { e.preventDefault(); handleNav(item.path) }}
              >
                {item.label}
              </a>
            )
          })}
        </nav>
      )}
    </header>
  )
}

export default Header
