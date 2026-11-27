import React, { useMemo, useState, useRef, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../../context/AuthContext';
import {
  MenuIcon,
  SunIcon,
  MoonIcon,
  GlobeIcon,
  ExternalLinkIcon,
  ChevronDownIcon,
  LogOutIcon,
} from '../icons/AdminIcons';
import styles from './AdminHeader.module.css';

const SEGMENT_KEY_MAP = {
  dashboard: 'sidebar.dashboard',
  decks: 'sidebar.decks',
  tags: 'sidebar.tags',
  lessons: 'sidebar.lessons',
  topics: 'routes.topics',
  plans: 'sidebar.subscriptionPlans',
  transactions: 'sidebar.transactions',
  'webhook-logs': 'sidebar.webhookLogs',
  posts: 'sidebar.posts',
  comments: 'sidebar.comments',
  groups: 'sidebar.groups',
  reports: 'sidebar.reports',
  users: 'sidebar.users',
  roles: 'sidebar.rolesAndPermissions',
  settings: 'sidebar.systemSettings',
};

export default function AdminHeader({
  currentPath = '/admin',
  theme,
  onToggleTheme,
  onOpenMobileSidebar,
  onNavigate,
}) {
  const { t, i18n } = useTranslation('admin');
  const { user, logout } = useAuth();
  const currentLang = i18n.language || 'vi';

  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  // Đóng dropdown khi click ngoài hoặc bấm Escape
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsDropdownOpen(false);
      }
    };
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setIsDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  const handleToggleLang = () => {
    const nextLang = currentLang.startsWith('vi') ? 'en' : 'vi';
    i18n.changeLanguage(nextLang);
  };

  const handleBackToLearnerApp = () => {
    setIsDropdownOpen(false);
    onNavigate('/dashboard');
  };

  const handleLogout = async () => {
    setIsDropdownOpen(false);
    await logout();
    onNavigate('/login');
  };

  const displayName = user?.fullName || user?.username || t('header.adminBadge');
  const avatarInitial = (displayName || 'A').trim().charAt(0).toUpperCase();

  // Phân tích đường dẫn thành các phần breadcrumb
  const breadcrumbs = useMemo(() => {
    const crumbs = [{ label: 'Elingo', path: '/admin' }];

    if (
      !currentPath ||
      currentPath === '/admin' ||
      currentPath === '/admin/' ||
      currentPath === '/admin/dashboard'
    ) {
      crumbs.push({
        label: t('sidebar.dashboard'),
        path: '/admin',
      });
      return crumbs;
    }

    // Khớp trường hợp lồng: /admin/decks/:deckId/topics/:topicId
    const nestedTopicMatch = currentPath.match(/^\/admin\/decks\/(\d+)\/topics\/(\d+)$/);
    if (nestedTopicMatch) {
      const deckId = nestedTopicMatch[1];
      const topicId = nestedTopicMatch[2];
      crumbs.push({
        label: t('sidebar.decks'),
        path: '/admin/decks',
      });
      crumbs.push({
        label: `#${deckId}`,
        path: `/admin/decks/${deckId}`,
      });
      crumbs.push({
        label: `#${topicId}`,
        path: `/admin/decks/${deckId}/topics/${topicId}`,
      });
      return crumbs;
    }

    // Khớp trường hợp: /admin/decks/:deckId
    const deckDetailMatch = currentPath.match(/^\/admin\/decks\/(\d+)$/);
    if (deckDetailMatch) {
      const deckId = deckDetailMatch[1];
      crumbs.push({
        label: t('sidebar.decks'),
        path: '/admin/decks',
      });
      crumbs.push({
        label: `#${deckId}`,
        path: `/admin/decks/${deckId}`,
      });
      return crumbs;
    }

    // Khớp trường hợp subpath: /admin/decks/:deckId/topics (không có topicId) -> fallback về /admin/decks/:deckId
    const deckTopicsFallbackMatch = currentPath.match(/^\/admin\/decks\/(\d+)\/topics\/?$/);
    if (deckTopicsFallbackMatch) {
      const deckId = deckTopicsFallbackMatch[1];
      crumbs.push({
        label: t('sidebar.decks'),
        path: '/admin/decks',
      });
      crumbs.push({
        label: `#${deckId}`,
        path: `/admin/decks/${deckId}`,
      });
      return crumbs;
    }

    const cleanPath = currentPath.split('?')[0];
    const segments = cleanPath
      .replace(/^\/admin\/?/, '')
      .split('/')
      .filter(Boolean);
    let accumulatedPath = '/admin';

    segments.forEach((seg) => {
      accumulatedPath += `/${seg}`;
      const i18nKey = SEGMENT_KEY_MAP[seg];
      let label = seg;

      if (i18nKey) {
        label = t(i18nKey);
      } else if (!isNaN(Number(seg))) {
        label = `#${seg}`;
      }

      crumbs.push({
        label,
        path: accumulatedPath,
      });
    });

    return crumbs;
  }, [currentPath, t]);

  return (
    <header className={styles.header}>
      {/* Mobile Menu Button */}
      <button
        type="button"
        className={styles.menuBtn}
        onClick={onOpenMobileSidebar}
        aria-label={t('header.openMobile')}
      >
        <MenuIcon size={20} />
      </button>

      {/* Dynamic Interactive Breadcrumb */}
      <nav aria-label="Breadcrumb" className={styles.breadcrumbNav}>
        <ol className={styles.breadcrumbList}>
          {breadcrumbs.map((crumb, idx) => {
            const isLast = idx === breadcrumbs.length - 1;

            return (
              <li key={crumb.path + idx} className={styles.breadcrumbItem}>
                {idx > 0 && (
                  <span className={styles.breadcrumbDivider} aria-hidden="true">
                    /
                  </span>
                )}
                {isLast ? (
                  <span
                    className={`${styles.breadcrumbBtn} ${styles.breadcrumbCurrent}`}
                    aria-current="page"
                  >
                    {crumb.label}
                  </span>
                ) : (
                  <button
                    type="button"
                    className={styles.breadcrumbBtn}
                    onClick={() => onNavigate && onNavigate(crumb.path)}
                  >
                    {crumb.label}
                  </button>
                )}
              </li>
            );
          })}
        </ol>
      </nav>

      {/* Actions */}
      <div className={styles.actionGroup}>
        {/* Toggle Language EN/VI */}
        <button
          type="button"
          className={styles.iconBtn}
          onClick={handleToggleLang}
          title={t('header.switchLang')}
          aria-label={t('header.switchLang')}
        >
          <GlobeIcon size={16} />
          <span className={styles.langLabel}>{currentLang.startsWith('vi') ? 'VI' : 'EN'}</span>
        </button>

        {/* Toggle Theme Light/Dark */}
        <button
          type="button"
          className={styles.iconBtn}
          onClick={onToggleTheme}
          title={t('header.switchTheme')}
          aria-label={t('header.switchTheme')}
        >
          {theme === 'dark' ? <SunIcon size={16} /> : <MoonIcon size={16} />}
        </button>

        <div className={styles.divider} />

        {/* Admin Identity Chip Dropdown */}
        <div className={styles.chipWrapper} ref={dropdownRef}>
          <button
            type="button"
            className={`${styles.adminProfileChip} ${isDropdownOpen ? styles.chipActive : ''}`}
            onClick={() => setIsDropdownOpen((prev) => !prev)}
            aria-haspopup="menu"
            aria-expanded={isDropdownOpen}
            title={displayName}
          >
            <div className={styles.adminAvatarBox}>
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt={displayName} className={styles.adminAvatarImg} />
              ) : (
                <span className={styles.adminAvatarText}>{avatarInitial}</span>
              )}
            </div>
            <span className={styles.adminNameText}>{displayName}</span>
            <span className={`${styles.chevronIcon} ${isDropdownOpen ? styles.chevronRotated : ''}`}>
              <ChevronDownIcon size={13} />
            </span>
          </button>

          {isDropdownOpen && (
            <div className={styles.profileDropdown} role="menu">
              {/* User overview section */}
              <div className={styles.dropdownUserInfo}>
                <span className={styles.dropdownUserName}>{displayName}</span>
                {user?.email && <span className={styles.dropdownUserEmail}>{user.email}</span>}
                <span className={styles.dropdownUserRoleBadge}>{t('header.adminBadge')}</span>
              </div>

              <div className={styles.dropdownDivider} />

              {/* 1. Link back to learner app */}
              <button
                type="button"
                role="menuitem"
                className={styles.dropdownItem}
                onClick={handleBackToLearnerApp}
              >
                <ExternalLinkIcon size={15} className={styles.itemIcon} />
                <span>{t('header.backToUserSite')}</span>
              </button>

              <div className={styles.dropdownDivider} />

              {/* 2. Log out */}
              <button
                type="button"
                role="menuitem"
                className={`${styles.dropdownItem} ${styles.dangerItem}`}
                onClick={handleLogout}
              >
                <LogOutIcon size={15} className={styles.itemIcon} />
                <span>{t('header.logout')}</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

AdminHeader.propTypes = {
  currentPath: PropTypes.string,
  theme: PropTypes.oneOf(['light', 'dark']).isRequired,
  onToggleTheme: PropTypes.func.isRequired,
  onOpenMobileSidebar: PropTypes.func.isRequired,
  onNavigate: PropTypes.func.isRequired,
};
