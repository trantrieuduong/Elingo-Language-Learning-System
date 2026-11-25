import { describe, it, expect, vi, beforeEach } from 'vitest';
import { subscribeTopic, sendMessage, onSocketStatus } from '@/services/stompClient';
import {
  subscribeSpeaking,
  onStatus,
  sendCreate,
  sendJoin,
  sendLeave,
  sendActiveRoom,
  sendSignal,
  sendMic,
  sendMute,
  sendTransferHost,
  sendEnd,
  sendKick,
  sendUnban,
  sendChat,
} from '@/modules/speaking/speakingSocket';

vi.mock('@/services/stompClient', () => ({
  subscribeTopic: vi.fn(),
  sendMessage: vi.fn(),
  onSocketStatus: vi.fn(),
}));

describe('speaking/speakingSocket.js', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sendMessage.mockReturnValue(true);
  });

  describe('subscription', () => {
    it('subscribeSpeaking subscribes to the user speaking queue and returns the unsubscribe function', () => {
      const unsubscribe = vi.fn();
      subscribeTopic.mockReturnValue(unsubscribe);
      const handler = vi.fn();

      const result = subscribeSpeaking(handler);

      expect(subscribeTopic).toHaveBeenCalledWith('/user/queue/speaking', handler);
      expect(result).toBe(unsubscribe);
    });

    it('onStatus is the shared STOMP onSocketStatus', () => {
      expect(onStatus).toBe(onSocketStatus);
    });
  });

  describe('sendCreate', () => {
    it('sends to /app/speaking/create with capacity and privacy', () => {
      sendCreate({ maxParticipants: 4, isPrivate: true });

      expect(sendMessage).toHaveBeenCalledWith('/app/speaking/create', {
        maxParticipants: 4,
        isPrivate: true,
      });
    });

    it('includes the trimmed topic when provided', () => {
      sendCreate({ topic: '  Travel plans  ', maxParticipants: 2, isPrivate: false });

      expect(sendMessage).toHaveBeenCalledWith('/app/speaking/create', {
        maxParticipants: 2,
        isPrivate: false,
        topic: 'Travel plans',
      });
    });

    it.each([[null], [undefined], [''], ['   ']])('omits the topic when it is %j', (topic) => {
      sendCreate({ topic, maxParticipants: 3, isPrivate: false });

      const body = sendMessage.mock.calls[0][1];
      expect(body).toEqual({ maxParticipants: 3, isPrivate: false });
      expect(body).not.toHaveProperty('topic');
    });

    it.each([
      [undefined, false],
      [null, false],
      [0, false],
      [1, true],
      [true, true],
    ])('coerces isPrivate %j to boolean %s', (isPrivate, expected) => {
      sendCreate({ maxParticipants: 4, isPrivate });

      expect(sendMessage.mock.calls[0][1].isPrivate).toBe(expected);
    });

    it('returns the result of sendMessage', () => {
      sendMessage.mockReturnValue(false);

      expect(sendCreate({ maxParticipants: 4 })).toBe(false);
    });
  });

  describe('sendJoin', () => {
    it('trims and uppercases the room code', () => {
      sendJoin('  abc234 ');

      expect(sendMessage).toHaveBeenCalledWith('/app/speaking/join', { roomCode: 'ABC234' });
    });

    it('returns the result of sendMessage', () => {
      sendMessage.mockReturnValue(false);

      expect(sendJoin('ABC234')).toBe(false);
    });
  });

  describe('commands without arguments', () => {
    it.each([
      ['sendLeave', sendLeave, '/app/speaking/leave'],
      ['sendActiveRoom', sendActiveRoom, '/app/speaking/active-room'],
    ])('%s sends an empty body to %s', (_name, fn, destination) => {
      const result = fn();

      expect(sendMessage).toHaveBeenCalledWith(destination, {});
      expect(result).toBe(true);
    });
  });

  describe('room commands', () => {
    it.each([
      [
        'sendSignal',
        () => sendSignal('ABC234', 2, 'OFFER', { sid: 's1' }),
        '/app/speaking/signal',
        { roomCode: 'ABC234', toUserId: 2, type: 'OFFER', payload: { sid: 's1' } },
      ],
      [
        'sendMic',
        () => sendMic('ABC234', true),
        '/app/speaking/mic',
        { roomCode: 'ABC234', muted: true },
      ],
      [
        'sendMute',
        () => sendMute('ABC234', 5),
        '/app/speaking/mute',
        { roomCode: 'ABC234', targetUserId: 5 },
      ],
      [
        'sendTransferHost',
        () => sendTransferHost('ABC234', 9),
        '/app/speaking/transfer-host',
        { roomCode: 'ABC234', newHostId: 9 },
      ],
      ['sendEnd', () => sendEnd('ABC234'), '/app/speaking/end', { roomCode: 'ABC234' }],
      [
        'sendKick',
        () => sendKick('ABC234', 5),
        '/app/speaking/kick',
        { roomCode: 'ABC234', userId: 5 },
      ],
      [
        'sendUnban',
        () => sendUnban('ABC234', 5),
        '/app/speaking/unban',
        { roomCode: 'ABC234', userId: 5 },
      ],
      [
        'sendChat',
        () => sendChat('ABC234', 'hello'),
        '/app/speaking/chat',
        { roomCode: 'ABC234', text: 'hello' },
      ],
    ])('%s sends the expected body to %s', (_name, invoke, destination, body) => {
      const result = invoke();

      expect(sendMessage).toHaveBeenCalledWith(destination, body);
      expect(result).toBe(true);
    });

    it('returns false when the socket is not connected', () => {
      sendMessage.mockReturnValue(false);

      expect(sendChat('ABC234', 'hi')).toBe(false);
      expect(sendMic('ABC234', false)).toBe(false);
    });
  });
});
