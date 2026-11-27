import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

const YOUTUBE_MODULE = '@/modules/admin/pages/LessonManagementPage/utils/youtube';

// Module có biến cache `apiPromise` ở cấp module -> mỗi test phải import module mới
// để state không rò rỉ giữa các test.
const loadFreshModule = async () => {
  vi.resetModules();
  return import(YOUTUBE_MODULE);
};

describe('youtube utilities', () => {
  describe('extractVideoId', () => {
    let extractVideoId;

    beforeEach(async () => {
      ({ extractVideoId } = await loadFreshModule());
    });

    it('extracts video ID from standard watch URL', () => {
      const url = 'https://www.youtube.com/watch?v=abc123defgh';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('extracts video ID from shortened youtu.be URL', () => {
      const url = 'https://youtu.be/abc123defgh';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('extracts video ID from embed URL', () => {
      const url = 'https://www.youtube.com/embed/abc123defgh';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('extracts video ID from shorts URL', () => {
      const url = 'https://www.youtube.com/shorts/abc123defgh';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('extracts video ID from mobile URL', () => {
      const url = 'https://m.youtube.com/watch?v=abc123defgh';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('extracts video ID from URL without protocol', () => {
      const url = 'youtube.com/watch?v=abc123defgh';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('extracts video ID from URL with additional parameters', () => {
      const url = 'https://www.youtube.com/watch?v=abc123defgh&t=30s&list=PLtest';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('extracts video ID when v= is not the first query param', () => {
      const url = 'https://www.youtube.com/watch?feature=share&v=abc123defgh';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('extracts video ID from URL with hash fragment', () => {
      const url = 'https://www.youtube.com/watch?v=abc123defgh#t=30';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('handles URL with extra query params after the ID', () => {
      const url = 'https://www.youtube.com/watch?v=abc123defgh&feature=share';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('returns null for invalid URL', () => {
      const url = 'https://www.example.com/video';
      expect(extractVideoId(url)).toBeNull();
    });

    it('returns null for empty string', () => {
      expect(extractVideoId('')).toBeNull();
    });

    it('returns null for null input', () => {
      expect(extractVideoId(null)).toBeNull();
    });

    it('returns null for undefined input', () => {
      expect(extractVideoId(undefined)).toBeNull();
    });

    it('returns null for URL without video ID', () => {
      const url = 'https://www.youtube.com/watch';
      expect(extractVideoId(url)).toBeNull();
    });

    it('returns null for video ID with invalid length', () => {
      const url = 'https://www.youtube.com/watch?v=abc'; // Too short
      expect(extractVideoId(url)).toBeNull();
    });

    it('handles video ID with hyphens and underscores', () => {
      const url = 'https://www.youtube.com/watch?v=abc-123_XYZ';
      expect(extractVideoId(url)).toBe('abc-123_XYZ');
    });

    it('trims whitespace from input', () => {
      const url = '  https://www.youtube.com/watch?v=abc123defgh  ';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('handles watch URL without www subdomain', () => {
      const url = 'https://youtube.com/watch?v=abc123defgh';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('handles HTTP protocol', () => {
      const url = 'http://www.youtube.com/watch?v=abc123defgh';
      expect(extractVideoId(url)).toBe('abc123defgh');
    });

    it('validates video ID is exactly 11 characters', () => {
      const urlTooLong = 'https://www.youtube.com/watch?v=abc123defgh12';
      expect(extractVideoId(urlTooLong)).toBeNull();

      const urlTooShort = 'https://www.youtube.com/watch?v=abc123def';
      expect(extractVideoId(urlTooShort)).toBeNull();
    });
  });

  describe('loadYouTubeIframeApi', () => {
    // Giả lập YouTube gọi callback toàn cục khi API sẵn sàng
    const fireApiReady = () => {
      window.YT = { Player: vi.fn() };
      window.onYouTubeIframeAPIReady?.();
    };

    beforeEach(() => {
      delete window.YT;
      delete window.onYouTubeIframeAPIReady;
      document.head.innerHTML = '';
    });

    afterEach(() => {
      // Khôi phục mọi vi.spyOn (createElement, appendChild...) để không ảnh hưởng test khác
      vi.restoreAllMocks();
      delete window.YT;
      delete window.onYouTubeIframeAPIReady;
      document.head.innerHTML = '';
    });

    it('returns existing YT object if already loaded', async () => {
      const { loadYouTubeIframeApi } = await loadFreshModule();
      window.YT = { Player: vi.fn() };

      const result = await loadYouTubeIframeApi();

      expect(result).toBe(window.YT);
    });

    it('loads YouTube IFrame API script', async () => {
      const { loadYouTubeIframeApi } = await loadFreshModule();
      const mockScript = { src: '', async: false, onerror: null };

      const createElementSpy = vi.spyOn(document, 'createElement').mockReturnValue(mockScript);
      const appendChildSpy = vi.spyOn(document.head, 'appendChild').mockImplementation(() => {});

      const promise = loadYouTubeIframeApi();
      setTimeout(fireApiReady, 0);
      await promise;

      expect(createElementSpy).toHaveBeenCalledWith('script');
      expect(mockScript.src).toBe('https://www.youtube.com/iframe_api');
      expect(mockScript.async).toBe(true);
      expect(appendChildSpy).toHaveBeenCalledWith(mockScript);
    });

    it('reuses existing API load promise', async () => {
      const { loadYouTubeIframeApi } = await loadFreshModule();

      const promise1 = loadYouTubeIframeApi();
      const promise2 = loadYouTubeIframeApi();

      expect(promise1).toBe(promise2);
    });

    it('does not create duplicate script tags', async () => {
      const { loadYouTubeIframeApi } = await loadFreshModule();

      // Script đã tồn tại sẵn trong <head>
      const existingScript = document.createElement('script');
      existingScript.src = 'https://www.youtube.com/iframe_api';
      document.head.appendChild(existingScript);

      const createElementSpy = vi.spyOn(document, 'createElement');

      const promise = loadYouTubeIframeApi();
      setTimeout(fireApiReady, 0);
      await promise;

      expect(createElementSpy).not.toHaveBeenCalledWith('script');
      expect(document.querySelectorAll('script[src*="youtube.com/iframe_api"]')).toHaveLength(1);
    });

    it('handles script load errors', async () => {
      const { loadYouTubeIframeApi } = await loadFreshModule();
      const mockScript = { src: '', async: false, onerror: null };

      vi.spyOn(document, 'createElement').mockReturnValue(mockScript);
      vi.spyOn(document.head, 'appendChild').mockImplementation(() => {
        // Giả lập script tải lỗi
        setTimeout(() => mockScript.onerror?.(), 0);
      });

      await expect(loadYouTubeIframeApi()).rejects.toThrow('YT_API_LOAD_FAILED');
    });

    it('allows retry after a script load error', async () => {
      const { loadYouTubeIframeApi } = await loadFreshModule();
      const mockScript = { src: '', async: false, onerror: null };

      vi.spyOn(document, 'createElement').mockReturnValue(mockScript);
      const appendChildSpy = vi
        .spyOn(document.head, 'appendChild')
        .mockImplementationOnce(() => {
          setTimeout(() => mockScript.onerror?.(), 0);
        })
        .mockImplementation(() => {});

      await expect(loadYouTubeIframeApi()).rejects.toThrow('YT_API_LOAD_FAILED');

      // apiPromise đã bị reset -> lần gọi sau phải tạo script mới
      const retry = loadYouTubeIframeApi();
      setTimeout(fireApiReady, 0);
      const result = await retry;
      expect(result).toBe(window.YT);
      expect(appendChildSpy).toHaveBeenCalledTimes(2);
    });

    it('preserves previous onYouTubeIframeAPIReady callback', async () => {
      const { loadYouTubeIframeApi } = await loadFreshModule();
      const previousCallback = vi.fn();
      window.onYouTubeIframeAPIReady = previousCallback;

      const promise = loadYouTubeIframeApi();
      setTimeout(fireApiReady, 0);
      await promise;

      expect(previousCallback).toHaveBeenCalledTimes(1);
    });

    it('resolves with YT object when API is ready', async () => {
      const { loadYouTubeIframeApi } = await loadFreshModule();

      const promise = loadYouTubeIframeApi();
      setTimeout(fireApiReady, 0);
      const result = await promise;

      expect(result).toBe(window.YT);
      expect(result.Player).toBeDefined();
    });

    it('handles multiple simultaneous load attempts', async () => {
      const { loadYouTubeIframeApi } = await loadFreshModule();

      const promise1 = loadYouTubeIframeApi();
      const promise2 = loadYouTubeIframeApi();
      const promise3 = loadYouTubeIframeApi();

      setTimeout(fireApiReady, 0);

      const results = await Promise.all([promise1, promise2, promise3]);

      expect(results[0]).toBe(results[1]);
      expect(results[1]).toBe(results[2]);
      // Chỉ chèn đúng 1 thẻ script dù gọi nhiều lần
      expect(document.querySelectorAll('script[src*="youtube.com/iframe_api"]')).toHaveLength(1);
    });
  });
});
