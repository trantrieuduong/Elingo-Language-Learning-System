import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CheckCircleIcon, CloseIcon, XCircleIcon } from '../../../components/icons/AdminIcons';
import { rejectReport, resolveReport } from '../../../services/adminCommunityApi';
import styles from '../../PostModerationPage/components/CommunityModal.module.css';

export default function ReportDecisionModal({
  isOpen,
  mode,
  report,
  onClose,
  onSuccess,
}) {
  const { t } = useTranslation('admin');
  const [resolutionNote, setResolutionNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setResolutionNote('');
      setError('');
    }
  }, [isOpen, mode, report]);

  if (!isOpen || !report) return null;

  const isReject = mode === 'reject';

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isReject && !resolutionNote.trim()) {
      setError(t('reports.decision.noteRequired'));
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      if (isReject) {
        await rejectReport(report.id, resolutionNote);
      } else {
        await resolveReport(report.id, resolutionNote);
      }
      onSuccess(mode);
    } catch (err) {
      const apiMsg = err?.response?.data?.error?.message;
      setError(apiMsg || t('reports.decision.submitError'));
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
                {isReject ? t('reports.decision.rejectTitle') : t('reports.decision.resolveTitle')}
              </h2>
              <p className={styles.modalSubtitle}>
                #{report.id} — {t(`reports.targetType.${report.targetType}`, report.targetType)} #{report.targetId}
              </p>
            </div>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('reports.detail.close')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className={styles.modalBody}>
            {error && <div className={styles.errorAlert}>{error}</div>}
            <div className={styles.formGroup}>
              <label htmlFor="report-resolution-note" className={styles.formLabel}>
                {t('reports.decision.noteLabel')}
                {isReject && <span className={styles.requiredMark}>*</span>}
              </label>
              <textarea
                id="report-resolution-note"
                className={styles.textarea}
                maxLength={500}
                value={resolutionNote}
                onChange={(e) => setResolutionNote(e.target.value)}
                placeholder={
                  isReject
                    ? t('reports.decision.rejectPlaceholder')
                    : t('reports.decision.resolvePlaceholder')
                }
              />
              <span className={styles.charCount}>{resolutionNote.length}/500</span>
            </div>
          </div>

          <div className={styles.modalFooter}>
            <button type="button" className={styles.cancelBtn} onClick={onClose} disabled={submitting}>
              {t('reports.decision.cancel')}
            </button>
            <button
              type="submit"
              className={isReject ? styles.rejectBtn : styles.approveBtn}
              disabled={submitting}
            >
              {submitting
                ? t('reports.decision.submitting')
                : isReject
                  ? t('reports.actions.reject')
                  : t('reports.actions.resolve')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

ReportDecisionModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['resolve', 'reject']).isRequired,
  report: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
