import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Toast from '../../../components/Toast/Toast';
import { useAuth } from '../../../context/AuthContext';
import { useBattleSocket } from '../context/BattleSocketContext';
import BattleAvatar from '../components/BattleAvatar/BattleAvatar';
import ScoreBoard from '../components/ScoreBoard/ScoreBoard';
import QuestionArea from '../components/QuestionArea/QuestionArea';
import BattleResult from '../components/BattleResult/BattleResult';
import './BattlePlayPage.css';

const BattlePlayPage = ({ matchId, onNavigate }) => {
  const { t } = useTranslation('battle');
  const { t: tApi } = useTranslation('api');
  const { user } = useAuth();
  const {
    status,
    match,
    error,
    question,
    serverOffsetMs,
    answered,
    opponentAnswered,
    roundResult,
    scores,
    finished,
    cancelReason,
    sendAnswer,
    exitBattle,
    clearError,
  } = useBattleSocket();

  const isMatchValid = match && String(match.matchId) === String(matchId);

  useEffect(() => {
    if (!isMatchValid) {
      onNavigate('/battle');
    }
  }, [isMatchValid, onNavigate]);

  const [remainingCountdown, setRemainingCountdown] = useState(() => {
    if (!match?.countdownMs || !match?.foundAt) return 0;
    const elapsed = Date.now() - match.foundAt;
    return Math.max(0, Math.ceil((match.countdownMs - elapsed) / 1000));
  });

  useEffect(() => {
    if (!match?.countdownMs || !match?.foundAt) return undefined;

    const interval = setInterval(() => {
      const elapsed = Date.now() - match.foundAt;
      const left = Math.max(0, Math.ceil((match.countdownMs - elapsed) / 1000));
      setRemainingCountdown(left);
      if (left <= 0) {
        clearInterval(interval);
      }
    }, 250);

    return () => clearInterval(interval);
  }, [match]);

  if (!isMatchValid) {
    return null;
  }

  // Chuẩn hóa me và opponent một lần
  const me = {
    userId: user?.id,
    name: user?.fullName || t('play.you'),
    avatarUrl: user?.avatarUrl,
  };

  const opponentPlayer = match.players?.find(
    (p) => String(p.userId) !== String(user?.id)
  );

  const opponent = {
    userId: opponentPlayer?.userId,
    name: opponentPlayer?.displayName || t('play.opponentFallback'),
    avatarUrl: opponentPlayer?.avatarUrl,
  };

  const handleExit = () => {
    exitBattle();
    onNavigate('/battle');
  };

  const errorMessage = error
    ? t(`error.${error}`, { defaultValue: '' }) ||
      tApi(`error.${error}`, { defaultValue: '' }) ||
      tApi('common.UNKNOWN_ERROR')
    : null;

  const totalRounds = match.totalRounds || 10;

  return (
    <main className="battle-play-page">
      <div className="battle-play-container">
        {/* 1. Màn Đếm Ngược chuẩn bị (starting) */}
        {status === 'starting' && (
          <div className="battle-play-countdown-card glass-card">
            <header className="battle-play-countdown-card__header">
              <span className="battle-play-countdown-card__badge">
                {match.mode === 'TYPING'
                  ? t('mode.typing')
                  : t('mode.multipleChoice')}
              </span>
              <p className="text-body-md text-disabled battle-play-countdown-card__subtitle">
                {t('play.preparing')}
              </p>
            </header>

            <div className="battle-play-versus">
              {/* Người chơi */}
              <div className="battle-play-player">
                <BattleAvatar
                  name={me.name}
                  avatarUrl={me.avatarUrl}
                  size="lg"
                />
                <span className="text-title-md battle-play-player__name">
                  {me.name}
                </span>
                <span className="battle-play-player__tag">{t('play.you')}</span>
              </div>

              {/* Chữ VS */}
              <div className="battle-play-versus__divider">
                <span className="battle-play-versus__text">{t('play.vs')}</span>
              </div>

              {/* Đối thủ */}
              <div className="battle-play-player">
                <BattleAvatar
                  name={opponent.name}
                  avatarUrl={opponent.avatarUrl}
                  size="lg"
                />
                <span className="text-title-md battle-play-player__name">
                  {opponent.name}
                </span>
              </div>
            </div>

            {/* Đồng hồ đếm ngược */}
            <div className="battle-play-countdown">
              <div className="battle-play-countdown__circle">
                <span className="battle-play-countdown__number">
                  {remainingCountdown > 0 ? remainingCountdown : 'GO!'}
                </span>
              </div>
            </div>
          </div>
        )}

        {/* 2. Màn Diễn Biến Trận Đấu (playing / roundResult) */}
        {(status === 'playing' || status === 'roundResult') && (
          <>
            <ScoreBoard
              me={me}
              opponent={opponent}
              scores={scores}
              roundNo={question?.roundNo || 1}
              totalRounds={totalRounds}
              meAnswered={answered}
              opponentAnswered={opponentAnswered}
            />

            {question ? (
              <QuestionArea
                key={question.questionId}
                question={question}
                answered={answered}
                roundResult={roundResult}
                serverOffsetMs={serverOffsetMs}
                myResult={roundResult?.results?.find(
                  (r) => String(r.userId) === String(me.userId)
                )}
                onSubmit={sendAnswer}
              />
            ) : (
              <div className="battle-play-loading glass-card">
                <div className="app-loading-spinner" />
              </div>
            )}
          </>
        )}

        {/* 3. Màn Kết Thúc Trận Đấu (finished) */}
        {status === 'finished' && finished && (
          <BattleResult
            me={me}
            opponent={opponent}
            finished={finished}
            matchType={match.matchType}
            totalRounds={totalRounds}
            onExit={handleExit}
          />
        )}

        {/* 4. Màn Hủy Trận Đấu (cancelled) */}
        {status === 'cancelled' && (
          <div className="battle-cancelled-card glass-card">
            <span className="material-symbols-outlined battle-cancelled-icon">
              cancel
            </span>
            <h1 className="text-headline-md battle-cancelled-title">
              {t('result.cancelled.title')}
            </h1>
            <p className="text-body-md text-disabled battle-cancelled-reason">
              {cancelReason === 'BOTH_DISCONNECTED'
                ? t('result.cancelled.BOTH_DISCONNECTED')
                : cancelReason === 'INTERNAL_ERROR'
                  ? t('result.cancelled.INTERNAL_ERROR')
                  : t('result.cancelled.unknown')}
            </p>
            <button
              type="button"
              className="btn-primary battle-cancelled-btn"
              onClick={handleExit}
            >
              {t('result.backToLobby')}
            </button>
          </div>
        )}
      </div>

      <Toast
        message={errorMessage}
        variant="error"
        onDismiss={clearError}
      />
    </main>
  );
};

export default BattlePlayPage;
