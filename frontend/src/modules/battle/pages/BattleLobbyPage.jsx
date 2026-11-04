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
  const { connection, status, match, error, joinQueue, leaveQueue, clearError } =
    useBattleSocket();

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
    joinQueue(selectedMode);
  };

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

  const errorMessage = error
    ? t(`error.${error}`, { defaultValue: '' }) ||
      tApi(`error.${error}`, { defaultValue: '' }) ||
      tApi('common.UNKNOWN_ERROR')
    : null;

  const isConnected = connection === 'connected';

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

        {/* Khối chính: Chọn chế độ HOẶC Đang tìm trận */}
        <div className="battle-lobby-card glass-card">
          <header className="battle-lobby-card__header">
            <h1 className="text-headline-md battle-lobby-card__title">{t('title')}</h1>
            <p className="text-body-md text-disabled battle-lobby-card__subtitle">
              {t('subtitle')}
            </p>
          </header>

          {status === 'searching' ? (
            <div className="battle-lobby-searching">
              <div className="app-loading-spinner battle-lobby-searching__spinner" />
              <h2 className="text-title-lg battle-lobby-searching__title">
                {t('searching')}
              </h2>
              <div className="battle-lobby-searching__meta">
                <span className="battle-lobby-searching__mode">
                  {t('searchingMode', {
                    mode: t(
                      selectedMode === 'MULTIPLE_CHOICE'
                        ? 'mode.multipleChoice'
                        : 'mode.typing'
                    ),
                  })}
                </span>
                <span className="battle-lobby-searching__time">
                  {t('searchingTime', { time: formatSeconds(searchSeconds) })}
                </span>
              </div>
              <button
                type="button"
                className="btn-secondary battle-lobby-searching__cancel-btn"
                onClick={leaveQueue}
              >
                {t('cancel')}
              </button>
            </div>
          ) : (
            <div className="battle-lobby-modes">
              <h2 className="text-title-sm battle-lobby-modes__label">
                {t('mode.title')}
              </h2>
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
                      {isSelected && (
                        <span className="material-symbols-outlined battle-mode-card__check">
                          check_circle
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>

              <div className="battle-lobby-actions">
                <button
                  type="button"
                  className="btn-primary battle-lobby-actions__find-btn"
                  disabled={!isConnected}
                  onClick={handleStartQueue}
                >
                  <span className="material-symbols-outlined">swords</span>
                  {t('findMatch')}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      <Toast
        message={errorMessage}
        variant="error"
        onDismiss={clearError}
      />
    </main>
  );
};

export default BattleLobbyPage;

