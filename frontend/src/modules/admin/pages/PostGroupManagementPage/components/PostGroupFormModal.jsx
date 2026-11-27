import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon, EditIcon, PlusIcon } from '../../../components/icons/AdminIcons';
import { createPostGroup, updatePostGroup } from '../../../services/adminCommunityApi';
import styles from '../../PostModerationPage/components/CommunityModal.module.css';

export default function PostGroupFormModal({
  isOpen,
  mode,
  group,
  onClose,
  onSuccess,
}) {
  const { t } = useTranslation('admin');
  const [groupName, setGroupName] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      if (mode === 'edit' && group) {
        setGroupName(group.groupName || '');
        setDescription(group.description || '');
      } else {
        setGroupName('');
        setDescription('');
      }
      setError('');
    }
  }, [isOpen, mode, group]);

  if (!isOpen) return null;

  const isEdit = mode === 'edit';

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!groupName.trim()) {
      setError(t('postGroups.form.nameRequired'));
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      const payload = { groupName, description };
      if (isEdit && group) {
        await updatePostGroup(group.id, payload);
      } else {
        await createPostGroup(payload);
      }
      onSuccess(mode);
    } catch (err) {
      const code = err?.response?.data?.error?.code;
      if (code === 'POST_GROUP_ALREADY_EXISTS') {
        setError(t('postGroups.errors.alreadyExists'));
      } else {
        setError(err?.response?.data?.error?.message || t('postGroups.form.submitError'));
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
            {isEdit ? (
              <EditIcon size={20} className={styles.headerIcon} />
            ) : (
              <PlusIcon size={20} className={styles.headerIcon} />
            )}
            <div>
              <h2 className={styles.modalTitle}>
                {isEdit ? t('postGroups.form.editTitle') : t('postGroups.form.createTitle')}
              </h2>
              <p className={styles.modalSubtitle}>
                {isEdit && group ? `#${group.id} — ${group.groupName}` : t('postGroups.form.createSubtitle')}
              </p>
            </div>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('postGroups.form.cancel')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className={styles.modalBody}>
            {error && <div className={styles.errorAlert}>{error}</div>}

            <div className={styles.formGroup}>
              <label htmlFor="post-group-name" className={styles.formLabel}>
                {t('postGroups.form.nameLabel')}
                <span className={styles.requiredMark}>*</span>
              </label>
              <input
                id="post-group-name"
                type="text"
                maxLength={255}
                className={styles.textInput}
                value={groupName}
                onChange={(e) => setGroupName(e.target.value)}
                placeholder={t('postGroups.form.namePlaceholder')}
              />
              <span className={styles.charCount}>{groupName.length}/255</span>
            </div>

            <div className={styles.formGroup}>
              <label htmlFor="post-group-desc" className={styles.formLabel}>
                {t('postGroups.form.descLabel')}
              </label>
              <textarea
                id="post-group-desc"
                className={styles.textarea}
                maxLength={500}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder={t('postGroups.form.descPlaceholder')}
              />
              <span className={styles.charCount}>{description.length}/500</span>
            </div>
          </div>

          <div className={styles.modalFooter}>
            <button type="button" className={styles.cancelBtn} onClick={onClose} disabled={submitting}>
              {t('postGroups.form.cancel')}
            </button>
            <button type="submit" className={styles.primaryBtn} disabled={submitting}>
              {submitting
                ? t('postGroups.form.submitting')
                : isEdit
                  ? t('postGroups.form.save')
                  : t('postGroups.form.create')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

PostGroupFormModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['create', 'edit']).isRequired,
  group: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
