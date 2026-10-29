import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import './ConfirmModal.css';

function ConfirmModal({
  isOpen,
  title,
  message,
  confirmText,
  cancelText,
  onConfirm,
  onCancel,
  isDanger = false,
  isLoading = false,
}) {
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && !isLoading) onCancel?.();
    };

    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isOpen, isLoading, onCancel]);

  if (!isOpen) return null;

  return createPortal(
    <div className="confirm-modal-overlay" onClick={() => !isLoading && onCancel?.()}>
      <div
        className="confirm-modal-container glass-capsule"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="confirm-modal-title" className="confirm-modal-title text-headline-sm">
          {title}
        </h2>
        {message && <p className="confirm-modal-message text-body-md">{message}</p>}
        <div className="confirm-modal-actions">
          <button
            type="button"
            className="btn-secondary confirm-modal-cancel-btn"
            onClick={onCancel}
            disabled={isLoading}
          >
            {cancelText}
          </button>
          <button
            type="button"
            className={`confirm-modal-confirm-btn ${isDanger ? 'confirm-modal-confirm-btn--danger' : 'btn-primary'}`}
            onClick={onConfirm}
            disabled={isLoading}
          >
            {isLoading ? <span className="confirm-modal-spinner" /> : confirmText}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}

export default ConfirmModal;
