import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { CheckCircleIcon, XCircleIcon } from '../icons/AdminIcons';
import AdminModal from './AdminModal';
import styles from './AdminModal.module.css';

/**
 * Modal quyết định (duyệt/từ chối hoặc xử lý/bỏ qua) kèm ô ghi chú và kiểm tra bắt buộc khi từ chối.
 */
export default function AdminDecisionModal({
  isOpen,
  isReject,
  resetKey,
  title,
  subtitle,
  closeLabel,
  noteInputId,
  noteLabel,
  notePlaceholder,
  noteRequiredMessage,
  submitErrorFallback,
  cancelLabel,
  submitLabel,
  submittingLabel,
  onClose,
  onConfirm,
}) {
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setNote('');
      setError('');
    }
  }, [isOpen, resetKey]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isReject && !note.trim()) {
      setError(noteRequiredMessage);
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      await onConfirm(note);
    } catch (err) {
      const apiMsg = err?.response?.data?.error?.message;
      setError(apiMsg || submitErrorFallback);
    } finally {
      setSubmitting(false);
    }
  };

  const icon = isReject ? (
    <XCircleIcon size={20} className={styles.iconRed} />
  ) : (
    <CheckCircleIcon size={20} className={styles.iconGreen} />
  );

  return (
    <AdminModal
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      subtitle={subtitle}
      icon={icon}
      closeLabel={closeLabel}
      closeDisabled={submitting}
    >
      <form onSubmit={handleSubmit}>
        <div className={styles.modalBody}>
          {error && <div className={styles.errorAlert}>{error}</div>}
          <div className={styles.formGroup}>
            <label htmlFor={noteInputId} className={styles.formLabel}>
              {noteLabel}
              {isReject && <span className={styles.requiredMark}>*</span>}
            </label>
            <textarea
              id={noteInputId}
              className={styles.textarea}
              maxLength={500}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder={notePlaceholder}
            />
            <span className={styles.charCount}>{note.length}/500</span>
          </div>
        </div>

        <div className={styles.modalFooter}>
          <button
            type="button"
            className={styles.cancelBtn}
            onClick={onClose}
            disabled={submitting}
          >
            {cancelLabel}
          </button>
          <button
            type="submit"
            className={isReject ? styles.rejectBtn : styles.approveBtn}
            disabled={submitting}
          >
            {submitting ? submittingLabel : submitLabel}
          </button>
        </div>
      </form>
    </AdminModal>
  );
}

AdminDecisionModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  isReject: PropTypes.bool.isRequired,
  resetKey: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string.isRequired,
  closeLabel: PropTypes.string.isRequired,
  noteInputId: PropTypes.string.isRequired,
  noteLabel: PropTypes.string.isRequired,
  notePlaceholder: PropTypes.string.isRequired,
  noteRequiredMessage: PropTypes.string.isRequired,
  submitErrorFallback: PropTypes.string.isRequired,
  cancelLabel: PropTypes.string.isRequired,
  submitLabel: PropTypes.string.isRequired,
  submittingLabel: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
};
