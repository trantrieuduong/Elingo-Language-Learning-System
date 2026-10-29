import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import { patchDictationProgressApi } from '@/modules/learning/dictation/dictationApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    patch: vi.fn(),
  },
}));

describe('dictation/dictationApi.js', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('patchDictationProgressApi', () => {
    it('calls PATCH /lessons/{lessonId}/segments/{segmentId}/dictation-progress with data', async () => {
      apiClient.patch.mockResolvedValue({
        data: {
          score: 80,
          passed: true,
          segmentProgress: { dictationBestScore: 80 },
          lessonProgress: {},
        },
      });

      const result = await patchDictationProgressApi(12, 34, {
        attemptCount: 2,
        hintUsedCount: 1,
      });

      expect(apiClient.patch).toHaveBeenCalledWith(
        '/lessons/12/segments/34/dictation-progress',
        { attemptCount: 2, hintUsedCount: 1 }
      );
      expect(result.score).toBe(80);
      expect(result.passed).toBe(true);
    });

    it('encodes lessonId and segmentId in the URL', async () => {
      apiClient.patch.mockResolvedValue({ data: { success: true } });

      await patchDictationProgressApi('lesson/1', 'seg/2', {
        attemptCount: 1,
        hintUsedCount: 0,
      });

      expect(apiClient.patch).toHaveBeenCalledWith(
        '/lessons/lesson%2F1/segments/seg%2F2/dictation-progress',
        { attemptCount: 1, hintUsedCount: 0 }
      );
    });
  });
});
