import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import FormAlert from '../../../../components/FormAlert/FormAlert';
import SpeakingAvatar from '../SpeakingAvatar/SpeakingAvatar';
import './LeaveRoomDialog.css';

const LeaveRoomDialog = ({
  open,
  members = [],
  onClose,
  onTransfer,
  onEnd,
  busy = false,
  error = null,
}) => {
  const { t } = useTranslation('speaking');
  const { t: tApi } = useTranslation('api');

  const [selectedHostId, setSelectedHostId] = useState(null);

  // Mặc định chọn người đầu tiên khi danh sách members thay đổi hoặc modal mở
  useEffect(() => {
    if (open && members.length > 0) {
      setSelectedHostId(members[0].userId);
    } else {
      setSelectedHostId(null);
    }
  }, [open, members]);

  // Handle phím Esc
  useEffect(() => {
    if (!open) return undefined;
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

  const handleTransferSubmit = () => {
    if (!selectedHostId || busy) return;
    onTransfer?.(selectedHostId);
  };

  const handleEndSubmit = () => {
    if (busy) return;
    onEnd?.();
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
      aria-labelledby="leave-room-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onClose?.();
      }}
    >
      <div className="speaking-modal glass-capsule" onClick={(e) => e.stopPropagation()}>
        <header className="speaking-modal__header">
          <div className="speaking-modal__header-info">
            <span className="material-symbols-outlined speaking-modal__header-icon speaking-leave-modal__icon">
              logout
            </span>
            <div>
              <h2 id="leave-room-title" className="text-headline-sm">
                {t('room.leaveDialog.title')}
              </h2>
              <p className="speaking-modal__header-subtitle text-body-sm">
                {t('room.leaveDialog.transferTitle')}
              </p>
            </div>
          </div>
          <button
            type="button"
            className="btn-icon speaking-modal__close"
            onClick={onClose}
            disabled={busy}
            aria-label={t('room.leaveDialog.cancel')}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        {errorMessage && (
          <div className="speaking-modal__alert">
            <FormAlert message={errorMessage} />
          </div>
        )}

        <div className="speaking-leave-modal__body">
          {/* Mục A: Chuyển host rồi rời */}
          <section className="speaking-leave-modal__section">
            <div className="speaking-leave-modal__section-header">
              <span className="material-symbols-outlined speaking-leave-modal__section-icon">
                person_add
              </span>
              <h3 className="speaking-form-label">{t('room.leaveDialog.transferTitle')}</h3>
            </div>

            <div className="speaking-leave-modal__member-list" role="radiogroup">
              {members.map((m) => {
                const isSelected = String(selectedHostId) === String(m.userId);
                return (
                  <label
                    key={m.userId}
                    className={`speaking-leave-modal__member-item ${
                      isSelected ? 'speaking-leave-modal__member-item--selected' : ''
                    }`}
                  >
                    <input
                      type="radio"
                      name="newHost"
                      value={m.userId}
                      checked={isSelected}
                      onChange={() => setSelectedHostId(m.userId)}
                      disabled={busy}
                      className="speaking-leave-modal__radio"
                    />
                    <SpeakingAvatar name={m.displayName} src={m.avatarUrl} size={36} />
                    <span className="speaking-leave-modal__member-name text-body-md">
                      {m.displayName}
                    </span>
                  </label>
                );
              })}
            </div>

            <button
              type="button"
              className="btn-primary speaking-leave-modal__action-btn"
              onClick={handleTransferSubmit}
              disabled={!selectedHostId || busy}
            >
              <span className="material-symbols-outlined">swap_horiz</span>
              <span>{t('room.leaveDialog.transferConfirm')}</span>
            </button>
          </section>

          <div className="speaking-leave-modal__divider" />

          {/* Mục B: Kết thúc phòng cho tất cả */}
          <section className="speaking-leave-modal__section speaking-leave-modal__section--danger">
            <div className="speaking-leave-modal__section-header">
              <span className="material-symbols-outlined speaking-leave-modal__section-icon speaking-leave-modal__section-icon--danger">
                cancel
              </span>
              <h3 className="speaking-form-label text-danger">
                {t('room.leaveDialog.endTitle')}
              </h3>
            </div>
            <p className="text-body-xs text-disabled speaking-leave-modal__warning">
              {t('room.leaveDialog.endWarning')}
            </p>
            <button
              type="button"
              className="speaking-leave-modal__action-btn speaking-leave-modal__action-btn--danger"
              onClick={handleEndSubmit}
              disabled={busy}
            >
              <span className="material-symbols-outlined">power_settings_new</span>
              <span>{t('room.leaveDialog.endConfirm')}</span>
            </button>
          </section>
        </div>

        <footer className="speaking-modal__actions">
          <button
            type="button"
            className="btn-secondary"
            onClick={onClose}
            disabled={busy}
          >
            {t('room.leaveDialog.cancel')}
          </button>
        </footer>
      </div>
    </div>,
    document.body
  );
};

export default LeaveRoomDialog;
