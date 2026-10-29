import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import { patchShadowingProgressApi } from '@/modules/learning/shadowing/shadowingApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    patch: vi.fn(),
  },
}));

describe('shadowing/shadowingApi.js', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('patchShadowingProgressApi', () => {
    it('calls PATCH /lessons/{lessonId}/segments/{segmentId}/shadowing-progress with payload', async () => {
      apiClient.patch.mockResolvedValue({
        data: {
          score: 85,
          passed: true,
          segmentProgress: { shadowingBestScore: 85 },
          lessonProgress: {},
        },
      });

      const payload = {
        attemptCount: 2,
        score: 85,
        wordsAccuracy: [{ word: 'hello', accuracy: 100 }],
        audioUrl: 'https://storage.example.com/audio.wav',
      };

      const result = await patchShadowingProgressApi(1, 2, payload);

      expect(apiClient.patch).toHaveBeenCalledWith(
        '/lessons/1/segments/2/shadowing-progress',
        payload
      );
      expect(result.score).toBe(85);
      expect(result.passed).toBe(true);
    });

    it('encodes lessonId and segmentId in the URL path', async () => {
      apiClient.patch.mockResolvedValue({ data: { success: true } });

      await patchShadowingProgressApi('lesson/1', 'segment/2', {});

      expect(apiClient.patch).toHaveBeenCalledWith(
        '/lessons/lesson%2F1/segments/segment%2F2/shadowing-progress',
        {}
      );
    });
  });
});
