import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  getLessonsApi,
  getLessonDetailApi,
  getLessonSegmentsApi,
} from '@/modules/learning/lesson/lessonApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('lesson/lessonApi.js', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getLessonsApi', () => {
    it('calls GET /lessons with clean params', async () => {
      apiClient.get.mockResolvedValue({
        data: {
          content: [{ id: 1, title: 'Lesson 1' }],
          page: 1,
          size: 10,
          totalPages: 1,
        },
      });

      const params = {
        keyword: 'english',
        cefrLevelIds: [1, 2],
        tagIds: [5],
        page: 2,
      };

      const result = await getLessonsApi(params);

      expect(apiClient.get).toHaveBeenCalledWith('/lessons', {
        params: {
          keyword: 'english',
          cefrLevelIds: [1, 2],
          tagIds: [5],
          page: 2,
        },
      });
      expect(result.content).toHaveLength(1);
    });

    it('ignores empty keyword, empty array filters, and handles empty params', async () => {
      apiClient.get.mockResolvedValue({ data: { content: [] } });

      await getLessonsApi({
        keyword: '',
        cefrLevelIds: [],
        tagIds: null,
      });

      expect(apiClient.get).toHaveBeenCalledWith('/lessons', {
        params: {},
      });
    });
  });

  describe('getLessonDetailApi', () => {
    it('calls GET /lessons/slug/{slug} with encoded slug', async () => {
      apiClient.get.mockResolvedValue({
        data: { id: 10, slug: 'test-lesson', title: 'Test Lesson' },
      });

      const result = await getLessonDetailApi('test lesson/special');

      expect(apiClient.get).toHaveBeenCalledWith('/lessons/slug/test%20lesson%2Fspecial');
      expect(result.id).toBe(10);
    });
  });

  describe('getLessonSegmentsApi', () => {
    it('calls GET /lessons/slug/{slug}/segments with encoded slug', async () => {
      apiClient.get.mockResolvedValue({
        data: [{ id: 101, startMs: 0, endMs: 5000 }],
      });

      const result = await getLessonSegmentsApi('my-lesson');

      expect(apiClient.get).toHaveBeenCalledWith('/lessons/slug/my-lesson/segments');
      expect(result).toHaveLength(1);
    });
  });
});
