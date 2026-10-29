import { describe, it, expect } from 'vitest';
import { formatDuration } from '@/modules/learning/lesson/utils/format';

describe('lesson/utils/format.js', () => {
  describe('formatDuration', () => {
    it('returns empty string for null, undefined, non-number, NaN or <= 0', () => {
      expect(formatDuration(null)).toBe('');
      expect(formatDuration(undefined)).toBe('');
      expect(formatDuration('1000')).toBe('');
      expect(formatDuration(NaN)).toBe('');
      expect(formatDuration(0)).toBe('');
      expect(formatDuration(-500)).toBe('');
    });

    it('formats duration in mm:ss format when less than 1 hour', () => {
      expect(formatDuration(1000)).toBe('00:01');
      expect(formatDuration(59000)).toBe('00:59');
      expect(formatDuration(60000)).toBe('01:00');
      expect(formatDuration(150000)).toBe('02:30');
      expect(formatDuration(3599000)).toBe('59:59');
    });

    it('formats duration in hh:mm:ss format when 1 hour or more', () => {
      expect(formatDuration(3600000)).toBe('01:00:00');
      expect(formatDuration(4500000)).toBe('01:15:00');
      expect(formatDuration(3661000)).toBe('01:01:01');
      expect(formatDuration(72000000)).toBe('20:00:00');
    });
  });
});
