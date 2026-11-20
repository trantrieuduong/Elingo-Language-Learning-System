import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import PageLoading from '../../../components/PageLoading/PageLoading';
import SpeakingAvatar from '../components/SpeakingAvatar/SpeakingAvatar';
import { useSpeakingSocket } from '../context/SpeakingSocketContext';
import './SpeakingRoomPage.css';

const SpeakingRoomPage = ({ roomCode, onNavigate }) => {
  const { t } = useTranslation('speaking');
  const {
    connection,
    roomState,
    leftSummary,
    error,
    joinRoom,
    leaveRoom,
  } = useSpeakingSocket();

  const joinedRef = useRef(false);
  const hadStateOnMountRef = useRef(roomState?.roomCode === roomCode);
  const leaveTimeoutRef = useRef(null);
  const staleErrorRef = useRef(error);
  const staleLeftRef = useRef(leftSummary);

  // Chỉ gửi join khi socket đã nối (chống F5 rớt kết nối bị đá về lobby)
  useEffect(() => {
    if (connection !== 'connected') {
      joinedRef.current = false; // rớt mạng: nối lại xong sẽ join lại (idempotent)
      hadStateOnMountRef.current = false;
      return;
    }
    if (!roomCode || joinedRef.current || hadStateOnMountRef.current) return;
    joinedRef.current = true;
    joinRoom(roomCode);
  }, [connection, roomCode, joinRoom]);

  // Xử lý khi nhận được event error từ WebSocket
  useEffect(() => {
    if (!error) {
      staleErrorRef.current = null;
      return;
    }
    if (error === 'SOCKET_NOT_CONNECTED') {
      return;
    }
    if (error === staleErrorRef.current) {
      return;
    }
    onNavigate('/speaking');
  }, [error, onNavigate]);

  // Xử lý khi rời phòng thành công (nhận room:left)
  useEffect(() => {
    if (!leftSummary) {
      staleLeftRef.current = null;
      return;
    }
    if (leftSummary === staleLeftRef.current) {
      return;
    }
    if (leaveTimeoutRef.current) clearTimeout(leaveTimeoutRef.current);
    onNavigate('/speaking');
  }, [leftSummary, onNavigate]);

  const handleLeave = () => {
    leaveRoom();
    // nếu 3 giây không nhận được room:left thì vẫn về lobby
    leaveTimeoutRef.current = setTimeout(() => {
      onNavigate('/speaking');
    }, 3000);
  };

  useEffect(() => {
    return () => {
      if (leaveTimeoutRef.current) clearTimeout(leaveTimeoutRef.current);
    };
  }, []);

  if (!roomState) {
    return (
      <main className="speaking-room-page">
        <PageLoading />
      </main>
    );
  }

  const peers = roomState.peers || [];

  return (
    <main className="speaking-room-page">
      <div className="speaking-room-container">
        {/* Room Header */}
        <section className="speaking-room-header glass-card">
          <div className="speaking-room-header__info">
            <div className="speaking-room-header__title-row">
              <h1 className="text-headline-sm speaking-room-header__code">
                {t('room.placeholderTitle', { code: roomState.roomCode || roomCode })}
              </h1>
              <span
                className={`speaking-room-badge ${
                  roomState.isPrivate
                    ? 'speaking-room-badge--private'
                    : 'speaking-room-badge--public'
                }`}
              >
                <span className="material-symbols-outlined">
                  {roomState.isPrivate ? 'lock' : 'public'}
                </span>
                <span>{roomState.isPrivate ? t('room.private') : t('room.public')}</span>
              </span>
            </div>

            {roomState.topic && (
              <p className="text-body-md speaking-room-header__topic">
                {roomState.topic}
              </p>
            )}
          </div>

          <div className="speaking-room-header__actions">
            <button
              type="button"
              className="btn-danger speaking-leave-btn"
              onClick={handleLeave}
            >
              <span className="material-symbols-outlined">call_end</span>
              <span>{t('room.leave')}</span>
            </button>
          </div>
        </section>

        {/* Participants Grid (Placeholder) */}
        <section className="speaking-room-peers-section">
          <div className="speaking-peers-grid">
            {peers.map((peer) => (
              <div key={peer.userId} className="speaking-peer-placeholder-tile glass-card">
                <SpeakingAvatar
                  name={peer.displayName || ''}
                  src={peer.avatarUrl || ''}
                  size={64}
                />
                <span className="speaking-peer-name">{peer.displayName || 'User'}</span>
                {peer.role === 'HOST' && (
                  <span className="speaking-peer-host-chip">HOST</span>
                )}
              </div>
            ))}
          </div>
        </section>
      </div>
    </main>
  );
};

export default SpeakingRoomPage;
