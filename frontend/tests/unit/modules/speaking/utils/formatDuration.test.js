import { describe, it, expect } from 'vitest';
import { formatDuration } from '@/modules/speaking/utils/formatDuration';

describe('speaking/utils/formatDuration.js', () => {
  it.each([
    [0, '0:00'],
    [5, '0:05'],
    [59, '0:59'],
    [60, '1:00'],
    [65, '1:05'],
    [600, '10:00'],
    [3599, '59:59'],
  ])('formats %s seconds below one hour as %s', (seconds, expected) => {
    expect(formatDuration(seconds)).toBe(expected);
  });

  it.each([
    [3600, '1:00:00'],
    [3661, '1:01:01'],
    [7325, '2:02:05'],
    [36000, '10:00:00'],
  ])('formats %s seconds from one hour as %s', (seconds, expected) => {
    expect(formatDuration(seconds)).toBe(expected);
  });

  it('rounds fractional seconds down', () => {
    expect(formatDuration(61.9)).toBe('1:01');
    expect(formatDuration(0.99)).toBe('0:00');
  });

  it('accepts numeric strings', () => {
    expect(formatDuration('125')).toBe('2:05');
  });

  it('clamps negative values to zero', () => {
    expect(formatDuration(-5)).toBe('0:00');
  });

  it.each([[undefined], [null], [NaN], ['abc'], [{}]])(
    'falls back to 0:00 for the invalid value %j',
    (value) => {
      expect(formatDuration(value)).toBe('0:00');
    }
  );
});
