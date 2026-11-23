import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  fetchAuditLogs,
  fetchAuditActions,
  fetchAuditTargetTypes,
} from '@/modules/admin/services/adminAuditApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('adminAuditApi service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('fetchAuditLogs', () => {
    it('calls GET /admin/audit-logs with normalized query parameters', async () => {
      const mockPageResponse = {
        content: [{ id: 1, action: 'USER_LOCK', adminId: 99 }],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockPageResponse } });

      const params = {
        adminId: 99,
        action: '  USER_LOCK  ',
        targetType: '  USER  ',
        targetId: '  123  ',
        from: '2026-11-01T00:00:00',
        to: '2026-11-20T23:59:59',
        page: 1,
        size: 20,
        sort: 'createdAt,desc',
      };

      const result = await fetchAuditLogs(params);

      expect(apiClient.get).toHaveBeenCalledWith('/admin/audit-logs', {
        params: {
          adminId: 99,
          action: 'USER_LOCK',
          targetType: 'USER',
          targetId: '123',
          from: '2026-11-01T00:00:00',
          to: '2026-11-20T23:59:59',
          page: 1,
          size: 20,
          sort: 'createdAt,desc',
        },
      });
      expect(result).toEqual(mockPageResponse);
    });

    it('calls GET /admin/audit-logs without extra params when empty object passed', async () => {
      const mockPageResponse = {
        content: [],
        page: 1,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        isLast: true,
      };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockPageResponse } });

      const result = await fetchAuditLogs();

      expect(apiClient.get).toHaveBeenCalledWith('/admin/audit-logs', {
        params: {},
      });
      expect(result).toEqual(mockPageResponse);
    });

    it('returns default fallback object when response data is undefined', async () => {
      apiClient.get.mockResolvedValueOnce({ data: {} });

      const result = await fetchAuditLogs();

      expect(result).toEqual({
        content: [],
        page: 1,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        isLast: true,
      });
    });
  });

  describe('fetchAuditActions', () => {
    it('calls GET /admin/audit-logs/actions and returns action array', async () => {
      const mockActions = ['USER_LOCK', 'SYSTEM_CONFIG_UPDATE', 'PLAN_CREATE'];
      apiClient.get.mockResolvedValueOnce({ data: { data: mockActions } });

      const result = await fetchAuditActions();

      expect(apiClient.get).toHaveBeenCalledWith('/admin/audit-logs/actions');
      expect(result).toEqual(mockActions);
    });

    it('returns empty array fallback when response is empty', async () => {
      apiClient.get.mockResolvedValueOnce({});

      const result = await fetchAuditActions();

      expect(result).toEqual([]);
    });
  });

  describe('fetchAuditTargetTypes', () => {
    it('calls GET /admin/audit-logs/target-types and returns target types array', async () => {
      const mockTargets = ['USER', 'DECK', 'SYSTEM_CONFIG'];
      apiClient.get.mockResolvedValueOnce({ data: { data: mockTargets } });

      const result = await fetchAuditTargetTypes();

      expect(apiClient.get).toHaveBeenCalledWith('/admin/audit-logs/target-types');
      expect(result).toEqual(mockTargets);
    });

    it('returns empty array fallback when response is empty', async () => {
      apiClient.get.mockResolvedValueOnce({});

      const result = await fetchAuditTargetTypes();

      expect(result).toEqual([]);
    });
  });
});
