import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import DisconnectBanner from '@/modules/battle/components/DisconnectBanner/DisconnectBanner';
import { tr } from '../../../../utils/i18n';

describe('DisconnectBanner component', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders as a status region with the remaining seconds', () => {
    render(<DisconnectBanner opponentName="Bob" graceMs={10000} at={Date.now()} />);

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(
      screen.getByText(tr('battle:play.opponentDisconnected', { name: 'Bob', seconds: 10 }))
    ).toBeInTheDocument();
  });

  it('counts down every second', () => {
    render(<DisconnectBanner opponentName="Bob" graceMs={10000} at={Date.now()} />);

    act(() => {
      vi.advanceTimersByTime(4000);
    });

    expect(
      screen.getByText(tr('battle:play.opponentDisconnected', { name: 'Bob', seconds: 6 }))
    ).toBeInTheDocument();
  });

  it('accounts for time already elapsed since the opponent disconnected', () => {
    render(<DisconnectBanner opponentName="Bob" graceMs={10000} at={Date.now() - 7000} />);

    expect(
      screen.getByText(tr('battle:play.opponentDisconnected', { name: 'Bob', seconds: 3 }))
    ).toBeInTheDocument();
  });

  it('shows the confirmation message when the grace period is over', () => {
    render(<DisconnectBanner opponentName="Bob" graceMs={3000} at={Date.now()} />);

    act(() => {
      vi.advanceTimersByTime(3500);
    });

    expect(screen.getByText(tr('battle:play.opponentDisconnectedConfirm'))).toBeInTheDocument();
  });

  it('shows the confirmation message when graceMs or at is missing', () => {
    const { rerender } = render(<DisconnectBanner opponentName="Bob" />);
    expect(screen.getByText(tr('battle:play.opponentDisconnectedConfirm'))).toBeInTheDocument();

    rerender(<DisconnectBanner opponentName="Bob" graceMs={10000} />);
    expect(screen.getByText(tr('battle:play.opponentDisconnectedConfirm'))).toBeInTheDocument();
  });

  it('falls back to the default opponent name', () => {
    render(<DisconnectBanner graceMs={10000} at={Date.now()} />);

    expect(
      screen.getByText(
        tr('battle:play.opponentDisconnected', {
          name: tr('battle:play.opponentFallback'),
          seconds: 10,
        })
      )
    ).toBeInTheDocument();
  });

  it('restarts the countdown when a new disconnect timestamp is received', () => {
    const { rerender } = render(<DisconnectBanner opponentName="Bob" graceMs={10000} at={Date.now()} />);

    act(() => {
      vi.advanceTimersByTime(8000);
    });
    expect(
      screen.getByText(tr('battle:play.opponentDisconnected', { name: 'Bob', seconds: 2 }))
    ).toBeInTheDocument();

    rerender(<DisconnectBanner opponentName="Bob" graceMs={10000} at={Date.now()} />);

    expect(
      screen.getByText(tr('battle:play.opponentDisconnected', { name: 'Bob', seconds: 10 }))
    ).toBeInTheDocument();
  });

  it('stops the timer on unmount', () => {
    const { unmount } = render(<DisconnectBanner opponentName="Bob" graceMs={10000} at={Date.now()} />);

    unmount();

    expect(vi.getTimerCount()).toBe(0);
  });
});
