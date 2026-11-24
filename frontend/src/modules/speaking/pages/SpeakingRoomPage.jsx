import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import ConfirmModal from '../../../components/ConfirmModal/ConfirmModal';
import EmptyState from '../../../components/EmptyState/EmptyState';
import PageLoading from '../../../components/PageLoading/PageLoading';
import Toast from '../../../components/Toast/Toast';
import { getBansApi } from '../speakingApi';
import BannedListPanel from '../components/BannedListPanel/BannedListPanel';
import ChatPanel from '../components/ChatPanel/ChatPanel';
import LeaveRoomDialog from '../components/LeaveRoomDialog/LeaveRoomDialog';
import PeerTile from '../components/PeerTile/PeerTile';
import RemoteAudio from '../components/RemoteAudio/RemoteAudio';
import RoomControls from '../components/RoomControls/RoomControls';
import RoomClock from '../components/RoomClock/RoomClock';
import SummaryModal from '../components/SummaryModal/SummaryModal';
import { useSpeakingRoom } from '../context/SpeakingRoomContext';
import './SpeakingRoomPage.css';

const renderRoomPhaseScreen = ({
  phase,
  micError,
  error,
  closedReason,
  kicked,
  roomCode,
  t,
  tApi,
  onNavigate,
  startSession,
}) => {
  if (phase === 'preparing') {
    return (
      <main className="speaking-room-page">
        <PageLoading message={t('room.preparing')} />
      </main>
    );
  }

  if (phase === 'error') {
    if (micError) {
      const micErrorTitle =
        micError === 'MIC_DENIED' ? t('room.micDenied') : t('room.micUnavailable');

      return (
        <main className="speaking-room-page speaking-room-page--center">
          <div className="speaking-room-error-wrapper">
            <EmptyState
              icon="mic_off"
              isError
              title={micErrorTitle}
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
      <main className="speaking-room-page">
        <div className="speaking-room-container">
          <EmptyState
            icon="error"
            isError
            title={errorTitle}
            actionLabel={t('room.backToLobby')}
            onAction={() => onNavigate('/speaking')}
            actionVariant="primary"
          />
        </div>
      </main>
    );
  }

  if (phase !== 'closed') return null;

  if (closedReason === 'KICKED' || kicked) {
    return (
      <main className="speaking-room-page">
        <div className="speaking-room-container">
          <EmptyState
            icon="block"
            isError
            title={t('room.kicked.title')}
            description={t('room.kicked.body')}
            actionLabel={t('room.kicked.backToLobby')}
            onAction={() => onNavigate('/speaking')}
            actionVariant="primary"
          />
        </div>
      </main>
    );
  }

  const closeDesc = closedReason
    ? t(`room.closed.${closedReason}`, { defaultValue: t('room.closed.EMPTY') })
    : t('room.closed.EMPTY');

  return (
    <main className="speaking-room-page">
      <div className="speaking-room-container">
        <EmptyState
          icon="meeting_room"
          title={t('room.closedTitle')}
          description={closeDesc}
          actionLabel={t('room.backToLobby')}
          onAction={() => onNavigate('/speaking')}
          actionVariant="primary"
        />
      </div>
    </main>
  );
};

const getNoticeMessage = ({ notice, selfId, peers, t }) => {
  if (notice?.type === 'hostChanged') {
    return String(notice.userId) === String(selfId)
      ? t('room.youAreHost')
      : t('room.hostChanged', {
          name: peers[notice.userId]?.displayName || t('room.host'),
        });
  }

  const noticeKeys = {
    mutedByHost: 'room.mutedByHost',
    peerKicked: 'room.peerKicked',
  };
  const translationKey = noticeKeys[notice?.type];

  return translationKey
    ? t(translationKey, {
        name: peers[notice.userId]?.displayName || t('room.host'),
      })
    : '';
};

const RoomHeader = ({
  room,
  currentCode,
  currentTopic,
  copied,
  isPrivate,
  onCopyCode,
  t,
}) => (
  <header className="speaking-room-header glass-card">
    <div className="speaking-room-header__main">
      <div className="speaking-room-header__title-group">
        <div className="speaking-room-header__code-row">
          <span className="speaking-room-header__code" title={currentCode}>
            {currentCode}
          </span>
          <span className="speaking-room-header__clock">
            <span className="material-symbols-outlined">timer</span>
            <RoomClock joinedAtMs={room?.joinedAtMs} />
          </span>
          <button
            type="button"
            className={`btn-secondary speaking-room-header__copy-btn ${
              copied ? 'speaking-room-header__copy-btn--copied' : ''
            }`}
            onClick={onCopyCode}
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
            <span className="material-symbols-outlined">{isPrivate ? 'lock' : 'public'}</span>
            <span>{isPrivate ? t('room.private') : t('room.public')}</span>
          </span>
        </div>
        <h1 className="text-title-lg speaking-room-header__topic">{currentTopic}</h1>
      </div>
      {isPrivate && (
        <p className="speaking-room-header__private-hint text-body-xs text-disabled">
          <span className="material-symbols-outlined">info</span>
          <span>{t('room.privateHint')}</span>
        </p>
      )}
    </div>
  </header>
);

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
    kickMember,
    unbanMember,
    kicked,
    summary,
    clearSummary,
    transferHostAndLeave,
    endRoom,
    startSession,
    leaveRoom,
    teardown,
    toggleMute,
    isPrivate,
    chatMessages,
    chatUnread,
    chatOpen,
    setChatOpen,
    sendChatMessage,
  } = useSpeakingRoom();

  const [copied, setCopied] = useState(false);
  const [leaveDialogOpen, setLeaveDialogOpen] = useState(false);
  const [leaveBusy, setLeaveBusy] = useState(false);
  const [leaveError, setLeaveError] = useState(null);
  const [kickTarget, setKickTarget] = useState(null); // { userId, name }
  const [kickLoading, setKickLoading] = useState(false);
  const [kickError, setKickError] = useState(null);
  const [bansOpen, setBansOpen] = useState(false);
  const [bansCount, setBansCount] = useState(0);
  const copyTimeoutRef = useRef(null);

  useEffect(() => {
    startSession(roomCode);
    return () => teardown();
  }, [roomCode, startSession, teardown]);

  // Tự động tải số lượng người bị cấm nếu là Host
  useEffect(() => {
    if (!isHost || !roomCode) return;
    let isMounted = true;
    getBansApi(roomCode)
      .then((res) => {
        if (!isMounted) return;
        const list = Array.isArray(res) ? res : res?.data ?? [];
        setBansCount(list.length);
      })
      .catch(() => {
        // im lặng nếu chưa tải được
      });
    return () => {
      isMounted = false;
    };
  }, [isHost, roomCode]);

  const handleDismissNotice = useCallback(() => {
    setNotice(null);
  }, [setNotice]);

  const noticeMessage = getNoticeMessage({ notice, selfId, peers, t });

  const handleLeave = async () => {
    const result = await leaveRoom();
    if (!result) onNavigate('/speaking');
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
      const result = await transferHostAndLeave(newHostId);
      setLeaveDialogOpen(false);
      if (!result) onNavigate('/speaking');
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
      const result = await endRoom();
      setLeaveDialogOpen(false);
      if (!result) onNavigate('/speaking');
    } catch (e) {
      setLeaveError(e?.code || 'UNKNOWN');
    } finally {
      setLeaveBusy(false);
    }
  };

  const handleKickConfirm = async () => {
    if (!kickTarget) return;
    setKickLoading(true);
    setKickError(null);
    try {
      await kickMember(kickTarget.userId);
      setKickTarget(null);
      setBansCount((prev) => prev + 1);
    } catch (e) {
      setKickError(e?.code || 'UNKNOWN');
    } finally {
      setKickLoading(false);
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

  const phaseScreen = renderRoomPhaseScreen({
    phase,
    micError,
    error,
    closedReason,
    kicked,
    roomCode,
    t,
    tApi,
    onNavigate,
    startSession,
  });

  if (phaseScreen) return phaseScreen;

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

        <RoomHeader
          room={room}
          currentCode={currentCode}
          currentTopic={currentTopic}
          copied={copied}
          isPrivate={isPrivate}
          onCopyCode={handleCopyCode}
          t={t}
        />

        {/* Bố cục chính: Cột trái (Người tham gia + Quy định) & Cột phải (Khung chat) */}
        <div className={`speaking-room-body ${chatOpen ? 'speaking-room-body--with-chat' : ''}`}>
          <div className="speaking-room-main-col">
            {/* Thanh tiêu đề Người tham gia */}
            <div className="speaking-room-stage__header">
              <div className="speaking-room-stage__header-left">
                <span className="material-symbols-outlined speaking-room-stage__header-icon">group</span>
                <h2 className="speaking-room-stage__title">
                  {t('room.participantsWithCount', { count: peerList.length })}
                </h2>
              </div>
              {isHost && (
                <div className="speaking-room-stage__header-right">
                  <button
                    type="button"
                    className="btn-secondary speaking-room-header__bans-btn"
                    onClick={() => setBansOpen(true)}
                    title={t('room.bans.openWithCount', {
                      count: bansCount,
                      defaultValue: `${t('room.bans.open')} (${bansCount})`,
                    })}
                    aria-label={t('room.bans.openWithCount', {
                      count: bansCount,
                      defaultValue: `${t('room.bans.open')} (${bansCount})`,
                    })}
                  >
                    <span className="material-symbols-outlined">group_off</span>
                    <span>
                      {t('room.bans.openWithCount', {
                        count: bansCount,
                        defaultValue: `${t('room.bans.open')} (${bansCount})`,
                      })}
                    </span>
                  </button>
                </div>
              )}
            </div>

            {/* Khu vực phòng: Nếu chỉ có 1 người (đang chờ) thì chia 2 cột: User bên trái, Đang chờ bên phải */}
            {otherPeers.length === 0 ? (
              <div className="speaking-room-solo-stage">
                <div className="speaking-room-solo-stage__user">
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
                        canKick={isHost}
                        onKick={(userId, name) => {
                          setKickError(null);
                          setKickTarget({ userId, name });
                        }}
                      />
                    );
                  })}
                </div>

                <div className="speaking-room-waiting-card glass-card">
                  <span className="material-symbols-outlined speaking-room-waiting-icon">
                    record_voice_over
                  </span>
                  <div className="speaking-room-waiting-text">
                    <h2 className="text-title-md speaking-room-waiting-title">
                      {t('room.waitingOthers')}
                    </h2>
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
              </div>
            ) : (
              /* Lưới các PeerTile khi có từ 2 người trở lên */
              <div
                className={`speaking-peer-grid speaking-peer-grid--count-${Math.min(
                  peerList.length,
                  4
                )} ${chatOpen ? 'speaking-peer-grid--with-chat' : 'speaking-peer-grid--no-chat'}`}
              >
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
                      canKick={isHost}
                      onKick={(userId, name) => {
                        setKickError(null);
                        setKickTarget({ userId, name });
                      }}
                    />
                  );
                })}
              </div>
            )}

            {/* Khối Quy tắc & Quy định phòng */}
            <section className="speaking-room-rules glass-card" aria-label={t('room.rules.title')}>
              <div className="speaking-room-rules__header">
                <div className="speaking-room-rules__title-group">
                  <span className="material-symbols-outlined speaking-room-rules__icon">assignment</span>
                  <h3 className="speaking-room-rules__title">{t('room.rules.title')}</h3>
                  <span className="speaking-room-rules__tag">{t('room.rules.tag')}</span>
                </div>
                <div className="speaking-room-rules__status">
                  <span className="material-symbols-outlined speaking-room-rules__signal">signal_cellular_alt</span>
                </div>
              </div>

              <div className="speaking-room-rules__grid">
                <div className="speaking-room-rules__item">
                  <span className="material-symbols-outlined speaking-room-rules__item-icon">handshake</span>
                  <span className="speaking-room-rules__item-text">{t('room.rules.respect')}</span>
                </div>
                <div className="speaking-room-rules__item">
                  <span className="material-symbols-outlined speaking-room-rules__item-icon">language</span>
                  <span className="speaking-room-rules__item-text">{t('room.rules.englishPriority')}</span>
                </div>
                <div className="speaking-room-rules__item">
                  <span className="material-symbols-outlined speaking-room-rules__item-icon">mic_off</span>
                  <span className="speaking-room-rules__item-text">{t('room.rules.muteWhenSilent')}</span>
                </div>
                <div className="speaking-room-rules__item">
                  <span className="material-symbols-outlined speaking-room-rules__item-icon">block</span>
                  <span className="speaking-room-rules__item-text">{t('room.rules.noSpam')}</span>
                </div>
              </div>
            </section>
          </div>

          {/* Cột Chat cố định bên phải */}
          {chatOpen && (
            <div className="speaking-room-chat-col">
              <ChatPanel
                open={chatOpen}
                messages={chatMessages}
                selfId={selfId}
                onSend={sendChatMessage}
                onClose={() => setChatOpen(false)}
              />
            </div>
          )}
        </div>

        {/* Thanh điều khiển Micro & Rời phòng */}
        <RoomControls
          muted={muted}
          onToggleMute={toggleMute}
          onLeave={handleLeaveClick}
          leaving={phase === 'leaving'}
          chatOpen={chatOpen}
          chatUnread={chatUnread}
          onToggleChat={() => setChatOpen(!chatOpen)}
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

        {/* Modal xác nhận Host kick thành viên */}
        <ConfirmModal
          isOpen={Boolean(kickTarget)}
          title={t('room.kickConfirm.title', { name: kickTarget?.name })}
          message={
            kickError
              ? `${t('room.kickConfirm.body')} (${tApi(`error.${kickError}`, {
                  defaultValue: tApi('common.fallback'),
                })})`
              : t('room.kickConfirm.body')
          }
          confirmText={t('room.kickConfirm.confirm')}
          cancelText={t('room.kickConfirm.cancel')}
          onConfirm={handleKickConfirm}
          onCancel={() => !kickLoading && setKickTarget(null)}
          isDanger
          isLoading={kickLoading}
        />

        {/* Modal Danh sách người bị chặn */}
        <BannedListPanel
          open={bansOpen}
          roomCode={currentCode}
          onClose={() => setBansOpen(false)}
          onUnban={unbanMember}
          onBansCountChange={setBansCount}
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

        <SummaryModal
          open={Boolean(summary)}
          summary={summary}
          onClose={() => {
            clearSummary();
            onNavigate('/speaking');
          }}
          onNavigate={() => {
            clearSummary();
            onNavigate('/speaking/history');
          }}
        />
      </div>
    </main>
  );
};

export default SpeakingRoomPage;
