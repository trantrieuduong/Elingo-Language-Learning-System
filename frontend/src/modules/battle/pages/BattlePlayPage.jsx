import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Toast from '../../../components/Toast/Toast';
import PageLoading from '../../../components/PageLoading/PageLoading';
import { useAuth } from '../../../context/AuthContext';
import { useBattleSocket } from '../context/BattleSocketContext';
import { useXp } from '../../gamification/context/XpContext';
import BattleAvatar from '../components/BattleAvatar/BattleAvatar';
import ScoreBoard from '../components/ScoreBoard/ScoreBoard';
import QuestionArea from '../components/QuestionArea/QuestionArea';
import BattleResult from '../components/BattleResult/BattleResult';
import DisconnectBanner from '../components/DisconnectBanner/DisconnectBanner';
import './BattlePlayPage.css';

const BattlePlayPage = ({ matchId, onNavigate }) => {
  const { t } = useTranslation('battle');
  const { t: tApi } = useTranslation('api');
  const { user } = useAuth();
  const { lastXp } = useXp();
  const {
    connection,
    status,
    match,
    error,
    activeMatchId,
    activeMatchChecked,
    opponentDisconnected,
    question,
    serverOffsetMs,
    answered,
    opponentAnswered,
    roundResult,
    scores,
    finished,
    cancelReason,
    rejoinMatch,
    sendAnswer,
    exitBattle,
    clearError,
  } = useBattleSocket();

  const isMatchValid = match && String(match.matchId) === String(matchId);
  const rejoiningAttemptedRef = useRef(false);
  const finishedAtRef = useRef(null);

  // Ghi nhận thời điểm kết thúc trận để đối chiếu với lastXp
  useEffect(() => {
    if (status === 'finished') {
      if (!finishedAtRef.current) {
        finishedAtRef.current = Date.now();
      }
    } else {
      finishedAtRef.current = null;
    }
  }, [status]);

  // 1. Khôi phục trận khi không có match khớp URL
  useEffect(() => {
    if (isMatchValid) return;

    if (!activeMatchChecked) {
      // Đang chờ socket phản hồi active-match
      return;
    }

    if (String(activeMatchId) === String(matchId)) {
      if (!rejoiningAttemptedRef.current) {
        rejoiningAttemptedRef.current = true;
        rejoinMatch(matchId);
      }
    } else {
      // activeMatchChecked mà không có trận khớp -> về sảnh
      onNavigate('/battle');
    }
  }, [isMatchValid, activeMatchChecked, activeMatchId, matchId, rejoinMatch, onNavigate]);

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

  // Nếu chưa có trận đấu hợp lệ (đang chờ activeMatchChecked hoặc đang rejoin)
  if (!isMatchValid) {
    return (
      <main className="battle-play-page">
        <div className="battle-play-loading-container">
          <PageLoading />
          <p className="text-body-md text-disabled battle-play-restoring-text">
            {t('play.restoring')}
          </p>
        </div>
      </main>
    );
  }

  // Chuẩn hóa me và opponent
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

  const handleViewHistory = () => {
    exitBattle();
    onNavigate('/battle/history');
  };

  const errorMessage = error
    ? t(`error.${error}`, { defaultValue: '' }) ||
      tApi(`error.${error}`, { defaultValue: '' }) ||
      tApi('common.UNKNOWN_ERROR')
    : null;

  const totalRounds = match.totalRounds || 10;
  const isConnected = connection === 'connected';

  // Tính xpGained cho trận RANDOM
  const xpGained =
    status === 'finished' &&
    match?.matchType === 'RANDOM' &&
    lastXp &&
    finishedAtRef.current &&
    lastXp.at >= finishedAtRef.current
      ? lastXp.amount
      : null;

  return (
    <main className="battle-play-page">
      <div className="battle-play-container">
        {/* Banner kết nối của chính mình */}
        {!isConnected && ['starting', 'playing', 'roundResult'].includes(status) && (
          <div className="battle-connection-banner glass-card" role="status">
            <span className="material-symbols-outlined battle-connection-banner__icon">
              wifi_off
            </span>
            <span className="battle-connection-banner__text">
              {t('connection.reconnecting')}
            </span>
          </div>
        )}

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
              roundNo={question?.roundNo || roundResult?.roundNo || 1}
              totalRounds={totalRounds}
              meAnswered={answered}
              opponentAnswered={opponentAnswered}
            />

            {/* Banner đối thủ mất kết nối */}
            {opponentDisconnected && (
              <DisconnectBanner
                opponentName={opponent.name}
                graceMs={opponentDisconnected.graceMs}
                at={opponentDisconnected.at}
              />
            )}

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
            ) : roundResult ? (
              /* Vòng nghỉ sau khi rejoin (status === 'roundResult' nhưng question === null) */
              <div className="battle-play-rejoin-round-result glass-card">
                <div className="battle-play-rejoin-round-result__badge glass-pill">
                  <span className="text-label-md">
                    {t('play.round', {
                      current: roundResult.roundNo,
                      total: totalRounds,
                    })}
                  </span>
                </div>
                <div className="battle-play-rejoin-round-result__answer">
                  <span className="text-body-md text-disabled">
                    {t('play.correctAnswer')}:
                  </span>
                  <strong className="text-title-lg text-primary">
                    {roundResult.correctAnswer}
                  </strong>
                </div>
                {roundResult.results?.some(
                  (r) => String(r.userId) === String(me.userId)
                ) && (
                  <div className="battle-play-rejoin-round-result__score text-body-md">
                    +{roundResult.results.find(
                      (r) => String(r.userId) === String(me.userId)
                    )?.pointsGained || 0}{' '}
                    {t('play.points')}
                  </div>
                )}
                <p className="text-body-sm text-disabled battle-play-rejoin-round-result__next">
                  {t('play.nextQuestionSoon')}
                </p>
              </div>
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
            xpGained={xpGained}
            onExit={handleExit}
            onViewHistory={handleViewHistory}
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
                  : cancelReason === 'CONNECTION_LOST'
                    ? t('result.cancelled.CONNECTION_LOST')
                    : t('result.cancelled.unknown')}
            </p>
            <div className="battle-cancelled-actions">
              <button
                type="button"
                className="btn-primary battle-cancelled-btn"
                onClick={handleExit}
              >
                {t('result.backToLobby')}
              </button>
              <button
                type="button"
                className="btn-secondary battle-cancelled-btn"
                onClick={handleViewHistory}
              >
                {t('result.viewHistory')}
              </button>
            </div>
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
