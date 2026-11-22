import { useTranslation } from 'react-i18next';
import SpeakingAvatar from '../SpeakingAvatar/SpeakingAvatar';
import './RoomCard.css';

const RoomCard = ({ room, onJoin, joining, disabledTitle }) => {
  const { t } = useTranslation('speaking');

  const isFull = room.participantCount >= room.maxParticipants;
  const isJoinDisabled = joining || isFull;

  return (
    <div className="speaking-room-card glass-card">
      <div className="speaking-room-card__header">
        <div className="speaking-room-card__topic-group">
          <h3 className="speaking-room-card__topic">
            {room.topic || t('lobby.noTopic')}
          </h3>
          <span className={`speaking-status-chip speaking-status-chip--${(room.status || '').toLowerCase()}`}>
            {t(`lobby.status.${room.status}`, { defaultValue: room.status })}
          </span>
        </div>
      </div>

      <div className="speaking-room-card__body">
        <div className="speaking-room-card__host">
          <SpeakingAvatar
            name={room.host?.displayName || ''}
            src={room.host?.avatarUrl || ''}
            size={36}
          />
          <div className="speaking-room-card__host-info">
            <span className="speaking-room-card__host-label text-disabled">Host</span>
            <span className="speaking-room-card__host-name">
              {room.host?.displayName || 'Unknown'}
            </span>
          </div>
        </div>

        <div className="speaking-room-card__capacity">
          <span className="material-symbols-outlined speaking-room-card__icon">group</span>
          <span className="speaking-room-card__count">
            {t('lobby.participants', { count: room.participantCount, max: room.maxParticipants })}
          </span>
        </div>
      </div>

      <div className="speaking-room-card__footer">
        <button
          type="button"
          className="btn-primary speaking-room-card__btn"
          disabled={isJoinDisabled}
          aria-disabled={isJoinDisabled}
          title={disabledTitle || undefined}
          onClick={() => onJoin(room.roomCode)}
        >
          {isFull ? (
            <span>{t('lobby.full')}</span>
          ) : (
            <>
              <span className="material-symbols-outlined">meeting_room</span>
              <span>{t('lobby.join')}</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};

export default RoomCard;
