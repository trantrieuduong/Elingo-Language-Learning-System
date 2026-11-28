import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { tr } from '../../../../utils/i18n';
import GamificationOverviewTab from '@/modules/gamification/components/GamificationOverviewTab/GamificationOverviewTab';
import * as gamificationApi from '@/modules/gamification/gamificationApi';

vi.mock('@/modules/gamification/gamificationApi');

describe('GamificationOverviewTab', () => {
  const mockBadges = [
    {
      id: 1,
      name: 'Early Bird',
      description: 'Study before 8am',
      iconUrl: null,
      earnedAt: null,
    },
  ];
  const mockRank = { rank: 2, totalXp: 900, diffToNextRank: 50, periodKey: '2026-W47' };
  const mockLevel = {
    currentLevel: 3,
    currentTitle: 'Challenger',
    totalXp: 350,
    nextLevel: 4,
    nextLevelTitle: 'Master',
    nextLevelRequiredXp: 600,
    xpToNextLevel: 250,
    progressPercent: 40,
  };
  const mockStreak = {
    currentStreak: 4,
    longestStreak: 10,
    checkedInToday: true,
    monthCheckinCount: 2,
    checkinDates: ['2026-01-05', '2026-01-15'],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.setSystemTime(new Date(2026, 0, 15)); // Jan 15, 2026

    gamificationApi.getUserBadgesApi.mockResolvedValue({ success: true, data: mockBadges });
    gamificationApi.getMyRankApi.mockResolvedValue({ success: true, data: mockRank });
    gamificationApi.getUserLevelProgressApi.mockResolvedValue({ success: true, data: mockLevel });
    gamificationApi.getUserStreakApi.mockResolvedValue({ success: true, data: mockStreak });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows loading initially and renders all overview cards with calendar check-ins', async () => {
    const { container } = render(<GamificationOverviewTab userId={5} />);

    expect(container.querySelector('.page-loading')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Early Bird')).toBeInTheDocument();
    });

    expect(gamificationApi.getUserBadgesApi).toHaveBeenCalledWith(5);
    expect(gamificationApi.getMyRankApi).toHaveBeenCalledWith('WEEKLY');
    expect(gamificationApi.getUserLevelProgressApi).toHaveBeenCalledWith(5);
    expect(gamificationApi.getUserStreakApi).toHaveBeenCalledWith(5, { year: 2026, month: 1 });

    expect(container.querySelector('[title="2026-01-05"]')).toHaveClass('overview-calendar__cell--checked');
    expect(container.querySelector('[title="2026-01-15"]')).toHaveClass('overview-calendar__cell--today');
  });

  it('refetches with MONTHLY when rank period tab changes', async () => {
    render(<GamificationOverviewTab userId={5} />);

    await waitFor(() => {
      expect(screen.getByText('Early Bird')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: tr('gamification:overview.rankCard.monthly') }));

    await waitFor(() => {
      expect(gamificationApi.getMyRankApi).toHaveBeenCalledWith('MONTHLY');
    });
  });

  it('handles prev/next month navigation across year boundaries', async () => {
    render(<GamificationOverviewTab userId={5} />);

    await waitFor(() => {
      expect(screen.getByText('Early Bird')).toBeInTheDocument();
    });

    // Jan 2026 -> Dec 2025
    fireEvent.click(screen.getByTitle(tr('gamification:overview.streakCard.prevMonth')));
    await waitFor(() => {
      expect(gamificationApi.getUserStreakApi).toHaveBeenCalledWith(5, { year: 2025, month: 12 });
    });

    // Dec 2025 -> Jan 2026
    fireEvent.click(screen.getByTitle(tr('gamification:overview.streakCard.nextMonth')));
    await waitFor(() => {
      expect(gamificationApi.getUserStreakApi).toHaveBeenLastCalledWith(5, { year: 2026, month: 1 });
    });
  });

  it('renders error EmptyState on API failure and retries on action click', async () => {
    gamificationApi.getUserBadgesApi.mockRejectedValueOnce(new Error('Network error'));

    render(<GamificationOverviewTab userId={5} />);

    await waitFor(() => {
      expect(screen.getByText(tr('gamification:common.loadError'))).toBeInTheDocument();
    });

    gamificationApi.getUserBadgesApi.mockResolvedValueOnce({ success: true, data: mockBadges });
    fireEvent.click(screen.getByRole('button', { name: tr('gamification:common.retry') }));

    await waitFor(() => {
      expect(screen.getByText('Early Bird')).toBeInTheDocument();
    });
  });
});
