import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminModal from '../../../components/AdminModal/AdminModal';
import { AlertTriangleIcon, LockIcon } from '../../../components/icons/AdminIcons';
import { lockUser } from '../../../services/adminUserApi';
import { mapUserActionError } from '../../../utils/userActionError';
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
      setError(mapUserActionError(err, t, 'users.lockModal.error'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AdminModal
      isOpen={Boolean(user)}
      onClose={onClose}
      closeOnBackdrop
      closeDisabled={submitting}
      closeLabel={t('common.close')}
      titleId="lock-modal-title"
      title={t('users.lockModal.title')}
      subtitle={t('users.lockModal.subtitle')}
      icon={
        <div className={styles.headerIconWrapper}>
          <LockIcon size={20} className={styles.headerIcon} />
        </div>
      }
    >
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.body}>
          <div className={styles.targetCard}>
            <span className={styles.targetLabel}>{t('users.lockModal.targetLabel')}:</span>
            <span className={styles.targetName}>
              {user.fullName || user.username} (@{user.username}) - #{user.id}
            </span>
            <span className={styles.targetEmail}>{user.email}</span>
          </div>

          <div className={styles.warningBox}>
            <AlertTriangleIcon size={16} className={styles.warningIcon} />
            <p className={styles.warningText}>{t('users.lockModal.warningText')}</p>
          </div>

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

          {error && <div className={styles.errorAlert}>{error}</div>}
        </div>

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
    </AdminModal>
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
