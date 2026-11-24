import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon, UnlockIcon } from '../../../components/icons/AdminIcons';
import { unlockUser } from '../../../services/adminUserApi';
import styles from './UnlockUserModal.module.css';

export default function UnlockUserModal({ user, onClose, onSuccess }) {
  const { t } = useTranslation('admin');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  if (!user) return null;

  const handleConfirm = async () => {
    try {
      setSubmitting(true);
      setError(null);
      const updatedUser = await unlockUser(user.id);
      onSuccess(updatedUser);
    } catch (err) {
      const errCode = err.response?.data?.error?.code;
      if (errCode === 'CANNOT_MODIFY_SELF') {
        setError(t('users.errors.cannotModifySelf'));
      } else if (errCode === 'CANNOT_MODIFY_ADMIN') {
        setError(t('users.errors.cannotModifyAdmin'));
      } else if (errCode === 'USER_ALREADY_ACTIVE') {
        setError(t('users.errors.userAlreadyActive'));
      } else if (errCode === 'USER_NOT_FOUND') {
        setError(t('users.errors.userNotFound'));
      } else {
        setError(t('users.unlockModal.error'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div
        className={styles.modal}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="unlock-modal-title"
      >
        {/* Header */}
        <div className={styles.header}>
          <div className={styles.headerIconWrapper}>
            <UnlockIcon size={20} className={styles.headerIcon} />
          </div>
          <div className={styles.headerText}>
            <h2 id="unlock-modal-title" className={styles.title}>
              {t('users.unlockModal.title')}
            </h2>
            <p className={styles.subtitle}>{t('users.unlockModal.subtitle')}</p>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            disabled={submitting}
            aria-label={t('common.close')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        {/* Body */}
        <div className={styles.body}>
          {/* Target User Info */}
          <div className={styles.targetCard}>
            <span className={styles.targetLabel}>{t('users.unlockModal.targetLabel')}:</span>
            <span className={styles.targetName}>
              {user.fullName || user.username} (@{user.username}) - #{user.id}
            </span>
            <span className={styles.targetEmail}>{user.email}</span>
          </div>

          <p className={styles.confirmMessage}>{t('users.unlockModal.confirmMessage')}</p>

          {/* Error Message */}
          {error && <div className={styles.errorAlert}>{error}</div>}
        </div>

        {/* Footer */}
        <div className={styles.footer}>
          <button
            type="button"
            className={styles.cancelBtn}
            onClick={onClose}
            disabled={submitting}
          >
            {t('users.unlockModal.cancelling')}
          </button>
          <button
            type="button"
            className={styles.submitBtn}
            onClick={handleConfirm}
            disabled={submitting}
          >
            <UnlockIcon size={14} />
            <span>{submitting ? t('common.save') : t('users.unlockModal.confirmBtn')}</span>
          </button>
        </div>
      </div>
    </div>
  );
}

UnlockUserModal.propTypes = {
  user: PropTypes.shape({
    id: PropTypes.number.isRequired,
    username: PropTypes.string.isRequired,
    email: PropTypes.string,
    fullName: PropTypes.string,
  }),
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
