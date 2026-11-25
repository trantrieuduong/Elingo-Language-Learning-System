import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import RoomClock from '@/modules/speaking/components/RoomClock/RoomClock';
import { LANGS, tr, useLang } from '../../../../utils/i18n';

describe('RoomClock component', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-01T00:00:00.000Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe.each(LANGS)('waiting state [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('shows the waiting text when the user has not joined yet', () => {
      render(<RoomClock joinedAtMs={null} />);

      expect(screen.getByText(tr('speaking:room.clock.waiting'))).toBeInTheDocument();
    });
  });

  it('does not start a timer while waiting', () => {
    render(<RoomClock joinedAtMs={null} />);

    expect(vi.getTimerCount()).toBe(0);
  });

  it('treats undefined like not joined', () => {
    render(<RoomClock />);

    expect(screen.getByText(tr('speaking:room.clock.waiting'))).toBeInTheDocument();
  });

  it('shows the elapsed time with an accessible label', () => {
    render(<RoomClock joinedAtMs={Date.now() - 65000} />);

    expect(screen.getByLabelText(tr('speaking:room.clock.label'))).toHaveTextContent('1:05');
  });

  it('ticks every second', () => {
    render(<RoomClock joinedAtMs={Date.now()} />);
    const clock = screen.getByLabelText(tr('speaking:room.clock.label'));
    expect(clock).toHaveTextContent('0:00');

    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(clock).toHaveTextContent('0:01');

    act(() => {
      vi.advanceTimersByTime(59000);
    });
    expect(clock).toHaveTextContent('1:00');
  });

  it('switches from waiting to counting when joinedAtMs becomes available', () => {
    const { rerender } = render(<RoomClock joinedAtMs={null} />);

    rerender(<RoomClock joinedAtMs={Date.now() - 3000} />);

    expect(screen.getByLabelText(tr('speaking:room.clock.label'))).toHaveTextContent('0:03');
    expect(vi.getTimerCount()).toBe(1);
  });

  it('clears its interval on unmount', () => {
    const { unmount } = render(<RoomClock joinedAtMs={Date.now()} />);
    expect(vi.getTimerCount()).toBe(1);

    unmount();

    expect(vi.getTimerCount()).toBe(0);
  });
});
