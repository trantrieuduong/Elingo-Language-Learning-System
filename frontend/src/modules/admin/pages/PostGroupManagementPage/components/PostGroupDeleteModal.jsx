import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon, TrashIcon } from '../../../components/icons/AdminIcons';
import { deletePostGroup } from '../../../services/adminCommunityApi';
import styles from '../../PostModerationPage/components/CommunityModal.module.css';

export default function PostGroupDeleteModal({
  isOpen,
  group,
  onClose,
  onSuccess,
}) {
  const { t } = useTranslation('admin');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setError('');
    }
  }, [isOpen, group]);

  if (!isOpen || !group) return null;

  const hasPosts = (group.postCount || 0) > 0;

  const handleConfirmDelete = async () => {
    setSubmitting(true);
    setError('');
    try {
      await deletePostGroup(group.id);
      onSuccess();
    } catch (err) {
      const code = err?.response?.data?.error?.code;
      if (code === 'POST_GROUP_IN_USE') {
        setError(t('postGroups.errors.inUse'));
      } else {
        setError(err?.response?.data?.error?.message || t('postGroups.delete.submitError'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.modal}>
        <div className={styles.modalHeader}>
          <div className={styles.headerTitleGroup}>
            <TrashIcon size={20} className={styles.iconRed} />
            <div>
              <h2 className={styles.modalTitle}>{t('postGroups.delete.title')}</h2>
              <p className={styles.modalSubtitle}>
                #{group.id} — {group.groupName}
              </p>
            </div>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('postGroups.delete.cancel')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <div className={styles.modalBody}>
          {error && <div className={styles.errorAlert}>{error}</div>}
          {hasPosts && (
            <div className={styles.warningAlert}>
              {t('postGroups.delete.inUseWarning', { count: group.postCount })}
            </div>
          )}
          <p className={styles.postContent}>
            {t('postGroups.delete.confirmMessage', { name: group.groupName })}
          </p>
        </div>

        <div className={styles.modalFooter}>
          <button type="button" className={styles.cancelBtn} onClick={onClose} disabled={submitting}>
            {t('postGroups.delete.cancel')}
          </button>
          <button
            type="button"
            className={styles.rejectBtn}
            onClick={handleConfirmDelete}
            disabled={submitting}
          >
            {submitting ? t('postGroups.delete.submitting') : t('postGroups.delete.confirm')}
          </button>
        </div>
      </div>
    </div>
  );
}

PostGroupDeleteModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  group: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
