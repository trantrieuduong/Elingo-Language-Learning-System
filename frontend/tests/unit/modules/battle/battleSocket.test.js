import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  subscribeTopic,
  sendMessage,
  onSocketStatus,
} from '@/services/stompClient';
import { subscribeBattle, send, onStatus } from '@/modules/battle/battleSocket';

vi.mock('@/services/stompClient', () => ({
  subscribeTopic: vi.fn(),
  sendMessage: vi.fn(),
  onSocketStatus: vi.fn(),
}));

describe('battle/battleSocket.js', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('subscribeBattle subscribes to the user battle queue and returns the unsubscribe function', () => {
    const unsubscribe = vi.fn();
    subscribeTopic.mockReturnValue(unsubscribe);
    const handler = vi.fn();

    const result = subscribeBattle(handler);

    expect(subscribeTopic).toHaveBeenCalledWith('/user/queue/battle', handler);
    expect(result).toBe(unsubscribe);
  });

  it('send is the shared STOMP sendMessage', () => {
    expect(send).toBe(sendMessage);
  });

  it('onStatus is the shared STOMP onSocketStatus', () => {
    expect(onStatus).toBe(onSocketStatus);
  });
});
