import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { tr } from '../../../../utils/i18n';
import GamificationLevelsTab from '@/modules/gamification/components/GamificationLevelsTab/GamificationLevelsTab';
import * as gamificationApi from '@/modules/gamification/gamificationApi';

vi.mock('@/modules/gamification/gamificationApi');

describe('GamificationLevelsTab', () => {
  const mockLevels = [
    { levelNo: 1, title: 'Novice', requiredXp: 0 },
    { levelNo: 2, title: 'Explorer', requiredXp: 100 },
    { levelNo: 3, title: 'Challenger', requiredXp: 300 },
  ];

  const mockProgress = {
    currentLevel: 2,
    currentTitle: 'Explorer',
    totalXp: 150,
    nextLevel: 3,
    nextLevelTitle: 'Challenger',
    nextLevelRequiredXp: 300,
    xpToNextLevel: 150,
    progressPercent: 25,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    gamificationApi.getAllLevelsApi.mockResolvedValue({ success: true, data: mockLevels });
    gamificationApi.getUserLevelProgressApi.mockResolvedValue({
      success: true,
      data: mockProgress,
    });
  });

  it('renders user progress banner and completed/current/locked level states', async () => {
    render(<GamificationLevelsTab userId={3} />);

    await waitFor(() => {
      expect(screen.getByText('Novice')).toBeInTheDocument();
    });

    expect(gamificationApi.getAllLevelsApi).toHaveBeenCalledTimes(1);
    expect(gamificationApi.getUserLevelProgressApi).toHaveBeenCalledWith(3);

    expect(screen.getByText(tr('gamification:levels.completedLevel'))).toBeInTheDocument();
    expect(screen.getByText(tr('gamification:levels.currentLevelBadge'))).toBeInTheDocument();
    expect(screen.getByText(tr('gamification:levels.lockedLevel'))).toBeInTheDocument();
    expect(
      screen.getByText(
        tr('gamification:overview.levelCard.nextLevel', {
          xp: 150,
          title: 'Challenger',
          level: 3,
        })
      )
    ).toBeInTheDocument();
  });

  it('renders maxLevel message in banner when user nextLevel is null', async () => {
    gamificationApi.getUserLevelProgressApi.mockResolvedValue({
      success: true,
      data: {
        currentLevel: 3,
        currentTitle: 'Challenger',
        totalXp: 500,
        nextLevel: null,
      },
    });

    render(<GamificationLevelsTab userId={3} />);

    await waitFor(() => {
      expect(screen.getByText(tr('gamification:overview.levelCard.maxLevel'))).toBeInTheDocument();
    });
  });

  it('renders emptyLevels state when levels list is empty', async () => {
    gamificationApi.getAllLevelsApi.mockResolvedValue({ success: true, data: [] });

    render(<GamificationLevelsTab userId={3} />);

    await waitFor(() => {
      expect(screen.getByText(tr('gamification:levels.emptyLevels'))).toBeInTheDocument();
    });
  });

  it('shows error EmptyState on failure and retries on click', async () => {
    gamificationApi.getAllLevelsApi.mockRejectedValueOnce(new Error('Server error'));

    render(<GamificationLevelsTab userId={3} />);

    await waitFor(() => {
      expect(screen.getByText(tr('gamification:common.loadError'))).toBeInTheDocument();
    });

    gamificationApi.getAllLevelsApi.mockResolvedValueOnce({ success: true, data: mockLevels });
    fireEvent.click(screen.getByRole('button', { name: tr('gamification:common.retry') }));

    await waitFor(() => {
      expect(screen.getByText('Novice')).toBeInTheDocument();
    });
  });
});
