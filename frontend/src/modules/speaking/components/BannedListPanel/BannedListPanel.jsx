import { useCallback, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import FormAlert from '../../../../components/FormAlert/FormAlert';
import { getBansApi } from '../../speakingApi';
import SpeakingAvatar from '../SpeakingAvatar/SpeakingAvatar';
import './BannedListPanel.css';

const BannedListPanel = ({
  open,
  roomCode,
  onClose,
  onUnban,
  onBansCountChange,
}) => {
  const { t, i18n } = useTranslation('speaking');
  const { t: tApi } = useTranslation('api');

  const [loading, setLoading] = useState(false);
  const [bans, setBans] = useState([]);
  const [loadError, setLoadError] = useState(null);
  const [actionUserId, setActionUserId] = useState(null);
  const [actionError, setActionError] = useState(null);

  const fetchBans = useCallback(async () => {
    if (!roomCode) return;
    setLoading(true);
    setLoadError(null);
    setActionError(null);
    try {
      const res = await getBansApi(roomCode);
      const list = Array.isArray(res) ? res : res?.data ?? [];
      setBans(list);
      onBansCountChange?.(list.length);
    } catch {
      setLoadError(t('room.bans.loadError'));
    } finally {
      setLoading(false);
    }
  }, [roomCode, t, onBansCountChange]);

  useEffect(() => {
    if (open) {
      fetchBans();
    } else {
      setBans([]);
      setLoadError(null);
      setActionError(null);
      setActionUserId(null);
    }
  }, [open, fetchBans]);

  useEffect(() => {
    if (!open) return undefined;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !actionUserId) {
        onClose?.();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [open, actionUserId, onClose]);

  if (!open) return null;

  const handleUnbanClick = async (userId) => {
    if (actionUserId || !onUnban) return;
    setActionUserId(userId);
    setActionError(null);
    try {
      await onUnban(userId);
      setBans((prev) => {
        const next = prev.filter((b) => String(b.userId) !== String(userId));
        onBansCountChange?.(next.length);
        return next;
      });
    } catch (e) {
      setActionError(e?.code ? tApi(`error.${e.code}`, { defaultValue: tApi('common.fallback') }) : tApi('common.fallback'));
    } finally {
      setActionUserId(null);
    }
  };

  const formatKickedAt = (kickedAt) => {
    if (!kickedAt) return '';
    try {
      const d = new Date(kickedAt);
      return Number.isNaN(d.getTime()) ? '' : d.toLocaleString(i18n.language);
    } catch {
      return '';
    }
  };

  return createPortal(
    <div
      className="speaking-modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="banned-list-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !actionUserId) onClose?.();
      }}
    >
      <div className="speaking-modal glass-capsule" onClick={(e) => e.stopPropagation()}>
        <header className="speaking-modal__header">
          <div className="speaking-modal__header-info">
            <span className="material-symbols-outlined speaking-modal__header-icon speaking-banned-modal__icon">
              block
            </span>
            <div>
              <h2 id="banned-list-title" className="text-headline-sm">
                {t('room.bans.titleWithCount', {
                  count: bans.length,
                  defaultValue: `${t('room.bans.title')} (${bans.length})`,
                })}
              </h2>
              <p className="speaking-modal__header-subtitle text-body-sm">
                {t('room.bans.hint')}
              </p>
            </div>
          </div>
          <button
            type="button"
            className="btn-icon speaking-modal__close"
            onClick={onClose}
            disabled={Boolean(actionUserId)}
            aria-label={t('room.bans.close')}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        {(loadError || actionError) && (
          <div className="speaking-modal__alert">
            <FormAlert message={actionError || loadError} />
          </div>
        )}

        <div className="speaking-banned-modal__body">
          {loading ? (
            <div className="speaking-banned-modal__loading text-body-sm text-disabled">
              <span className="speaking-banned-modal__spinner" />
              <span>{t('room.preparing')}</span>
            </div>
          ) : loadError ? (
            <div className="speaking-banned-modal__error-state">
              <button
                type="button"
                className="btn-secondary"
                onClick={fetchBans}
              >
                {t('room.bans.retry')}
              </button>
            </div>
          ) : bans.length === 0 ? (
            <div className="speaking-banned-modal__empty">
              <span className="material-symbols-outlined speaking-banned-modal__empty-icon">
                check_circle
              </span>
              <p className="text-body-md text-secondary">
                {t('room.bans.empty')}
              </p>
            </div>
          ) : (
            <div className="speaking-banned-modal__list">
              {bans.map((item) => {
                const isItemBusy = String(actionUserId) === String(item.userId);
                const timeStr = formatKickedAt(item.kickedAt);
                return (
                  <div key={item.userId} className="speaking-banned-modal__item glass-card">
                    <div className="speaking-banned-modal__user-info">
                      <SpeakingAvatar
                        name={item.displayName}
                        src={item.avatarUrl}
                        size={40}
                      />
                      <div className="speaking-banned-modal__user-meta">
                        <span className="speaking-banned-modal__user-name text-body-md" title={item.displayName}>
                          {item.displayName}
                        </span>
                        {timeStr && (
                          <span className="speaking-banned-modal__user-time text-body-xs text-disabled">
                            {timeStr}
                          </span>
                        )}
                      </div>
                    </div>
                    <button
                      type="button"
                      className="btn-secondary speaking-banned-modal__unban-btn"
                      onClick={() => handleUnbanClick(item.userId)}
                      disabled={Boolean(actionUserId)}
                    >
                      <span className="material-symbols-outlined">lock_open</span>
                      <span>{isItemBusy ? t('room.preparing') : t('room.bans.unban')}</span>
                    </button>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <footer className="speaking-modal__actions">
          <button
            type="button"
            className="btn-secondary"
            onClick={onClose}
            disabled={Boolean(actionUserId)}
          >
            {t('room.bans.close')}
          </button>
        </footer>
      </div>
    </div>,
    document.body
  );
};

export default BannedListPanel;
