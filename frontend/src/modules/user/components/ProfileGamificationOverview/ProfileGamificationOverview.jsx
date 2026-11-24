import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import GamificationBadgesCard from '../../../gamification/components/GamificationOverviewTab/GamificationBadgesCard';
import GamificationLevelCard from '../../../gamification/components/GamificationOverviewTab/GamificationLevelCard';
import GamificationStreakCard from '../../../gamification/components/GamificationOverviewTab/GamificationStreakCard';
import '../../../gamification/components/GamificationOverviewTab/GamificationOverviewTab.css';
import {
  getUserBadgesApi,
  getUserLevelProgressApi,
  getUserStreakApi,
} from '../../../gamification/gamificationApi';
import ProfileCard from '../ProfileCard/ProfileCard';
import './ProfileGamificationOverview.css';

function ProfileGamificationOverview({ profile, actionMenu }) {
  const { t } = useTranslation('gamification');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [levelProgress, setLevelProgress] = useState(null);
  const [badges, setBadges] = useState([]);
  const today = new Date();
  const [calendarYear, setCalendarYear] = useState(today.getFullYear());
  const [calendarMonth, setCalendarMonth] = useState(today.getMonth() + 1);
  const [streakData, setStreakData] = useState(null);

  const fetchProfileGamification = async () => {
    setLoading(true);
    setError('');
    try {
      const [badgesRes, levelRes, streakRes] = await Promise.all([
        getUserBadgesApi(profile.id),
        getUserLevelProgressApi(profile.id),
        getUserStreakApi(profile.id, { year: calendarYear, month: calendarMonth }),
      ]);
      if (badgesRes?.success) setBadges(badgesRes.data || []);
      if (levelRes?.success) setLevelProgress(levelRes.data || null);
      if (streakRes?.success) setStreakData(streakRes.data || null);
    } catch {
      setError(t('common.loadError'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfileGamification();
  }, [profile.id, calendarYear, calendarMonth]);

  const handlePrevMonth = () => {
    if (calendarMonth === 1) {
      setCalendarMonth(12);
      setCalendarYear((year) => year - 1);
    } else setCalendarMonth((month) => month - 1);
  };

  const handleNextMonth = () => {
    if (calendarMonth === 12) {
      setCalendarMonth(1);
      setCalendarYear((year) => year + 1);
    } else setCalendarMonth((month) => month + 1);
  };

  const renderCalendar = () => {
    const daysInMonth = new Date(calendarYear, calendarMonth, 0).getDate();
    const firstDayIndex = new Date(calendarYear, calendarMonth - 1, 1).getDay();
    const startOffset = (firstDayIndex + 6) % 7;
    const checkinSet = new Set(streakData?.checkinDates || []);
    const cells = [];
    for (let index = 0; index < startOffset; index += 1) {
      cells.push(
        <div
          key={`empty-${index}`}
          className="overview-calendar__cell overview-calendar__cell--empty"
        />
      );
    }
    for (let day = 1; day <= daysInMonth; day += 1) {
      const dateKey = `${calendarYear}-${String(calendarMonth).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      const isCheckedIn = checkinSet.has(dateKey);
      const isToday =
        today.getFullYear() === calendarYear &&
        today.getMonth() + 1 === calendarMonth &&
        today.getDate() === day;
      cells.push(
        <div
          key={dateKey}
          className={`overview-calendar__cell ${isCheckedIn ? 'overview-calendar__cell--checked' : ''} ${isToday ? 'overview-calendar__cell--today' : ''}`}
          title={dateKey}
        >
          <span className="overview-calendar__day-num">{day}</span>
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

  if (loading) return <PageLoading />;
  if (error)
    return (
      <EmptyState
        icon="error"
        isError
        description={error}
        actionLabel={t('common.retry')}
        onAction={fetchProfileGamification}
      />
    );

  return (
    <div className="profile-gamification-overview">
      <div className="profile-gamification-overview__grid">
        <div className="profile-gamification-overview__left-column">
          <ProfileCard profile={profile} actionMenu={actionMenu} />
          <GamificationLevelCard levelProgress={levelProgress} t={t} />
        </div>
        <GamificationStreakCard
          streakData={streakData}
          calendarYear={calendarYear}
          calendarMonth={calendarMonth}
          calendarCells={renderCalendar()}
          onPrevMonth={handlePrevMonth}
          onNextMonth={handleNextMonth}
          t={t}
        />
      </div>
      <GamificationBadgesCard badges={badges} t={t} />
    </div>
  );
}

export default ProfileGamificationOverview;
