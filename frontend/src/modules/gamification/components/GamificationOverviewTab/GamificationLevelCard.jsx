import ProgressBar from '../../../../components/ProgressBar/ProgressBar';
import './GamificationLevelCard.css';

function GamificationLevelCard({ levelProgress, t }) {
  return (
    <section className="glass-card gamification-overview__card gamification-overview__card--level">
      <div className="gamification-overview__card-header">
        <div className="gamification-overview__icon-wrap">
          <span className="material-symbols-outlined">stars</span>
        </div>
        <div>
          <h2 className="text-headline-sm">{t('overview.levelCard.title')}</h2>
          <span className="text-body-sm text-disabled">
            {levelProgress?.currentTitle}
          </span>
        </div>
      </div>
      <div className="gamification-overview__card-body">
        <div className="overview-level-info">
          <span className="overview-level-badge badge badge--primary">
            {t('overview.levelCard.level', { level: levelProgress?.currentLevel ?? 1 })}
          </span>
          <span className="text-body-md">
            {t('overview.levelCard.totalXp', { xp: levelProgress?.totalXp ?? 0 })}
          </span>
        </div>
        <div className="overview-level-progress">
          <ProgressBar
            title={t('overview.levelCard.progressTitle')}
            current={levelProgress?.totalXp ?? 0}
            total={levelProgress?.nextLevelRequiredXp ?? levelProgress?.totalXp ?? 0}
            percent={levelProgress?.progressPercent ?? (levelProgress?.nextLevel ? 0 : 100)}
          />
        </div>
        <p className="overview-level-hint text-body-sm text-disabled">
          {levelProgress?.nextLevel
            ? t('overview.levelCard.nextLevel', {
                xp: levelProgress.xpToNextLevel ?? 0,
                title: levelProgress.nextLevelTitle || '',
                level: levelProgress.nextLevel,
              })
            : t('overview.levelCard.maxLevel')}
        </p>
      </div>
    </section>
  );
}

export default GamificationLevelCard;
