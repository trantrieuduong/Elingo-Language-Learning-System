import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon, AlertTriangleIcon, LockIcon } from '../../../components/icons/AdminIcons';
import { lockUser } from '../../../services/adminUserApi';
import styles from './LockUserModal.module.css';

export default function LockUserModal({ user, onClose, onSuccess }) {
  const { t } = useTranslation('admin');
  const [reason, setReason] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  if (!user) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    const trimmedReason = reason.trim();

    if (!trimmedReason) {
      setError(t('users.lockModal.reasonRequired'));
      return;
    }
    if (trimmedReason.length > 500) {
      setError(t('users.lockModal.reasonMaxLength'));
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const updatedUser = await lockUser(user.id, trimmedReason);
      onSuccess(updatedUser);
    } catch (err) {
      const errCode = err.response?.data?.error?.code;
      if (errCode === 'CANNOT_MODIFY_SELF') {
        setError(t('users.errors.cannotModifySelf'));
      } else if (errCode === 'CANNOT_MODIFY_ADMIN') {
        setError(t('users.errors.cannotModifyAdmin'));
      } else if (errCode === 'USER_INACTIVE') {
        setError(t('users.errors.userInactive'));
      } else if (errCode === 'USER_NOT_FOUND') {
        setError(t('users.errors.userNotFound'));
      } else {
        setError(t('users.lockModal.error'));
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
        aria-labelledby="lock-modal-title"
      >
        {/* Header */}
        <div className={styles.header}>
          <div className={styles.headerIconWrapper}>
            <LockIcon size={20} className={styles.headerIcon} />
          </div>
          <div className={styles.headerText}>
            <h2 id="lock-modal-title" className={styles.title}>
              {t('users.lockModal.title')}
            </h2>
            <p className={styles.subtitle}>{t('users.lockModal.subtitle')}</p>
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

        {/* Form Body */}
        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.body}>
            {/* Target User Info */}
            <div className={styles.targetCard}>
              <span className={styles.targetLabel}>{t('users.lockModal.targetLabel')}:</span>
              <span className={styles.targetName}>
                {user.fullName || user.username} (@{user.username}) - #{user.id}
              </span>
              <span className={styles.targetEmail}>{user.email}</span>
            </div>

            {/* Warning Callout */}
            <div className={styles.warningBox}>
              <AlertTriangleIcon size={16} className={styles.warningIcon} />
              <p className={styles.warningText}>{t('users.lockModal.warningText')}</p>
            </div>

            {/* Reason Textarea */}
            <div className={styles.fieldGroup}>
              <div className={styles.fieldHeader}>
                <label htmlFor="lock-reason" className={styles.fieldLabel}>
                  {t('users.lockModal.reasonLabel')} <span className={styles.required}>*</span>
                </label>
                <span className={styles.charCounter}>
                  {reason.length}/500 {t('users.lockModal.charCount')}
                </span>
              </div>
              <textarea
                id="lock-reason"
                className={styles.textarea}
                rows={4}
                maxLength={500}
                placeholder={t('users.lockModal.reasonPlaceholder')}
                value={reason}
                onChange={(e) => {
                  setReason(e.target.value);
                  if (error) setError(null);
                }}
                disabled={submitting}
                autoFocus
              />
            </div>

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
              {t('users.lockModal.cancelling')}
            </button>
            <button
              type="submit"
              className={styles.submitBtn}
              disabled={submitting || !reason.trim()}
            >
              <LockIcon size={14} />
              <span>{submitting ? t('common.save') : t('users.lockModal.confirmBtn')}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

LockUserModal.propTypes = {
  user: PropTypes.shape({
    id: PropTypes.number.isRequired,
    username: PropTypes.string.isRequired,
    email: PropTypes.string,
    fullName: PropTypes.string,
  }),
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
