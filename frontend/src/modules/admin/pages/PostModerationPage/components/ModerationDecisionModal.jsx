import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CheckCircleIcon, CloseIcon, XCircleIcon } from '../../../components/icons/AdminIcons';
import { approveModerationPost, rejectModerationPost } from '../../../services/adminCommunityApi';
import styles from './CommunityModal.module.css';

export default function ModerationDecisionModal({
  isOpen,
  mode,
  post,
  onClose,
  onSuccess,
}) {
  const { t } = useTranslation('admin');
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setNote('');
      setError('');
    }
  }, [isOpen, mode, post]);

  if (!isOpen || !post) return null;

  const isReject = mode === 'reject';

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isReject && !note.trim()) {
      setError(t('postModeration.decision.noteRequired'));
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      if (isReject) {
        await rejectModerationPost(post.id, note);
      } else {
        await approveModerationPost(post.id, note);
      }
      onSuccess(mode);
    } catch (err) {
      const apiMsg = err?.response?.data?.error?.message;
      setError(apiMsg || t('postModeration.decision.submitError'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.modal}>
        <div className={styles.modalHeader}>
          <div className={styles.headerTitleGroup}>
            {isReject ? (
              <XCircleIcon size={20} className={styles.iconRed} />
            ) : (
              <CheckCircleIcon size={20} className={styles.iconGreen} />
            )}
            <div>
              <h2 className={styles.modalTitle}>
                {isReject
                  ? t('postModeration.decision.rejectTitle')
                  : t('postModeration.decision.approveTitle')}
              </h2>
              <p className={styles.modalSubtitle}>
                #{post.id} — {post.title}
              </p>
            </div>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('postModeration.detail.close')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className={styles.modalBody}>
            {error && <div className={styles.errorAlert}>{error}</div>}
            <div className={styles.formGroup}>
              <label htmlFor="moderation-note" className={styles.formLabel}>
                {t('postModeration.decision.noteLabel')}
                {isReject && <span className={styles.requiredMark}>*</span>}
              </label>
              <textarea
                id="moderation-note"
                className={styles.textarea}
                maxLength={500}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder={
                  isReject
                    ? t('postModeration.decision.rejectPlaceholder')
                    : t('postModeration.decision.approvePlaceholder')
                }
              />
              <span className={styles.charCount}>{note.length}/500</span>
            </div>
          </div>

          <div className={styles.modalFooter}>
            <button type="button" className={styles.cancelBtn} onClick={onClose} disabled={submitting}>
              {t('postModeration.decision.cancel')}
            </button>
            <button
              type="submit"
              className={isReject ? styles.rejectBtn : styles.approveBtn}
              disabled={submitting}
            >
              {submitting
                ? t('postModeration.decision.submitting')
                : isReject
                  ? t('postModeration.actions.reject')
                  : t('postModeration.actions.approve')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

ModerationDecisionModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['approve', 'reject']).isRequired,
  post: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
