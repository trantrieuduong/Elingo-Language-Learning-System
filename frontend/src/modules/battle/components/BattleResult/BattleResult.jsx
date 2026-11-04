import { useTranslation } from 'react-i18next';
import BattleAvatar from '../BattleAvatar/BattleAvatar';
import './BattleResult.css';

const BattleResult = ({
  me,
  opponent,
  finished,
  matchType = 'RANDOM',
  totalRounds = 10,
  onExit,
}) => {
  const { t } = useTranslation('battle');

  const winnerId = finished?.winnerId;
  const isDraw = winnerId === null;
  const isWinner = !isDraw && String(winnerId) === String(me?.userId);
  const isLoser = !isDraw && !isWinner;

  let titleKey = 'result.draw';
  let descKey = 'result.drawDesc';
  let themeClass = 'battle-result--draw';

  if (isWinner) {
    titleKey = 'result.win';
    descKey = 'result.winDesc';
    themeClass = 'battle-result--win';
  } else if (isLoser) {
    titleKey = 'result.lose';
    descKey = 'result.loseDesc';
    themeClass = 'battle-result--lose';
  }

  // Xử lý Forfeit
  const isForfeit = finished?.reason === 'FORFEIT';

  // Thông tin player từ finished
  const playersList = finished?.players || [];
  const myStats = playersList.find(
    (p) => String(p.userId) === String(me?.userId)
  );
  const opponentStats = playersList.find(
    (p) => String(p.userId) === String(opponent?.userId)
  );

  const showXpHint =
    matchType === 'RANDOM' && (myStats?.correctCount ?? 0) < 3;

  return (
    <div className={`battle-result-card glass-card ${themeClass}`}>
      <header className="battle-result-header">
        <div className="battle-result-badge glass-pill">
          <span className="material-symbols-outlined battle-result-badge__icon">
            {isWinner ? 'trophy' : isDraw ? 'handshake' : 'sentiment_dissatisfied'}
          </span>
          <span className="battle-result-badge__text text-label-md">
            {t(titleKey)}
          </span>
        </div>
        <h1 className="battle-result-title text-headline-lg">{t(titleKey)}</h1>
        <p className="battle-result-desc text-body-md text-disabled">
          {t(descKey)}
        </p>

        {isForfeit && (
          <p className="battle-result-forfeit text-label-md">
            {isWinner ? t('result.forfeitWin') : t('result.forfeitLose')}
          </p>
        )}
      </header>

      {/* Bảng điểm 2 người chơi */}
      <div className="battle-result-players">
        {/* Người chơi hiện tại */}
        <div
          className={`battle-result-row glass-card ${
            myStats?.isWinner ? 'battle-result-row--winner' : ''
          }`}
        >
          <div className="battle-result-row__user">
            <BattleAvatar name={me?.name} avatarUrl={me?.avatarUrl} size="md" />
            <div className="battle-result-row__details">
              <span className="battle-result-row__name text-title-md">
                {me?.name || t('play.you')}
              </span>
              <span className="battle-result-row__stats text-body-sm text-disabled">
                {t('result.correctCount', {
                  count: myStats?.correctCount ?? 0,
                  total: totalRounds,
                })}
              </span>
            </div>
          </div>
          <div className="battle-result-row__score text-headline-md">
            {myStats?.score ?? 0}
          </div>
        </div>

        {/* Đối thủ */}
        <div
          className={`battle-result-row glass-card ${
            opponentStats?.isWinner ? 'battle-result-row--winner' : ''
          }`}
        >
          <div className="battle-result-row__user">
            <BattleAvatar
              name={opponent?.name}
              avatarUrl={opponent?.avatarUrl}
              size="md"
            />
            <div className="battle-result-row__details">
              <span className="battle-result-row__name text-title-md">
                {opponent?.name || t('play.opponentFallback')}
              </span>
              <span className="battle-result-row__stats text-body-sm text-disabled">
                {t('result.correctCount', {
                  count: opponentStats?.correctCount ?? 0,
                  total: totalRounds,
                })}
              </span>
            </div>
          </div>
          <div className="battle-result-row__score text-headline-md">
            {opponentStats?.score ?? 0}
          </div>
        </div>
      </div>

      {/* Gợi ý XP tĩnh nếu chưa đủ 3 câu đúng */}
      {showXpHint && (
        <div className="battle-result-xp-hint glass-thumb">
          <span className="material-symbols-outlined battle-result-xp-hint__icon">
            info
          </span>
          <span className="text-body-sm">{t('result.xpHint')}</span>
        </div>
      )}

      {/* Nút Chơi lại / Về sảnh */}
      <div className="battle-result-actions">
        <button
          type="button"
          className="btn-primary battle-result-actions__btn"
          onClick={onExit}
        >
          <span className="material-symbols-outlined">replay</span>
          {t('result.playAgain')}
        </button>
      </div>
    </div>
  );
};

export default BattleResult;

