import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useDebouncedValue } from '@/modules/speaking/hooks/useDebouncedValue';

describe('speaking/hooks/useDebouncedValue.js', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  const render = (initialProps) =>
    renderHook(({ value, delay }) => useDebouncedValue(value, delay), { initialProps });

  const advance = (ms) => {
    act(() => {
      vi.advanceTimersByTime(ms);
    });
  };

  it('returns the initial value immediately', () => {
    const { result } = render({ value: 'a', delay: 300 });

    expect(result.current).toBe('a');
  });

  it('updates the value only after the delay has elapsed', () => {
    const { result, rerender } = render({ value: 'a', delay: 300 });

    rerender({ value: 'b', delay: 300 });
    expect(result.current).toBe('a');

    advance(299);
    expect(result.current).toBe('a');

    advance(1);
    expect(result.current).toBe('b');
  });

  it('restarts the timer when the value changes again before the delay ends', () => {
    const { result, rerender } = render({ value: 'a', delay: 300 });

    rerender({ value: 'b', delay: 300 });
    advance(200);
    rerender({ value: 'c', delay: 300 });
    advance(200);

    expect(result.current).toBe('a');

    advance(100);
    expect(result.current).toBe('c');
  });

  it('uses a 400ms delay by default', () => {
    const { result, rerender } = render({ value: 'a' });

    rerender({ value: 'b' });
    advance(399);
    expect(result.current).toBe('a');

    advance(1);
    expect(result.current).toBe('b');
  });

  it('supports non-string values', () => {
    const { result, rerender } = render({ value: null, delay: 100 });

    rerender({ value: 3, delay: 100 });
    advance(100);

    expect(result.current).toBe(3);
  });

  it('clears the pending timer on unmount', () => {
    const { unmount, rerender } = render({ value: 'a', delay: 300 });

    rerender({ value: 'b', delay: 300 });
    expect(vi.getTimerCount()).toBe(1);

    unmount();

    expect(vi.getTimerCount()).toBe(0);
  });
});
