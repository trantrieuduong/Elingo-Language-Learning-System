import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import SpeakingAvatar from '../SpeakingAvatar/SpeakingAvatar';
import './PeerTile.css';

const PeerTile = ({
  peer,
  isSelf = false,
  isHost = false,
  muted = false,
  grace = false,
  micMuted = false,
  canMute = false,
  onMute,
}) => {
  const { t } = useTranslation('speaking');
  const [muteCooldowned, setMuteCooldowned] = useState(false);

  const displayName = peer?.displayName || t('room.you');
  const linkState = peer?.link; // 'connecting' | 'connected' | 'failed' | undefined (cho chính mình)
  const isGrace = Boolean(grace || peer?.grace);
  const showHostBadge = Boolean(isHost || peer?.role === 'HOST');
  const isPeerHost = peer?.role === 'HOST' || isHost;
  const isOtherMuted = Boolean(micMuted || peer?.micMuted);

  const showMuteButton = Boolean(
    !isSelf &&
      canMute &&
      !isOtherMuted &&
      linkState === 'connected' &&
      !isGrace
  );

  const handleMuteClick = () => {
    if (muteCooldowned || !onMute) return;
    setMuteCooldowned(true);
    onMute(peer.userId);
    setTimeout(() => setMuteCooldowned(false), 2000);
  };

  return (
    <article
      className={`speaking-peer-tile glass-card ${isSelf ? 'speaking-peer-tile--self' : ''} ${
        isGrace ? 'speaking-peer-tile--grace' : ''
      }`}
    >
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
        {!isSelf && isOtherMuted && (
          <div
            className="speaking-peer-tile__mic-badge speaking-peer-tile__mic-badge--muted"
            title={t('room.peerMuted', { name: displayName })}
            aria-label={t('room.peerMuted', { name: displayName })}
          >
            <span className="material-symbols-outlined">mic_off</span>
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
          {showHostBadge && (
            <span className="speaking-peer-badge speaking-peer-badge--host">
              {t('room.host')}
            </span>
          )}
        </div>

        {!isSelf && (
          <div
            className={`speaking-peer-tile__link ${
              isGrace ? 'speaking-peer-tile__link--grace' : linkState ? `speaking-peer-tile__link--${linkState}` : ''
            }`}
          >
            <span className="speaking-peer-tile__link-dot" />
            <span className="speaking-peer-tile__link-text">
              {isGrace
                ? isPeerHost
                  ? t('room.link.hostGrace')
                  : t('room.link.grace')
                : linkState
                ? t(`room.link.${linkState}`)
                : ''}
            </span>
          </div>
        )}

        {showMuteButton && (
          <div className="speaking-peer-tile__actions">
            <button
              type="button"
              className="speaking-peer-tile__mute-btn btn-secondary"
              onClick={handleMuteClick}
              disabled={muteCooldowned}
              title={t('room.muteAction', { name: displayName })}
              aria-label={t('room.muteAction', { name: displayName })}
            >
              <span className="material-symbols-outlined">mic_off</span>
              <span>{t('room.mute')}</span>
            </button>
          </div>
        )}
      </div>
    </article>
  );
};

export default PeerTile;
