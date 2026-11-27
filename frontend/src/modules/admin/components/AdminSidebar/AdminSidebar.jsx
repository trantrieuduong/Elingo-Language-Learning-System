import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  DashboardIcon,
  BookOpenIcon,
  CreditCardIcon,
  MessageSquareIcon,
  UsersIcon,
  SettingsIcon,
  ChevronDownIcon,
  ChevronRightIcon,
  CloseIcon,
  ChevronLeftIcon,
} from '../icons/AdminIcons';
import styles from './AdminSidebar.module.css';

const NAV_ITEMS = [
  {
    type: 'link',
    key: 'dashboard',
    path: '/admin',
    labelKey: 'sidebar.dashboard',
    Icon: DashboardIcon,
    isActive: (path) => path === '/admin' || path === '/admin/dashboard',
  },
  {
    type: 'group',
    key: 'content',
    labelKey: 'sidebar.contentAndLearning',
    Icon: BookOpenIcon,
    matchPrefixes: ['/admin/decks', '/admin/lessons', '/admin/tags'],
    items: [
      { path: '/admin/decks', labelKey: 'sidebar.decks', matchPrefix: '/admin/decks' },
      { path: '/admin/lessons', labelKey: 'sidebar.lessons', matchPrefix: '/admin/lessons' },
      { path: '/admin/tags', labelKey: 'sidebar.tags', matchPrefix: '/admin/tags' },
    ],
  },
  {
    type: 'group',
    key: 'billing',
    labelKey: 'sidebar.premiumAndBilling',
    Icon: CreditCardIcon,
    matchPrefixes: ['/admin/plans', '/admin/transactions', '/admin/webhook-logs'],
    items: [
      { path: '/admin/plans', labelKey: 'sidebar.subscriptionPlans', matchPrefix: '/admin/plans' },
      { path: '/admin/transactions', labelKey: 'sidebar.transactions', matchPrefix: '/admin/transactions' },
      { path: '/admin/webhook-logs', labelKey: 'sidebar.webhookLogs', matchPrefix: '/admin/webhook-logs' },
    ],
  },
  {
    type: 'group',
    key: 'community',
    labelKey: 'sidebar.communityAndPosts',
    Icon: MessageSquareIcon,
    matchPrefixes: ['/admin/posts', '/admin/reports', '/admin/groups'],
    items: [
      { path: '/admin/posts', labelKey: 'sidebar.posts', matchPrefix: '/admin/posts' },
      { path: '/admin/reports', labelKey: 'sidebar.reports', matchPrefix: '/admin/reports' },
      { path: '/admin/groups', labelKey: 'sidebar.groups', matchPrefix: '/admin/groups' },
    ],
  },
  {
    type: 'link',
    key: 'users',
    path: '/admin/users',
    labelKey: 'sidebar.users',
    Icon: UsersIcon,
    isActive: (path) => path.startsWith('/admin/users'),
  },
  {
    type: 'group',
    key: 'settings',
    labelKey: 'sidebar.systemSettings',
    Icon: SettingsIcon,
    matchPrefixes: ['/admin/settings'],
    items: [
      {
        path: '/admin/settings/configs',
        labelKey: 'sidebar.systemConfigs',
        matchExact: ['/admin/settings', '/admin/settings/configs'],
      },
      {
        path: '/admin/settings/audit-logs',
        labelKey: 'sidebar.auditLogs',
        matchExact: ['/admin/settings/audit-logs'],
      },
    ],
  },
];

const matchesGroup = (path, prefixes = []) => prefixes.some((prefix) => path.startsWith(prefix));

const isSubItemActive = (path, subItem) => {
  if (subItem.matchExact) {
    return subItem.matchExact.includes(path);
  }
  return subItem.matchPrefix ? path.startsWith(subItem.matchPrefix) : false;
};

