import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { tr, trRx } from '../../../../utils/i18n';
import GamificationLeaderboardTab from '@/modules/gamification/components/GamificationLeaderboardTab/GamificationLeaderboardTab';
import * as gamificationApi from '@/modules/gamification/gamificationApi';

vi.mock('@/modules/gamification/gamificationApi');

describe('GamificationLeaderboardTab', () => {
  const mockRealtimeRows = [
    { rank: 1, userId: 1, username: 'alice', avatarUrl: 'https://cdn.example.com/1.png', totalXp: 1500 },
    { rank: 2, userId: 2, username: 'bob', avatarUrl: null, totalXp: 1200 },
    { rank: 3, userId: 3, username: 'carol', avatarUrl: null, totalXp: 900 },
    { rank: 4, userId: 4, username: null, avatarUrl: null, totalXp: 600 },
  ];

  const mockRealtimeResponse = {
    success: true,
    data: {
      periodType: 'WEEKLY',
      periodKey: '2026-W47',
      leaderboard: {
        content: mockRealtimeRows,
        totalPages: 2,
      },
    },
  };

  beforeEach(() => {
    vi.clearAllMocks();
    gamificationApi.getRealtimeLeaderboardApi.mockResolvedValue(mockRealtimeResponse);
    gamificationApi.getHistoricalLeaderboardApi.mockResolvedValue({
      success: true,
      data: {
        content: [{ rank: 1, userId: 9, username: 'hist_user', avatarUrl: null, totalXp: 2000 }],
        totalPages: 1,
      },
    });
  });

  it('loads realtime weekly leaderboard, renders medals, fallback avatar, anonymous user, and period badge', async () => {
    render(<GamificationLeaderboardTab />);

    await waitFor(() => {
      expect(screen.getByText('alice')).toBeInTheDocument();
    });

    expect(gamificationApi.getRealtimeLeaderboardApi).toHaveBeenCalledWith({
      periodType: 'WEEKLY',
      page: 1,
      size: 20,
    });
    expect(screen.getByText(tr('gamification:leaderboard.periodKey', { key: '2026-W47' }))).toBeInTheDocument();
    expect(screen.getByLabelText('Rank 1')).toBeInTheDocument();
    expect(screen.getByLabelText('Rank 2')).toBeInTheDocument();
    expect(screen.getByLabelText('Rank 3')).toBeInTheDocument();
    expect(screen.getByText('#4')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'alice' })).toHaveAttribute('src', 'https://cdn.example.com/1.png');
  });

  it('switches periodType to MONTHLY and supports pagination', async () => {
    render(<GamificationLeaderboardTab />);

    await waitFor(() => {
      expect(screen.getByText('alice')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: '2' }));
    await waitFor(() => {
      expect(gamificationApi.getRealtimeLeaderboardApi).toHaveBeenCalledWith({
        periodType: 'WEEKLY',
        page: 2,
        size: 20,
      });
    });

    fireEvent.click(screen.getByRole('button', { name: tr('gamification:leaderboard.monthly') }));
    await waitFor(() => {
      expect(gamificationApi.getRealtimeLeaderboardApi).toHaveBeenCalledWith({
        periodType: 'MONTHLY',
        page: 1,
        size: 20,
      });
    });
  });

  it('switches to history mode, auto-selects suggested periodKey, and fetches historical leaderboard', async () => {
    render(<GamificationLeaderboardTab />);

    await waitFor(() => {
      expect(screen.getByText('alice')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: trRx('gamification:leaderboard.historyTab') }));

    await waitFor(() => {
      expect(screen.getByText('hist_user')).toBeInTheDocument();
    });

    expect(gamificationApi.getHistoricalLeaderboardApi).toHaveBeenCalledWith(
      expect.objectContaining({
        periodType: 'WEEKLY',
        periodKey: expect.stringMatching(/^\d{4}-W\d{2}$/),
        page: 1,
        size: 20,
      })
    );

    const select = screen.getByLabelText(new RegExp(tr('gamification:leaderboard.selectPeriod')));
    const secondOption = select.querySelectorAll('option')[1].value;
    fireEvent.change(select, { target: { value: secondOption } });

    await waitFor(() => {
      expect(gamificationApi.getHistoricalLeaderboardApi).toHaveBeenCalledWith(
        expect.objectContaining({
          periodType: 'WEEKLY',
          periodKey: secondOption,
          page: 1,
          size: 20,
        })
      );
    });
  });

  it('renders empty state when leaderboard content is empty', async () => {
    gamificationApi.getRealtimeLeaderboardApi.mockResolvedValue({
      success: true,
      data: { periodKey: '2026-W47', leaderboard: { content: [], totalPages: 1 } },
    });

    render(<GamificationLeaderboardTab />);

    await waitFor(() => {
      expect(screen.getByText(tr('gamification:leaderboard.emptyTitle'))).toBeInTheDocument();
    });
  });

  it('renders error EmptyState on API failure and retries on click', async () => {
    gamificationApi.getRealtimeLeaderboardApi.mockRejectedValueOnce(new Error('Network error'));

    render(<GamificationLeaderboardTab />);

    await waitFor(() => {
      expect(screen.getByText(tr('gamification:common.loadError'))).toBeInTheDocument();
    });

    gamificationApi.getRealtimeLeaderboardApi.mockResolvedValueOnce(mockRealtimeResponse);
    fireEvent.click(screen.getByRole('button', { name: tr('gamification:common.retry') }));

    await waitFor(() => {
      expect(screen.getByText('alice')).toBeInTheDocument();
    });
  });
});
