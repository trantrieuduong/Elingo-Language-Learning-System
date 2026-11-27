import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  getMyBadgesApi,
  getUserBadgesApi,
  getMyLevelProgressApi,
  getMyRankApi,
  getUserLevelProgressApi,
  getMyStreakApi,
  getUserStreakApi,
  getAllLevelsApi,
  getRealtimeLeaderboardApi,
  getHistoricalLeaderboardApi,
} from '@/modules/gamification/gamificationApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('gamificationApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getMyBadgesApi calls GET /users/me/badges and returns response.data', async () => {
    const mockResponse = { data: { success: true, data: [{ id: 1 }] } };
    apiClient.get.mockResolvedValue(mockResponse);

    const result = await getMyBadgesApi();

    expect(apiClient.get).toHaveBeenCalledWith('/users/me/badges');
    expect(result).toEqual(mockResponse.data);
  });

  it('getUserBadgesApi calls GET /users/:userId/badges and returns response.data', async () => {
    const mockResponse = { data: { success: true, data: [{ id: 2 }] } };
    apiClient.get.mockResolvedValue(mockResponse);

    const result = await getUserBadgesApi(42);

    expect(apiClient.get).toHaveBeenCalledWith('/users/42/badges');
    expect(result).toEqual(mockResponse.data);
  });

  it('getMyLevelProgressApi calls GET /users/me/levels and returns response.data', async () => {
    const mockResponse = { data: { success: true, data: { currentLevel: 3 } } };
    apiClient.get.mockResolvedValue(mockResponse);

    const result = await getMyLevelProgressApi();

    expect(apiClient.get).toHaveBeenCalledWith('/users/me/levels');
    expect(result).toEqual(mockResponse.data);
  });

  it('getMyRankApi calls GET /users/me/rank with default WEEKLY periodType', async () => {
    const mockResponse = { data: { success: true, data: { rank: 5 } } };
    apiClient.get.mockResolvedValue(mockResponse);

    const result = await getMyRankApi();

    expect(apiClient.get).toHaveBeenCalledWith('/users/me/rank', {
      params: { periodType: 'WEEKLY' },
    });
    expect(result).toEqual(mockResponse.data);
  });

  it('getMyRankApi forwards custom periodType', async () => {
    const mockResponse = { data: { success: true, data: { rank: 2 } } };
    apiClient.get.mockResolvedValue(mockResponse);

    const result = await getMyRankApi('MONTHLY');

    expect(apiClient.get).toHaveBeenCalledWith('/users/me/rank', {
      params: { periodType: 'MONTHLY' },
    });
    expect(result).toEqual(mockResponse.data);
  });

  it('getUserLevelProgressApi calls GET /users/:userId/levels and returns response.data', async () => {
    const mockResponse = { data: { success: true, data: { currentLevel: 4 } } };
    apiClient.get.mockResolvedValue(mockResponse);

    const result = await getUserLevelProgressApi(7);

    expect(apiClient.get).toHaveBeenCalledWith('/users/7/levels');
    expect(result).toEqual(mockResponse.data);
  });

  it('getMyStreakApi calls GET /users/me/streaks with params', async () => {
    const mockResponse = { data: { success: true, data: { currentStreak: 3 } } };
    apiClient.get.mockResolvedValue(mockResponse);

    const defaultRes = await getMyStreakApi();
    expect(apiClient.get).toHaveBeenCalledWith('/users/me/streaks', { params: {} });
    expect(defaultRes).toEqual(mockResponse.data);

    const params = { year: 2026, month: 11 };
    const customRes = await getMyStreakApi(params);
    expect(apiClient.get).toHaveBeenCalledWith('/users/me/streaks', { params });
    expect(customRes).toEqual(mockResponse.data);
  });

  it('getUserStreakApi calls GET /users/:userId/streaks with params', async () => {
    const mockResponse = { data: { success: true, data: { currentStreak: 10 } } };
    apiClient.get.mockResolvedValue(mockResponse);

    const defaultRes = await getUserStreakApi(9);
    expect(apiClient.get).toHaveBeenCalledWith('/users/9/streaks', { params: {} });
    expect(defaultRes).toEqual(mockResponse.data);

    const params = { year: 2026, month: 5 };
    const customRes = await getUserStreakApi(9, params);
    expect(apiClient.get).toHaveBeenCalledWith('/users/9/streaks', { params });
    expect(customRes).toEqual(mockResponse.data);
  });

  it('getAllLevelsApi calls GET /gamification/levels and returns response.data', async () => {
    const mockResponse = { data: { success: true, data: [{ levelNo: 1 }] } };
    apiClient.get.mockResolvedValue(mockResponse);

    const result = await getAllLevelsApi();

    expect(apiClient.get).toHaveBeenCalledWith('/gamification/levels');
    expect(result).toEqual(mockResponse.data);
  });

  it('getRealtimeLeaderboardApi calls GET /gamification/leaderboards with default and custom params', async () => {
    const mockResponse = { data: { success: true, data: { leaderboard: { content: [] } } } };
    apiClient.get.mockResolvedValue(mockResponse);

    const defaultRes = await getRealtimeLeaderboardApi();
    expect(apiClient.get).toHaveBeenCalledWith('/gamification/leaderboards', {
      params: { periodType: 'WEEKLY', page: 1, size: 20 },
    });
    expect(defaultRes).toEqual(mockResponse.data);

    const customRes = await getRealtimeLeaderboardApi({
      periodType: 'MONTHLY',
      page: 2,
      size: 10,
    });
    expect(apiClient.get).toHaveBeenCalledWith('/gamification/leaderboards', {
      params: { periodType: 'MONTHLY', page: 2, size: 10 },
    });
    expect(customRes).toEqual(mockResponse.data);
  });

  it('getHistoricalLeaderboardApi calls GET /gamification/leaderboards/history with params', async () => {
    const mockResponse = { data: { success: true, data: { content: [] } } };
    apiClient.get.mockResolvedValue(mockResponse);

    const result = await getHistoricalLeaderboardApi({
      periodType: 'WEEKLY',
      periodKey: '2026-W45',
      page: 3,
      size: 15,
    });

    expect(apiClient.get).toHaveBeenCalledWith('/gamification/leaderboards/history', {
      params: { periodType: 'WEEKLY', periodKey: '2026-W45', page: 3, size: 15 },
    });
    expect(result).toEqual(mockResponse.data);
  });
});
