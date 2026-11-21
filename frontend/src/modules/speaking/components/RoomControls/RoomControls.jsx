import { useTranslation } from 'react-i18next';
import './RoomControls.css';

const RoomControls = ({
  muted = false,
  onToggleMute,
  onLeave,
  leaving = false,
  chatOpen = false,
  chatUnread = 0,
  onToggleChat,
}) => {
  const { t } = useTranslation('speaking');

  const unreadBadgeText = chatUnread > 9 ? '9+' : chatUnread > 0 ? String(chatUnread) : null;

  return (
    <footer className="speaking-room-controls glass-capsule" role="toolbar" aria-label="Controls">
      <div className="speaking-room-controls__inner">
        {/* Nút bật/tắt Micro */}
        <button
          type="button"
          className={`speaking-control-btn speaking-control-btn--mic ${
            muted ? 'speaking-control-btn--muted' : ''
          }`}
          onClick={onToggleMute}
          disabled={leaving}
          aria-label={muted ? t('room.unmute') : t('room.mute')}
          aria-pressed={muted}
          title={muted ? t('room.unmute') : t('room.mute')}
        >
          <span className="material-symbols-outlined">
            {muted ? 'mic_off' : 'mic'}
          </span>
          <span className="speaking-control-btn__label">
            {muted ? t('room.unmute') : t('room.mute')}
          </span>
        </button>

        {/* Nút Khung Chat */}
        {onToggleChat && (
          <button
            type="button"
            className={`speaking-control-btn speaking-control-btn--chat ${
              chatOpen ? 'speaking-control-btn--chat-open' : ''
            }`}
            onClick={onToggleChat}
            disabled={leaving}
            aria-label={
              chatUnread > 0
                ? t('chat.unreadLabel', { count: chatUnread })
                : chatOpen
                ? t('chat.close')
                : t('chat.toggle')
            }
            aria-pressed={chatOpen}
            title={chatOpen ? t('chat.close') : t('chat.toggle')}
          >
            <div className="speaking-control-btn__icon-wrapper">
              <span className="material-symbols-outlined">chat</span>
              {unreadBadgeText && !chatOpen && (
                <span className="speaking-chat-unread-badge" aria-hidden="true">
                  {unreadBadgeText}
                </span>
              )}
            </div>
            <span className="speaking-control-btn__label">{t('chat.title')}</span>
          </button>
        )}

        {/* Nút Rời phòng */}
        <button
          type="button"
          className="speaking-control-btn speaking-control-btn--leave"
          onClick={onLeave}
          disabled={leaving}
          aria-label={t('room.leave')}
          title={t('room.leave')}
        >
          {leaving ? (
            <span className="app-loading-spinner app-loading-spinner--sm" />
          ) : (
            <span className="material-symbols-outlined">call_end</span>
          )}
          <span className="speaking-control-btn__label">{t('room.leave')}</span>
        </button>
      </div>
    </footer>
  );
};

export default RoomControls;
