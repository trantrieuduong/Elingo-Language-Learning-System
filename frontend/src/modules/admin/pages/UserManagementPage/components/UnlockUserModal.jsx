import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminModal from '../../../components/AdminModal/AdminModal';
import { UnlockIcon } from '../../../components/icons/AdminIcons';
import { unlockUser } from '../../../services/adminUserApi';
import { mapUserActionError } from '../../../utils/userActionError';
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
      setError(mapUserActionError(err, t, 'users.unlockModal.error'));
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
      titleId="unlock-modal-title"
      title={t('users.unlockModal.title')}
      subtitle={t('users.unlockModal.subtitle')}
      icon={
        <div className={styles.headerIconWrapper}>
          <UnlockIcon size={20} className={styles.headerIcon} />
        </div>
      }
      footer={
        <>
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
        </>
      }
    >
      <div className={styles.body}>
        <div className={styles.targetCard}>
          <span className={styles.targetLabel}>{t('users.unlockModal.targetLabel')}:</span>
          <span className={styles.targetName}>
            {user.fullName || user.username} (@{user.username}) - #{user.id}
          </span>
          <span className={styles.targetEmail}>{user.email}</span>
        </div>

        <p className={styles.confirmMessage}>{t('users.unlockModal.confirmMessage')}</p>

        {error && <div className={styles.errorAlert}>{error}</div>}
      </div>
    </AdminModal>
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
