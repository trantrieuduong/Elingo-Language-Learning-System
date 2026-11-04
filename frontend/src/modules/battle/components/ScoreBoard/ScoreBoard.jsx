import { useTranslation } from 'react-i18next';
import BattleAvatar from '../BattleAvatar/BattleAvatar';
import './ScoreBoard.css';

// ScoreBoard - Bảng điểm trận đấu 1v1
const ScoreBoard = ({
  me,
  opponent,
  scores = {},
  roundNo = 1,
  totalRounds = 10,
  meAnswered = false,
  opponentAnswered = false,
}) => {
  const { t } = useTranslation('battle');

  const myScore = me?.userId != null ? scores[me.userId] ?? 0 : 0;
  const opponentScore =
    opponent?.userId != null ? scores[opponent.userId] ?? 0 : 0;

  return (
    <div className="battle-scoreboard glass-card">
      {/* Cột người chơi hiện tại */}
      <div className="battle-scoreboard__player battle-scoreboard__player--me">
        <BattleAvatar name={me?.name} avatarUrl={me?.avatarUrl} size="md" />
        <div className="battle-scoreboard__info">
          <span className="battle-scoreboard__name text-title-md">
            {me?.name || t('play.you')}
          </span>
          <span className="battle-scoreboard__score text-headline-sm">
            {myScore}
          </span>
          {meAnswered && (
            <span className="battle-scoreboard__status-chip glass-thumb">
              <span className="material-symbols-outlined battle-scoreboard__check-icon">
                check
              </span>
              {t('play.answered')}
            </span>
          )}
        </div>
      </div>

      {/* Cột giữa: Huy hiệu vòng đấu */}
      <div className="battle-scoreboard__center">
        <div className="battle-scoreboard__round-badge glass-pill">
          <span className="battle-scoreboard__round-text text-label-md">
            {t('play.round', { current: roundNo, total: totalRounds })}
          </span>
        </div>
      </div>

      {/* Cột đối thủ */}
      <div className="battle-scoreboard__player battle-scoreboard__player--opponent">
        <div className="battle-scoreboard__info battle-scoreboard__info--opponent">
          <span className="battle-scoreboard__name text-title-md">
            {opponent?.name || t('play.opponentFallback')}
          </span>
          <span className="battle-scoreboard__score text-headline-sm">
            {opponentScore}
          </span>
          {opponentAnswered && (
            <span className="battle-scoreboard__status-chip glass-thumb">
              <span className="material-symbols-outlined battle-scoreboard__check-icon">
                check
              </span>
              {t('play.answered')}
            </span>
          )}
        </div>
        <BattleAvatar
          name={opponent?.name}
          avatarUrl={opponent?.avatarUrl}
          size="md"
        />
      </div>
    </div>
  );
};

export default ScoreBoard;

