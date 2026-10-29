import { describe, it, expect } from 'vitest';
import { getYouTubeVideoId, getYouTubeThumbnail } from '@/modules/learning/lesson/utils/youtube';

describe('lesson/utils/youtube.js', () => {
  describe('getYouTubeVideoId', () => {
    it('returns empty string if url is invalid or not a string', () => {
      expect(getYouTubeVideoId(null)).toBe('');
      expect(getYouTubeVideoId(undefined)).toBe('');
      expect(getYouTubeVideoId('')).toBe('');
      expect(getYouTubeVideoId(12345)).toBe('');
      expect(getYouTubeVideoId('https://example.com/video')).toBe('');
    });

    it('extracts 11-char video ID from standard watch URL', () => {
      expect(getYouTubeVideoId('https://www.youtube.com/watch?v=dQw4w9WgXcQ')).toBe('dQw4w9WgXcQ');
      expect(getYouTubeVideoId('http://youtube.com/watch?v=dQw4w9WgXcQ&t=10s')).toBe('dQw4w9WgXcQ');
      expect(getYouTubeVideoId('https://youtube.com/watch?feature=shared&v=dQw4w9WgXcQ')).toBe(
        'dQw4w9WgXcQ'
      );
    });

    it('extracts ID from mobile youtube URL', () => {
      expect(getYouTubeVideoId('https://m.youtube.com/watch?v=dQw4w9WgXcQ')).toBe('dQw4w9WgXcQ');
      expect(getYouTubeVideoId('https://m.youtube.com/watch?app=desktop&v=dQw4w9WgXcQ')).toBe(
        'dQw4w9WgXcQ'
      );
    });

    it('extracts ID from youtu.be short URL', () => {
      expect(getYouTubeVideoId('https://youtu.be/dQw4w9WgXcQ')).toBe('dQw4w9WgXcQ');
      expect(getYouTubeVideoId('https://youtu.be/dQw4w9WgXcQ?t=20')).toBe('dQw4w9WgXcQ');
    });

    it('extracts ID from embed URL', () => {
      expect(getYouTubeVideoId('https://www.youtube.com/embed/dQw4w9WgXcQ')).toBe('dQw4w9WgXcQ');
    });

    it('extracts ID from shorts URL', () => {
      expect(getYouTubeVideoId('https://www.youtube.com/shorts/dQw4w9WgXcQ')).toBe('dQw4w9WgXcQ');
    });
  });

  describe('getYouTubeThumbnail', () => {
    it('returns empty string if url is invalid or no video ID found', () => {
      expect(getYouTubeThumbnail(null)).toBe('');
      expect(getYouTubeThumbnail('')).toBe('');
      expect(getYouTubeThumbnail('https://othersite.com')).toBe('');
    });

    it('returns hqdefault thumbnail URL for valid video URL', () => {
      expect(getYouTubeThumbnail('https://www.youtube.com/watch?v=dQw4w9WgXcQ')).toBe(
        'https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg'
      );
    });
  });
});
