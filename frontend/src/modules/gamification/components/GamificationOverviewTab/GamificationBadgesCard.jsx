import EmptyState from '../../../../components/EmptyState/EmptyState';
import './GamificationBadgesCard.css';

function GamificationBadgesCard({ badges, t }) {
  return (
    <section className="glass-card gamification-overview__badges-card">
      <div className="gamification-overview__card-header">
        <div className="gamification-overview__icon-wrap">
          <span className="material-symbols-outlined">workspace_premium</span>
        </div>
        <div>
          <h2 className="text-headline-sm">{t('overview.badgesCard.title')}</h2>
          <span className="text-body-sm text-disabled">
            {t('overview.badgesCard.badgeCount', { count: badges.length })}
          </span>
        </div>
      </div>
      {badges.length === 0 ? (
        <EmptyState
          variant="compact"
          icon="military_tech"
          title={t('overview.badgesCard.emptyTitle')}
          description={t('overview.badgesCard.emptyDesc')}
        />
      ) : (
        <div className="overview-badges-grid">
          {badges.map((badge) => (
            <div key={badge.id} className="overview-badge-item glass-well">
              <div className="overview-badge-item__icon-wrap">
                {badge.iconUrl ? (
                  <img src={badge.iconUrl} alt={badge.name} className="overview-badge-item__icon" />
                ) : (
                  <span className="material-symbols-outlined overview-badge-item__default-icon">
                    shield
                  </span>
                )}
              </div>
              <div className="overview-badge-item__info">
                <h3 className="text-title-md overview-badge-item__name">{badge.name}</h3>
                <p className="text-body-sm text-disabled overview-badge-item__desc">
                  {badge.description}
                </p>
                {badge.earnedAt && (
                  <span className="overview-badge-item__date text-body-sm">
                    {t('overview.badgesCard.earnedAt', {
                      date: new Date(badge.earnedAt).toLocaleDateString(),
                    })}
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export default GamificationBadgesCard;
