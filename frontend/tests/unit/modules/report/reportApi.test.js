import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  createReportApi,
  getMyReportsApi,
  getMyReportApi,
} from '@/modules/report/reportApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

describe('reportApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('createReportApi', () => {
    it('returns response.data with imageUrls', async () => {
      const mockResponse = { data: { success: true, data: { id: 1 } } };
      apiClient.post.mockResolvedValue(mockResponse);

      const input = {
        targetType: 'POST',
        targetId: '123',
        description: 'Nội dung vi phạm kéo dài hơn 10 ký tự',
        imageUrls: ['https://example.com/img1.png'],
      };

      const result = await createReportApi(input);

      expect(apiClient.post).toHaveBeenCalledWith('/reports', {
        targetType: 'POST',
        targetId: 123,
        description: 'Nội dung vi phạm kéo dài hơn 10 ký tự',
        imageUrls: ['https://example.com/img1.png'],
      });
      expect(result).toEqual(mockResponse.data);
    });

    it('pass imageUrls as undefined when not provided', async () => {
      apiClient.post.mockResolvedValue({ data: { success: true } });

      await createReportApi({
        targetType: 'COMMENT',
        targetId: 456,
        description: 'Spam comment liên tục trong bài học',
      });

      expect(apiClient.post).toHaveBeenCalledWith('/reports', {
        targetType: 'COMMENT',
        targetId: 456,
        description: 'Spam comment liên tục trong bài học',
        imageUrls: undefined,
      });
    });
  });

  describe('getMyReportsApi', () => {
    it('calls GET /reports with params', async () => {
      const mockData = {
        success: true,
        data: {
          content: [{ id: 1, targetType: 'LESSON', status: 'PENDING' }],
          totalPages: 1,
        },
      };
      apiClient.get.mockResolvedValue({ data: mockData });

      const params = { page: 1, size: 10 };
      const result = await getMyReportsApi(params);

      expect(apiClient.get).toHaveBeenCalledWith('/reports', { params });
      expect(result).toEqual(mockData);
    });
  });

  describe('getMyReportApi', () => {
    it('calls GET /reports/:reportId with reportId in path', async () => {
      const mockReport = {
        success: true,
        data: { id: 99, targetType: 'USER', description: 'User xúc phạm người khác' },
      };
      apiClient.get.mockResolvedValue({ data: mockReport });

      const result = await getMyReportApi(99);

      expect(apiClient.get).toHaveBeenCalledWith('/reports/99');
      expect(result).toEqual(mockReport);
    });
  });
});
