import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  DashboardIcon,
  BookOpenIcon,
  CreditCardIcon,
  MessageSquareIcon,
  AlertTriangleIcon,
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

  // Quản lý trạng thái mở dropdown của từng nhóm module
  const [openDropdowns, setOpenDropdowns] = useState({
    content: currentPath.startsWith('/admin/decks') || currentPath.startsWith('/admin/tags'),
    billing: false,
    community: false,
    users: currentPath.startsWith('/admin/users') || currentPath.startsWith('/admin/roles'),
    settings: currentPath.startsWith('/admin/settings'),
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
                currentPath.startsWith('/admin/decks') || currentPath.startsWith('/admin/tags')
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
                    currentPath.startsWith('/admin/tags') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/tags')}
                >
                  <span>{t('sidebar.tags')}</span>
                </button>
              </div>
            )}
          </div>

          {/* 3. Premium và thanh toán (dropdown - chưa làm) */}
          <div className={styles.dropdownGroup}>
            <button
              type="button"
              className={`${styles.navItem} ${styles.dropdownTrigger}`}
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
                  className={styles.subMenuItem}
                  onClick={() => handleNav('/admin/plans')}
                >
                  <span>{t('sidebar.subscriptionPlans')}</span>
                  <span className={styles.pendingBadge}>{t('sidebar.pendingBadge')}</span>
                </button>
                <button
                  type="button"
                  className={styles.subMenuItem}
                  onClick={() => handleNav('/admin/transactions')}
                >
                  <span>{t('sidebar.transactions')}</span>
                  <span className={styles.pendingBadge}>{t('sidebar.pendingBadge')}</span>
                </button>
              </div>
            )}
          </div>

          {/* 4. Bài viết và cộng đồng (dropdown - chưa làm) */}
          <div className={styles.dropdownGroup}>
            <button
              type="button"
              className={`${styles.navItem} ${styles.dropdownTrigger}`}
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
                  className={styles.subMenuItem}
                  onClick={() => handleNav('/admin/posts')}
                >
                  <span>{t('sidebar.posts')}</span>
                  <span className={styles.pendingBadge}>{t('sidebar.pendingBadge')}</span>
                </button>
                <button
                  type="button"
                  className={styles.subMenuItem}
                  onClick={() => handleNav('/admin/comments')}
                >
                  <span>{t('sidebar.comments')}</span>
                  <span className={styles.pendingBadge}>{t('sidebar.pendingBadge')}</span>
                </button>
                <button
                  type="button"
                  className={styles.subMenuItem}
                  onClick={() => handleNav('/admin/groups')}
                >
                  <span>{t('sidebar.groups')}</span>
                  <span className={styles.pendingBadge}>{t('sidebar.pendingBadge')}</span>
                </button>
              </div>
            )}
          </div>

          {/* 5. Báo cáo (chưa làm) */}
          <button
            type="button"
            className={`${styles.navItem} ${currentPath === '/admin/reports' ? styles.active : ''}`}
            onClick={() => handleNav('/admin/reports')}
            title={t('sidebar.reports')}
          >
            <AlertTriangleIcon size={18} className={styles.navIcon} />
            {!isCollapsed && (
              <>
                <span className={styles.navLabel}>{t('sidebar.reports')}</span>
                <span className={styles.pendingBadge}>{t('sidebar.pendingBadge')}</span>
              </>
            )}
          </button>

          {/* 6. Người dùng (dropdown) */}
          <div className={styles.dropdownGroup}>
            <button
              type="button"
              className={`${styles.navItem} ${styles.dropdownTrigger} ${
                currentPath.startsWith('/admin/users') || currentPath.startsWith('/admin/roles')
                  ? styles.active
                  : ''
              }`}
              onClick={() => toggleDropdown('users')}
              title={t('sidebar.users')}
            >
              <UsersIcon size={18} className={styles.navIcon} />
              {!isCollapsed && (
                <>
                  <span className={styles.navLabel}>{t('sidebar.users')}</span>
                  <span className={styles.chevron}>
                    {openDropdowns.users ? (
                      <ChevronDownIcon size={14} />
                    ) : (
                      <ChevronRightIcon size={14} />
                    )}
                  </span>
                </>
              )}
            </button>
            {!isCollapsed && openDropdowns.users && (
              <div className={styles.subMenuList}>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    currentPath.startsWith('/admin/users') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/users')}
                >
                  <span>{t('sidebar.userList')}</span>
                </button>
                <button
                  type="button"
                  className={`${styles.subMenuItem} ${
                    currentPath.startsWith('/admin/roles') ? styles.active : ''
                  }`}
                  onClick={() => handleNav('/admin/roles')}
                >
                  <span>{t('sidebar.rolesAndPermissions')}</span>
                  <span className={styles.pendingBadge}>{t('sidebar.pendingBadge')}</span>
                </button>
              </div>
            )}
          </div>

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
