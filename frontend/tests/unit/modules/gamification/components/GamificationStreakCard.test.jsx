import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { LANGS, tr, useLang } from '../../../../utils/i18n';
import GamificationStreakCard from '@/modules/gamification/components/GamificationOverviewTab/GamificationStreakCard';

const t = (key, options) => tr(`gamification:${key}`, options);

describe('GamificationStreakCard', () => {
  describe.each(LANGS)('localized rendering (%s)', (lang) => {
    it('renders checked-in state without checkinHelp', async () => {
      await useLang(lang);
      render(
        <GamificationStreakCard
          streakData={{ currentStreak: 7, longestStreak: 14, checkedInToday: true, monthCheckinCount: 12 }}
          calendarYear={2026}
          calendarMonth={11}
          calendarCells={[<div key="1">cell-1</div>]}
          onPrevMonth={vi.fn()}
          onNextMonth={vi.fn()}
          t={t}
        />
      );

      expect(screen.getByText(t('overview.streakCard.title'))).toBeInTheDocument();
      expect(screen.getByText(t('overview.streakCard.longestStreak', { days: 14 }))).toBeInTheDocument();
      expect(screen.getByText('7')).toBeInTheDocument();
      expect(screen.getByText(t('overview.streakCard.checkedInToday'))).toBeInTheDocument();
      expect(screen.queryByText(t('overview.streakCard.checkinHelp'))).not.toBeInTheDocument();
      expect(
        screen.getByText(t('overview.streakCard.calendarTitle', { month: 11, year: 2026 }))
      ).toBeInTheDocument();
      expect(
        screen.getByText(t('overview.streakCard.checkinDaysCount', { count: 12 }))
      ).toBeInTheDocument();
      expect(screen.getByText('cell-1')).toBeInTheDocument();
      expect(screen.getByText(t('overview.streakCard.weekDays.mon'))).toBeInTheDocument();
      expect(screen.getByText(t('overview.streakCard.weekDays.sun'))).toBeInTheDocument();
    });

    it('renders not-checked-in state and checkinHelp', async () => {
      await useLang(lang);
      render(
        <GamificationStreakCard
          streakData={{ currentStreak: 0, longestStreak: 3, checkedInToday: false, monthCheckinCount: 0 }}
          calendarYear={2026}
          calendarMonth={11}
          calendarCells={[]}
          onPrevMonth={vi.fn()}
          onNextMonth={vi.fn()}
          t={t}
        />
      );

      expect(screen.getByText(t('overview.streakCard.notCheckedInToday'))).toBeInTheDocument();
      expect(screen.getByText(t('overview.streakCard.checkinHelp'))).toBeInTheDocument();
    });
  });

  it('calls onPrevMonth and onNextMonth when navigation buttons are clicked', () => {
    const onPrevMonth = vi.fn();
    const onNextMonth = vi.fn();

    render(
      <GamificationStreakCard
        streakData={null}
        calendarYear={2026}
        calendarMonth={11}
        calendarCells={[]}
        onPrevMonth={onPrevMonth}
        onNextMonth={onNextMonth}
        t={t}
      />
    );

    fireEvent.click(screen.getByTitle(t('overview.streakCard.prevMonth')));
    fireEvent.click(screen.getByTitle(t('overview.streakCard.nextMonth')));

    expect(onPrevMonth).toHaveBeenCalledTimes(1);
    expect(onNextMonth).toHaveBeenCalledTimes(1);
  });
});
