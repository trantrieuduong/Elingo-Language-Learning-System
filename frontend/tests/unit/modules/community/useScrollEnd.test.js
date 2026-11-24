import { describe, expect, it } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import useScrollEnd from '@/modules/community/hooks/useScrollEnd';

describe('useScrollEnd', () => {
  it('marks the container as at end when its remaining scroll distance is within one pixel', () => {
    const { result } = renderHook(() => useScrollEnd(false));
    const element = document.createElement('div');
    Object.defineProperties(element, {
      scrollHeight: { value: 300 },
      clientHeight: { value: 100 },
      scrollTop: { value: 199, writable: true },
    });
    result.current.scrollContainerRef.current = element;

    act(() => result.current.handleScroll());
    expect(result.current.isAtEnd).toBe(true);

    element.scrollTop = 150;
    act(() => result.current.handleScroll());
    expect(result.current.isAtEnd).toBe(false);
  });

  it('keeps the initial state when no scroll container is attached', () => {
    const { result } = renderHook(() => useScrollEnd(false));
    act(() => result.current.handleScroll());
    expect(result.current.isAtEnd).toBe(false);
  });
});
