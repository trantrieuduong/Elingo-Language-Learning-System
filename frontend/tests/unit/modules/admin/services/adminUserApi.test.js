import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  searchUsers,
  getUserById,
  lockUser,
  unlockUser,
} from '@/modules/admin/services/adminUserApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  },
}));

describe('adminUserApi service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('searchUsers', () => {
    it('calls GET /admin/users with normalized query parameters', async () => {
      const mockResponse = {
        content: [{ id: 1, username: 'admin1', email: 'admin@el.io', role: 'ADMIN' }],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockResponse } });

      const params = {
        keyword: '  johndoe  ',
        role: '  USER  ',
        isVerified: 'true',
        isActive: 'true',
        createdFrom: '2026-11-01',
        createdTo: '2026-11-20',
        page: 2,
        size: 10,
        sort: 'createdAt,desc',
      };

      const result = await searchUsers(params);

      expect(apiClient.get).toHaveBeenCalledWith('/admin/users', {
        params: {
          keyword: 'johndoe',
          role: 'USER',
          isVerified: true,
          isActive: true,
          createdFrom: '2026-11-01',
          createdTo: '2026-11-20',
          page: 2,
          size: 10,
          sort: 'createdAt,desc',
        },
      });
      expect(result).toEqual(mockResponse);
    });

    it('handles empty query parameters gracefully', async () => {
      const mockResponse = {
        content: [],
        page: 1,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        isLast: true,
      };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockResponse } });

      const result = await searchUsers();

      expect(apiClient.get).toHaveBeenCalledWith('/admin/users', {
        params: {},
      });
      expect(result).toEqual(mockResponse);
    });

    it('returns fallback default object when response data is missing', async () => {
      apiClient.get.mockResolvedValueOnce({ data: {} });

      const result = await searchUsers({});

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

  describe('getUserById', () => {
    it('calls GET /admin/users/:id and returns user details', async () => {
      const mockUser = {
        id: 42,
        username: 'learner42',
        email: 'learner42@el.io',
        role: 'USER',
      };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockUser } });

      const result = await getUserById(42);

      expect(apiClient.get).toHaveBeenCalledWith('/admin/users/42');
      expect(result).toEqual(mockUser);
    });
  });

  describe('lockUser', () => {
    it('calls PUT /admin/users/:id/lock with trimmed reason', async () => {
      const mockLockedUser = {
        id: 10,
        username: 'spammer',
        isActive: false,
      };
      apiClient.put.mockResolvedValueOnce({ data: { data: mockLockedUser } });

      const result = await lockUser(10, '  Spamming messages in community  ');

      expect(apiClient.put).toHaveBeenCalledWith('/admin/users/10/lock', {
        reason: 'Spamming messages in community',
      });
      expect(result).toEqual(mockLockedUser);
    });

    it('sends empty string if reason is not provided', async () => {
      apiClient.put.mockResolvedValueOnce({ data: { data: { id: 10, isActive: false } } });

      await lockUser(10);

      expect(apiClient.put).toHaveBeenCalledWith('/admin/users/10/lock', {
        reason: '',
      });
    });
  });

  describe('unlockUser', () => {
    it('calls PUT /admin/users/:id/unlock without payload', async () => {
      const mockUnlockedUser = {
        id: 10,
        username: 'unlockedUser',
        isActive: true,
      };
      apiClient.put.mockResolvedValueOnce({ data: { data: mockUnlockedUser } });

      const result = await unlockUser(10);

      expect(apiClient.put).toHaveBeenCalledWith('/admin/users/10/unlock');
      expect(result).toEqual(mockUnlockedUser);
    });
  });
});
