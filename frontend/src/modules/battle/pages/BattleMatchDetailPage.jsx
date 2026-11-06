import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { getBattleDetailApi } from '../battleApi';
import { useAuth } from '../../../context/AuthContext';
import BackButton from '../../../components/BackButton/BackButton';
import BattleAvatar from '../components/BattleAvatar/BattleAvatar';
import PageLoading from '../../../components/PageLoading/PageLoading';
import './BattleMatchDetailPage.css';

const BattleMatchDetailPage = ({ matchId, onNavigate }) => {
  const { t, i18n } = useTranslation('battle');
  const { t: tApi } = useTranslation('api');
  const { user } = useAuth();

  const [matchDetail, setMatchDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);
  const [expandedRounds, setExpandedRounds] = useState({});

  const toggleRound = (roundNo) => {
    setExpandedRounds((prev) => ({
      ...prev,
      [roundNo]: !prev[roundNo],
    }));
  };

  useEffect(() => {
    let isActive = true;

    const fetchDetail = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await getBattleDetailApi(matchId);
        if (!isActive) return;

        if (res?.success && res?.data) {
          setMatchDetail(res.data);
        } else {
          setError(t('detail.loadError'));
        }
      } catch (err) {
        if (!isActive) return;
        const errCode = err.response?.data?.code;
        if (
          errCode === 'BATTLE_PARTICIPANT_NOT_FOUND' ||
          errCode === 'BATTLE_MATCH_NOT_FOUND'
        ) {
          setError(
            tApi(`error.${errCode}`, { defaultValue: '' }) || t('detail.loadError')
          );
        } else {
          setError(t('detail.loadError'));
        }
      } finally {
        if (isActive) {
          setLoading(false);
        }
      }
    };

    fetchDetail();

    return () => {
      isActive = false;
    };
  }, [matchId, retryCount, t, tApi]);

  if (loading) {
    return (
      <main className="battle-detail-page">
        <div className="battle-detail-loading">
          <PageLoading />
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="battle-detail-page">
        <div className="battle-detail-container">
          <div className="battle-detail-error glass-card">
            <span className="material-symbols-outlined battle-detail-error__icon">
              error
            </span>
            <p className="text-body-md text-error">{error}</p>
            <div className="battle-detail-error__actions">
              <button
                type="button"
                className="btn-primary"
                onClick={() => onNavigate('/battle/history')}
              >
                {t('detail.back')}
              </button>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setRetryCount((prev) => prev + 1)}
              >
                {t('history.retry')}
              </button>
            </div>
          </div>
        </div>
      </main>
    );
  }

  if (!matchDetail) return null;

  // Tìm me & opponent
  const currentUserId = user?.id;
  const myPlayer = matchDetail.players?.find(
    (p) => String(p.userId) === String(currentUserId)
  );
  const oppPlayer = matchDetail.players?.find(
    (p) => String(p.userId) !== String(currentUserId)
  );

  const myResult = matchDetail.results?.find(
    (r) => String(r.userId) === String(currentUserId)
  );
  const oppResult = matchDetail.results?.find(
    (r) => String(r.userId) !== String(currentUserId)
  );

  const isWinner =
    matchDetail.winnerId !== null &&
    String(matchDetail.winnerId) === String(currentUserId);
  const isDraw = matchDetail.winnerId === null;
  const isLoser = !isDraw && !isWinner;

  const resultBadgeText = isWinner
    ? t('history.result.WIN')
    : isLoser
      ? t('history.result.LOSE')
      : t('history.result.DRAW');

  const resultBadgeClass = isWinner
    ? 'battle-detail-badge--win'
    : isLoser
      ? 'battle-detail-badge--lose'
      : 'battle-detail-badge--draw';

  const finishedAtText = matchDetail.finishedAt
    ? new Date(matchDetail.finishedAt).toLocaleString(i18n.language)
    : '';

  return (
    <main className="battle-detail-page">
      <div className="battle-detail-container">
        {/* Nút quay lại */}
        <div className="battle-detail-top-nav">
          <BackButton
            label={t('detail.back')}
            onClick={() => onNavigate('/battle/history')}
          />
        </div>

        <div className="battle-detail-layout">
          {/* Cột trái: Tiêu đề + Thẻ tóm tắt đối đầu */}
          <div className="battle-detail-left-col">
            <header className="battle-detail-header">
              <div className="battle-detail-header__title-row">
                <h1 className="text-headline-md battle-detail-title">
                  {t('detail.title')} #{matchDetail.matchId}
                </h1>
                <span
                  className={`battle-detail-result-badge ${resultBadgeClass}`}
                >
                  {resultBadgeText}
                </span>
              </div>
              <div className="battle-detail-header__date-row">
                <span className="material-symbols-outlined battle-detail-header__date-icon">
                  schedule
                </span>
                <span>{finishedAtText}</span>
              </div>
            </header>

            {/* Thẻ tóm tắt đối đầu */}
            <div className="battle-detail-summary-card glass-card">
              <div className="battle-detail-mode-tabs">
                <span className="battle-detail-tab-item">
                  {t(
                    matchDetail.mode === 'MULTIPLE_CHOICE'
                      ? 'mode.multipleChoice'
                      : 'mode.typing'
                  )}
                </span>
                <span className="battle-detail-tab-divider" aria-hidden="true" />
                <span className="battle-detail-tab-item">
                  {t(
                    matchDetail.matchType === 'ROOM'
                      ? 'history.matchType.ROOM'
                      : 'history.matchType.RANDOM'
                  )}
                </span>
              </div>

                <div className="battle-detail-versus">
                  {/* Cột Bạn */}
                  <div className="battle-detail-player">
                    <BattleAvatar
                      name={myPlayer?.displayName || t('detail.you')}
                      avatarUrl={myPlayer?.avatarUrl}
                      size="lg"
                    />
                    <span className="text-title-md battle-detail-player__name">
                      {myPlayer?.displayName || t('detail.you')}
                    </span>
                    <span className="battle-detail-player__role">
                      {t('play.you')}
                    </span>
                    <div className="battle-detail-player__score">
                      {myResult?.score ?? 0}
                    </div>
                    <span className="battle-detail-player__correct">
                      {t('result.correctCount', {
                        count: myResult?.correctCount ?? 0,
                        total: matchDetail.totalRounds,
                      })}
                    </span>
                  </div>

                  <div className="battle-detail-versus__text">VS</div>

                  {/* Cột Đối thủ */}
                  <div className="battle-detail-player">
                    <BattleAvatar
                      name={oppPlayer?.displayName || t('detail.opponent')}
                      avatarUrl={oppPlayer?.avatarUrl}
                      size="lg"
                    />
                    <span className="text-title-md battle-detail-player__name">
                      {oppPlayer?.displayName || t('detail.opponent')}
                    </span>
                    <span className="battle-detail-player__role">
                      {t('detail.opponent')}
                    </span>
                    <div className="battle-detail-player__score">
                      {oppResult?.score ?? 0}
                    </div>
                    <span className="battle-detail-player__correct">
                      {t('result.correctCount', {
                        count: oppResult?.correctCount ?? 0,
                        total: matchDetail.totalRounds,
                      })}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Cột phải: Accordion danh sách câu hỏi */}
            <div className="battle-detail-right-col glass-card">
              <div className="battle-detail-rounds-header">
                <div className="battle-detail-rounds-header__title text-title-md">
                  <span className="material-symbols-outlined battle-detail-rounds-header__icon">
                    format_list_bulleted
                  </span>
                  <span>{t('detail.roundsTitle')}</span>
                </div>
              </div>

              <div className="battle-detail-rounds-list">
                {matchDetail.rounds?.map((round) => {
                  const myAns = round.answers?.find(
                    (a) => String(a.userId) === String(currentUserId)
                  );
                  const oppAns = round.answers?.find(
                    (a) => String(a.userId) !== String(currentUserId)
                  );
                  const isExpanded = !!expandedRounds[round.roundNo];
                  const isUserCorrect = myAns?.isCorrect;

                  return (
                    <div
                      key={round.roundNo}
                      className="battle-detail-round-item"
                    >
                      <button
                        type="button"
                        className="battle-detail-round-header-btn"
                        onClick={() => toggleRound(round.roundNo)}
                        aria-expanded={isExpanded}
                      >
                        <div className="battle-detail-round-header-left">
                          <div className="battle-detail-round-number-circle">
                            {round.roundNo}
                          </div>
                          <div className="battle-detail-round-info">
                            <span className="battle-detail-round-term">
                              {round.term}
                            </span>
                            <div className="battle-detail-round-answer-preview">
                              <span
                                className={`material-symbols-outlined battle-detail-round-preview-icon ${
                                  isUserCorrect ? 'text-success' : 'text-error'
                                }`}
                              >
                                {isUserCorrect ? 'check_circle' : 'cancel'}
                              </span>
                              <span className={isUserCorrect ? 'text-success' : 'text-error'}>
                                {t('detail.youAnswered')}: {myAns?.answered ? myAns.answerText : t('detail.noAnswer')}
                              </span>
                            </div>
                          </div>
                        </div>

                        <div className="battle-detail-round-header-right">
                          <span
                            className={`battle-detail-round-badge-pill ${
                              isUserCorrect
                                ? 'battle-detail-round-badge-pill--correct'
                                : 'battle-detail-round-badge-pill--wrong'
                            }`}
                          >
                            {isUserCorrect ? t('detail.correct') : t('detail.wrong')}
                          </span>
                          <span
                            className={`material-symbols-outlined battle-detail-round-arrow ${
                              isExpanded ? 'battle-detail-round-arrow--expanded' : ''
                            }`}
                          >
                            keyboard_arrow_down
                          </span>
                        </div>
                      </button>

                      {isExpanded && (
                        <div className="battle-detail-round-body">
                          <div className="battle-detail-round-correct-line">
                            {t('detail.correctAnswer')}:{' '}
                            <strong className="text-primary font-bold">
                              {round.correctAnswer}
                            </strong>
                          </div>

                          <div className="battle-detail-round-columns">
                            {/* Bạn */}
                            <div className="battle-detail-answer-col">
                              <span className="battle-detail-col-title text-disabled">
                                {t('detail.you')}
                              </span>
                              <div className="battle-detail-answer-box glass-thumb">
                                <div className="battle-detail-answer-row">
                                  <span className="battle-detail-answer-text">
                                    {myAns?.answered ? myAns.answerText : t('detail.noAnswer')}
                                  </span>
                                  {myAns?.answered && (
                                    <span
                                      className={`material-symbols-outlined battle-detail-status-icon ${
                                        myAns.isCorrect ? 'text-success' : 'text-error'
                                      }`}
                                    >
                                      {myAns.isCorrect ? 'check_circle' : 'cancel'}
                                    </span>
                                  )}
                                </div>
                                <div className="battle-detail-answer-meta text-body-xs text-disabled">
                                  {myAns?.answered && myAns.responseTimeMs != null && (
                                    <span>
                                      {t('detail.seconds', {
                                        sec: (myAns.responseTimeMs / 1000).toFixed(1),
                                      })}
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>

                            {/* Đối thủ */}
                            <div className="battle-detail-answer-col">
                              <span className="battle-detail-col-title text-disabled">
                                {t('detail.opponent')}
                              </span>
                              <div className="battle-detail-answer-box glass-thumb">
                                <div className="battle-detail-answer-row">
                                  <span className="battle-detail-answer-text">
                                    {oppAns?.answered ? oppAns.answerText : t('detail.noAnswer')}
                                  </span>
                                  {oppAns?.answered && (
                                    <span
                                      className={`material-symbols-outlined battle-detail-status-icon ${
                                        oppAns.isCorrect ? 'text-success' : 'text-error'
                                      }`}
                                    >
                                      {oppAns.isCorrect ? 'check_circle' : 'cancel'}
                                    </span>
                                  )}
                                </div>
                                <div className="battle-detail-answer-meta text-body-xs text-disabled">
                                  {oppAns?.answered && oppAns.responseTimeMs != null && (
                                    <span>
                                      {t('detail.seconds', {
                                        sec: (oppAns.responseTimeMs / 1000).toFixed(1),
                                      })}
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
      </div>
    </main>
  );
};

export default BattleMatchDetailPage;

