import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EmptyState from '../../../components/EmptyState/EmptyState';
import PageLoading from '../../../components/PageLoading/PageLoading';
import Pagination from '../../../components/Pagination/Pagination';
import SearchInput from '../../../components/SearchInput/SearchInput';
import Toast from '../../../components/Toast/Toast';
import CreateRoomModal from '../components/CreateRoomModal/CreateRoomModal';
import RoomCard from '../components/RoomCard/RoomCard';
import { useSpeakingSocket } from '../context/SpeakingSocketContext';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import { getSpeakingRoomsApi } from '../speakingApi';
import './SpeakingLobbyPage.css';

const CODE_REGEX = /^[A-HJ-NP-Z2-9]{6}$/;

const SpeakingLobbyPage = ({ onNavigate }) => {
  const { t } = useTranslation('speaking');
  const { t: tApi } = useTranslation('api');
  const {
    connection,
    busy,
    error: socketError,
    roomState,
    createdRoomCode,
    activeRoomCode,
    activeRoomChecked,
    createRoom,
    joinRoom,
    checkActiveRoom,
    clearError,
    clearCreated,
  } = useSpeakingSocket();

  // Search & Filter state
  const [keywordInput, setKeywordInput] = useState('');
  const debouncedKeyword = useDebouncedValue(keywordInput, 400);
  const [maxParticipants, setMaxParticipants] = useState(null); // null | 2 | 3 | 4
  const [page, setPage] = useState(1);

  // danh sách phòng từ REST
  const [rooms, setRooms] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loadingRooms, setLoadingRooms] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // UI state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [inputCode, setInputCode] = useState('');
  const [localError, setLocalError] = useState('');

  const activeReqIdRef = useRef(0);
  const pendingJoinRef = useRef(null);

  const startJoin = (code) => {
    clearError();
    setLocalError('');
    pendingJoinRef.current = code;
    joinRoom(code);
  };

  // khi kết nối socket thành công thì check active room
  useEffect(() => {
    if (connection === 'connected') {
      checkActiveRoom();
    }
  }, [connection, checkActiveRoom]);

  // khi tạo phòng thành công thì chuyển sang phòng mới
  useEffect(() => {
    if (createdRoomCode) {
      clearCreated();
      onNavigate(`/speaking/room/${createdRoomCode}`);
    }
  }, [createdRoomCode, clearCreated, onNavigate]);

  // Effect chuyển trang khi vào được phòng qua WebSocket
  useEffect(() => {
    const code = pendingJoinRef.current;
    if (code && roomState?.roomCode === code) {
      pendingJoinRef.current = null;
      onNavigate(`/speaking/room/${code}`);
    }
  }, [roomState, onNavigate]);

  // Dọn mã đang chờ khi có lỗi socket
  useEffect(() => {
    if (socketError) {
      pendingJoinRef.current = null;
    }
  }, [socketError]);

  // Cleanup khi lobby unmount
  useEffect(() => {
    return () => {
      pendingJoinRef.current = null;
    };
  }, []);

  useEffect(() => {
    setPage(1);
  }, [debouncedKeyword, maxParticipants]);

  // tải danh sách phòng từ REST API
  const fetchRooms = useCallback(async () => {
    const reqId = ++activeReqIdRef.current;
    setLoadingRooms(true);
    setLoadError(null);

    try {
      const res = await getSpeakingRoomsApi({
        page,
        size: 10,
        keyword: debouncedKeyword,
        maxParticipants: maxParticipants || undefined,
      });

      if (reqId === activeReqIdRef.current) {
        setRooms(res?.data?.content || []);
        setTotalPages(res?.data?.totalPages || 0);
      }
    } catch {
      if (reqId === activeReqIdRef.current) {
        setLoadError('loadError');
      }
    } finally {
      if (reqId === activeReqIdRef.current) {
        setLoadingRooms(false);
      }
    }
  }, [page, debouncedKeyword, maxParticipants]);

  useEffect(() => {
    fetchRooms();
  }, [fetchRooms]);

  const handleOpenCreateModal = () => {
    clearError();
    setLocalError('');
    setCreateModalOpen(true);
  };

  const handleCreateSubmit = (payload) => {
    createRoom(payload);
  };

  const handleJoinByCode = (e) => {
    e.preventDefault();
    clearError();
    setLocalError('');

    const code = (inputCode || '').trim().toUpperCase();
    if (code.length !== 6 || !CODE_REGEX.test(code)) {
      setLocalError(t('lobby.codeInvalid'));
      return;
    }

    startJoin(code);
  };

  const handleJoinFromCard = (roomCode) => {
    startJoin(roomCode);
  };

  const handleBackToActiveRoom = () => {
    clearError();
    setLocalError('');
    onNavigate(`/speaking/room/${activeRoomCode}`);
  };

  const handleClearFilters = () => {
    setKeywordInput('');
    setMaxParticipants(null);
    setPage(1);
  };

  const handleDismissToast = () => {
    clearError();
    setLocalError('');
  };

  const isSocketConnected = connection === 'connected';
  const hasActiveRoom = activeRoomChecked && Boolean(activeRoomCode);
  const isActionDisabled = busy || !isSocketConnected || hasActiveRoom;

  // Khi modal tạo phòng đang mở thì modal tự hiện lỗi (tránh hiện đôi)
  const generalSocketError =
    !createModalOpen && socketError && socketError !== 'SPEAKING_ALREADY_IN_ROOM' ? socketError : null;
  const generalSocketErrorMessage = generalSocketError
    ? generalSocketError === 'SOCKET_NOT_CONNECTED'
      ? t('error.socketNotConnected')
      : tApi(`error.${generalSocketError}`, { defaultValue: tApi('common.fallback') })
    : '';

  const toastMessage = localError || generalSocketErrorMessage;

  return (
    <main className="speaking-lobby-page">
      <div className="speaking-lobby-container">
        {/* Banner mất kết nối */}
        {!isSocketConnected && (
          <div className="speaking-connection-banner">
            <span className="material-symbols-outlined speaking-connection-banner__icon">
              wifi_off
            </span>
            <span className="speaking-connection-banner__text">
              {t('error.socketNotConnected')}
            </span>
          </div>
        )}

        {/* Banner phòng đang tham gia (active-room từ server) */}
        {hasActiveRoom && (
          <div className="speaking-active-room-banner">
            <div className="speaking-active-room-banner__content">
              <span className="material-symbols-outlined speaking-active-room-banner__icon">
                meeting_room
              </span>
              <div className="speaking-active-room-banner__text">
                <span className="speaking-active-room-banner__title">
                  {t('lobby.activeRoom.title', { code: activeRoomCode })}
                </span>
                <span className="speaking-active-room-banner__desc">
                  {t('lobby.activeRoom.desc')}
                </span>
              </div>
            </div>
            <button
              type="button"
              className="btn-primary speaking-active-room-banner__btn"
              onClick={handleBackToActiveRoom}
            >
              <span className="material-symbols-outlined">arrow_forward</span>
              <span>{t('lobby.activeRoom.back')}</span>
            </button>
          </div>
        )}

        {/* Tiêu đề trang và lối vào lịch sử */}
        <header className="speaking-lobby-header">
          <div className="speaking-lobby-header__left">
            <div className="speaking-lobby-header__icon-box glass-thumb">
              <span className="material-symbols-outlined speaking-lobby-header__icon">
                record_voice_over
              </span>
            </div>
            <div className="speaking-lobby-header__title-group">
              <h1 className="text-headline-md speaking-lobby-header__title">
                {t('lobby.title')}
              </h1>
              <p className="text-body-md text-disabled speaking-lobby-header__subtitle">
                {t('lobby.subtitle')}
              </p>
            </div>
          </div>

          <button
            type="button"
            className="btn-secondary speaking-lobby-history-link"
            onClick={() => onNavigate('/speaking/history')}
            aria-label={t('lobby.history')}
          >
            <span className="material-symbols-outlined">history</span>
            <span>{t('lobby.history')}</span>
          </button>
        </header>

        {/* 2 Card trên: Tạo phòng & Vào phòng bằng mã */}
        <section className="speaking-lobby-room-grid">
          {/* Card Tạo phòng */}
          <div className="speaking-lobby-action-card glass-card">
            <div className="speaking-lobby-action-card__header">
              <div className="speaking-lobby-action-card__icon-wrapper glass-thumb">
                <span className="material-symbols-outlined speaking-lobby-action-card__icon">
                  add_circle
                </span>
              </div>
              <div className="speaking-lobby-action-card__title-group">
                <h2 className="text-title-sm speaking-lobby-action-card__title">
                  {t('lobby.createRoom')}
                </h2>
                <p className="text-body-sm text-disabled speaking-lobby-action-card__desc">
                  {t('lobby.createRoomDesc')}
                </p>
              </div>
            </div>
            <button
              type="button"
              className="btn-primary speaking-lobby-action-card__action-btn"
              onClick={handleOpenCreateModal}
              disabled={isActionDisabled}
              title={hasActiveRoom ? t('lobby.activeRoom.blocked') : undefined}
            >
              {t('lobby.createRoom')}
            </button>
          </div>

          {/* Card Vào phòng bằng mã */}
          <div className="speaking-lobby-action-card glass-card">
            <div className="speaking-lobby-action-card__header">
              <div className="speaking-lobby-action-card__icon-wrapper glass-thumb">
                <span className="material-symbols-outlined speaking-lobby-action-card__icon">
                  login
                </span>
              </div>
              <div className="speaking-lobby-action-card__title-group">
                <h2 className="text-title-sm speaking-lobby-action-card__title">
                  {t('lobby.joinByCode')}
                </h2>
                <p className="text-body-sm text-disabled speaking-lobby-action-card__desc">
                  {t('lobby.joinByCodeDesc')}
                </p>
              </div>
            </div>

            <form onSubmit={handleJoinByCode} className="speaking-lobby-room-input-group">
              <input
                type="text"
                className="speaking-lobby-room-input"
                placeholder={t('lobby.codePlaceholder')}
                maxLength={6}
                autoComplete="off"
                spellCheck={false}
                value={inputCode}
                onChange={(e) => {
                  setInputCode(e.target.value.toUpperCase());
                  if (localError) setLocalError('');
                }}
                disabled={isActionDisabled}
              />
              <button
                type="submit"
                className="btn-primary speaking-lobby-room-join-btn"
                disabled={isActionDisabled || !inputCode.trim()}
                title={hasActiveRoom ? t('lobby.activeRoom.blocked') : undefined}
              >
                {t('lobby.join')}
              </button>
            </form>
          </div>
        </section>

        {/* Bộ lọc danh sách — Bố cục & giao diện đồng bộ với FilterSection của Deck và Lesson */}
        <section className="speaking-lobby-filters filter-section glass-well">
          <div className="speaking-lobby-filters__top">
            <SearchInput
              id="speaking-lobby-search"
              className="speaking-lobby-search-input"
              placeholder={t('lobby.searchPlaceholder')}
              value={keywordInput}
              onChange={(e) => setKeywordInput(e.target.value)}
              onClear={() => setKeywordInput('')}
              clearLabel={t('lobby.clearFilters')}
            />

            <div className="speaking-capacity-filter-group">
              <span className="speaking-capacity-filter-label text-label-md">
                {t('lobby.capacityFilter')}:
              </span>
              <div className="speaking-capacity-pills">
                <button
                  type="button"
                  className={`filter-pill ${
                    maxParticipants === null ? 'filter-pill--active' : ''
                  }`}
                  onClick={() => setMaxParticipants(null)}
                >
                  {t('lobby.capacityAll')}
                </button>
                {[2, 3, 4].map((cap) => (
                  <button
                    key={cap}
                    type="button"
                    className={`filter-pill ${
                      maxParticipants === cap ? 'filter-pill--active' : ''
                    }`}
                    onClick={() => setMaxParticipants(cap)}
                  >
                    {t('lobby.capacityOption', { count: cap })}
                  </button>
                ))}
              </div>
            </div>

            <div className="speaking-lobby-filters__actions">
              {(keywordInput.trim() || maxParticipants !== null) && (
                <button
                  type="button"
                  className="btn-clear-filter"
                  onClick={handleClearFilters}
                >
                  <span className="material-symbols-outlined">filter_alt_off</span>
                  <span>{t('lobby.clearFilters')}</span>
                </button>
              )}

              <button
                type="button"
                className="btn-secondary speaking-refresh-btn"
                onClick={fetchRooms}
                disabled={loadingRooms}
              >
                <span className={`material-symbols-outlined ${loadingRooms ? 'spinning' : ''}`}>
                  refresh
                </span>
                <span>{t('lobby.refresh')}</span>
              </button>
            </div>
          </div>
        </section>

        {/* Danh sách phòng */}
        <section className="speaking-lobby-rooms">
          <div className="speaking-lobby-rooms__header">
            <h2 className="text-title-lg speaking-lobby-rooms__title">
              {t('lobby.publicRooms')}
            </h2>
          </div>

          {loadingRooms ? (
            <PageLoading />
          ) : loadError ? (
            <EmptyState
              icon="error"
              isError
              title={t('lobby.loadError')}
              actionLabel={t('lobby.retry')}
              onAction={fetchRooms}
            />
          ) : rooms.length === 0 ? (
            debouncedKeyword.trim() || maxParticipants !== null ? (
              <EmptyState
                icon="filter_alt_off"
                title={t('lobby.emptyFiltered')}
                actionLabel={t('lobby.clearFilters')}
                onAction={handleClearFilters}
              />
            ) : (
              <EmptyState
                icon="forum"
                title={t('lobby.empty')}
                actionLabel={t('lobby.createRoom')}
                onAction={handleOpenCreateModal}
                actionVariant="primary"
                actionDisabled={isActionDisabled}
                actionTitle={hasActiveRoom ? t('lobby.activeRoom.blocked') : undefined}
              />
            )
          ) : (
            <>
              <div className="speaking-rooms-grid">
                {rooms.map((room) => (
                  <RoomCard
                    key={room.roomCode}
                    room={room}
                    joining={isActionDisabled}
                    disabledTitle={hasActiveRoom ? t('lobby.activeRoom.blocked') : undefined}
                    onJoin={handleJoinFromCard}
                  />
                ))}
              </div>

              <Pagination
                currentPage={page}
                totalPages={totalPages}
                onPageChange={(newPage) => setPage(newPage)}
              />
            </>
          )}
        </section>
      </div>

      <Toast message={toastMessage} variant="error" onDismiss={handleDismissToast} />

      <CreateRoomModal
        open={createModalOpen}
        onClose={() => {
          setCreateModalOpen(false);
          clearError();
        }}
        onSubmit={handleCreateSubmit}
        busy={busy}
        error={socketError}
      />
    </main>
  );
};

export default SpeakingLobbyPage;
