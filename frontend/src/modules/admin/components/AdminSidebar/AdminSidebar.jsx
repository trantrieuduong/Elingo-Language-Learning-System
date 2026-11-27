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

  // Quản lý trạng thái mở dropdown của từng nhóm module
  const [openDropdowns, setOpenDropdowns] = useState({
    content:
      normalizedPath.startsWith('/admin/decks') ||
      normalizedPath.startsWith('/admin/lessons') ||
      normalizedPath.startsWith('/admin/tags'),
    billing:
      normalizedPath.startsWith('/admin/plans') ||
      normalizedPath.startsWith('/admin/transactions') ||
      normalizedPath.startsWith('/admin/webhook-logs'),
    community:
      normalizedPath.startsWith('/admin/posts') ||
      normalizedPath.startsWith('/admin/reports') ||
      normalizedPath.startsWith('/admin/groups'),
    settings: normalizedPath.startsWith('/admin/settings'),
  });

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

  const isDashboardActive = currentPath === '/admin' || currentPath === '/admin/dashboard';

  const handleNav = (path) => {
    onNavigate(path);
    if (onCloseMobile) onCloseMobile();
  };

  return (
    <>
      {/* Backdrop trên màn hình di động */}
      {isMobileOpen && (
        <div className={styles.mobileBackdrop} onClick={onCloseMobile} aria-hidden="true" />
      )}

      <aside
        className={`${styles.sidebar} ${isCollapsed ? styles.collapsed : ''} ${
          isMobileOpen ? styles.mobileOpen : ''
        }`}
      >
        {/* Brand Header */}
        <div className={styles.brandHeader}>
          <div className={styles.brandLogoBox}>E</div>
          {!isCollapsed && (
            <div className={styles.brandInfo}>
              <span className={styles.brandTitle}>{t('brand')}</span>
              <span className={styles.brandSubtitle}>{t('brandSub')}</span>
            </div>
          )}
          {/* Nút đóng trên mobile */}
          <button
            type="button"
            className={styles.closeMobileBtn}
            onClick={onCloseMobile}
            aria-label={t('sidebar.closeMobile')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        {/* Navigation List */}
        <nav className={styles.navContainer} aria-label={t('sidebar.navAria')}>
          {/* 1. Dashboard */}
          <button
            type="button"
            className={`${styles.navItem} ${isDashboardActive ? styles.active : ''}`}
            onClick={() => handleNav('/admin')}
            title={t('sidebar.dashboard')}
          >
            <DashboardIcon size={18} className={styles.navIcon} />
            {!isCollapsed && <span className={styles.navLabel}>{t('sidebar.dashboard')}</span>}
          </button>

          {/* 2. Nội dung và học tập */}
          <div className={styles.dropdownGroup}>
            <button
              type="button"
              className={`${styles.navItem} ${styles.dropdownTrigger} ${
                currentPath.startsWith('/admin/decks') ||
                currentPath.startsWith('/admin/lessons') ||
                currentPath.startsWith('/admin/tags')
                  ? styles.active
                  : ''
              }`}
              onClick={() => toggleDropdown('content')}
              title={t('sidebar.contentAndLearning')}
            >
              <BookOpenIcon size={18} className={styles.navIcon} />
              {!isCollapsed && (
                <>
                  <span className={styles.navLabel}>{t('sidebar.contentAndLearning')}</span>
                  <span className={styles.chevron}>
                    {openDropdowns.content ? (
                      <ChevronDownIcon size={14} />
                    ) : (
                      <ChevronRightIcon size={14} />
                    )}
                  </span>
                </>
              )}
            </button>
            {!isCollapsed && openDropdowns.content && (
              <div className={styles.subMenuList}>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    currentPath.startsWith('/admin/decks') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/decks')}
                >
                  <span>{t('sidebar.decks')}</span>
                </button>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    currentPath.startsWith('/admin/lessons') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/lessons')}
                >
                  <span>{t('sidebar.lessons')}</span>
                </button>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    currentPath.startsWith('/admin/tags') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/tags')}
                >
                  <span>{t('sidebar.tags')}</span>
                </button>
              </div>
            )}
          </div>

          {/* 3. Premium và thanh toán */}
          <div className={styles.dropdownGroup}>
            <button
              type="button"
              className={`${styles.navItem} ${styles.dropdownTrigger} ${
                normalizedPath.startsWith('/admin/plans') ||
                normalizedPath.startsWith('/admin/transactions') ||
                normalizedPath.startsWith('/admin/webhook-logs')
                  ? styles.active
                  : ''
              }`}
              onClick={() => toggleDropdown('billing')}
              title={t('sidebar.premiumAndBilling')}
            >
              <CreditCardIcon size={18} className={styles.navIcon} />
              {!isCollapsed && (
                <>
                  <span className={styles.navLabel}>{t('sidebar.premiumAndBilling')}</span>
                  <span className={styles.chevron}>
                    {openDropdowns.billing ? (
                      <ChevronDownIcon size={14} />
                    ) : (
                      <ChevronRightIcon size={14} />
                    )}
                  </span>
                </>
              )}
            </button>
            {!isCollapsed && openDropdowns.billing && (
              <div className={styles.subMenuList}>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    normalizedPath.startsWith('/admin/plans') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/plans')}
                >
                  <span>{t('sidebar.subscriptionPlans')}</span>
                </button>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    normalizedPath.startsWith('/admin/transactions') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/transactions')}
                >
                  <span>{t('sidebar.transactions')}</span>
                </button>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    normalizedPath.startsWith('/admin/webhook-logs') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/webhook-logs')}
                >
                  <span>{t('sidebar.webhookLogs')}</span>
                </button>
              </div>
            )}
          </div>

          {/* 4. Bài viết và cộng đồng (gồm Kiểm duyệt bài viết & Báo cáo vi phạm) */}
          <div className={styles.dropdownGroup}>
            <button
              type="button"
              className={`${styles.navItem} ${styles.dropdownTrigger} ${
                normalizedPath.startsWith('/admin/posts') ||
                normalizedPath.startsWith('/admin/reports') ||
                normalizedPath.startsWith('/admin/groups')
                  ? styles.active
                  : ''
              }`}
              onClick={() => toggleDropdown('community')}
              title={t('sidebar.communityAndPosts')}
            >
              <MessageSquareIcon size={18} className={styles.navIcon} />
              {!isCollapsed && (
                <>
                  <span className={styles.navLabel}>{t('sidebar.communityAndPosts')}</span>
                  <span className={styles.chevron}>
                    {openDropdowns.community ? (
                      <ChevronDownIcon size={14} />
                    ) : (
                      <ChevronRightIcon size={14} />
                    )}
                  </span>
                </>
              )}
            </button>
            {!isCollapsed && openDropdowns.community && (
              <div className={styles.subMenuList}>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    normalizedPath.startsWith('/admin/posts') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/posts')}
                >
                  <span>{t('sidebar.posts')}</span>
                </button>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    normalizedPath.startsWith('/admin/reports') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/reports')}
                >
                  <span>{t('sidebar.reports')}</span>
                </button>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    normalizedPath.startsWith('/admin/groups') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/groups')}
                >
                  <span>{t('sidebar.groups')}</span>
                </button>
              </div>
            )}
          </div>

          {/* 5. Người dùng */}
          <button
            type="button"
            className={`${styles.navItem} ${
              normalizedPath.startsWith('/admin/users') ? styles.active : ''
            }`}
            onClick={() => handleNav('/admin/users')}
            title={t('sidebar.users')}
          >
            <UsersIcon size={18} className={styles.navIcon} />
            {!isCollapsed && <span className={styles.navLabel}>{t('sidebar.users')}</span>}
          </button>

          {/* 7. Cấu hình hệ thống (dropdown) */}
          <div className={styles.dropdownGroup}>
            <button
              type="button"
              className={`${styles.navItem} ${styles.dropdownTrigger} ${
                currentPath.startsWith('/admin/settings') ? styles.active : ''
              }`}
              onClick={() => toggleDropdown('settings')}
              title={t('sidebar.systemSettings')}
            >
              <SettingsIcon size={18} className={styles.navIcon} />
              {!isCollapsed && (
                <>
                  <span className={styles.navLabel}>{t('sidebar.systemSettings')}</span>
                  <span className={styles.chevron}>
                    {openDropdowns.settings ? (
                      <ChevronDownIcon size={14} />
                    ) : (
                      <ChevronRightIcon size={14} />
                    )}
                  </span>
                </>
              )}
            </button>
            {!isCollapsed && openDropdowns.settings && (
              <div className={styles.subMenuList}>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    currentPath === '/admin/settings' || currentPath === '/admin/settings/configs'
                      ? styles.active
                      : ''
                  }`}
                  onClick={() => handleNav('/admin/settings/configs')}
                >
                  <span>{t('sidebar.systemConfigs')}</span>
                </button>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    currentPath === '/admin/settings/audit-logs' ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/settings/audit-logs')}
                >
                  <span>{t('sidebar.auditLogs')}</span>
                </button>
              </div>
            )}
          </div>
        </nav>

        {/* Footer Toggle Collapse (Desktop only) */}
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
