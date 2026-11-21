import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EmptyState from '../../../components/EmptyState/EmptyState';
import PageLoading from '../../../components/PageLoading/PageLoading';
import Toast from '../../../components/Toast/Toast';
import LeaveRoomDialog from '../components/LeaveRoomDialog/LeaveRoomDialog';
import PeerTile from '../components/PeerTile/PeerTile';
import RemoteAudio from '../components/RemoteAudio/RemoteAudio';
import RoomControls from '../components/RoomControls/RoomControls';
import { useSpeakingRoom } from '../context/SpeakingRoomContext';
import './SpeakingRoomPage.css';

const SpeakingRoomPage = ({ roomCode, onNavigate }) => {
  const { t } = useTranslation('speaking');
  const { t: tApi } = useTranslation('api');

  const {
    selfId,
    connection,
    phase,
    room,
    peers,
    streams,
    muted,
    micError,
    error,
    closedReason,
    notice,
    setNotice,
    isHost,
    requestMute,
    transferHostAndLeave,
    endRoom,
    startSession,
    leaveRoom,
    teardown,
    toggleMute,
    isPrivate,
  } = useSpeakingRoom();

  const [copied, setCopied] = useState(false);
  const [leaveDialogOpen, setLeaveDialogOpen] = useState(false);
  const [leaveBusy, setLeaveBusy] = useState(false);
  const [leaveError, setLeaveError] = useState(null);
  const copyTimeoutRef = useRef(null);

  useEffect(() => {
    startSession(roomCode);
    return () => teardown();
  }, [roomCode, startSession, teardown]);

  const handleDismissNotice = useCallback(() => {
    setNotice(null);
  }, [setNotice]);

  const noticeMessage =
    notice?.type === 'hostChanged'
      ? String(notice.userId) === String(selfId)
        ? t('room.youAreHost')
        : t('room.hostChanged', {
            name: peers[notice.userId]?.displayName || t('room.host'),
          })
      : notice?.type === 'mutedByHost'
      ? t('room.mutedByHost', {
          name: peers[notice.userId]?.displayName || t('room.host'),
        })
      : '';

  const handleLeave = async () => {
    await leaveRoom();
    onNavigate('/speaking');
  };

  const handleLeaveClick = () => {
    // Nếu là host và còn người khác trong phòng -> mở modal chuyển host/kết thúc phòng
    // Nếu chỉ có 1 mình host (hoặc là guest) -> rời phòng luôn
    if (isHost && otherPeers.length > 0) {
      setLeaveError(null);
      setLeaveDialogOpen(true);
    } else {
      handleLeave();
    }
  };

  const handleTransferAndLeave = async (newHostId) => {
    setLeaveBusy(true);
    setLeaveError(null);
    try {
      await transferHostAndLeave(newHostId);
      setLeaveDialogOpen(false);
      onNavigate('/speaking');
    } catch (e) {
      setLeaveError(e?.code || 'UNKNOWN');
    } finally {
      setLeaveBusy(false);
    }
  };

  const handleEndRoom = async () => {
    setLeaveBusy(true);
    setLeaveError(null);
    try {
      await endRoom();
      setLeaveDialogOpen(false);
      onNavigate('/speaking');
    } catch (e) {
      setLeaveError(e?.code || 'UNKNOWN');
    } finally {
      setLeaveBusy(false);
    }
  };

  const handleCopyCode = async () => {
    const code = room?.roomCode || roomCode;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(code);
        setCopied(true);
        if (copyTimeoutRef.current) clearTimeout(copyTimeoutRef.current);
        copyTimeoutRef.current = setTimeout(() => setCopied(false), 2000);
      }
    } catch {
      // Fallback không ném lỗi nếu clipboard bị chặn
    }
  };

  useEffect(() => {
    return () => {
      if (copyTimeoutRef.current) clearTimeout(copyTimeoutRef.current);
    };
  }, []);

  // 1. Chuẩn bị (xin micro + kết nối socket)
  if (phase === 'preparing') {
    return (
      <main className="speaking-room-page">
        <PageLoading message={t('room.preparing')} />
      </main>
    );
  }

  // 2. Giai đoạn lỗi (micro hoặc lỗi vào phòng)
  if (phase === 'error') {
    if (micError) {
      return (
        <main className="speaking-room-page speaking-room-page--center">
          <div className="speaking-room-error-wrapper">
            <EmptyState
              icon="mic_off"
              isError
              title={
                micError === 'MIC_DENIED'
                  ? t('room.micDenied')
                  : t('room.micUnavailable')
              }
              actionLabel={t('room.retry')}
              onAction={() => startSession(roomCode)}
              actionVariant="primary"
            />
            <div className="speaking-room-error-subaction">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => onNavigate('/speaking')}
              >
                {t('room.backToLobby')}
              </button>
            </div>
          </div>
        </main>
      );
    }

    const errorTitle =
      error === 'SPEAKING_ROOM_ACCESS_DENIED'
        ? t('room.removed')
        : error
        ? tApi(`error.${error}`, { defaultValue: tApi('common.fallback') })
        : tApi('common.fallback');

    return (
      <main className="speaking-room-page speaking-room-page--center">
        <EmptyState
          icon="error"
          isError
          title={errorTitle}
          actionLabel={t('room.backToLobby')}
          onAction={() => onNavigate('/speaking')}
          actionVariant="primary"
        />
      </main>
    );
  }

  // 3. Phòng bị đóng
  if (phase === 'closed') {
    const closeDesc = closedReason
      ? t(`room.closed.${closedReason}`, { defaultValue: t('room.closed.EMPTY') })
      : t('room.closed.EMPTY');

    return (
      <main className="speaking-room-page speaking-room-page--center">
        <EmptyState
          icon="meeting_room"
          title={t('room.closedTitle')}
          description={closeDesc}
          actionLabel={t('room.backToLobby')}
          onAction={() => onNavigate('/speaking')}
          actionVariant="primary"
        />
      </main>
    );
  }

  const currentCode = room?.roomCode || roomCode;
  const currentTopic = room?.topic || t('lobby.noTopic');
  const peerList = Object.values(peers);
  const otherPeers = peerList.filter((p) => String(p.userId) !== String(selfId));

  return (
    <main className="speaking-room-page">
      <div className="speaking-room-container">
        {/* Banner mất kết nối WebSocket */}
        {connection !== 'connected' && (
          <div className="speaking-connection-banner" role="alert">
            <span className="material-symbols-outlined">wifi_off</span>
            <span>{t('room.reconnecting')}</span>
          </div>
        )}

        {/* Toast thông báo (đổi host) */}
        <Toast
          message={noticeMessage}
          variant="success"
          onDismiss={handleDismissNotice}
          duration={4000}
        />

        {/* Header thông tin phòng */}
        <header className="speaking-room-header glass-card">
          <div className="speaking-room-header__main">
            <div className="speaking-room-header__title-group">
              <div className="speaking-room-header__code-row">
                <span className="speaking-room-header__code" title={currentCode}>
                  {currentCode}
                </span>
                <button
                  type="button"
                  className={`btn-secondary speaking-room-header__copy-btn ${
                    copied ? 'speaking-room-header__copy-btn--copied' : ''
                  }`}
                  onClick={handleCopyCode}
                  aria-label={t('room.copyAria', { code: currentCode })}
                  title={t('room.copyAria', { code: currentCode })}
                >
                  <span className="material-symbols-outlined">
                    {copied ? 'check' : 'content_copy'}
                  </span>
                  <span>{copied ? t('room.copied') : t('room.copy')}</span>
                </button>
                <span
                  className={`speaking-room-badge ${
                    isPrivate
                      ? 'speaking-room-badge--private'
                      : 'speaking-room-badge--public'
                  }`}
                >
                  <span className="material-symbols-outlined">
                    {isPrivate ? 'lock' : 'public'}
                  </span>
                  <span>{isPrivate ? t('room.private') : t('room.public')}</span>
                </span>
              </div>

              <h1 className="text-title-lg speaking-room-header__topic">
                {currentTopic}
              </h1>
            </div>

            {isPrivate && (
              <p className="speaking-room-header__private-hint text-body-xs text-disabled">
                <span className="material-symbols-outlined">info</span>
                <span>{t('room.privateHint')}</span>
              </p>
            )}
          </div>
        </header>

        {/* Khu vực hiển thị danh sách người tham gia */}
        <section className="speaking-room-stage" aria-label="Participants">
          {otherPeers.length === 0 ? (
            <div className="speaking-room-waiting-state glass-card">
              <span className="material-symbols-outlined speaking-room-waiting-icon">
                record_voice_over
              </span>
              <div className="speaking-room-waiting-text">
                <h2 className="text-title-md">{t('room.waitingOthers')}</h2>
                <p className="text-body-sm text-disabled">{t('room.shareCode')}</p>
              </div>
              <div className="speaking-room-waiting-code-box">
                <span className="speaking-room-waiting-code">{currentCode}</span>
                <button
                  type="button"
                  className="btn-secondary btn-sm"
                  onClick={handleCopyCode}
                >
                  <span className="material-symbols-outlined">
                    {copied ? 'check' : 'content_copy'}
                  </span>
                  <span>{copied ? t('room.copied') : t('room.copy')}</span>
                </button>
              </div>
            </div>
          ) : null}

          {/* Lưới các PeerTile */}
          <div className="speaking-peer-grid">
            {peerList.map((p) => {
              const pIsSelf = String(p.userId) === String(selfId);
              const pIsHost = p.role === 'HOST' || String(p.userId) === String(room?.hostId);
              return (
                <PeerTile
                  key={p.userId}
                  peer={p}
                  isSelf={pIsSelf}
                  isHost={pIsHost}
                  grace={Boolean(p.grace)}
                  muted={pIsSelf ? muted : false}
                  micMuted={Boolean(p.micMuted)}
                  canMute={isHost}
                  onMute={requestMute}
                />
              );
            })}
          </div>
        </section>

        {/* Thanh điều khiển Micro & Rời phòng */}
        <RoomControls
          muted={muted}
          onToggleMute={toggleMute}
          onLeave={handleLeaveClick}
          leaving={phase === 'leaving'}
        />

        {/* Dialog Host rời phòng */}
        <LeaveRoomDialog
          open={leaveDialogOpen}
          members={otherPeers}
          onClose={() => !leaveBusy && setLeaveDialogOpen(false)}
          onTransfer={handleTransferAndLeave}
          onEnd={handleEndRoom}
          busy={leaveBusy}
          error={leaveError}
        />

        {/* Audio elements ngầm cho từng peer khác */}
        <div className="speaking-remote-audios-container">
          {Object.entries(streams).map(([uid, stream]) => (
            <RemoteAudio
              key={uid}
              stream={stream}
            />
          ))}
        </div>
      </div>
    </main>
  );
};

export default SpeakingRoomPage;
