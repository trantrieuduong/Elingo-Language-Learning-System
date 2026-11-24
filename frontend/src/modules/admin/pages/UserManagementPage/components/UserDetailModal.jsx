import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  CloseIcon,
  CheckCircleIcon,
  XCircleIcon,
  LockIcon,
  UnlockIcon,
  KeyIcon,
  ClockIcon,
} from '../../../components/icons/AdminIcons';
import styles from './UserDetailModal.module.css';

export default function UserDetailModal({
  user,
  onClose,
  onOpenLock,
  onOpenUnlock,
  canModify = true,
}) {
  const { t } = useTranslation('admin');

  if (!user) return null;

  const formatDate = (isoString) => {
    if (!isoString) return t('users.detailModal.never');
    return isoString.replace('T', ' ').substring(0, 19);
  };

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div
        className={styles.modal}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="user-detail-modal-title"
      >
        {/* Header */}
        <div className={styles.header}>
          <div className={styles.headerText}>
            <h2 id="user-detail-modal-title" className={styles.title}>
              {t('users.detailModal.title')}
            </h2>
            <p className={styles.subtitle}>{t('users.detailModal.subtitle')}</p>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('common.close')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        {/* Body */}
        <div className={styles.body}>
          {/* User Header Card */}
          <div className={styles.userHeaderCard}>
            <div className={styles.avatarWrapper}>
              {user.avatarUrl ? (
                <img
                  src={user.avatarUrl}
                  alt={user.fullName || user.username}
                  className={styles.avatarImg}
                />
              ) : (
                <div className={styles.avatarFallback}>
                  {(user.fullName || user.username || 'U').charAt(0).toUpperCase()}
                </div>
              )}
            </div>
            <div className={styles.userInfo}>
              <div className={styles.nameRow}>
                <span className={styles.fullName}>{user.fullName || '—'}</span>
                <span className={styles.idBadge}>#{user.id}</span>
              </div>
              <span className={styles.username}>@{user.username}</span>
            </div>
          </div>

          {/* Grid Meta Information */}
          <div className={styles.metaGrid}>
            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>{t('users.detailModal.email')}</span>
              <span className={styles.metaValue}>{user.email || '—'}</span>
            </div>

            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>{t('users.detailModal.role')}</span>
              <div>
                <span
                  className={`${styles.badge} ${
                    user.role === 'ADMIN' ? styles.badgeAdmin : styles.badgeUser
                  }`}
                >
                  {user.role}
                </span>
              </div>
            </div>

            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>{t('users.detailModal.accountStatus')}</span>
              <div>
                <span
                  className={`${styles.badge} ${
                    user.isActive ? styles.badgeActive : styles.badgeLocked
                  }`}
                >
                  {user.isActive ? (
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
              </div>
            </div>

            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>{t('users.detailModal.emailVerified')}</span>
              <div>
                <span
                  className={`${styles.badge} ${
                    user.isVerified ? styles.badgeVerified : styles.badgeUnverified
                  }`}
                >
                  {user.isVerified
                    ? t('users.badges.verified')
                    : t('users.badges.unverified')}
                </span>
              </div>
            </div>
          </div>

          {/* Security & Authentication Section */}
          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>
              <KeyIcon size={15} />
              {t('users.detailModal.securitySection')}
            </h3>

            <div className={styles.securityGrid}>
              <div className={styles.securityItem}>
                <span className={styles.metaLabel}>{t('users.detailModal.hasPassword')}</span>
                <span className={styles.metaValue}>
                  {user.hasPassword ? t('users.badges.hasPassword') : t('users.badges.noPassword')}
                </span>
              </div>

              <div className={styles.securityItem}>
                <span className={styles.metaLabel}>{t('users.detailModal.linkedGoogle')}</span>
                <span className={styles.metaValue}>
                  {user.linkedGoogle
                    ? t('users.badges.googleLinked')
                    : t('users.badges.googleNotLinked')}
                </span>
              </div>

              <div className={styles.securityItem}>
                <span className={styles.metaLabel}>
                  {t('users.detailModal.passwordChangedAt')}
                </span>
                <span className={styles.metaValue}>
                  {formatDate(user.passwordChangedAt)}
                </span>
              </div>

              <div className={styles.securityItem}>
                <span className={styles.metaLabel}>
                  {t('users.detailModal.usernameChangedAt')}
                </span>
                <span className={styles.metaValue}>
                  {formatDate(user.usernameChangedAt)}
                </span>
              </div>
            </div>
          </div>

          {/* Account Timestamps Section */}
          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>
              <ClockIcon size={15} />
              {t('users.detailModal.timestampsSection')}
            </h3>

            <div className={styles.securityGrid}>
              <div className={styles.securityItem}>
                <span className={styles.metaLabel}>
                  {t('users.detailModal.createdAt')}
                </span>
                <span className={styles.metaValue}>
                  {formatDate(user.createdAt)}
                </span>
              </div>

              <div className={styles.securityItem}>
                <span className={styles.metaLabel}>
                  {t('users.detailModal.updatedAt')}
                </span>
                <span className={styles.metaValue}>
                  {formatDate(user.updatedAt)}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className={styles.footer}>
          <div className={styles.footerLeft}>
            {canModify && user.isActive && (
              <button
                type="button"
                className={styles.lockActionBtn}
                onClick={() => {
                  onClose();
                  onOpenLock(user);
                }}
              >
                <LockIcon size={14} />
                <span>{t('users.detailModal.lockAction')}</span>
              </button>
            )}
            {canModify && !user.isActive && (
              <button
                type="button"
                className={styles.unlockActionBtn}
                onClick={() => {
                  onClose();
                  onOpenUnlock(user);
                }}
              >
                <UnlockIcon size={14} />
                <span>{t('users.detailModal.unlockAction')}</span>
              </button>
            )}
          </div>

          <button type="button" className={styles.closeModalBtn} onClick={onClose}>
            {t('common.close')}
          </button>
        </div>
      </div>
    </div>
  );
}

UserDetailModal.propTypes = {
  user: PropTypes.shape({
    id: PropTypes.number.isRequired,
    username: PropTypes.string.isRequired,
    email: PropTypes.string,
    fullName: PropTypes.string,
    avatarUrl: PropTypes.string,
    role: PropTypes.string,
    isVerified: PropTypes.bool,
    isActive: PropTypes.bool,
    passwordChangedAt: PropTypes.string,
    usernameChangedAt: PropTypes.string,
    hasPassword: PropTypes.bool,
    linkedGoogle: PropTypes.bool,
    createdAt: PropTypes.string,
    updatedAt: PropTypes.string,
  }),
  onClose: PropTypes.func.isRequired,
  onOpenLock: PropTypes.func.isRequired,
  onOpenUnlock: PropTypes.func.isRequired,
  canModify: PropTypes.bool,
};
