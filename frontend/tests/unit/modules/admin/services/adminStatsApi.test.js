import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  fetchUserGrowth,
  fetchUserActivity,
  fetchUserBreakdown,
  fetchRevenueTimeSeries,
  fetchRevenueBreakdown,
  fetchSubscriptionStats,
  fetchLearningActivity,
  fetchDeckStats,
  fetchLessonStats,
  fetchSocialStats,
  fetchModerationStats,
  fetchReportStats,
} from '@/modules/admin/services/adminStatsApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('adminStatsApi service', () => {
  const sampleFilter = {
    from: '2026-10-01',
    to: '2026-10-31',
    granularity: 'DAY',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('User stats APIs', () => {
    it('fetchUserGrowth calls correct endpoint and filters query params', async () => {
      const mockData = { totalUsers: 1500, newUsers: [] };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const result = await fetchUserGrowth(sampleFilter);

      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/users/growth', {
        params: {
          from: '2026-10-01',
          to: '2026-10-31',
          granularity: 'DAY',
        },
      });
      expect(result).toEqual(mockData);
    });

    it('fetchUserActivity calls correct endpoint with partial params', async () => {
      const mockData = { dauTimeSeries: [] };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const result = await fetchUserActivity({ from: '2026-10-01' });

      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/users/activity', {
        params: {
          from: '2026-10-01',
        },
      });
      expect(result).toEqual(mockData);
    });

    it('fetchUserBreakdown calls endpoint without query params', async () => {
      const mockData = { levelDistribution: [] };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const result = await fetchUserBreakdown();

      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/users/breakdown');
      expect(result).toEqual(mockData);
    });
  });

  describe('Revenue stats APIs', () => {
    it('fetchRevenueTimeSeries calls correct endpoint with params', async () => {
      const mockData = { totalGrossRevenue: 10000000 };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const result = await fetchRevenueTimeSeries(sampleFilter);

      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/revenue/timeseries', {
        params: sampleFilter,
      });
      expect(result).toEqual(mockData);
    });

    it('fetchRevenueBreakdown calls correct endpoint with params', async () => {
      const mockData = { byPlan: [] };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const result = await fetchRevenueBreakdown(sampleFilter);

      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/revenue/breakdown', {
        params: sampleFilter,
      });
      expect(result).toEqual(mockData);
    });

    it('fetchSubscriptionStats calls endpoint without params', async () => {
      const mockData = { activeSubscriptions: 350 };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const result = await fetchSubscriptionStats();

      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/revenue/subscriptions');
      expect(result).toEqual(mockData);
    });
  });

  describe('Learning stats APIs', () => {
    it('fetchLearningActivity calls correct endpoint with params', async () => {
      const mockData = { totalCardReviews: 45000 };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const result = await fetchLearningActivity(sampleFilter);

      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/learning/activity', {
        params: sampleFilter,
      });
      expect(result).toEqual(mockData);
    });

    it('fetchDeckStats sends default limit 10 or specified limit', async () => {
      const mockData = { topDecks: [] };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const resDefault = await fetchDeckStats();
      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/learning/decks', {
        params: { limit: 10 },
      });
      expect(resDefault).toEqual(mockData);

      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });
      await fetchDeckStats(25);
      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/learning/decks', {
        params: { limit: 25 },
      });
    });

    it('fetchLessonStats sends default limit 10 or specified limit', async () => {
      const mockData = { topLessons: [] };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const result = await fetchLessonStats();
      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/learning/lessons', {
        params: { limit: 10 },
      });
      expect(result).toEqual(mockData);
    });
  });

  describe('Community stats APIs', () => {
    it('fetchSocialStats calls correct endpoint with params', async () => {
      const mockData = { totalPosts: 120, totalLikes: 450 };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const result = await fetchSocialStats(sampleFilter);

      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/community/social', {
        params: sampleFilter,
      });
      expect(result).toEqual(mockData);
    });

    it('fetchModerationStats calls correct endpoint with params', async () => {
      const mockData = { actions: { totalActions: 85 } };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const result = await fetchModerationStats(sampleFilter);

      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/community/moderation', {
        params: sampleFilter,
      });
      expect(result).toEqual(mockData);
    });

    it('fetchReportStats calls correct endpoint with params', async () => {
      const mockData = { pendingReports: 4, totalReports: 18 };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const result = await fetchReportStats(sampleFilter);

      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats/community/reports', {
        params: sampleFilter,
      });
      expect(result).toEqual(mockData);
    });
  });

  describe('Edge cases and error handling', () => {
    it('safely handles empty response payload', async () => {
      apiClient.get.mockResolvedValueOnce({ data: null });
      const result = await fetchUserGrowth();
      expect(result).toBeUndefined();
    });

    it('propagates API rejection error', async () => {
      apiClient.get.mockRejectedValueOnce(new Error('Network error'));
      await expect(fetchUserGrowth(sampleFilter)).rejects.toThrow('Network error');
    });
  });
});
