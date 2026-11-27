import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon } from '../../../components/icons/AdminIcons';
import {
  publishAdminLesson,
  unpublishAdminLesson,
  archiveAdminLesson,
} from '../../../services/adminContentApi';
import styles from './LessonStatusModal.module.css';

const ACTIONS = {
  PUBLISHED: publishAdminLesson,
  DRAFT: unpublishAdminLesson,
  ARCHIVED: archiveAdminLesson,
};

export default function LessonStatusModal({ isOpen, lesson, onClose, onSuccess }) {
  const { t } = useTranslation('admin');
  const [status, setStatus] = useState('DRAFT');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (isOpen && lesson) {
      setStatus(lesson.status || 'DRAFT');
      setError(null);
      setSubmitting(false);
    }
  }, [isOpen, lesson]);

  if (!isOpen || !lesson) return null;

  const isUnchanged = status === lesson.status;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isUnchanged || submitting) return;

    try {
      setSubmitting(true);
      setError(null);
      const updated = await ACTIONS[status](lesson.id);
      onSuccess(updated);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || t('feedback.errorDesc'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.modalBackdrop} onClick={() => !submitting && onClose()}>
      <div
        className={styles.modalCard}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="lesson-status-modal-title"
      >
        <div className={styles.modalHeader}>
          <h3 id="lesson-status-modal-title" className={styles.modalTitle}>
            {t('content.lessons.changeStatusBtn')}
          </h3>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            disabled={submitting}
            aria-label={t('content.lessons.cancelBtn')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className={styles.modalBody}>
            {error && <p className={styles.errorMessage}>{error}</p>}

            <p className={styles.lessonName}>{lesson.title}</p>

            <div className={styles.formGroup}>
              <label htmlFor="lessonStatusSelect" className={styles.formLabel}>
                {t('content.lessons.statusFilter')}
              </label>
              <select
                id="lessonStatusSelect"
                className={styles.select}
                value={status}
                onChange={(e) => {
                  setStatus(e.target.value);
                  if (error) setError(null);
                }}
                disabled={submitting}
              >
                <option value="DRAFT">{t('content.lessons.statusDraft')}</option>
                <option value="PUBLISHED">{t('content.lessons.statusPublished')}</option>
                <option value="ARCHIVED">{t('content.lessons.statusArchived')}</option>
              </select>
            </div>

            {status === 'PUBLISHED' && (
              <p className={styles.hintText}>{t('content.lessons.statusPublishedHint')}</p>
            )}

            {status === 'ARCHIVED' && (
              <p className={styles.hintText}>{t('content.lessons.statusArchivedHint')}</p>
            )}
          </div>

          <div className={styles.modalFooter}>
            <button
              type="button"
              className={styles.cancelBtn}
              onClick={onClose}
              disabled={submitting}
            >
              {t('content.lessons.cancelBtn')}
            </button>
            <button
              type="submit"
              className={styles.primaryBtn}
              disabled={submitting || isUnchanged}
            >
              {t('content.lessons.saveBtn')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

LessonStatusModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  lesson: PropTypes.shape({
    id: PropTypes.number.isRequired,
    title: PropTypes.string.isRequired,
    status: PropTypes.string.isRequired,
  }),
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
