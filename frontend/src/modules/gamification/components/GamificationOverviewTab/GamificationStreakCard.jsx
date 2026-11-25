import './GamificationStreakCard.css';

function GamificationStreakCard({
  streakData,
  calendarYear,
  calendarMonth,
  calendarCells,
  onPrevMonth,
  onNextMonth,
  t,
}) {
  return (
    <section className="glass-card gamification-overview__card gamification-overview__card--streak">
      <div className="gamification-overview__card-header">
        <div className="gamification-overview__icon-wrap overview-icon--fire">
          <span className="material-symbols-outlined">local_fire_department</span>
        </div>
        <div>
          <h2 className="text-headline-sm">{t('overview.streakCard.title')}</h2>
          <span className="text-body-sm text-disabled">
            {t('overview.streakCard.longestStreak', { days: streakData?.longestStreak ?? 0 })}
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
            className={`overview-streak-status badge ${streakData?.checkedInToday ? 'badge--mint' : 'badge--pink'}`}
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
        <div className="overview-calendar">
          <div className="overview-calendar__header">
            <button
              type="button"
              className="btn-icon"
              onClick={onPrevMonth}
              title={t('overview.streakCard.prevMonth')}
            >
              <span className="material-symbols-outlined">chevron_left</span>
            </button>
            <span className="text-label-lg">
              {t('overview.streakCard.calendarTitle', { month: calendarMonth, year: calendarYear })}
            </span>
            <button
              type="button"
              className="btn-icon"
              onClick={onNextMonth}
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
          <div className="overview-calendar__grid">{calendarCells}</div>
          <div className="overview-calendar__footer text-body-sm text-disabled">
            {t('overview.streakCard.checkinDaysCount', {
              count: streakData?.monthCheckinCount ?? 0,
            })}
          </div>
        </div>
      </div>
    </section>
  );
}

export default GamificationStreakCard;
