import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import {
  SpeakingSocketProvider,
  useSpeakingSocket,
} from '@/modules/speaking/context/SpeakingSocketContext';
import {
  subscribeSpeaking,
  onStatus,
  sendCreate,
  sendJoin,
  sendLeave,
  sendActiveRoom,
} from '@/modules/speaking/speakingSocket';
import { getSocketStatus } from '@/services/stompClient';

vi.mock('@/modules/speaking/speakingSocket', () => ({
  subscribeSpeaking: vi.fn(),
  onStatus: vi.fn(),
  sendCreate: vi.fn(),
  sendJoin: vi.fn(),
  sendLeave: vi.fn(),
  sendActiveRoom: vi.fn(),
}));

vi.mock('@/services/stompClient', () => ({
  getSocketStatus: vi.fn(),
}));

describe('SpeakingSocketContext', () => {
  let eventHandler;
  let statusHandler;
  let unsubscribeSpeaking;
  let unsubscribeStatus;

  const roomState = (overrides = {}) => ({
    roomCode: 'ABC234',
    topic: 'Travel',
    hostId: 1,
    maxParticipants: 4,
    peers: [
      { userId: 1, displayName: 'Alice' },
      { userId: 2, displayName: 'Bob' },
    ],
    ...overrides,
  });

  const renderProvider = () =>
    renderHook(() => useSpeakingSocket(), { wrapper: SpeakingSocketProvider });

  const emit = (event, data) => {
    act(() => {
      eventHandler({ event, data });
    });
  };

  const changeConnection = (status) => {
    act(() => {
      statusHandler(status);
    });
  };

  beforeEach(() => {
    vi.clearAllMocks();
    unsubscribeSpeaking = vi.fn();
    unsubscribeStatus = vi.fn();
    getSocketStatus.mockReturnValue('disconnected');
    sendCreate.mockReturnValue(true);
    sendJoin.mockReturnValue(true);
    sendLeave.mockReturnValue(true);
    subscribeSpeaking.mockImplementation((handler) => {
      eventHandler = handler;
      return unsubscribeSpeaking;
    });
    onStatus.mockImplementation((handler) => {
      statusHandler = handler;
      return unsubscribeStatus;
    });
  });

  describe('provider', () => {
    it('throws when useSpeakingSocket is used outside the provider', () => {
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});

      expect(() => renderHook(() => useSpeakingSocket())).toThrow(
        'useSpeakingSocket must be used within a SpeakingSocketProvider'
      );

      spy.mockRestore();
    });

    it('provides the idle initial state', () => {
      const { result } = renderProvider();

      expect(result.current).toMatchObject({
        connection: 'disconnected',
        busy: false,
        error: null,
        roomState: null,
        leftSummary: null,
        createdRoomCode: null,
        activeRoomCode: null,
        activeRoomChecked: false,
      });
    });

    it('starts from the current socket status', () => {
      getSocketStatus.mockReturnValue('connected');

      const { result } = renderProvider();

      expect(result.current.connection).toBe('connected');
    });

    it('subscribes to speaking events and socket status, and cleans up on unmount', () => {
      const { unmount } = renderProvider();

      expect(subscribeSpeaking).toHaveBeenCalledTimes(1);
      expect(onStatus).toHaveBeenCalledTimes(1);

      unmount();

      expect(unsubscribeSpeaking).toHaveBeenCalledTimes(1);
      expect(unsubscribeStatus).toHaveBeenCalledTimes(1);
    });

    it('keeps action callbacks stable across renders', () => {
      const { result, rerender } = renderProvider();
      const { createRoom, joinRoom, leaveRoom, checkActiveRoom, clearError, clearCreated } =
        result.current;

      rerender();

      expect(result.current.createRoom).toBe(createRoom);
      expect(result.current.joinRoom).toBe(joinRoom);
      expect(result.current.leaveRoom).toBe(leaveRoom);
      expect(result.current.checkActiveRoom).toBe(checkActiveRoom);
      expect(result.current.clearError).toBe(clearError);
      expect(result.current.clearCreated).toBe(clearCreated);
    });
  });

  describe('connection status', () => {
    it('follows socket status changes', () => {
      const { result } = renderProvider();

      changeConnection('connecting');
      expect(result.current.connection).toBe('connecting');

      changeConnection('connected');
      expect(result.current.connection).toBe('connected');
    });

    it('releases busy when the connection leaves the connected state', () => {
      getSocketStatus.mockReturnValue('connected');
      const { result } = renderProvider();

      act(() => {
        result.current.createRoom({ maxParticipants: 4 });
      });
      expect(result.current.busy).toBe(true);

      changeConnection('disconnected');

      expect(result.current.busy).toBe(false);
      expect(result.current.connection).toBe('disconnected');
    });
  });

  describe('server events', () => {
    it('ignores malformed envelopes and unknown events', () => {
      const { result } = renderProvider();

      act(() => {
        eventHandler(null);
        eventHandler({});
        eventHandler({ data: { foo: 'bar' } });
      });
      emit('unknown:event', { foo: 'bar' });

      expect(result.current).toMatchObject({
        busy: false,
        error: null,
        roomState: null,
        activeRoomCode: null,
        activeRoomChecked: false,
      });
    });

    describe('room:active', () => {
      it('stores the room the user is already in', () => {
        const { result } = renderProvider();

        emit('room:active', { roomCode: 'ABC234' });

        expect(result.current.activeRoomCode).toBe('ABC234');
        expect(result.current.activeRoomChecked).toBe(true);
      });

      it('marks the check as done with no active room when the payload is empty', () => {
        const { result } = renderProvider();

        emit('room:active', null);

        expect(result.current.activeRoomCode).toBeNull();
        expect(result.current.activeRoomChecked).toBe(true);
      });
    });

    describe('room:created', () => {
      it('stores the created room code and releases busy', () => {
        const { result } = renderProvider();
        act(() => {
          result.current.createRoom({ maxParticipants: 4 });
        });

        emit('room:created', { roomCode: 'NEW234' });

        expect(result.current.busy).toBe(false);
        expect(result.current.createdRoomCode).toBe('NEW234');
        expect(result.current.activeRoomCode).toBe('NEW234');
      });
    });

    describe('room:state', () => {
      it('stores the room snapshot, marks it active and releases busy', () => {
        const { result } = renderProvider();
        act(() => {
          result.current.joinRoom('ABC234');
        });
        const snapshot = roomState();

        emit('room:state', snapshot);

        expect(result.current.busy).toBe(false);
        expect(result.current.roomState).toEqual(snapshot);
        expect(result.current.activeRoomCode).toBe('ABC234');
      });
    });

    describe('room:left', () => {
      it('clears the room, stores the summary and releases busy', () => {
        const { result } = renderProvider();
        emit('room:state', roomState());
        act(() => {
          result.current.leaveRoom();
        });
        const summary = { roomCode: 'ABC234', durationSeconds: 120 };

        emit('room:left', summary);

        expect(result.current.busy).toBe(false);
        expect(result.current.roomState).toBeNull();
        expect(result.current.leftSummary).toEqual(summary);
        expect(result.current.activeRoomCode).toBeNull();
      });
    });

    describe.each(['room:closed', 'room:kicked'])('%s', (event) => {
      it('clears the active room without touching the room snapshot', () => {
        const { result } = renderProvider();
        const snapshot = roomState();
        emit('room:state', snapshot);

        emit(event, { reason: 'whatever' });

        expect(result.current.activeRoomCode).toBeNull();
        expect(result.current.roomState).toEqual(snapshot);
      });
    });

    describe('peer:joined', () => {
      it('appends the new peer to the room', () => {
        const { result } = renderProvider();
        emit('room:state', roomState());

        emit('peer:joined', { userId: 3, displayName: 'Cara' });

        expect(result.current.roomState.peers.map((p) => p.userId)).toEqual([1, 2, 3]);
      });

      it('replaces an existing peer instead of duplicating it', () => {
        const { result } = renderProvider();
        emit('room:state', roomState());

        emit('peer:joined', { userId: 2, displayName: 'Bobby' });

        const { peers } = result.current.roomState;
        expect(peers).toHaveLength(2);
        expect(peers.find((p) => p.userId === 2).displayName).toBe('Bobby');
      });

      it('does nothing when there is no room snapshot', () => {
        const { result } = renderProvider();

        emit('peer:joined', { userId: 3, displayName: 'Cara' });

        expect(result.current.roomState).toBeNull();
      });
    });

    describe('peer:left', () => {
      it('removes the peer from the room', () => {
        const { result } = renderProvider();
        emit('room:state', roomState());

        emit('peer:left', { userId: 2 });

        expect(result.current.roomState.peers.map((p) => p.userId)).toEqual([1]);
      });

      it('does nothing when there is no room snapshot', () => {
        const { result } = renderProvider();

        emit('peer:left', { userId: 2 });

        expect(result.current.roomState).toBeNull();
      });
    });

    describe('error', () => {
      it('stores the error code and releases busy', () => {
        const { result } = renderProvider();
        act(() => {
          result.current.joinRoom('ABC234');
        });

        emit('error', { code: 'SPEAKING_ROOM_FULL' });

        expect(result.current.busy).toBe(false);
        expect(result.current.error).toBe('SPEAKING_ROOM_FULL');
      });
    });
  });

  describe('actions', () => {
    describe('createRoom', () => {
      it('clears previous results, marks busy and sends the create command', () => {
        const { result } = renderProvider();
        emit('error', { code: 'SPEAKING_ROOM_FULL' });
        emit('room:left', { roomCode: 'OLD234' });
        const payload = { topic: 'Travel', maxParticipants: 3, isPrivate: true };

        act(() => {
          result.current.createRoom(payload);
        });

        expect(sendCreate).toHaveBeenCalledWith(payload);
        expect(result.current.busy).toBe(true);
        expect(result.current.error).toBeNull();
        expect(result.current.leftSummary).toBeNull();
      });

      it('reports SOCKET_NOT_CONNECTED and releases busy when the command cannot be sent', () => {
        sendCreate.mockReturnValue(false);
        const { result } = renderProvider();

        act(() => {
          result.current.createRoom({ maxParticipants: 4 });
        });

        expect(result.current.busy).toBe(false);
        expect(result.current.error).toBe('SOCKET_NOT_CONNECTED');
      });
    });

    describe('joinRoom', () => {
      it('clears previous results, marks busy and sends the join command', () => {
        const { result } = renderProvider();
        emit('error', { code: 'SPEAKING_ROOM_FULL' });
        emit('room:left', { roomCode: 'OLD234' });

        act(() => {
          result.current.joinRoom('ABC234');
        });

        expect(sendJoin).toHaveBeenCalledWith('ABC234');
        expect(result.current.busy).toBe(true);
        expect(result.current.error).toBeNull();
        expect(result.current.leftSummary).toBeNull();
      });

      it('reports SOCKET_NOT_CONNECTED and releases busy when the command cannot be sent', () => {
        sendJoin.mockReturnValue(false);
        const { result } = renderProvider();

        act(() => {
          result.current.joinRoom('ABC234');
        });

        expect(result.current.busy).toBe(false);
        expect(result.current.error).toBe('SOCKET_NOT_CONNECTED');
      });
    });

    describe('leaveRoom', () => {
      it('clears the error, marks busy and sends the leave command', () => {
        const { result } = renderProvider();
        emit('error', { code: 'SPEAKING_ROOM_FULL' });

        act(() => {
          result.current.leaveRoom();
        });

        expect(sendLeave).toHaveBeenCalledTimes(1);
        expect(result.current.busy).toBe(true);
        expect(result.current.error).toBeNull();
      });

      it('reports SOCKET_NOT_CONNECTED and releases busy when the command cannot be sent', () => {
        sendLeave.mockReturnValue(false);
        const { result } = renderProvider();

        act(() => {
          result.current.leaveRoom();
        });

        expect(result.current.busy).toBe(false);
        expect(result.current.error).toBe('SOCKET_NOT_CONNECTED');
      });
    });

    it('checkActiveRoom asks the server for the active room', () => {
      const { result } = renderProvider();

      act(() => {
        result.current.checkActiveRoom();
      });

      expect(sendActiveRoom).toHaveBeenCalledTimes(1);
    });

    it('clearError resets the error', () => {
      const { result } = renderProvider();
      emit('error', { code: 'SPEAKING_ROOM_FULL' });

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });

    it('clearCreated resets the created room code', () => {
      const { result } = renderProvider();
      emit('room:created', { roomCode: 'NEW234' });

      act(() => {
        result.current.clearCreated();
      });

      expect(result.current.createdRoomCode).toBeNull();
      expect(result.current.activeRoomCode).toBe('NEW234');
    });
  });
});
