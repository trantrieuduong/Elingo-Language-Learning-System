import React, { useState, useEffect, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../../context/AuthContext';
import DataTable from '../../components/DataTable/DataTable';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  UsersIcon,
  RefreshIcon,
  EyeIcon,
  LockIcon,
  UnlockIcon,
  CheckCircleIcon,
  XCircleIcon,
  KeyIcon,
} from '../../components/icons/AdminIcons';
import { searchUsers } from '../../services/adminUserApi';
import AdminPagination from '../../components/AdminPagination/AdminPagination';
import UserFilterBar from './components/UserFilterBar';
import UserDetailModal from './components/UserDetailModal';
import LockUserModal from './components/LockUserModal';
import UnlockUserModal from './components/UnlockUserModal';
import styles from './UserManagementPage.module.css';

export default function UserManagementPage({ onNavigate: _onNavigate }) {
  const { t } = useTranslation('admin');
  const { user: currentAdmin } = useAuth();

  // Filter state
  const [keyword, setKeyword] = useState('');
  const [role, setRole] = useState('');
  const [isVerified, setIsVerified] = useState('');
  const [isActive, setIsActive] = useState('');
  const [createdFrom, setCreatedFrom] = useState('');
  const [createdTo, setCreatedTo] = useState('');

  // Pagination state (1-indexed)
  const [page, setPage] = useState(1);
  const pageSize = 20;

  // Data & loading state
  const [userData, setUserData] = useState({
    content: [],
    page: 1,
    size: 20,
    totalElements: 0,
    totalPages: 1,
    isLast: true,
  });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Success Feedback Toast
  const [toastMessage, setToastMessage] = useState(null);

  // Modals state
  const [detailModal, setDetailModal] = useState({ isOpen: false, user: null });
  const [lockModal, setLockModal] = useState({ isOpen: false, user: null });
  const [unlockModal, setUnlockModal] = useState({ isOpen: false, user: null });

  // Load users callback
  const loadUsers = useCallback(
    async (isManualRefresh = false) => {
      try {
        if (isManualRefresh) {
          setRefreshing(true);
        } else {
          setLoading(true);
        }
        setError(null);

        const params = {
          page,
          size: pageSize,
          sort: 'createdAt,desc',
        };

        if (keyword.trim()) params.keyword = keyword.trim();
        if (role) params.role = role;
        if (isVerified !== '') params.isVerified = isVerified === 'true';
        if (isActive !== '') params.isActive = isActive === 'true';
        if (createdFrom) params.createdFrom = createdFrom;
        if (createdTo) params.createdTo = createdTo;

        const res = await searchUsers(params);
        setUserData(
          res || {
            content: [],
            page: 1,
            size: pageSize,
            totalElements: 0,
            totalPages: 1,
            isLast: true,
          }
        );
      } catch (err) {
        setError(err.message || t('feedback.errorDesc'));
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [page, pageSize, keyword, role, isVerified, isActive, createdFrom, createdTo, t]
  );

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  // Handle Filter Apply (Reset to page 1)
  const handleApplyFilter = () => {
    setPage(1);
  };

  // Handle Filter Reset
  const handleResetFilter = () => {
    setKeyword('');
    setRole('');
    setIsVerified('');
    setIsActive('');
    setCreatedFrom('');
    setCreatedTo('');
    setPage(1);
  };

  // Toast Auto-Dismiss
  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => setToastMessage(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [toastMessage]);

  // Helper check if target user can be modified by current administrator
  const canModifyUser = useCallback(
    (target) => {
      if (!target) return false;
      if (currentAdmin?.id && target.id === currentAdmin.id) return false;
      if (target.role === 'ADMIN') return false;
      return true;
    },
    [currentAdmin]
  );

  // Success handler for Lock
  const handleLockSuccess = (updatedUser) => {
    setUserData((prev) => ({
      ...prev,
      content: prev.content.map((item) =>
        item.id === updatedUser.id ? { ...item, ...updatedUser } : item
      ),
    }));
    setLockModal({ isOpen: false, user: null });
    setToastMessage(t('users.lockModal.success'));
  };

  // Success handler for Unlock
  const handleUnlockSuccess = (updatedUser) => {
    setUserData((prev) => ({
      ...prev,
      content: prev.content.map((item) =>
        item.id === updatedUser.id ? { ...item, ...updatedUser } : item
      ),
    }));
    setUnlockModal({ isOpen: false, user: null });
    setToastMessage(t('users.unlockModal.success'));
  };

  // Define Table Columns
  const columns = useMemo(
    () => [
      {
        key: 'user',
        title: t('users.columns.user'),
        width: '22%',
        render: (_val, record) => {
          const initials = (record.fullName || record.username || 'U')
            .charAt(0)
            .toUpperCase();
          return (
            <div className={styles.userCell}>
              <div className={styles.avatarBox}>
                {record.avatarUrl ? (
                  <img
                    src={record.avatarUrl}
                    alt={record.fullName || record.username}
                    className={styles.avatarImg}
                  />
                ) : (
                  <span className={styles.avatarText}>{initials}</span>
                )}
              </div>
              <div className={styles.userInfo}>
                <div className={styles.nameLine}>
                  <span className={styles.fullName}>{record.fullName || '—'}</span>
                  <span className={styles.userIdBadge}>#{record.id}</span>
                </div>
                <span className={styles.username}>@{record.username}</span>
              </div>
            </div>
          );
        },
      },
      {
        key: 'email',
        title: t('users.columns.email'),
        dataIndex: 'email',
        width: '18%',
        render: (val) => <span className={styles.emailText}>{val || '—'}</span>,
      },
      {
        key: 'role',
        title: t('users.columns.role'),
        dataIndex: 'role',
        width: '9%',
        render: (val) => (
          <span
            className={`${styles.badge} ${
              val === 'ADMIN' ? styles.badgeAdmin : styles.badgeUser
            }`}
          >
            {val}
          </span>
        ),
      },
      {
        key: 'isVerified',
        title: t('users.columns.verified'),
        dataIndex: 'isVerified',
        width: '10%',
        render: (val) => (
          <span
            className={`${styles.badge} ${
              val ? styles.badgeVerified : styles.badgeUnverified
            }`}
          >
            {val ? t('users.badges.verified') : t('users.badges.unverified')}
          </span>
        ),
      },
      {
        key: 'isActive',
        title: t('users.columns.status'),
        dataIndex: 'isActive',
        width: '11%',
        render: (val) => (
          <span
            className={`${styles.badge} ${
              val ? styles.badgeActive : styles.badgeLocked
            }`}
          >
            {val ? (
              <>
                <CheckCircleIcon size={12} />
                {t('users.badges.active')}
              </>
            ) : (
              <>
                <XCircleIcon size={12} />
                {t('users.badges.locked')}
              </>
            )}
          </span>
        ),
      },
      {
        key: 'createdAt',
        title: t('users.columns.createdAt'),
        dataIndex: 'createdAt',
        width: '12%',
        render: (val) => {
          if (!val) return '—';
          return (
            <span className={styles.dateCell} title={val.replace('T', ' ')}>
              {val.substring(0, 10)}
            </span>
          );
        },
      },
      {
        key: 'authMethod',
        title: t('users.columns.authMethod'),
        width: '9%',
        render: (_val, record) => (
          <div className={styles.authTags}>
            {record.hasPassword && (
              <span className={styles.authPill} title={t('users.badges.hasPassword')}>
                <KeyIcon size={12} />
              </span>
            )}
            {record.linkedGoogle && (
              <span className={styles.authPill} title={t('users.badges.googleLinked')}>
                G
              </span>
            )}
          </div>
        ),
      },
      {
        key: 'actions',
        title: t('users.columns.actions'),
        width: '8%',
        align: 'right',
        render: (_val, record) => {
          const canModify = canModifyUser(record);
          const tooltipText = !canModify
            ? currentAdmin?.id === record.id
              ? t('users.actions.cannotModifySelfTooltip')
              : t('users.actions.cannotModifyAdminTooltip')
            : '';

          return (
            <div className={styles.actionsCell}>
              {/* Detail button */}
              <button
                type="button"
                className={styles.actionBtn}
                title={t('users.actions.viewDetail')}
                onClick={() => setDetailModal({ isOpen: true, user: record })}
                aria-label={t('users.actions.viewDetail')}
              >
                <EyeIcon size={16} />
              </button>

              {/* Lock / Unlock button */}
              {record.isActive ? (
                <button
                  type="button"
                  className={`${styles.actionBtn} ${styles.lockBtn} ${
                    !canModify ? styles.disabledBtn : ''
                  }`}
                  title={tooltipText || t('users.actions.lock')}
                  disabled={!canModify}
                  onClick={() => {
                    if (canModify) setLockModal({ isOpen: true, user: record });
                  }}
                  aria-label={t('users.actions.lock')}
                >
                  <LockIcon size={16} />
                </button>
              ) : (
                <button
                  type="button"
                  className={`${styles.actionBtn} ${styles.unlockBtn} ${
                    !canModify ? styles.disabledBtn : ''
                  }`}
                  title={tooltipText || t('users.actions.unlock')}
                  disabled={!canModify}
                  onClick={() => {
                    if (canModify) setUnlockModal({ isOpen: true, user: record });
                  }}
                  aria-label={t('users.actions.unlock')}
                >
                  <UnlockIcon size={16} />
                </button>
              )}
            </div>
          );
        },
      },
    ],
    [t, canModifyUser, currentAdmin]
  );

  return (
    <div className={styles.pageContainer}>
      {/* Toast Alert */}
      {toastMessage && (
        <div className={styles.toast}>
          <CheckCircleIcon size={16} className={styles.toastIcon} />
          <span>{toastMessage}</span>
          <button
            type="button"
            className={styles.toastCloseBtn}
            onClick={() => setToastMessage(null)}
          >
            ×
          </button>
        </div>
      )}

      {/* Page Header */}
      <div className={styles.pageHeader}>
        <div className={styles.headerInfo}>
          <div className={styles.titleRow}>
            <UsersIcon size={24} className={styles.headerIcon} />
            <h1 className={styles.title}>{t('users.title')}</h1>
          </div>
          <p className={styles.subtitle}>{t('users.subtitle')}</p>
        </div>

        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => loadUsers(true)}
            disabled={loading || refreshing}
            title={t('common.refresh')}
          >
            <RefreshIcon
              size={16}
              className={`${styles.refreshIcon} ${refreshing ? styles.spinning : ''}`}
            />
            <span>{t('common.refresh')}</span>
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <UserFilterBar
        keyword={keyword}
        onKeywordChange={setKeyword}
        role={role}
        onRoleChange={setRole}
        isVerified={isVerified}
        onVerifiedChange={setIsVerified}
        isActive={isActive}
        onActiveChange={setIsActive}
        createdFrom={createdFrom}
        onCreatedFromChange={setCreatedFrom}
        createdTo={createdTo}
        onCreatedToChange={setCreatedTo}
        onApply={handleApplyFilter}
        onReset={handleResetFilter}
        loading={loading || refreshing}
      />

      {/* Error state */}
      {error && (
        <AdminErrorState
          title={t('feedback.errorTitle')}
          description={error}
          onRetry={() => loadUsers(false)}
        />
      )}

      {/* Table & Pagination */}
      {!error && (
        <div className={styles.tableCard}>
          <DataTable
            borderless
            columns={columns}
            data={userData.content}
            isLoading={loading}
            emptyMessage={t('feedback.emptyList')}
            keyField="id"
          />

          {/* Admin Pagination component with total user count */}
          <AdminPagination
            page={page}
            totalPages={userData.totalPages}
            totalElements={userData.totalElements}
            isLast={Boolean(userData.isLast ?? userData.last)}
            onPageChange={setPage}
            unitLabel={t('users.usersCountSuffix')}
            isLoading={loading}
          />
        </div>
      )}

      {/* Detail Modal */}
      {detailModal.isOpen && (
        <UserDetailModal
          user={detailModal.user}
          onClose={() => setDetailModal({ isOpen: false, user: null })}
          onOpenLock={(u) => setLockModal({ isOpen: true, user: u })}
          onOpenUnlock={(u) => setUnlockModal({ isOpen: true, user: u })}
          canModify={canModifyUser(detailModal.user)}
        />
      )}

      {/* Lock User Modal */}
      {lockModal.isOpen && (
        <LockUserModal
          user={lockModal.user}
          onClose={() => setLockModal({ isOpen: false, user: null })}
          onSuccess={handleLockSuccess}
        />
      )}

      {/* Unlock User Modal */}
      {unlockModal.isOpen && (
        <UnlockUserModal
          user={unlockModal.user}
          onClose={() => setUnlockModal({ isOpen: false, user: null })}
          onSuccess={handleUnlockSuccess}
        />
      )}
    </div>
  );
}

UserManagementPage.propTypes = {
  onNavigate: PropTypes.func,
};
