import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  getMyBadgesApi,
  getMyRankApi,
  getMyLevelProgressApi,
  getMyStreakApi,
} from '../../gamificationApi';
import ProgressBar from '../../../../components/ProgressBar/ProgressBar';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import Tabs from '../../../../components/Tabs/Tabs';
import './GamificationOverviewTab.css';

function GamificationOverviewTab() {
  const { t } = useTranslation('gamification');

  // Loading & error
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Data states
  const [rankPeriod, setRankPeriod] = useState('WEEKLY');
  const rankPeriodTabs = [
    { key: 'WEEKLY', label: t('overview.rankCard.weekly') },
    { key: 'MONTHLY', label: t('overview.rankCard.monthly') },
  ];
  const [rankData, setRankData] = useState(null);
  const [levelProgress, setLevelProgress] = useState(null);
  const [badges, setBadges] = useState([]);

  // Streak states & calendar
  const today = new Date();
  const [calendarYear, setCalendarYear] = useState(today.getFullYear());
  const [calendarMonth, setCalendarMonth] = useState(today.getMonth() + 1);
  const [streakData, setStreakData] = useState(null);

  const fetchOverviewData = async () => {
    setLoading(true);
    setError('');
    try {
      const [badgesRes, rankRes, levelRes, streakRes] = await Promise.all([
        getMyBadgesApi(),
        getMyRankApi(rankPeriod),
        getMyLevelProgressApi(),
        getMyStreakApi({ year: calendarYear, month: calendarMonth }),
      ]);

      if (badgesRes?.success) setBadges(badgesRes.data || []);
      if (rankRes?.success) setRankData(rankRes.data || null);
      if (levelRes?.success) setLevelProgress(levelRes.data || null);
      if (streakRes?.success) setStreakData(streakRes.data || null);
    } catch {
      setError(t('common.loadError'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOverviewData();
  }, [rankPeriod, calendarYear, calendarMonth]);

  const handlePrevMonth = () => {
    if (calendarMonth === 1) {
      setCalendarMonth(12);
      setCalendarYear((y) => y - 1);
    } else {
      setCalendarMonth((m) => m - 1);
    }
  };

  const handleNextMonth = () => {
    if (calendarMonth === 12) {
      setCalendarMonth(1);
      setCalendarYear((y) => y + 1);
    } else {
      setCalendarMonth((m) => m + 1);
    }
  };

  // Calendar matrix generator
  const renderCalendar = () => {
    const daysInMonth = new Date(calendarYear, calendarMonth, 0).getDate();
    // 0 = Sunday, 1 = Monday...
    const firstDayIndex = new Date(calendarYear, calendarMonth - 1, 1).getDay();
    // Monday as first column (0 = Monday, ..., 6 = Sunday)
    const startOffset = (firstDayIndex + 6) % 7;

    const checkinSet = new Set(streakData?.checkinDates || []);

    const cells = [];
    for (let i = 0; i < startOffset; i++) {
      cells.push(
        <div
          key={`empty-${i}`}
          className="overview-calendar__cell overview-calendar__cell--empty"
        />
      );
    }

    for (let d = 1; d <= daysInMonth; d++) {
      const monthStr = String(calendarMonth).padStart(2, '0');
      const dayStr = String(d).padStart(2, '0');
      const dateKey = `${calendarYear}-${monthStr}-${dayStr}`;
      const isCheckedIn = checkinSet.has(dateKey);
      const isToday =
        today.getFullYear() === calendarYear &&
        today.getMonth() + 1 === calendarMonth &&
        today.getDate() === d;

      cells.push(
        <div
          key={dateKey}
          className={`overview-calendar__cell ${isCheckedIn ? 'overview-calendar__cell--checked' : ''} ${
            isToday ? 'overview-calendar__cell--today' : ''
          }`}
          title={dateKey}
        >
          <span className="overview-calendar__day-num">{d}</span>
          {isCheckedIn && (
            <span className="material-symbols-outlined overview-calendar__check-icon">
              check_circle
            </span>
          )}
        </div>
      );
    }

    return cells;
  };

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
        onAction={fetchOverviewData}
      />
    );
  }

  return (
    <div className="gamification-overview">
      {/* Grid Cards Row 1: Left (Rank + Level stacked) vs Right (Streak & Calendar) */}
      <div className="gamification-overview__grid">
        {/* Cột trái: Thứ hạng cá nhân & Cấp độ hiện tại xếp chồng */}
        <div className="gamification-overview__col-left">
          {/* Card 1: Thứ hạng cá nhân */}
          <section className="glass-card gamification-overview__card gamification-overview__card--rank">
            <div className="gamification-overview__card-header">
              <div className="gamification-overview__icon-wrap">
                <span className="material-symbols-outlined">military_tech</span>
              </div>
              <div>
                <h2 className="text-headline-sm">{t('overview.rankCard.title')}</h2>
                <span className="text-body-sm text-disabled">{rankData?.periodKey || ''}</span>
              </div>
              {/* Period Switcher */}
              <Tabs
                tabs={rankPeriodTabs}
                activeTab={rankPeriod}
                onChange={setRankPeriod}
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

          {/* Card 2: Cấp độ hiện tại & Tiến độ */}
          <section className="glass-card gamification-overview__card gamification-overview__card--level">
            <div className="gamification-overview__card-header">
              <div className="gamification-overview__icon-wrap">
                <span className="material-symbols-outlined">stars</span>
              </div>
              <div>
                <h2 className="text-headline-sm">{t('overview.levelCard.title')}</h2>
                <span className="text-body-sm text-disabled">
                  {levelProgress?.currentTitle || 'Elingo Learner'}
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
        </div>

        {/* Cột phải: Streak cá nhân & Lịch Check-in */}
        <section className="glass-card gamification-overview__card gamification-overview__card--streak">
          <div className="gamification-overview__card-header">
            <div className="gamification-overview__icon-wrap overview-icon--fire">
              <span className="material-symbols-outlined">local_fire_department</span>
            </div>
            <div>
              <h2 className="text-headline-sm">{t('overview.streakCard.title')}</h2>
              <span className="text-body-sm text-disabled">
                {t('overview.streakCard.longestStreak', {
                  days: streakData?.longestStreak ?? 0,
                })}
              </span>
            </div>
          </div>

          <div className="gamification-overview__card-body">
            <div className="overview-streak-highlight">
              <div className="overview-streak-count">
                <span className="material-symbols-outlined overview-fire-icon">
                  local_fire_department
                </span>
                <span className="overview-streak-days">{streakData?.currentStreak ?? 0}</span>
                <span className="overview-streak-label">days streak</span>
              </div>

              <div
                className={`overview-streak-status badge ${
                  streakData?.checkedInToday ? 'badge--mint' : 'badge--pink'
                }`}
              >
                <span className="material-symbols-outlined">
                  {streakData?.checkedInToday ? 'task_alt' : 'schedule'}
                </span>
                <span>
                  {streakData?.checkedInToday
                    ? t('overview.streakCard.checkedInToday')
                    : t('overview.streakCard.notCheckedInToday')}
                </span>
              </div>
            </div>

            {!streakData?.checkedInToday && (
              <p className="overview-checkin-help text-body-sm">
                {t('overview.streakCard.checkinHelp')}
              </p>
            )}

            {/* Monthly Calendar View */}
            <div className="overview-calendar">
              <div className="overview-calendar__header">
                <button
                  type="button"
                  className="btn-icon"
                  onClick={handlePrevMonth}
                  title={t('overview.streakCard.prevMonth')}
                >
                  <span className="material-symbols-outlined">chevron_left</span>
                </button>
                <span className="text-label-lg">
                  {t('overview.streakCard.calendarTitle', {
                    month: calendarMonth,
                    year: calendarYear,
                  })}
                </span>
                <button
                  type="button"
                  className="btn-icon"
                  onClick={handleNextMonth}
                  title={t('overview.streakCard.nextMonth')}
                >
                  <span className="material-symbols-outlined">chevron_right</span>
                </button>
              </div>

              <div className="overview-calendar__weekdays">
                {['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'].map((day) => (
                  <span key={day} className="overview-calendar__weekday-label">
                    {t(`overview.streakCard.weekDays.${day}`)}
                  </span>
                ))}
              </div>

              <div className="overview-calendar__grid">{renderCalendar()}</div>

              <div className="overview-calendar__footer text-body-sm text-disabled">
                {t('overview.streakCard.checkinDaysCount', {
                  count: streakData?.monthCheckinCount ?? 0,
                })}
              </div>
            </div>
          </div>
        </section>
      </div>

      {/* Row 2: Badges List */}
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
                    <img
                      src={badge.iconUrl}
                      alt={badge.name}
                      className="overview-badge-item__icon"
                    />
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
    </div>
  );
}

export default GamificationOverviewTab;