export default function AdminSidebar({
  currentPath,
  onNavigate,
  isCollapsed,
  onToggleCollapse,
  onExpand,
  isMobileOpen,
  onCloseMobile,
}) {
  const { t } = useTranslation('admin');
  const normalizedPath = currentPath.split('?')[0];

  const [openDropdowns, setOpenDropdowns] = useState(() =>
    NAV_ITEMS.reduce((acc, item) => {
      if (item.type === 'group') {
        acc[item.key] = matchesGroup(normalizedPath, item.matchPrefixes);
      }
      return acc;
    }, {}),
  );

  const toggleDropdown = (key) => {
    if (isCollapsed) {
      if (onExpand) {
        onExpand();
      } else if (onToggleCollapse) {
        onToggleCollapse();
      }
      setOpenDropdowns((prev) => ({ ...prev, [key]: true }));
      return;
    }

    setOpenDropdowns((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const handleNav = (path) => {
    onNavigate(path);
    if (onCloseMobile) onCloseMobile();
  };

  return (
    <>
      {isMobileOpen && (
        <div className={styles.mobileBackdrop} onClick={onCloseMobile} aria-hidden="true" />
      )}

      <aside
        className={`${styles.sidebar} ${isCollapsed ? styles.collapsed : ''} ${
          isMobileOpen ? styles.mobileOpen : ''
        }`}
      >
        <div className={styles.brandHeader}>
          <div className={styles.brandLogoBox}>E</div>
          {!isCollapsed && (
            <div className={styles.brandInfo}>
              <span className={styles.brandTitle}>{t('brand')}</span>
              <span className={styles.brandSubtitle}>{t('brandSub')}</span>
            </div>
          )}
          <button
            type="button"
            className={styles.closeMobileBtn}
            onClick={onCloseMobile}
            aria-label={t('sidebar.closeMobile')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <nav className={styles.navContainer} aria-label={t('sidebar.navAria')}>
          {NAV_ITEMS.map((entry) => {
            const IconComponent = entry.Icon;

            if (entry.type === 'link') {
              const active = entry.isActive(normalizedPath);
              return (
                <button
                  key={entry.key}
                  type="button"
                  className={`${styles.navItem} ${active ? styles.active : ''}`}
                  onClick={() => handleNav(entry.path)}
                  title={t(entry.labelKey)}
                >
                  <IconComponent size={18} className={styles.navIcon} />
                  {!isCollapsed && <span className={styles.navLabel}>{t(entry.labelKey)}</span>}
                </button>
              );
            }

            const groupActive = matchesGroup(normalizedPath, entry.matchPrefixes);
            const isOpen = Boolean(openDropdowns[entry.key]);

            return (
              <div key={entry.key} className={styles.dropdownGroup}>
                <button
                  type="button"
                  className={`${styles.navItem} ${styles.dropdownTrigger} ${
                    groupActive ? styles.active : ''
                  }`}
                  onClick={() => toggleDropdown(entry.key)}
                  title={t(entry.labelKey)}
                >
                  <IconComponent size={18} className={styles.navIcon} />
                  {!isCollapsed && (
                    <>
                      <span className={styles.navLabel}>{t(entry.labelKey)}</span>
                      <span className={styles.chevron}>
                        {isOpen ? <ChevronDownIcon size={14} /> : <ChevronRightIcon size={14} />}
                      </span>
                    </>
                  )}
                </button>
                {!isCollapsed && isOpen && (
                  <div className={styles.subMenuList}>
                    {entry.items.map((subItem) => (
                      <button
                        key={subItem.path}
                        type="button"
                        className={`${styles.subMenuItem} ${
                          isSubItemActive(normalizedPath, subItem) ? styles.active : ''
                        }`}
                        onClick={() => handleNav(subItem.path)}
                      >
                        <span>{t(subItem.labelKey)}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </nav>

        <div className={styles.sidebarFooter}>
          <button
            type="button"
            className={styles.collapseBtn}
            onClick={onToggleCollapse}
            title={isCollapsed ? t('sidebar.expandSidebar') : t('sidebar.collapseSidebar')}
            aria-label={isCollapsed ? t('sidebar.expandSidebar') : t('sidebar.collapseSidebar')}
          >
            {isCollapsed ? <ChevronRightIcon size={16} /> : <ChevronLeftIcon size={16} />}
            {!isCollapsed && (
              <span className={styles.collapseLabel}>{t('sidebar.collapseSidebar')}</span>
            )}
          </button>
        </div>
      </aside>
    </>
  );
}

AdminSidebar.propTypes = {
  currentPath: PropTypes.string.isRequired,
  onNavigate: PropTypes.func.isRequired,
  isCollapsed: PropTypes.bool.isRequired,
  onToggleCollapse: PropTypes.func.isRequired,
  onExpand: PropTypes.func,
  isMobileOpen: PropTypes.bool.isRequired,
  onCloseMobile: PropTypes.func.isRequired,
};
