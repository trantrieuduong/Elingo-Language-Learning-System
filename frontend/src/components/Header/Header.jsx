import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useNotification } from '../../modules/notification/context/NotificationContext';
import NotificationPanel from '../../modules/notification/components/NotificationPanel/NotificationPanel';
import './Header.css';

/**
 * Header component — glassmorphism navbar, fixed floating style.
 * Nhận onNavigate prop thay vì tự navigate trực tiếp.
 *
 * @param {Function} onNavigate - navigate function từ App.jsx
 * @param {string} currentPath - currentPath state từ App.jsx
 */
function Header({ onNavigate, currentPath }) {
  const { t, i18n } = useTranslation('nav');
  const { user, logout } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const { unreadCount, loadInitialNotifications } = useNotification();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isUserDropdownOpen, setIsUserDropdownOpen] = useState(false);
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const dropdownRef = useRef(null);
  const notificationRef = useRef(null);
  const [openDropdownKey, setOpenDropdownKey] = useState(null);
  const [mobileExpandedKeys, setMobileExpandedKeys] = useState({});
  const navContainerRef = useRef(null);
  const closeTimeoutRef = useRef(null);

  const USER_NAV_ITEMS = [
    {
      key: 'home',
      label: t('home'),
      path: user ? '/dashboard' : '/',
    },
    {
      key: 'features',
      label: t('features'),
      children: [
        {
          key: 'speakingRoom',
          label: t('speakingRoom'),
          path: '/speaking',
          icon: 'record_voice_over',
        },
        { key: 'arena', label: t('arena'), path: '/battle', icon: 'swords' },
      ],
    },
    {
      key: 'learning',
      label: t('learning'),
      children: [
        { key: 'lessons', label: t('lessons'), path: '/lessons', icon: 'menu_book' },
        { key: 'vocabulary', label: t('vocabulary'), path: '/decks', icon: 'style' },
        { key: 'review', label: t('review'), path: '/review', icon: 'replay' },
      ],
    },
    {
      key: 'community',
      label: t('community'),
      children: [
        {
          key: 'postGroups',
          label: t('postGroups'),
          path: '/community/groups',
          icon: 'diversity_3',
        },
        { key: 'feed', label: t('feed'), path: '/community', icon: 'forum' },
        { key: 'myPosts', label: t('myPosts'), path: '/community/my-posts', icon: 'article' },
      ],
    },
    {
      key: 'pricing',
      label: t('pricing'),
      children: [
        {
          key: 'premiumPackages',
          label: t('premiumPackages'),
          path: '/premium',
          icon: 'workspace_premium',
        },
        {
          key: 'mySubscription',
          label: t('mySubscription'),
          path: '/premium/my-subscription',
          icon: 'credit_card',
        },
      ],
    },
  ];

  // ── Đóng dropdown khi click outside hoặc nhấn Esc ──
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsUserDropdownOpen(false);
      }
      if (notificationRef.current && !notificationRef.current.contains(event.target)) {
        setIsNotificationOpen(false);
      }
      if (navContainerRef.current && !navContainerRef.current.contains(event.target)) {
        setOpenDropdownKey(null);
      }
    };
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setIsUserDropdownOpen(false);
        setIsNotificationOpen(false);
        setOpenDropdownKey(null);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  // ── Đóng mobile menu & dropdown khi navigate ──
  useEffect(() => {
    setIsMobileMenuOpen(false);
    setIsNotificationOpen(false);
    if (closeTimeoutRef.current) clearTimeout(closeTimeoutRef.current);
    setOpenDropdownKey(null);
  }, [currentPath]);

  useEffect(() => {
    return () => {
      if (closeTimeoutRef.current) clearTimeout(closeTimeoutRef.current);
    };
  }, []);

  const handleLogout = async () => {
    setIsUserDropdownOpen(false);
    setOpenDropdownKey(null);
    await logout();
    if (onNavigate) onNavigate('/login');
  };

  const handleNav = (path) => {
    setIsMobileMenuOpen(false);
    setOpenDropdownKey(null);
    if (onNavigate) onNavigate(path);
  };

  const isSubItemActive = (subPath, allSiblingPaths = []) => {
    if (currentPath === subPath) return true;
    // Nếu có sibling path dài hơn khớp với currentPath (ví dụ /community/my-posts), thì /community không active
    const hasLongerSiblingMatch = allSiblingPaths.some(
      (otherPath) =>
        otherPath !== subPath &&
        otherPath.length > subPath.length &&
        (currentPath === otherPath || currentPath.startsWith(otherPath + '/'))
    );
    if (hasLongerSiblingMatch) return false;
    return currentPath.startsWith(subPath + '/');
  };

  const isItemActive = (item) => {
    if (item.path) {
      return currentPath === item.path || currentPath.startsWith(item.path + '/');
    }
    if (item.children) {
      const siblingPaths = item.children.map((child) => child.path);
      return item.children.some((child) => isSubItemActive(child.path, siblingPaths));
    }
    return false;
  };

  const handleItemMouseEnter = (key) => {
    if (closeTimeoutRef.current) {
      clearTimeout(closeTimeoutRef.current);
      closeTimeoutRef.current = null;
    }
    setOpenDropdownKey(key);
  };

  const handleItemMouseLeave = () => {
    if (closeTimeoutRef.current) {
      clearTimeout(closeTimeoutRef.current);
    }
    closeTimeoutRef.current = setTimeout(() => {
      setOpenDropdownKey(null);
      closeTimeoutRef.current = null;
    }, 200);
  };

  const toggleDropdown = (key) => {
    if (closeTimeoutRef.current) {
      clearTimeout(closeTimeoutRef.current);
      closeTimeoutRef.current = null;
    }
    setOpenDropdownKey((prev) => (prev === key ? null : key));
  };

  const toggleMobileSubmenu = (key) => {
    setMobileExpandedKeys((prev) => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  const changeLanguage = (lng) => {
    i18n.changeLanguage(lng);
  };

  const currentLanguage = i18n.language || 'vi';

  return (
    <header
      className={`header ${currentPath !== '/' ? 'header--auth' : ''} ${isMobileMenuOpen ? 'header--open' : ''} glass-capsule glass-sheen`}
    >
      <div className="header__inner container">
        {/* Brand */}
        <a
          href="/"
          className="header__brand"
          onClick={(e) => {
            e.preventDefault();
            handleNav('/');
          }}
        >
          <span className="header__brand-name">Elingo</span>
        </a>

        {/* Desktop Navigation */}
        <nav className="header__nav" ref={navContainerRef}>
          {USER_NAV_ITEMS.map((item) => {
            const itemIsActive = isItemActive(item);
            if (item.children) {
              const isOpen = openDropdownKey === item.key;
              return (
                <div
                  key={item.key}
                  className="header__nav-item"
                  onMouseEnter={() => handleItemMouseEnter(item.key)}
                  onMouseLeave={handleItemMouseLeave}
                >
                  <button
                    type="button"
                    className={`header__nav-link header__nav-btn ${itemIsActive ? 'header__nav-link--active' : ''}`}
                    onClick={() => toggleDropdown(item.key)}
                  >
                    <span>{item.label}</span>
                    <span
                      className={`material-symbols-outlined header__nav-arrow ${isOpen ? 'header__nav-arrow--open' : ''}`}
                    >
                      keyboard_arrow_down
                    </span>
                  </button>

                  {isOpen && (
                    <div className="header__nav-dropdown-menu glass-card">
                      {item.children.map((sub) => {
                        const siblingPaths = item.children.map((c) => c.path);
                        const subIsActive = isSubItemActive(sub.path, siblingPaths);
                        return (
                          <a
                            key={sub.key}
                            href={sub.path}
                            className={`header__dropdown-item ${subIsActive ? 'header__dropdown-item--active' : ''}`}
                            onClick={(e) => {
                              e.preventDefault();
                              handleNav(sub.path);
                            }}
                          >
                            {sub.icon && (
                              <span className="material-symbols-outlined">{sub.icon}</span>
                            )}
                            <span>{sub.label}</span>
                          </a>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            }

            return (
              <a
                key={item.key}
                href={item.path}
                className={`header__nav-link ${itemIsActive ? 'header__nav-link--active' : ''}`}
                onClick={(e) => {
                  e.preventDefault();
                  handleNav(item.path);
                }}
              >
                {item.label}
              </a>
            );
          })}
        </nav>

        {/* Right side */}
        <div className="header__right">
          {/* Language switch button */}
          <button
            type="button"
            className="btn-icon header__lang-btn"
            onClick={() => changeLanguage(currentLanguage.startsWith('vi') ? 'en' : 'vi')}
            title={
              currentLanguage.startsWith('vi') ? 'Switch to English' : 'Chuyển sang Tiếng Việt'
            }
          >
            <span className="header__lang-code">
              {currentLanguage.startsWith('vi') ? 'VI' : 'EN'}
            </span>
          </button>

          {/* Theme toggle */}
          <button type="button" className="btn-icon" onClick={toggleTheme}>
            <span className="material-symbols-outlined">{isDark ? 'light_mode' : 'dark_mode'}</span>
          </button>

          {user ? (
            <>
              {/* Notification bell & panel */}
              <div className="header__notification-wrapper" ref={notificationRef}>
                <button
                  type="button"
                  className="btn-icon header__notification-btn"
                  onClick={() => {
                    const nextState = !isNotificationOpen;
                    setIsNotificationOpen(nextState);
                    if (nextState && loadInitialNotifications) {
                      loadInitialNotifications();
                    }
                  }}
                >
                  <span className="material-symbols-outlined">notifications</span>
                  {unreadCount > 0 && (
                    <span className="header__notification-badge badge badge--primary">
                      {unreadCount > 99 ? '99+' : unreadCount}
                    </span>
                  )}
                </button>

                {isNotificationOpen && (
                  <NotificationPanel
                    onClose={() => setIsNotificationOpen(false)}
                    onNavigate={handleNav}
                  />
                )}
              </div>

              {/* User dropdown */}
              <div className="header__user-dropdown" ref={dropdownRef}>
                <button
                  type="button"
                  className="header__avatar btn-icon"
                  onClick={() => setIsUserDropdownOpen((prev) => !prev)}
                >
                  <span className="material-symbols-outlined">account_circle</span>
                </button>

                {isUserDropdownOpen && (
                  <div className="header__dropdown-menu glass-card">
                    <div className="header__dropdown-user">
                      <span className="text-label-lg">{user.fullName ?? user.email}</span>
                      <span
                        className="text-body-sm"
                        style={{ color: 'var(--color-text-disabled)' }}
                      >
                        {user.email}
                      </span>
                    </div>
                    <hr className="header__dropdown-divider" />
                    <a
                      href="/profile"
                      className={`header__dropdown-item${currentPath === '/profile' ? ' header__dropdown-item--active' : ''}`}
                      onClick={(e) => {
                        e.preventDefault();
                        setIsUserDropdownOpen(false);
                        handleNav('/profile');
                      }}
                    >
                      <span className="material-symbols-outlined">person</span>
                      {t('profile')}
                    </a>
                    <a
                      href="/gamification"
                      className={`header__dropdown-item${currentPath === '/gamification' ? ' header__dropdown-item--active' : ''}`}
                      onClick={(e) => {
                        e.preventDefault();
                        setIsUserDropdownOpen(false);
                        handleNav('/gamification');
                      }}
                    >
                      <span className="material-symbols-outlined">insights</span>
                      {t('gamification')}
                    </a>
                    <a
                      href="/premium"
                      className={`header__dropdown-item${currentPath === '/premium' ? ' header__dropdown-item--active' : ''}`}
                      onClick={(e) => {
                        e.preventDefault();
                        setIsUserDropdownOpen(false);
                        handleNav('/premium');
                      }}
                    >
                      <span className="material-symbols-outlined">star</span>
                      {t('premium')}
                    </a>
                    <a
                      href="/reports"
                      className={`header__dropdown-item${currentPath === '/reports' ? ' header__dropdown-item--active' : ''}`}
                      onClick={(e) => {
                        e.preventDefault();
                        setIsUserDropdownOpen(false);
                        handleNav('/reports');
                      }}
                    >
                      <span className="material-symbols-outlined">flag</span>
                      {t('myReports')}
                    </a>
                    {user?.role === 'ADMIN' && (
                      <a
                        href="/admin"
                        className="header__dropdown-item"
                        style={{ color: 'var(--color-primary)', fontWeight: 600 }}
                        onClick={(e) => {
                          e.preventDefault();
                          setIsUserDropdownOpen(false);
                          handleNav('/admin');
                        }}
                      >
                        <span className="material-symbols-outlined">admin_panel_settings</span>
                        {t('adminPortal')}
                      </a>
                    )}
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
                className={
                  currentPath === '/login'
                    ? 'btn-primary header__auth-button--active'
                    : 'btn-secondary'
                }
                onClick={() => handleNav('/login')}
              >
                {t('login')}
              </button>
              <button
                type="button"
                className={
                  currentPath === '/signup'
                    ? 'btn-primary header__auth-button--active'
                    : 'btn-secondary'
                }
                onClick={() => handleNav('/signup')}
              >
                {t('signup')}
              </button>
            </div>
          )}

          {/* Mobile hamburger */}
          <button
            type="button"
            className="header__hamburger btn-icon"
            onClick={() => setIsMobileMenuOpen((prev) => !prev)}
          >
            <span className="material-symbols-outlined">{isMobileMenuOpen ? 'close' : 'menu'}</span>
          </button>
        </div>
      </div>

      {/* Mobile Navigation */}
      {isMobileMenuOpen && (
        <nav className="header__mobile-nav glass-capsule">
          {USER_NAV_ITEMS.map((item) => {
            const itemIsActive = isItemActive(item);
            if (item.children) {
              const isExpanded = mobileExpandedKeys[item.key] ?? itemIsActive;
              return (
                <div key={item.key} className="header__mobile-group">
                  <button
                    type="button"
                    className={`header__mobile-nav-link header__mobile-group-toggle ${itemIsActive ? 'header__mobile-nav-link--active' : ''}`}
                    onClick={() => toggleMobileSubmenu(item.key)}
                  >
                    <span>{item.label}</span>
                    <span
                      className={`material-symbols-outlined header__nav-arrow ${isExpanded ? 'header__nav-arrow--open' : ''}`}
                    >
                      keyboard_arrow_down
                    </span>
                  </button>

                  {isExpanded && (
                    <div className="header__mobile-subnav">
                      {item.children.map((sub) => {
                        const siblingPaths = item.children.map((c) => c.path);
                        const subIsActive = isSubItemActive(sub.path, siblingPaths);
                        return (
                          <a
                            key={sub.key}
                            href={sub.path}
                            className={`header__mobile-subnav-link ${subIsActive ? 'header__mobile-subnav-link--active' : ''}`}
                            onClick={(e) => {
                              e.preventDefault();
                              handleNav(sub.path);
                            }}
                          >
                            {sub.icon && (
                              <span className="material-symbols-outlined">{sub.icon}</span>
                            )}
                            <span>{sub.label}</span>
                          </a>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            }

            return (
              <a
                key={item.key}
                href={item.path}
                className={`header__mobile-nav-link ${itemIsActive ? 'header__mobile-nav-link--active' : ''}`}
                onClick={(e) => {
                  e.preventDefault();
                  handleNav(item.path);
                }}
              >
                {item.label}
              </a>
            );
          })}
        </nav>
      )}
    </header>
  );
}

export default Header;
