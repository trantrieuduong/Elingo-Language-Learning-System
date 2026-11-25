import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { trRx } from '../../../utils/i18n';
import GamificationPage from '../../../../src/modules/gamification/pages/GamificationPage';
import * as gamificationApi from '../../../../src/modules/gamification/gamificationApi';

// Mock các API functions
vi.mock('../../../../src/modules/gamification/gamificationApi');

describe('GamificationPage', () => {
  const mockBadges = [
    {
      id: 1,
      code: 'STREAK_7_DAYS',
      name: '7-Day Streak Master',
      description: 'Maintain a learning streak for 7 consecutive days',
      iconUrl: 'https://cdn.elingo.com/badges/streak-7.png',
      earnedAt: '2026-11-16T10:30:00',
    },
  ];

  const mockRank = {
    rank: 3,
    totalXp: 850,
    diffToNextRank: 50,
    periodType: 'WEEKLY',
    periodKey: '2026-W46',
  };

  const mockLevelProgress = {
    userId: 1,
    currentLevel: 2,
    currentTitle: 'Explorer',
    totalXp: 150,
    currentLevelRequiredXp: 100,
    nextLevel: 3,
    nextLevelTitle: 'Challenger',
    nextLevelRequiredXp: 300,
    xpToNextLevel: 150,
    progressPercent: 25.0,
  };

  const mockStreak = {
    userId: 1,
    currentStreak: 5,
    longestStreak: 14,
    lastActiveDate: '2026-11-16',
    checkedInToday: true,
    monthCheckinCount: 5,
    checkinDates: ['2026-11-01', '2026-11-02', '2026-11-16'],
  };

  const mockAllLevels = [
    { levelNo: 1, requiredXp: 0, title: 'Novice' },
    { levelNo: 2, requiredXp: 100, title: 'Explorer' },
    { levelNo: 3, requiredXp: 300, title: 'Challenger' },
  ];

  const mockLeaderboard = {
    periodType: 'WEEKLY',
    periodKey: '2026-W46',
    leaderboard: {
      content: [
        {
          rank: 1,
          userId: 10,
          username: 'alex_master',
          avatarUrl: 'https://cdn.elingo.com/avatars/user10.png',
          totalXp: 1250,
        },
        {
          rank: 2,
          userId: 25,
          username: 'sarah_lang',
          avatarUrl: 'https://cdn.elingo.com/avatars/user25.png',
          totalXp: 1100,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      isLast: true,
    },
    myRank: {
      rank: 3,
      totalXp: 850,
      diffToNextRank: 250,
      periodType: 'WEEKLY',
      periodKey: '2026-W46',
    },
  };

  beforeEach(() => {
    vi.clearAllMocks();

    gamificationApi.getUserBadgesApi.mockResolvedValue({
      success: true,
      data: mockBadges,
    });
    gamificationApi.getMyRankApi.mockResolvedValue({
      success: true,
      data: mockRank,
    });
    gamificationApi.getUserLevelProgressApi.mockResolvedValue({
      success: true,
      data: mockLevelProgress,
    });
    gamificationApi.getUserStreakApi.mockResolvedValue({
      success: true,
      data: mockStreak,
    });
    gamificationApi.getAllLevelsApi.mockResolvedValue({
      success: true,
      data: mockAllLevels,
    });
    gamificationApi.getRealtimeLeaderboardApi.mockResolvedValue({
      success: true,
      data: mockLeaderboard,
    });
    gamificationApi.getHistoricalLeaderboardApi.mockResolvedValue({
      success: true,
      data: {
        content: mockLeaderboard.leaderboard.content,
        totalPages: 1,
        totalElements: 2,
      },
    });
  });

  it('renders Gamification page with tabs and loads Overview data initially', async () => {
    render(<GamificationPage userId={1} />);

    // Chờ loading kết thúc và kiểm tra dữ liệu Overview được hiển thị
    await waitFor(() => {
      expect(screen.getByText('7-Day Streak Master')).toBeInTheDocument();
    });

    expect(gamificationApi.getUserBadgesApi).toHaveBeenCalledWith(1);
    expect(gamificationApi.getMyRankApi).toHaveBeenCalledWith('WEEKLY');
    expect(gamificationApi.getUserLevelProgressApi).toHaveBeenCalledWith(1);
    expect(gamificationApi.getUserStreakApi).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ year: expect.any(Number), month: expect.any(Number) })
    );
  });

  it('switches to Levels tab and loads roadmap data', async () => {
    render(<GamificationPage userId={1} />);


    const levelsTabBtn = screen.getByRole('button', { name: trRx('gamification:tabs.levels') });
    fireEvent.click(levelsTabBtn);

    await waitFor(() => {
      expect(screen.getByText('Novice')).toBeInTheDocument();
      expect(screen.getByText('Explorer')).toBeInTheDocument();
      expect(screen.getByText('Challenger')).toBeInTheDocument();
    });

    expect(gamificationApi.getAllLevelsApi).toHaveBeenCalledTimes(1);
    expect(gamificationApi.getUserLevelProgressApi).toHaveBeenCalledTimes(2);
  });

  it('switches to Leaderboard tab and renders realtime rankings and user banner', async () => {
    render(<GamificationPage userId={1} />);

    // Click tab Bảng xếp hạng / Leaderboard
    const lbTabBtn = screen.getByRole('button', {
      name: trRx('gamification:tabs.leaderboard'),
    });
    fireEvent.click(lbTabBtn);

    await waitFor(() => {
      expect(screen.getByText('alex_master')).toBeInTheDocument();
      expect(screen.getByText('sarah_lang')).toBeInTheDocument();
    });

    expect(gamificationApi.getRealtimeLeaderboardApi).toHaveBeenCalled();
  });
});
