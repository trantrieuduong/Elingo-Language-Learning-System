import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminModal from '../../../components/AdminModal/AdminModal';
import { TrashIcon } from '../../../components/icons/AdminIcons';
import { deletePostGroup } from '../../../services/adminCommunityApi';
import modalStyles from '../../../components/AdminModal/AdminModal.module.css';
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
    <AdminModal
      isOpen={isOpen}
      onClose={onClose}
      title={t('postGroups.delete.title')}
      subtitle={`#${group.id} — ${group.groupName}`}
      icon={<TrashIcon size={20} className={modalStyles.iconRed} />}
      closeLabel={t('postGroups.delete.cancel')}
      closeDisabled={submitting}
      footer={
        <>
          <button
            type="button"
            className={modalStyles.cancelBtn}
            onClick={onClose}
            disabled={submitting}
          >
            {t('postGroups.delete.cancel')}
          </button>
          <button
            type="button"
            className={modalStyles.rejectBtn}
            onClick={handleConfirmDelete}
            disabled={submitting}
          >
            {submitting ? t('postGroups.delete.submitting') : t('postGroups.delete.confirm')}
          </button>
        </>
      }
    >
      <div className={modalStyles.modalBody}>
        {error && <div className={modalStyles.errorAlert}>{error}</div>}
        {hasPosts && (
          <div className={styles.warningAlert}>
            {t('postGroups.delete.inUseWarning', { count: group.postCount })}
          </div>
        )}
        <p className={styles.postContent}>
          {t('postGroups.delete.confirmMessage', { name: group.groupName })}
        </p>
      </div>
    </AdminModal>
  );
}

PostGroupDeleteModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  group: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
