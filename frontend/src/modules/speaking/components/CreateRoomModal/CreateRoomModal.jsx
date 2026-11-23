import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import FormAlert from '../../../../components/FormAlert/FormAlert';
import './CreateRoomModal.css';

const CAPACITIES = [2, 3, 4];

const CreateRoomModal = ({ open, onClose, onSubmit, busy, error }) => {
  const { t } = useTranslation('speaking');
  const { t: tApi } = useTranslation('api');

  const [topic, setTopic] = useState('');
  const [maxParticipants, setMaxParticipants] = useState(4);
  const [isPrivate, setIsPrivate] = useState(false);

  const topicInputRef = useRef(null);

  useEffect(() => {
    if (open) {
      setTopic('');
      setMaxParticipants(4);
      setIsPrivate(false);
      setTimeout(() => topicInputRef.current?.focus(), 50);
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !busy) {
        onClose?.();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [open, busy, onClose]);

  if (!open) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (busy) return;
    onSubmit({
      topic: topic.trim() || null,
      maxParticipants,
      isPrivate,
    });
  };

  const errorMessage = error
    ? error === 'SOCKET_NOT_CONNECTED'
      ? t('error.socketNotConnected')
      : tApi(`error.${error}`, { defaultValue: tApi('common.fallback') })
    : null;

  return createPortal(
    <div
      className="speaking-modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="create-room-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onClose?.();
      }}
    >
      <div className="speaking-modal glass-capsule" onClick={(e) => e.stopPropagation()}>
        <header className="speaking-modal__header">
          <div className="speaking-modal__header-info">
            <span className="material-symbols-outlined speaking-modal__header-icon">
              add_circle
            </span>
            <div>
              <h2 id="create-room-title" className="text-headline-sm">
                {t('create.title')}
              </h2>
              <p className="speaking-modal__header-subtitle text-body-sm">
                {t('create.subtitle')}
              </p>
            </div>
          </div>
          <button
            type="button"
            className="btn-icon speaking-modal__close"
            onClick={onClose}
            disabled={busy}
            aria-label={t('create.cancel')}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        {errorMessage && (
          <div className="speaking-modal__alert">
            <FormAlert message={errorMessage} />
          </div>
        )}

        <form onSubmit={handleSubmit} className="speaking-modal__form">
          <div className="speaking-form-group">
            <label htmlFor="create-room-topic" className="speaking-form-label">
              {t('create.topic')}
            </label>
            <input
              id="create-room-topic"
              ref={topicInputRef}
              type="text"
              maxLength={255}
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              placeholder={t('lobby.searchPlaceholder')}
              className="speaking-input"
              disabled={busy}
            />
          </div>

          <div className="speaking-form-group">
            <label className="speaking-form-label">{t('create.capacity')}</label>
            <div className="speaking-capacity-options" role="radiogroup">
              {CAPACITIES.map((cap) => (
                <button
                  key={cap}
                  type="button"
                  role="radio"
                  aria-checked={maxParticipants === cap}
                  className={`speaking-capacity-btn ${
                    maxParticipants === cap ? 'speaking-capacity-btn--active' : ''
                  }`}
                  onClick={() => setMaxParticipants(cap)}
                  disabled={busy}
                >
                  <span className="material-symbols-outlined">group</span>
                  <span>{t('lobby.capacityOption', { count: cap })}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="speaking-form-group speaking-form-group--switch">
            <div className="speaking-switch-row">
              <div className="speaking-switch-text">
                <span className="speaking-switch-title">{t('create.private')}</span>
                <span className="speaking-switch-hint">
                  {t('create.privateHint')}
                </span>
              </div>
              <button
                type="button"
                role="switch"
                aria-checked={isPrivate}
                className={`speaking-switch ${isPrivate ? 'speaking-switch--checked' : ''}`}
                onClick={() => setIsPrivate(!isPrivate)}
                disabled={busy}
              >
                <span className="speaking-switch__thumb" />
              </button>
            </div>
          </div>

          <footer className="speaking-modal__actions">
            <button
              type="button"
              className="btn-secondary speaking-modal__cancel-btn"
              onClick={onClose}
              disabled={busy}
            >
              {t('create.cancel')}
            </button>
            <button type="submit" className="btn-primary" disabled={busy}>
              {busy ? (
                <>
                  <span className="app-loading-spinner app-loading-spinner--sm" />
                  <span>{t('create.submit')}</span>
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined">add</span>
                  <span>{t('create.submit')}</span>
                </>
              )}
            </button>
          </footer>
        </form>
      </div>
    </div>,
    document.body
  );
};

export default CreateRoomModal;
