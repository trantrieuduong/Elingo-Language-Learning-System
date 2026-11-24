import Tabs from '../../../../components/Tabs/Tabs';
import './GamificationRankCard.css';

function GamificationRankCard({ rankData, rankPeriod, rankPeriodTabs, onPeriodChange, t }) {
  return (
    <section className="glass-card gamification-overview__card gamification-overview__card--rank">
      <div className="gamification-overview__card-header">
        <div className="gamification-overview__icon-wrap">
          <span className="material-symbols-outlined">military_tech</span>
        </div>
        <div>
          <h2 className="text-headline-sm">{t('overview.rankCard.title')}</h2>
          <span className="text-body-sm text-disabled">{rankData?.periodKey || ''}</span>
        </div>
        <Tabs
          tabs={rankPeriodTabs}
          activeTab={rankPeriod}
          onChange={onPeriodChange}
          className="gamification-overview__period-tabs"
        />
      </div>

      <div className="gamification-overview__card-body">
        <div className="overview-rank-display">
          <span className="overview-rank-display__num">
            {rankData?.rank
              ? t('overview.rankCard.rank', { rank: rankData.rank })
              : t('overview.rankCard.unranked')}
          </span>
          <span className="overview-rank-display__xp">
            {t('overview.rankCard.totalXp', { xp: rankData?.totalXp ?? 0 })}
          </span>
        </div>

        {rankData?.rank === 1 ? (
          <div className="overview-rank-tip overview-rank-tip--lead">
            <span className="material-symbols-outlined">emoji_events</span>
            <span>{t('overview.rankCard.topRank')}</span>
          </div>
        ) : rankData?.diffToNextRank != null && rankData.diffToNextRank > 0 ? (
          <div className="overview-rank-tip">
            <span className="material-symbols-outlined">trending_up</span>
            <span>{t('overview.rankCard.diffToNext', { xp: rankData.diffToNextRank })}</span>
          </div>
        ) : null}
      </div>
    </section>
  );
}

export default GamificationRankCard;
