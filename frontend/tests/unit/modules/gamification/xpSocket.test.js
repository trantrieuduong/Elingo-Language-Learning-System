import { describe, it, expect, vi, beforeEach } from 'vitest';
import { subscribeTopic } from '@/services/stompClient';
import { subscribeXp } from '@/modules/gamification/xpSocket';

vi.mock('@/services/stompClient', () => ({
  subscribeTopic: vi.fn(),
}));

describe('xpSocket', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('subscribeXp subscribes to /user/queue/xp-event and returns unsubscribe function', () => {
    const unsubscribe = vi.fn();
    const handler = vi.fn();
    subscribeTopic.mockReturnValue(unsubscribe);

    const result = subscribeXp(handler);

    expect(subscribeTopic).toHaveBeenCalledWith('/user/queue/xp-event', handler);
    expect(result).toBe(unsubscribe);
  });
});
