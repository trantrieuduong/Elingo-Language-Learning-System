import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import './LessonModeModal.css';

function LessonModeModal({ isOpen, lesson, onSelectMode, onClose }) {
  const { t } = useTranslation(['learning', 'common']);

  // Đóng modal khi nhấn Escape
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose?.();
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !lesson) return null;

  const { title, userProgress } = lesson;

  // Helper tính trạng thái và nhãn hành động cho từng chế độ
  const getModeActionInfo = (status, progressPct) => {
    if (status === 'COMPLETED') {
      return {
        actionLabel: t('lessonMode.restart'),
        statusLabel: t('lessonMode.completedPct', { pct: 100 }),
        statusType: 'completed',
      };
    }
    if (status === 'IN_PROGRESS') {
      const pct = typeof progressPct === 'number' ? Math.round(progressPct) : 0;
      return {
        actionLabel: t('lessonMode.continue'),
        statusLabel: t('lessonMode.progressPct', { pct }),
        statusType: 'progress',
      };
    }
    return {
      actionLabel: t('lessonMode.start'),
      statusLabel: t('lessonMode.notStarted'),
      statusType: 'not_started',
    };
  };

  const dictationInfo = getModeActionInfo(
    userProgress?.dictationStatus,
    userProgress?.dictationProgressPct
  );
  const shadowingInfo = getModeActionInfo(
    userProgress?.shadowingStatus,
    userProgress?.shadowingProgressPct
  );

  return createPortal(
    <div
      className="lesson-mode-modal-overlay"
      onClick={onClose}
    >
      <div className="lesson-mode-modal glass-capsule" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="lesson-mode-modal__header">
          <div className="lesson-mode-modal__title-group">
            <h2 id="lesson-mode-title" className="lesson-mode-modal__title">
              {t('lessonMode.title')}
            </h2>
            <p className="lesson-mode-modal__subtitle">{title}</p>
          </div>
          <button
            type="button"
            className="btn-icon"
            onClick={onClose}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* Body: Hai thẻ chọn chế độ */}
        <div className="lesson-mode-modal__body">
          {/* Dictation Card */}
          <div
            className="lesson-mode-card glass-card"
            tabIndex={0}
            onClick={() => onSelectMode('dictation', lesson)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onSelectMode('dictation', lesson);
              }
            }}
          >
            <div className="lesson-mode-card__icon-wrapper lesson-mode-card__icon-wrapper--blue">
              <span className="material-symbols-outlined lesson-mode-card__icon">edit_note</span>
            </div>
            <div className="lesson-mode-card__content">
              <div className="lesson-mode-card__header-row">
                <h3 className="lesson-mode-card__name">{t('lessonList.dictation')}</h3>
                <span
                  className={`lesson-mode-card__status-tag lesson-mode-card__status-tag--${dictationInfo.statusType}`}
                >
                  {dictationInfo.statusLabel}
                </span>
              </div>
              <p className="lesson-mode-card__desc">{t('lessonMode.dictationDesc')}</p>
            </div>
            <div className="lesson-mode-card__action">
              <span className="btn-primary lesson-mode-card__btn">
                {dictationInfo.actionLabel}
                <span className="material-symbols-outlined lesson-mode-card__arrow">
                  arrow_forward
                </span>
              </span>
            </div>
          </div>

          {/* Shadowing Card */}
          <div
            className="lesson-mode-card glass-card"
            tabIndex={0}
            onClick={() => onSelectMode('shadowing', lesson)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onSelectMode('shadowing', lesson);
              }
            }}
          >
            <div className="lesson-mode-card__icon-wrapper lesson-mode-card__icon-wrapper--lavender">
              <span className="material-symbols-outlined lesson-mode-card__icon">
                record_voice_over
              </span>
            </div>
            <div className="lesson-mode-card__content">
              <div className="lesson-mode-card__header-row">
                <h3 className="lesson-mode-card__name">{t('lessonList.shadowing')}</h3>
                <span
                  className={`lesson-mode-card__status-tag lesson-mode-card__status-tag--${shadowingInfo.statusType}`}
                >
                  {shadowingInfo.statusLabel}
                </span>
              </div>
              <p className="lesson-mode-card__desc">{t('lessonMode.shadowingDesc')}</p>
            </div>
            <div className="lesson-mode-card__action">
              <span className="btn-primary lesson-mode-card__btn">
                {shadowingInfo.actionLabel}
                <span className="material-symbols-outlined lesson-mode-card__arrow">
                  arrow_forward
                </span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
}

export default LessonModeModal;
