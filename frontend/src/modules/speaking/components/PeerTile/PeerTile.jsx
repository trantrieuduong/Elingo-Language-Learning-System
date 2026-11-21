import { useTranslation } from 'react-i18next';
import SpeakingAvatar from '../SpeakingAvatar/SpeakingAvatar';
import './PeerTile.css';

const PeerTile = ({ peer, isSelf = false, isHost = false, muted = false }) => {
  const { t } = useTranslation('speaking');

  const displayName = peer?.displayName || t('room.you');
  const linkState = peer?.link; // 'connecting' | 'connected' | 'failed' | undefined (cho chính mình)

  return (
    <article className={`speaking-peer-tile glass-card ${isSelf ? 'speaking-peer-tile--self' : ''}`}>
      <div className="speaking-peer-tile__avatar-wrapper">
        <SpeakingAvatar
          name={displayName}
          src={peer?.avatarUrl}
          size={72}
          className="speaking-peer-tile__avatar"
        />
        {isSelf && (
          <div
            className={`speaking-peer-tile__mic-badge ${
              muted ? 'speaking-peer-tile__mic-badge--muted' : ''
            }`}
            title={muted ? t('room.mute') : t('room.unmute')}
          >
            <span className="material-symbols-outlined">
              {muted ? 'mic_off' : 'mic'}
            </span>
          </div>
        )}
      </div>

      <div className="speaking-peer-tile__info">
        <div className="speaking-peer-tile__name-row">
          <span className="speaking-peer-tile__name" title={displayName}>
            {displayName}
          </span>
          {isSelf && (
            <span className="speaking-peer-badge speaking-peer-badge--self">
              {t('room.you')}
            </span>
          )}
          {isHost && (
            <span className="speaking-peer-badge speaking-peer-badge--host">
              {t('room.host')}
            </span>
          )}
        </div>

        {!isSelf && linkState && (
          <div className={`speaking-peer-tile__link speaking-peer-tile__link--${linkState}`}>
            <span className="speaking-peer-tile__link-dot" />
            <span className="speaking-peer-tile__link-text">
              {t(`room.link.${linkState}`)}
            </span>
          </div>
        )}
      </div>
    </article>
  );
};

export default PeerTile;
