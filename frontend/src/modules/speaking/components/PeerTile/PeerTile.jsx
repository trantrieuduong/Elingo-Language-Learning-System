import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import SpeakingAvatar from '../SpeakingAvatar/SpeakingAvatar';
import './PeerTile.css';

const getPeerDisplayState = ({ peer, grace, isHost, micMuted, t }) => ({
  displayName: peer?.displayName || t('room.you'),
  isGrace: Boolean(grace || peer?.grace),
  showHostBadge: Boolean(isHost || peer?.role === 'HOST'),
  isPeerHost: peer?.role === 'HOST' || isHost,
  isOtherMuted: Boolean(micMuted || peer?.micMuted),
  linkState: peer?.link,
});

const getLinkPresentation = ({ isGrace, isPeerHost, linkState, t }) => {
  if (isGrace) {
    return {
      className: 'speaking-peer-tile__link--grace',
      label: isPeerHost ? t('room.link.hostGrace') : t('room.link.grace'),
    };
  }

  return {
    className: linkState ? `speaking-peer-tile__link--${linkState}` : '',
    label: linkState ? t(`room.link.${linkState}`) : '',
  };
};

const getPeerActionState = ({ isSelf, canMute, isOtherMuted, linkState, isGrace, canKick }) => ({
  showMuteButton: Boolean(!isSelf && canMute && !isOtherMuted && linkState === 'connected' && !isGrace),
  showKickButton: Boolean(!isSelf && canKick),
});

const PeerActions = ({
  displayName,
  muteCooldowned,
  onKick,
  onMute,
  showKickButton,
  showMuteButton,
  t,
}) => {
  if (!showMuteButton && !showKickButton) return null;

  return (
    <div className="speaking-peer-tile__actions">
      {showMuteButton && (
        <button
          type="button"
          className="speaking-peer-tile__mute-btn btn-secondary"
          onClick={onMute}
          disabled={muteCooldowned}
          title={t('room.muteAction', { name: displayName })}
          aria-label={t('room.muteAction', { name: displayName })}
        >
          <span className="material-symbols-outlined">mic_off</span>
          <span>{t('room.mute')}</span>
        </button>
      )}
      {showKickButton && (
        <button
          type="button"
          className="speaking-peer-tile__kick-btn btn-secondary"
          onClick={onKick}
          title={t('room.kickAction', { name: displayName })}
          aria-label={t('room.kickAction', { name: displayName })}
        >
          <span className="material-symbols-outlined">person_remove</span>
          <span>{t('room.kick')}</span>
        </button>
      )}
    </div>
  );
};

const PeerTile = ({
  peer,
  isSelf = false,
  isHost = false,
  muted = false,
  grace = false,
  micMuted = false,
  canMute = false,
  onMute,
  canKick = false,
  onKick,
}) => {
  const { t } = useTranslation('speaking');
  const [muteCooldowned, setMuteCooldowned] = useState(false);

  const { displayName, isGrace, showHostBadge, isPeerHost, isOtherMuted, linkState } =
    getPeerDisplayState({ peer, grace, isHost, micMuted, t });
  const { className: linkClassName, label: linkLabel } = getLinkPresentation({
    isGrace,
    isPeerHost,
    linkState,
    t,
  });
  const { showMuteButton, showKickButton } = getPeerActionState({
    isSelf,
    canMute,
    isOtherMuted,
    linkState,
    isGrace,
    canKick,
  });
  const isMuted = (isSelf && muted) || (!isSelf && isOtherMuted);

  const handleMuteClick = () => {
    if (muteCooldowned || !onMute) return;
    setMuteCooldowned(true);
    onMute(peer.userId);
    setTimeout(() => setMuteCooldowned(false), 2000);
  };

  const handleKickClick = () => {
    if (!onKick) return;
    onKick(peer.userId, displayName);
  };

  return (
    <article
      className={`speaking-peer-tile glass-card ${
        isSelf ? 'speaking-peer-tile--self' : ''
      } ${isGrace ? 'speaking-peer-tile--grace' : ''}`}
    >
      <div className="speaking-peer-tile__avatar-wrapper">
        <SpeakingAvatar
          name={displayName}
          src={peer?.avatarUrl}
          size={84}
          className="speaking-peer-tile__avatar"
        />
      </div>

      <div className="speaking-peer-tile__info">
        <div className="speaking-peer-tile__name-row">
          <span className="speaking-peer-tile__name" title={displayName}>
            {displayName}
          </span>
        </div>

        {!isSelf && (
          <div
            className={`speaking-peer-tile__link ${linkClassName}`}
          >
            <span className="speaking-peer-tile__link-dot" />
            <span className="speaking-peer-tile__link-text">{linkLabel}</span>
          </div>
        )}

        <div className="speaking-peer-tile__badges-and-actions">
          {showHostBadge && (
            <span className="speaking-peer-badge speaking-peer-badge--host">
              {t('room.host')}
            </span>
          )}
          {isSelf && (
            <span className="speaking-peer-badge speaking-peer-badge--self">
              {t('room.you')}
            </span>
          )}

          <PeerActions
            displayName={displayName}
            muteCooldowned={muteCooldowned}
            onKick={handleKickClick}
            onMute={handleMuteClick}
            showKickButton={showKickButton}
            showMuteButton={showMuteButton}
            t={t}
          />
        </div>
      </div>

      {/* Trạng thái mic */}
      <div className="speaking-peer-tile__status-icon">
        <span className={`material-symbols-outlined ${isMuted ? 'speaking-peer-tile__status-icon--muted' : 'speaking-peer-tile__status-icon--active'}`}>
          {isMuted ? 'mic_off' : 'mic'}
        </span>
      </div>
    </article>
  );
};

export default PeerTile;
