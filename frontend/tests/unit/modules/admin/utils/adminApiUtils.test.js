import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  EMPTY_PAGE_RESPONSE,
  buildAdminQuery,
  fetchPagedAdminResource,
} from '@/modules/admin/utils/adminApiUtils';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('adminApiUtils', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('buildAdminQuery', () => {
    it('normalizes supported field types and pagination params', () => {
      const query = buildAdminQuery(
        {
          keyword: '  hello  ',
          planId: '12',
          isActive: 'true',
          isVerified: false,
          isPremium: true,
          tagIds: [1, 2],
          groupId: 0,
          createdFrom: '2026-11-01',
          unknownField: 'ignored',
          page: 1,
          size: 20,
          sort: 'createdAt,desc',
        },
        {
          keyword: 'string',
          planId: 'number',
          isActive: 'boolean',
          isVerified: 'boolean',
          isPremium: 'strictBoolean',
          tagIds: 'array',
          groupId: 'value',
          createdFrom: 'truthy',
          unknownField: 'unsupportedType',
        },
      );

      expect(query).toEqual({
        keyword: 'hello',
        planId: 12,
        isActive: true,
        isVerified: false,
        isPremium: true,
        tagIds: [1, 2],
        groupId: 0,
        createdFrom: '2026-11-01',
        page: 1,
        size: 20,
        sort: 'createdAt,desc',
      });
    });

    it('omits empty, null, undefined, and invalid values', () => {
      const query = buildAdminQuery(
        {
          keyword: '   ',
          planId: '',
          isActive: null,
          isPremium: 'true',
          tagIds: [],
          groupId: '',
          createdFrom: '',
          page: undefined,
          size: null,
          sort: '',
        },
        {
          keyword: 'string',
          planId: 'number',
          isActive: 'boolean',
          isPremium: 'strictBoolean',
          tagIds: 'array',
          groupId: 'value',
          createdFrom: 'truthy',
        },
      );

      expect(query).toEqual({});
    });
  });

  describe('fetchPagedAdminResource', () => {
    it('calls apiClient.get with built query and returns response data', async () => {
      const pageData = {
        content: [{ id: 1 }],
        page: 1,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      };
      apiClient.get.mockResolvedValueOnce({ data: { data: pageData } });

      const result = await fetchPagedAdminResource(
        '/admin/users',
        { keyword: '  alice  ', page: 1 },
        { keyword: 'string' },
      );

      expect(apiClient.get).toHaveBeenCalledWith('/admin/users', {
        params: { keyword: 'alice', page: 1 },
      });
      expect(result).toEqual(pageData);
    });

    it('returns EMPTY_PAGE_RESPONSE when api data is missing', async () => {
      apiClient.get.mockResolvedValueOnce({ data: {} });

      const result = await fetchPagedAdminResource('/admin/users');

      expect(result).toEqual(EMPTY_PAGE_RESPONSE);
    });
  });
});
