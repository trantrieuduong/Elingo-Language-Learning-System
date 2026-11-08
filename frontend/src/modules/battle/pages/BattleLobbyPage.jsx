import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Toast from '../../../components/Toast/Toast';
import { useBattleSocket } from '../context/BattleSocketContext';
import './BattleLobbyPage.css';

const MODES = [
  {
    key: 'MULTIPLE_CHOICE',
    icon: 'checklist',
    labelKey: 'mode.multipleChoice',
    descKey: 'mode.multipleChoiceDesc',
  },
  {
    key: 'TYPING',
    icon: 'keyboard',
    labelKey: 'mode.typing',
    descKey: 'mode.typingDesc',
  },
];

const MODE_STORAGE_KEY = 'battle.mode';

const BattleLobbyPage = ({ onNavigate }) => {
  const { t } = useTranslation('battle');
  const { t: tApi } = useTranslation('api');
  const {
    connection,
    status,
    match,
    error,
    roomCode,
    activeMatchId,
    joinQueue,
    leaveQueue,
    createRoom,
    joinRoom,
    leaveRoom,
    clearError,
  } = useBattleSocket();

  const [selectedMode, setSelectedMode] = useState(() => {
    try {
      const saved = localStorage.getItem(MODE_STORAGE_KEY);
      if (saved === 'MULTIPLE_CHOICE' || saved === 'TYPING') {
        return saved;
      }
    } catch {
      // Bỏ qua lỗi truy cập storage
    }
    return 'MULTIPLE_CHOICE';
  });

  const [searchSeconds, setSearchSeconds] = useState(0);
  const [inputRoomCode, setInputRoomCode] = useState('');
  const [roomError, setRoomError] = useState(null);
  const [isCopied, setIsCopied] = useState(false);

  const handleSelectMode = (newMode) => {
    setSelectedMode(newMode);
    try {
      localStorage.setItem(MODE_STORAGE_KEY, newMode);
    } catch {
      // Bỏ qua lỗi truy cập storage
    }
  };

  const handleStartQueue = () => {
    clearError();
    setRoomError(null);
    joinQueue(selectedMode);
  };

  const handleCreateRoom = () => {
    clearError();
    setRoomError(null);
    createRoom(selectedMode);
  };

  const handleJoinRoom = () => {
    clearError();
    setRoomError(null);
    const code = inputRoomCode.trim().toUpperCase();
    if (code.length !== 6) {
      setRoomError(t('room.codeInvalid'));
      return;
    }
    joinRoom(code);
  };

  const handleCopyCode = async () => {
    if (!roomCode) return;
    try {
      await navigator.clipboard.writeText(roomCode);
      setIsCopied(true);
      setTimeout(() => {
        setIsCopied(false);
      }, 2000);
    } catch {
      // Fallback nếu clipboard API bị lỗi
      setIsCopied(true);
      setTimeout(() => {
        setIsCopied(false);
      }, 2000);
    }
  };

  const isConnected = connection === 'connected';
  const hasActiveMatch = status === 'idle' && !!activeMatchId;

  // Đếm giây khi đang tìm trận
  useEffect(() => {
    if (status !== 'searching') {
      setSearchSeconds(0);
      return undefined;
    }

    const timer = setInterval(() => {
      setSearchSeconds((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(timer);
  }, [status]);

  // Điều hướng khi ghép trận thành công
  useEffect(() => {
    if (status === 'starting' && match?.matchId) {
      onNavigate(`/battle/play/${match.matchId}`);
    }
  }, [status, match, onNavigate]);

  const errorMessage =
    roomError ||
    (error
      ? t(`error.${error}`, { defaultValue: '' }) ||
        tApi(`error.${error}`, { defaultValue: '' }) ||
        tApi('common.UNKNOWN_ERROR')
      : null);

  const formatSeconds = (sec) => {
    const mins = Math.floor(sec / 60);
    const secs = sec % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  return (
    <main className="battle-lobby-page">
      <div className="battle-lobby-container">
        {/* Banner mất kết nối */}
        {!isConnected && (
          <div className="battle-connection-banner glass-card" role="status">
            <span className="material-symbols-outlined battle-connection-banner__icon">
              wifi_off
            </span>
            <span className="battle-connection-banner__text">
              {t('connection.reconnecting')}
            </span>
          </div>
        )}

        {/* Banner trận dở */}
        {hasActiveMatch && (
          <div className="battle-lobby-resume glass-card">
            <div className="battle-lobby-resume__content">
              <div className="battle-lobby-resume__header">
                <span className="material-symbols-outlined battle-lobby-resume__icon">
                  history_toggle_off
                </span>
                <span className="text-title-md battle-lobby-resume__title">
                  {t('resume.title')}
                </span>
              </div>
              <p className="text-body-sm text-disabled battle-lobby-resume__desc">
                {t('resume.desc')}
              </p>
            </div>
            <button
              type="button"
              className="btn-primary battle-lobby-resume__btn"
              onClick={() => onNavigate(`/battle/play/${activeMatchId}`)}
            >
              <span className="material-symbols-outlined">play_arrow</span>
              {t('resume.continue')}
            </button>
          </div>
        )}

        {/* Khối chính: Chọn chế độ hoặc Đang tìm trận hoặc Đang chờ phòng */}
        <div className="battle-lobby-card glass-card">
          <header className="battle-lobby-card__header">
            <div className="battle-lobby-card__header-left">
              <div className="battle-lobby-card__header-icon-box">
                <span className="material-symbols-outlined battle-lobby-card__header-icon">
                  swords
                </span>
              </div>
              <div className="battle-lobby-card__title-group">
                <h1 className="text-headline-md battle-lobby-card__title">{t('title')}</h1>
                <p className="text-body-md text-disabled battle-lobby-card__subtitle">
                  {t('subtitle')}
                </p>
              </div>
            </div>
            <button
              type="button"
              className="battle-lobby-history-link"
              onClick={() => onNavigate('/battle/history')}
              aria-label={t('history.link')}
            >
              <span className="material-symbols-outlined">history</span>
              <span>{t('history.link')}</span>
            </button>
          </header>

          <div className="battle-lobby-modes">
            <div className="battle-lobby-divider">
              <span>{t('mode.title')}</span>
            </div>
            <div className="battle-lobby-modes__grid" role="radiogroup">
              {MODES.map((item) => {
                const isSelected = selectedMode === item.key;
                return (
                  <button
                    key={item.key}
                    type="button"
                    role="radio"
                    aria-checked={isSelected}
                    className={`battle-mode-card glass-card ${
                      isSelected ? 'battle-mode-card--selected' : ''
                    }`}
                    onClick={() => handleSelectMode(item.key)}
                    disabled={hasActiveMatch}
                  >
                    <div className="battle-mode-card__icon-wrapper glass-thumb">
                      <span className="material-symbols-outlined battle-mode-card__icon">
                        {item.icon}
                      </span>
                    </div>
                    <div className="battle-mode-card__content">
                      <span className="text-title-md battle-mode-card__title">
                        {t(item.labelKey)}
                      </span>
                      <p className="text-body-sm text-disabled battle-mode-card__desc">
                        {t(item.descKey)}
                      </p>
                    </div>
                    <span className="material-symbols-outlined battle-mode-card__arrow">
                      chevron_right
                    </span>
                    {isSelected && (
                      <span className="material-symbols-outlined battle-mode-card__check">
                        check_circle
                      </span>
                    )}
                  </button>
                );
              })}
            </div>

            {hasActiveMatch && (
              <div className="battle-lobby-blocked-notice text-body-sm text-warning">
                <span className="material-symbols-outlined">info</span>
                {t('resume.blocked')}
              </div>
            )}

            {/* Nút tìm trận ngẫu nhiên */}
            <div className="battle-lobby-actions">
              <button
                type="button"
                className="btn-primary battle-lobby-actions__find-btn"
                disabled={!isConnected || hasActiveMatch}
                onClick={handleStartQueue}
              >
                <span className="material-symbols-outlined">swords</span>
                <span>{t('findMatch')}</span>
                <span className="material-symbols-outlined">arrow_forward</span>
              </button>
            </div>

            {/* Khối phòng riêng */}
            <div className="battle-lobby-room-section">
              <div className="battle-lobby-room-divider">
                <span>{t('room.title')}</span>
              </div>

              <div className="battle-lobby-room-grid">
                {/* Tạo phòng */}
                <div className="battle-lobby-room-card glass-card">
                  <div className="battle-lobby-room-card__header">
                    <div className="battle-lobby-room-card__icon-wrapper glass-thumb">
                      <span className="material-symbols-outlined battle-lobby-room-card__icon">
                        add_circle
                      </span>
                    </div>
                    <div className="battle-lobby-room-card__title-group">
                      <span className="text-title-sm">{t('room.create')}</span>
                      <p className="text-body-sm text-disabled battle-lobby-room-card__desc">
                        {t('room.createDesc')}
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    className="btn-primary battle-lobby-room-card__action-btn"
                    disabled={!isConnected || hasActiveMatch}
                    onClick={handleCreateRoom}
                  >
                    {t('room.create')}
                  </button>
                </div>

                {/* Vào phòng bằng mã */}
                <div className="battle-lobby-room-card glass-card">
                  <div className="battle-lobby-room-card__header">
                    <div className="battle-lobby-room-card__icon-wrapper glass-thumb">
                      <span className="material-symbols-outlined battle-lobby-room-card__icon">
                        login
                      </span>
                    </div>
                    <div className="battle-lobby-room-card__title-group">
                      <span className="text-title-sm">{t('room.joinTitle')}</span>
                      <p className="text-body-sm text-disabled battle-lobby-room-card__desc">
                        {t('room.waitingDesc')}
                      </p>
                    </div>
                  </div>
                  <div className="battle-lobby-room-input-group">
                    <input
                      type="text"
                      className="battle-lobby-room-input"
                      placeholder={t('room.codePlaceholder')}
                      aria-label={t('room.codeLabel')}
                      maxLength={6}
                      autoComplete="off"
                      spellCheck={false}
                      value={inputRoomCode}
                      onChange={(e) => {
                        setInputRoomCode(e.target.value.toUpperCase());
                        if (roomError) setRoomError(null);
                      }}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          if (isConnected && !hasActiveMatch) handleJoinRoom();
                        }
                      }}
                      disabled={hasActiveMatch}
                    />
                    <button
                      type="button"
                      className="btn-primary battle-lobby-room-join-btn"
                      disabled={!isConnected || hasActiveMatch}
                      onClick={handleJoinRoom}
                    >
                      {t('room.join')}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Modal tìm trận ngẫu nhiên (Popup) */}
        {status === 'searching' && (
          <div className="battle-modal-overlay">
            <div className="battle-search-modal" role="dialog" aria-modal="true">
              <div className="app-loading-spinner battle-search-modal__spinner" />
              <h2 className="battle-search-modal__title">
                {t('searching')}
              </h2>
              <div className="battle-search-modal__meta">
                <span className="battle-search-modal__mode">
                  {t('mode.title')}:{' '}
                  <strong className="text-primary">
                    {t(
                      selectedMode === 'MULTIPLE_CHOICE'
                        ? 'mode.multipleChoice'
                        : 'mode.typing'
                    )}
                  </strong>
                </span>
                <span className="battle-search-modal__time">
                  {t('searchingTime', { time: formatSeconds(searchSeconds) })}
                </span>
              </div>
              <button
                type="button"
                className="btn-secondary battle-search-modal__cancel-btn"
                onClick={leaveQueue}
              >
                {t('cancel')}
              </button>
            </div>
          </div>
        )}

        {/* Modal chờ phòng riêng (Popup) */}
        {status === 'roomWaiting' && (
          <div className="battle-modal-overlay">
            <div className="battle-room-modal" role="dialog" aria-modal="true">
              <div className="app-loading-spinner battle-search-modal__spinner" />
              <h2 className="battle-search-modal__title">
                {t('room.waitingTitle')}
              </h2>
              <p className="battle-lobby-room-waiting__desc">
                {t('room.waitingDesc')}
              </p>
              <div className="battle-lobby-room-code-display">
                <span className="battle-lobby-room-code-text">{roomCode}</span>
                <button
                  type="button"
                  className="btn-secondary battle-lobby-room-code-copy-btn"
                  onClick={handleCopyCode}
                >
                  <span className="material-symbols-outlined">
                    {isCopied ? 'check' : 'content_copy'}
                  </span>
                  <span>{isCopied ? t('room.copied') : t('room.copy')}</span>
                </button>
              </div>
              <div className="battle-lobby-room-waiting__mode">
                <span>{t('mode.title')}:</span>{' '}
                <strong className="text-primary">
                  {t(
                    selectedMode === 'MULTIPLE_CHOICE'
                      ? 'mode.multipleChoice'
                      : 'mode.typing'
                  )}
                </strong>
              </div>
              <button
                type="button"
                className="btn-secondary battle-search-modal__cancel-btn"
                onClick={leaveRoom}
              >
                {t('room.cancel')}
              </button>
            </div>
          </div>
        )}
      </div>

      <Toast
        message={errorMessage}
        variant="error"
        onDismiss={() => {
          setRoomError(null);
          clearError();
        }}
      />
    </main>
  );
};

export default BattleLobbyPage;
