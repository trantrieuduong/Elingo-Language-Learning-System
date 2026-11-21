import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { formatDuration } from '../../utils/formatDuration';
import '../CreateRoomModal/CreateRoomModal.css';
import './SummaryModal.css';

const SummaryModal = ({ open, summary, onClose, onNavigate }) => {
  const { t } = useTranslation('speaking');
  const closeButtonRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose();
      if (event.key === 'Tab') {
        event.preventDefault();
        closeButtonRef.current?.focus();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';
    closeButtonRef.current?.focus();
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);

  if (!open || !summary) return null;

  return createPortal(
    <div
      className="speaking-modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="speaking-summary-title"
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        className="speaking-modal glass-capsule"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="speaking-modal__header">
          <div className="speaking-modal__header-info">
            <span className="material-symbols-outlined speaking-modal__header-icon">
              celebration
            </span>
            <h2 id="speaking-summary-title" className="text-headline-sm">
              {t('room.summary.title')}
            </h2>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            className="btn-icon speaking-modal__close"
            onClick={onClose}
            aria-label={t('room.summary.toLobby')}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        <div className="speaking-summary-modal__details">
          <div>
            <span className="speaking-summary-modal__label">{t('room.summary.topic')}</span>
            <span className="speaking-summary-modal__value">
              {summary.topic || t('lobby.noTopic')}
            </span>
          </div>
          <div>
            <span className="speaking-summary-modal__label">{t('room.summary.roomCode')}</span>
            <span className="speaking-summary-modal__value">{summary.roomCode}</span>
          </div>
          <div>
            <span className="speaking-summary-modal__label">{t('room.summary.duration')}</span>
            <span className="speaking-summary-modal__value">
              {formatDuration(summary.durationSeconds)}
            </span>
          </div>
        </div>
        <footer className="speaking-modal__actions">
          <button
            type="button"
            className="btn-secondary speaking-modal__cancel-btn"
            onClick={onClose}
          >
            {t('room.summary.toLobby')}
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={onNavigate}
          >
            {t('room.summary.toHistory')}
          </button>
        </footer>
      </section>
    </div>,
    document.body
  );
};

export default SummaryModal;
