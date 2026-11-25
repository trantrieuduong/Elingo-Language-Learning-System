import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { getAllLevelsApi, getUserLevelProgressApi } from '../../gamificationApi';
import ProgressBar from '../../../../components/ProgressBar/ProgressBar';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import './GamificationLevelsTab.css';

function GamificationLevelsTab({ userId }) {
  const { t } = useTranslation('gamification');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [allLevels, setAllLevels] = useState([]);
  const [myProgress, setMyProgress] = useState(null);

  const fetchLevelsData = async () => {
    setLoading(true);
    setError('');
    try {
      const [levelsRes, progressRes] = await Promise.all([
        getAllLevelsApi(),
        getUserLevelProgressApi(userId),
      ]);

      if (levelsRes?.success) {
        setAllLevels(levelsRes.data || []);
      }
      if (progressRes?.success) {
        setMyProgress(progressRes.data || null);
      }
    } catch {
      setError(t('common.loadError'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLevelsData();
  }, [userId]);

  if (loading) {
    return <PageLoading />;
  }

  if (error) {
    return (
      <EmptyState
        icon="error"
        isError={true}
        description={error}
        actionLabel={t('common.retry')}
        onAction={fetchLevelsData}
      />
    );
  }

  const currentLevelNo = myProgress?.currentLevel ?? 1;

  return (
    <div className="gamification-levels">
      {/* Header Banner: Current Progress */}
      {myProgress && (
        <section className="glass-card gamification-levels__banner">
          <div className="gamification-levels__banner-left">
            <div className="gamification-levels__avatar-circle">
              <span className="material-symbols-outlined">stars</span>
            </div>
            <div>
              <span className="text-label-md text-disabled">{t('levels.myProgress')}</span>
              <h2 className="text-headline-sm">
                {t('levels.level', { level: currentLevelNo })}: {myProgress.currentTitle}
              </h2>
              <p className="text-body-md text-mid">
                {t('overview.levelCard.totalXp', { xp: myProgress.totalXp ?? 0 })}
              </p>
            </div>
          </div>

          <div className="gamification-levels__banner-right">
            <ProgressBar
              title={t('overview.levelCard.progressTitle')}
              current={myProgress.totalXp ?? 0}
              total={myProgress.nextLevelRequiredXp ?? myProgress.totalXp ?? 0}
              percent={myProgress.progressPercent ?? (myProgress.nextLevel ? 0 : 100)}
            />
            <span className="text-body-sm text-disabled">
              {myProgress.nextLevel
                ? t('overview.levelCard.nextLevel', {
                    xp: myProgress.xpToNextLevel ?? 0,
                    title: myProgress.nextLevelTitle || '',
                    level: myProgress.nextLevel,
                  })
                : t('overview.levelCard.maxLevel')}
            </span>
          </div>
        </section>
      )}

      {/* Levels Roadmap List */}
      <section className="gamification-levels__roadmap">
        <div className="gamification-levels__roadmap-header">
          <h3 className="text-headline-sm">{t('levels.title')}</h3>
          <p className="text-body-md text-disabled">{t('levels.desc')}</p>
        </div>

        {allLevels.length === 0 ? (
          <EmptyState variant="compact" icon="stairs" title={t('levels.emptyLevels')} />
        ) : (
          <div className="gamification-levels__timeline">
            {allLevels.map((lvl) => {
              const isCurrent = lvl.levelNo === currentLevelNo;
              const isCompleted = lvl.levelNo < currentLevelNo;
              const isLocked = lvl.levelNo > currentLevelNo;

              return (
                <div
                  key={lvl.levelNo}
                  className={`gamification-levels__item glass-card ${
                    isCurrent ? 'gamification-levels__item--current' : ''
                  } ${isCompleted ? 'gamification-levels__item--completed' : ''} ${
                    isLocked ? 'gamification-levels__item--locked' : ''
                  }`}
                >
                  <div className="gamification-levels__marker">
                    <span className="material-symbols-outlined">
                      {isCompleted ? 'check_circle' : isCurrent ? 'stars' : 'lock'}
                    </span>
                  </div>

                  <div className="gamification-levels__content">
                    <div className="gamification-levels__content-top">
                      <span className="text-headline-sm gamification-levels__level-no">
                        {t('levels.level', { level: lvl.levelNo })}
                      </span>
                      {isCurrent && (
                        <span className="badge badge--primary">
                          {t('levels.currentLevelBadge')}
                        </span>
                      )}
                      {isCompleted && (
                        <span className="badge badge--mint">{t('levels.completedLevel')}</span>
                      )}
                      {isLocked && (
                        <span className="badge badge--default">{t('levels.lockedLevel')}</span>
                      )}
                    </div>

                    <h4 className="text-title-md gamification-levels__title">{lvl.title}</h4>

                    <span className="text-body-sm text-disabled gamification-levels__xp-req">
                      {t('levels.requiredXp', { xp: lvl.requiredXp })}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

export default GamificationLevelsTab;
