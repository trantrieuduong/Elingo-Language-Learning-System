import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  getBattleHistoryApi,
  getBattleDetailApi,
  getBattleStatsApi,
} from '@/modules/battle/battleApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('battle/battleApi.js', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getBattleHistoryApi', () => {
    it('calls GET /battles/history with default page and size', async () => {
      apiClient.get.mockResolvedValue({
        data: { success: true, data: { content: [], totalPages: 1 } },
      });

      const result = await getBattleHistoryApi();

      expect(apiClient.get).toHaveBeenCalledWith('/battles/history', {
        params: { page: 1, size: 10 },
      });
      expect(result).toEqual({ success: true, data: { content: [], totalPages: 1 } });
    });

    it('sends page and size when provided', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true } });

      await getBattleHistoryApi({ page: 3, size: 5 });

      expect(apiClient.get).toHaveBeenCalledWith('/battles/history', {
        params: { page: 3, size: 5 },
      });
    });

    it('sends result, mode and matchType filters when provided', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true } });

      await getBattleHistoryApi({
        page: 2,
        result: 'WIN',
        mode: 'TYPING',
        matchType: 'ROOM',
      });

      expect(apiClient.get).toHaveBeenCalledWith('/battles/history', {
        params: { page: 2, size: 10, result: 'WIN', mode: 'TYPING', matchType: 'ROOM' },
      });
    });

    it('omits filters that are undefined, null or empty', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true } });

      await getBattleHistoryApi({ result: undefined, mode: null, matchType: '' });

      const { params } = apiClient.get.mock.calls[0][1];
      expect(params).toEqual({ page: 1, size: 10 });
      expect(params).not.toHaveProperty('result');
      expect(params).not.toHaveProperty('mode');
      expect(params).not.toHaveProperty('matchType');
    });

    it('propagates errors from apiClient', async () => {
      apiClient.get.mockRejectedValue(new Error('Network error'));

      await expect(getBattleHistoryApi()).rejects.toThrow('Network error');
    });
  });

  describe('getBattleDetailApi', () => {
    it('calls GET /battles/{matchId} and returns response data', async () => {
      apiClient.get.mockResolvedValue({
        data: { success: true, data: { matchId: 100, mode: 'TYPING' } },
      });

      const result = await getBattleDetailApi(100);

      expect(apiClient.get).toHaveBeenCalledWith('/battles/100');
      expect(result.data.matchId).toBe(100);
    });

    it('propagates errors from apiClient', async () => {
      const error = { response: { status: 404, data: { success: false } } };
      apiClient.get.mockRejectedValue(error);

      await expect(getBattleDetailApi(999)).rejects.toBe(error);
    });
  });

  describe('getBattleStatsApi', () => {
    it('calls GET /battles/stats and returns response data', async () => {
      apiClient.get.mockResolvedValue({
        data: {
          success: true,
          data: { totalMatches: 10, wins: 6, losses: 3, draws: 1, winRate: 60 },
        },
      });

      const result = await getBattleStatsApi();

      expect(apiClient.get).toHaveBeenCalledWith('/battles/stats');
      expect(result.data.winRate).toBe(60);
    });

    it('propagates errors from apiClient', async () => {
      apiClient.get.mockRejectedValue(new Error('Server error'));

      await expect(getBattleStatsApi()).rejects.toThrow('Server error');
    });
  });
});
