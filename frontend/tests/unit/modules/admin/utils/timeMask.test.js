import { describe, it, expect } from 'vitest';
import {
  formatMs,
  parseTime,
  maskTimeInput,
  clampMs,
} from '@/modules/admin/pages/LessonManagementPage/utils/timeMask';

describe('timeMask utilities', () => {
  describe('formatMs', () => {
    it('formats milliseconds to mm:ss', () => {
      expect(formatMs(0)).toBe('00:00');
      expect(formatMs(1000)).toBe('00:01');
      expect(formatMs(60000)).toBe('01:00');
      expect(formatMs(90000)).toBe('01:30');
      expect(formatMs(600000)).toBe('10:00');
    });

    it('handles minutes over 60', () => {
      expect(formatMs(4500000)).toBe('75:00'); // 75 minutes
      expect(formatMs(7200000)).toBe('120:00'); // 120 minutes
    });

    it('pads single digit seconds with zero', () => {
      expect(formatMs(5000)).toBe('00:05');
      expect(formatMs(65000)).toBe('01:05');
    });

    it('pads single digit minutes with zero', () => {
      expect(formatMs(300000)).toBe('05:00');
      expect(formatMs(540000)).toBe('09:00');
    });

    it('handles zero', () => {
      expect(formatMs(0)).toBe('00:00');
    });

    it('handles negative values as zero', () => {
      expect(formatMs(-1000)).toBe('00:00');
      expect(formatMs(-60000)).toBe('00:00');
    });

    it('handles null and undefined as zero', () => {
      expect(formatMs(null)).toBe('00:00');
      expect(formatMs(undefined)).toBe('00:00');
    });

    it('handles non-numeric input as zero', () => {
      expect(formatMs('invalid')).toBe('00:00');
    });

    it('rounds down to nearest second', () => {
      expect(formatMs(1500)).toBe('00:01'); // 1.5 seconds
      expect(formatMs(1999)).toBe('00:01'); // 1.999 seconds
    });

    it('handles large values', () => {
      expect(formatMs(36000000)).toBe('600:00'); // 10 hours
    });
  });

  describe('parseTime', () => {
    it('parses valid mm:ss format', () => {
      expect(parseTime('00:00')).toBe(0);
      expect(parseTime('00:30')).toBe(30000);
      expect(parseTime('01:00')).toBe(60000);
      expect(parseTime('01:30')).toBe(90000);
      expect(parseTime('10:00')).toBe(600000);
    });

    it('handles minutes with 1-3 digits', () => {
      expect(parseTime('5:30')).toBe(330000);
      expect(parseTime('75:00')).toBe(4500000);
      expect(parseTime('120:30')).toBe(7230000);
    });

    it('requires seconds to be 00-59', () => {
      expect(parseTime('01:59')).toBe(119000); // Valid
      expect(parseTime('01:60')).toBeNull(); // Invalid
      expect(parseTime('01:99')).toBeNull(); // Invalid
    });

    it('returns null for invalid format', () => {
      expect(parseTime('invalid')).toBeNull();
      expect(parseTime('1')).toBeNull();
      expect(parseTime('1:2')).toBeNull(); // Single digit seconds not padded
      expect(parseTime('1:2:3')).toBeNull();
      expect(parseTime('a:bc')).toBeNull();
    });

    it('returns null for empty or null input', () => {
      expect(parseTime('')).toBeNull();
      expect(parseTime(null)).toBeNull();
      expect(parseTime(undefined)).toBeNull();
    });

    it('trims whitespace', () => {
      expect(parseTime('  01:30  ')).toBe(90000);
      expect(parseTime(' 10:00 ')).toBe(600000);
    });

    it('requires colon separator', () => {
      expect(parseTime('0100')).toBeNull();
      expect(parseTime('01-00')).toBeNull();
    });

    it('handles zero values', () => {
      expect(parseTime('00:00')).toBe(0);
      expect(parseTime('0:00')).toBe(0);
    });

    it('validates seconds have exactly 2 digits', () => {
      expect(parseTime('01:5')).toBeNull(); // Single digit
      expect(parseTime('01:05')).toBe(65000); // Valid
      expect(parseTime('01:005')).toBeNull(); // Three digits
    });

    it('handles maximum valid seconds (59)', () => {
      expect(parseTime('00:59')).toBe(59000);
      expect(parseTime('10:59')).toBe(659000);
    });
  });

  describe('maskTimeInput', () => {
    it('keeps digits only', () => {
      expect(maskTimeInput('abc123')).toBe('1:23'); // 3 chữ số -> tự chèn ':'
      expect(maskTimeInput('ab')).toBe('');
      expect(maskTimeInput('12:34')).toBe('12:34');
    });

    it('inserts colon after first digits when more than 2 digits', () => {
      expect(maskTimeInput('123')).toBe('1:23');
      expect(maskTimeInput('1234')).toBe('12:34');
      expect(maskTimeInput('12345')).toBe('123:45');
    });

    it('does not insert colon for 1-2 digits', () => {
      expect(maskTimeInput('1')).toBe('1');
      expect(maskTimeInput('12')).toBe('12');
    });

    it('limits to 5 digits maximum', () => {
      expect(maskTimeInput('123456')).toBe('123:45');
      expect(maskTimeInput('1234567890')).toBe('123:45');
    });

    it('handles empty input', () => {
      expect(maskTimeInput('')).toBe('');
      expect(maskTimeInput(null)).toBe('');
      expect(maskTimeInput(undefined)).toBe('');
    });

    it('removes non-digit characters', () => {
      expect(maskTimeInput('1a2b3c')).toBe('1:23');
      expect(maskTimeInput('12-34')).toBe('12:34');
      expect(maskTimeInput('12.34')).toBe('12:34');
    });

    it('handles already formatted input', () => {
      expect(maskTimeInput('12:34')).toBe('12:34');
      expect(maskTimeInput('1:23')).toBe('1:23');
    });

    it('formats common user inputs correctly', () => {
      expect(maskTimeInput('130')).toBe('1:30');
      expect(maskTimeInput('105')).toBe('1:05');
      expect(maskTimeInput('1015')).toBe('10:15');
    });
  });

  describe('clampMs', () => {
    it('returns value within range', () => {
      expect(clampMs(5000, 10000)).toBe(5000);
      expect(clampMs(0, 10000)).toBe(0);
    });

    it('clamps negative values to 0', () => {
      expect(clampMs(-1000, 10000)).toBe(0);
      expect(clampMs(-5000, null)).toBe(0);
    });

    it('clamps values exceeding max to max', () => {
      expect(clampMs(15000, 10000)).toBe(10000);
      expect(clampMs(100000, 50000)).toBe(50000);
    });

    it('handles null maxMs as no upper limit', () => {
      expect(clampMs(5000, null)).toBe(5000);
      expect(clampMs(100000, null)).toBe(100000);
    });

    it('handles undefined maxMs as no upper limit', () => {
      expect(clampMs(5000, undefined)).toBe(5000);
      expect(clampMs(100000, undefined)).toBe(100000);
    });

    it('treats zero maxMs as no upper limit (maxMs falsy)', () => {
      // clampMs dùng `maxMs ? min(...) : v` nên maxMs = 0 coi như không giới hạn
      expect(clampMs(5000, 0)).toBe(5000);
      expect(clampMs(0, 0)).toBe(0);
      expect(clampMs(-100, 0)).toBe(0);
    });

    it('handles exact max value', () => {
      expect(clampMs(10000, 10000)).toBe(10000);
    });

    it('handles zero value', () => {
      expect(clampMs(0, 10000)).toBe(0);
      expect(clampMs(0, null)).toBe(0);
    });

    it('handles large values', () => {
      expect(clampMs(1000000, 500000)).toBe(500000);
      expect(clampMs(1000000, 2000000)).toBe(1000000);
    });

    it('handles fractional values', () => {
      expect(clampMs(5500.5, 10000)).toBe(5500.5);
      expect(clampMs(-0.5, 10000)).toBe(0);
    });

    it('handles edge case of max = 1', () => {
      expect(clampMs(0, 1)).toBe(0);
      expect(clampMs(1, 1)).toBe(1);
      expect(clampMs(2, 1)).toBe(1);
    });
  });

  describe('integration tests', () => {
    it('formats and parses consistently', () => {
      const originalMs = 90000; // 01:30
      const formatted = formatMs(originalMs);
      const parsed = parseTime(formatted);
      expect(parsed).toBe(originalMs);
    });

    it('masks and parses user input correctly', () => {
      const userInput = '130';
      const masked = maskTimeInput(userInput);
      const parsed = parseTime(masked);
      expect(masked).toBe('1:30');
      expect(parsed).toBe(90000);
    });

    it('handles complete workflow: input -> mask -> parse -> clamp -> format', () => {
      const userInput = '1234';
      const masked = maskTimeInput(userInput); // "12:34"
      const parsed = parseTime(masked); // 754000ms
      const clamped = clampMs(parsed, 600000); // Clamp to 10 minutes
      const formatted = formatMs(clamped); // "10:00"
      
      expect(masked).toBe('12:34');
      expect(parsed).toBe(754000);
      expect(clamped).toBe(600000);
      expect(formatted).toBe('10:00');
    });
  });
});
