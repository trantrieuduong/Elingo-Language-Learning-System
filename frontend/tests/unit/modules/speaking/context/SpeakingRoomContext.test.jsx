import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import {
  SpeakingRoomProvider,
  useSpeakingRoom,
} from '@/modules/speaking/context/SpeakingRoomContext';
import { useAuth } from '@/context/AuthContext';
import { getSocketStatus } from '@/services/stompClient';
import { getIceConfigApi } from '@/modules/speaking/speakingApi';
import {
  subscribeSpeaking,
  onStatus,
  sendJoin,
  sendLeave,
  sendSignal,
  sendMic,
  sendMute,
  sendTransferHost,
  sendEnd,
  sendKick,
  sendUnban,
  sendChat,
} from '@/modules/speaking/speakingSocket';
import { createPeerManager } from '@/modules/speaking/webrtc/peerManager';

vi.mock('@/context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/services/stompClient', () => ({
  getSocketStatus: vi.fn(),
}));

vi.mock('@/modules/speaking/speakingApi', () => ({
  getIceConfigApi: vi.fn(),
}));

vi.mock('@/modules/speaking/speakingSocket', () => ({
  subscribeSpeaking: vi.fn(),
  onStatus: vi.fn(),
  sendJoin: vi.fn(),
  sendLeave: vi.fn(),
  sendSignal: vi.fn(),
  sendMic: vi.fn(),
  sendMute: vi.fn(),
  sendTransferHost: vi.fn(),
  sendEnd: vi.fn(),
  sendKick: vi.fn(),
  sendUnban: vi.fn(),
  sendChat: vi.fn(),
}));

vi.mock('@/modules/speaking/webrtc/peerManager', () => ({
  createPeerManager: vi.fn(),
}));

describe('SpeakingRoomContext', () => {
  const ROOM_CODE = 'ABC234';
  const ICE_SERVERS = [{ urls: 'stun:stun.example.org:3478' }];

  let eventHandler;
  let statusHandler;
  let unsubscribeSpeaking;
  let unsubscribeStatus;
  let manager;
  let track;
  let stream;
  let getUserMedia;

  const roomState = (overrides = {}) => ({
    roomCode: ROOM_CODE,
    topic: 'Travel',
    hostId: 1,
    maxParticipants: 4,
    isPrivate: false,
    peers: [
      { userId: 1, displayName: 'Me', role: 'HOST' },
      { userId: 2, displayName: 'Bob', role: 'GUEST' },
    ],
    mutedUserIds: [],
    ...overrides,
  });

  const renderProvider = () =>
    renderHook(() => useSpeakingRoom(), { wrapper: SpeakingRoomProvider });

  // Tuỳ chọn mà context truyền vào createPeerManager (chứa các callback onLinkState, onRemoteStream...)
  const peerOptions = () => createPeerManager.mock.calls.at(-1)[0];

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

  const advance = (ms) => {
    act(() => {
      vi.advanceTimersByTime(ms);
    });
  };

  const advanceAsync = async (ms) => {
    await act(async () => {
      await vi.advanceTimersByTimeAsync(ms);
    });
  };

  // Chạy startSession đến khi gửi lệnh join đầu tiên (sau độ trễ 200ms)
  const start = async (result, code = ROOM_CODE) => {
    await act(async () => {
      result.current.startSession(code);
      await vi.advanceTimersByTimeAsync(250);
    });
  };

  const joined = async (result, overrides) => {
    await start(result);
    emit('room:state', roomState(overrides));
  };

  const chatMessage = (overrides = {}) => ({
    fromUserId: 2,
    displayName: 'Bob',
    text: 'hello',
    timestamp: 1700000000000,
    ...overrides,
  });

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-01T00:00:00.000Z'));
    vi.clearAllMocks();

    useAuth.mockReturnValue({ user: { id: 1 } });
    getSocketStatus.mockReturnValue('connected');

    track = { enabled: true, stop: vi.fn() };
    stream = {
      getTracks: vi.fn(() => [track]),
      getAudioTracks: vi.fn(() => [track]),
    };
    getUserMedia = vi.fn().mockResolvedValue(stream);
    Object.defineProperty(navigator, 'mediaDevices', {
      value: { getUserMedia },
      configurable: true,
      writable: true,
    });

    getIceConfigApi.mockResolvedValue({ data: { iceServers: ICE_SERVERS } });

    manager = {
      addPeer: vi.fn(),
      handleSignal: vi.fn(),
      removePeer: vi.fn(),
      closeAll: vi.fn(),
      destroy: vi.fn(),
      restartPeer: vi.fn(),
      has: vi.fn(),
    };
    createPeerManager.mockReturnValue(manager);

    [sendJoin, sendLeave, sendSignal, sendMic, sendMute, sendTransferHost, sendEnd, sendKick, sendUnban, sendChat].forEach(
      (fn) => fn.mockReturnValue(true)
    );

    unsubscribeSpeaking = vi.fn();
    unsubscribeStatus = vi.fn();
    subscribeSpeaking.mockImplementation((handler) => {
      eventHandler = handler;
      return unsubscribeSpeaking;
    });
    onStatus.mockImplementation((handler) => {
      statusHandler = handler;
      return unsubscribeStatus;
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('provider', () => {
    it('throws when useSpeakingRoom is used outside the provider', () => {
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});

      expect(() => renderHook(() => useSpeakingRoom())).toThrow(
        'useSpeakingRoom must be used within a SpeakingRoomProvider'
      );

      spy.mockRestore();
    });

    it('provides the idle initial state', () => {
      const { result } = renderProvider();

      expect(result.current).toMatchObject({
        selfId: 1,
        connection: 'connected',
        phase: 'idle',
        room: null,
        peers: {},
        streams: {},
        muted: false,
        micError: null,
        error: null,
        closedReason: null,
        notice: null,
        isHost: false,
        kicked: false,
        summary: null,
        isPrivate: false,
        chatMessages: [],
        chatUnread: 0,
        chatOpen: false,
      });
    });

    it('has no selfId when the user is not loaded', () => {
      useAuth.mockReturnValue({ user: null });

      const { result } = renderProvider();

      expect(result.current.selfId).toBeUndefined();
    });

    it('subscribes to the socket status on mount and cleans up on unmount', () => {
      const { unmount } = renderProvider();

      expect(onStatus).toHaveBeenCalledTimes(1);
      expect(subscribeSpeaking).not.toHaveBeenCalled();

      unmount();

      expect(unsubscribeStatus).toHaveBeenCalledTimes(1);
    });

    it('follows socket status changes', () => {
      const { result } = renderProvider();

      changeConnection('disconnected');

      expect(result.current.connection).toBe('disconnected');
    });
  });

  describe('startSession', () => {
    it('shows the preparing phase while waiting for the microphone', async () => {
      let resolveMedia;
      getUserMedia.mockReturnValue(
        new Promise((resolve) => {
          resolveMedia = resolve;
        })
      );
      const { result } = renderProvider();

      act(() => {
        result.current.startSession(ROOM_CODE);
      });

      expect(result.current.phase).toBe('preparing');
      expect(result.current.room).toBeNull();

      await act(async () => {
        resolveMedia(stream);
        await vi.advanceTimersByTimeAsync(250);
      });

      expect(result.current.phase).toBe('connecting');
    });

    it('acquires the microphone, builds the peer manager and joins the room', async () => {
      const { result } = renderProvider();

      await start(result);

      expect(getUserMedia).toHaveBeenCalledWith({
        audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
      });
      expect(getIceConfigApi).toHaveBeenCalledTimes(1);
      expect(createPeerManager).toHaveBeenCalledWith(
        expect.objectContaining({ selfId: 1, iceServers: ICE_SERVERS, localStream: stream })
      );
      expect(subscribeSpeaking).toHaveBeenCalledTimes(1);
      expect(sendJoin).toHaveBeenCalledTimes(1);
      expect(sendJoin).toHaveBeenCalledWith(ROOM_CODE);
      expect(result.current.phase).toBe('connecting');
    });

    it('falls back to no ICE servers when the config is empty', async () => {
      getIceConfigApi.mockResolvedValue({});
      const { result } = renderProvider();

      await start(result);

      expect(createPeerManager).toHaveBeenCalledWith(expect.objectContaining({ iceServers: [] }));
    });

    it.each([
      ['NotAllowedError', 'MIC_DENIED'],
      ['SecurityError', 'MIC_DENIED'],
      ['NotFoundError', 'MIC_UNAVAILABLE'],
      ['AbortError', 'MIC_UNAVAILABLE'],
    ])('maps the microphone error %s to %s', async (name, expected) => {
      getUserMedia.mockRejectedValue({ name });
      const { result } = renderProvider();

      await start(result);

      expect(result.current.micError).toBe(expected);
      expect(result.current.phase).toBe('error');
      expect(getIceConfigApi).not.toHaveBeenCalled();
      expect(createPeerManager).not.toHaveBeenCalled();
      expect(sendJoin).not.toHaveBeenCalled();
    });

    it('treats a microphone failure without details as unavailable', async () => {
      getUserMedia.mockRejectedValue(undefined);
      const { result } = renderProvider();

      await start(result);

      expect(result.current.micError).toBe('MIC_UNAVAILABLE');
      expect(result.current.phase).toBe('error');
    });

    it.each([
      ['an API error code', { response: { data: { code: 'ICE_FAIL' } } }, 'ICE_FAIL'],
      ['a plain error code', { code: 'BOOM' }, 'BOOM'],
      ['an error without code', new Error('network'), 'UNKNOWN'],
    ])('reports %s when the ICE config cannot be loaded', async (_label, failure, expected) => {
      getIceConfigApi.mockRejectedValue(failure);
      const { result } = renderProvider();

      await start(result);

      expect(result.current.error).toBe(expected);
      expect(result.current.phase).toBe('error');
      expect(track.stop).toHaveBeenCalledTimes(1);
      expect(subscribeSpeaking).not.toHaveBeenCalled();
      expect(sendJoin).not.toHaveBeenCalled();
    });

    it('retries the join every 2 seconds, three attempts in total, until the room answers', async () => {
      const { result } = renderProvider();
      await start(result);
      expect(sendJoin).toHaveBeenCalledTimes(1);

      await advanceAsync(2000);
      expect(sendJoin).toHaveBeenCalledTimes(2);

      await advanceAsync(2000);
      expect(sendJoin).toHaveBeenCalledTimes(3);

      await advanceAsync(10000);
      expect(sendJoin).toHaveBeenCalledTimes(3);
    });

    it('stops retrying the join once the room state arrives', async () => {
      const { result } = renderProvider();
      await start(result);

      emit('room:state', roomState());
      await advanceAsync(10000);

      expect(sendJoin).toHaveBeenCalledTimes(1);
    });

    it('abandons the session when it is torn down while waiting for the microphone', async () => {
      let resolveMedia;
      getUserMedia.mockReturnValue(
        new Promise((resolve) => {
          resolveMedia = resolve;
        })
      );
      const { result } = renderProvider();
      act(() => {
        result.current.startSession(ROOM_CODE);
      });

      act(() => {
        result.current.teardown();
      });
      await act(async () => {
        resolveMedia(stream);
        await vi.advanceTimersByTimeAsync(300);
      });

      expect(track.stop).toHaveBeenCalledTimes(1);
      expect(getIceConfigApi).not.toHaveBeenCalled();
      expect(createPeerManager).not.toHaveBeenCalled();
      expect(sendJoin).not.toHaveBeenCalled();
    });

    it('abandons the session when it is torn down while waiting for the ICE config', async () => {
      let resolveIce;
      getIceConfigApi.mockReturnValue(
        new Promise((resolve) => {
          resolveIce = resolve;
        })
      );
      const { result } = renderProvider();
      await act(async () => {
        result.current.startSession(ROOM_CODE);
        await vi.advanceTimersByTimeAsync(10);
      });

      act(() => {
        result.current.teardown();
      });
      await act(async () => {
        resolveIce({ data: { iceServers: ICE_SERVERS } });
        await vi.advanceTimersByTimeAsync(300);
      });

      expect(track.stop).toHaveBeenCalled();
      expect(createPeerManager).not.toHaveBeenCalled();
      expect(sendJoin).not.toHaveBeenCalled();
    });

    it('tears down the previous session and clears its state when started again', async () => {
      const { result } = renderProvider();
      await joined(result);
      emit('room:kicked', {});
      expect(result.current.kicked).toBe(true);
      expect(result.current.phase).toBe('closed');

      await start(result, 'XYZ789');

      expect(result.current.phase).toBe('connecting');
      expect(result.current.kicked).toBe(false);
      expect(result.current.closedReason).toBeNull();
      expect(result.current.error).toBeNull();
      expect(result.current.room).toBeNull();
      expect(result.current.peers).toEqual({});
      expect(sendJoin).toHaveBeenLastCalledWith('XYZ789');
    });
  });

  describe('room:state', () => {
    it('enters the room with its details, peers and mic status', async () => {
      const { result } = renderProvider();
      await start(result);

      emit('room:state', roomState({ mutedUserIds: [2] }));

      expect(result.current.phase).toBe('in');
      expect(result.current.room).toEqual({
        roomCode: ROOM_CODE,
        topic: 'Travel',
        hostId: 1,
        maxParticipants: 4,
        joinedAtMs: Date.now(),
      });
      expect(result.current.peers[1]).toMatchObject({
        userId: 1,
        displayName: 'Me',
        role: 'HOST',
        grace: false,
        micMuted: false,
      });
      expect(result.current.peers[1].link).toBeUndefined();
      expect(result.current.peers[2]).toMatchObject({
        userId: 2,
        displayName: 'Bob',
        link: 'connecting',
        grace: false,
        micMuted: true,
      });
      expect(manager.closeAll).toHaveBeenCalledTimes(1);
      expect(manager.addPeer).toHaveBeenCalledTimes(2);
      expect(manager.addPeer).toHaveBeenCalledWith(1);
      expect(manager.addPeer).toHaveBeenCalledWith(2);
      expect(sendMic).toHaveBeenCalledWith(ROOM_CODE, false);
    });

    it('treats a snapshot without mutedUserIds as nobody muted', async () => {
      const { result } = renderProvider();
      await start(result);

      emit('room:state', roomState({ mutedUserIds: undefined }));

      expect(result.current.peers[2].micMuted).toBe(false);
    });

    it('tracks whether the room is private', async () => {
      const { result } = renderProvider();
      await start(result);

      emit('room:state', roomState({ isPrivate: true }));

      expect(result.current.isPrivate).toBe(true);
    });

    it.each([
      [1, true],
      ['1', true],
      [2, false],
    ])('sets isHost for hostId %j to %s when the user id is 1', async (hostId, expected) => {
      const { result } = renderProvider();
      await start(result);

      emit('room:state', roomState({ hostId }));

      expect(result.current.isHost).toBe(expected);
    });

    it('keeps the original join time across resyncs', async () => {
      const { result } = renderProvider();
      await start(result);
      emit('room:state', roomState());
      const firstJoinedAt = result.current.room.joinedAtMs;

      await advanceAsync(5000);
      emit('room:state', roomState({ topic: 'Updated topic' }));

      expect(result.current.room.topic).toBe('Updated topic');
      expect(result.current.room.joinedAtMs).toBe(firstJoinedAt);
    });

    it('reports the current mute state after joining', async () => {
      const { result } = renderProvider();
      await start(result);
      act(() => {
        result.current.toggleMute();
      });
      sendMic.mockClear();

      emit('room:state', roomState());

      expect(sendMic).toHaveBeenCalledWith(ROOM_CODE, true);
    });

    it('drops stale remote streams when a fresh snapshot arrives', async () => {
      const { result } = renderProvider();
      await joined(result);
      act(() => {
        peerOptions().onRemoteStream(2, { id: 'remote' });
      });
      expect(result.current.streams).toEqual({ 2: { id: 'remote' } });

      emit('room:state', roomState());

      expect(result.current.streams).toEqual({});
    });
  });

  describe('peer events', () => {
    it('peer:joined adds the peer and starts connecting to it', async () => {
      const { result } = renderProvider();
      await joined(result);
      manager.addPeer.mockClear();

      emit('peer:joined', { userId: 3, displayName: 'Cara', role: 'GUEST' });

      expect(result.current.peers[3]).toMatchObject({
        userId: 3,
        displayName: 'Cara',
        link: 'connecting',
        grace: false,
        micMuted: false,
      });
      expect(manager.addPeer).toHaveBeenCalledWith(3);
    });

    it('peer:left removes the peer and its connection', async () => {
      const { result } = renderProvider();
      await joined(result);

      emit('peer:left', { userId: 2 });

      expect(result.current.peers[2]).toBeUndefined();
      expect(result.current.peers[1]).toBeDefined();
      expect(manager.removePeer).toHaveBeenCalledWith(2);
    });

    it('peer:disconnected flags the peer as waiting to come back', async () => {
      const { result } = renderProvider();
      await joined(result);

      emit('peer:disconnected', { userId: 2 });

      expect(result.current.peers[2].grace).toBe(true);
    });

    it('peer:disconnected ignores unknown peers', async () => {
      const { result } = renderProvider();
      await joined(result);

      emit('peer:disconnected', { userId: 99 });

      expect(result.current.peers[99]).toBeUndefined();
    });

    it('peer:reconnected resets the peer and restarts its connection', async () => {
      const { result } = renderProvider();
      await joined(result);
      emit('peer:disconnected', { userId: 2 });
      act(() => {
        peerOptions().onLinkState(2, 'failed');
      });

      emit('peer:reconnected', { userId: 2, displayName: 'Bobby' });

      expect(result.current.peers[2]).toMatchObject({
        userId: 2,
        displayName: 'Bobby',
        link: 'connecting',
        grace: false,
        micMuted: false,
      });
      expect(manager.restartPeer).toHaveBeenCalledWith(2);
    });

    it('peer:mic updates the mic status of a known peer', async () => {
      const { result } = renderProvider();
      await joined(result);

      emit('peer:mic', { userId: 2, muted: true });
      expect(result.current.peers[2].micMuted).toBe(true);

      emit('peer:mic', { userId: 2, muted: false });
      expect(result.current.peers[2].micMuted).toBe(false);
    });

    it('peer:mic ignores unknown peers', async () => {
      const { result } = renderProvider();
      await joined(result);

      emit('peer:mic', { userId: 99, muted: true });

      expect(result.current.peers[99]).toBeUndefined();
    });

    it('signal is handed to the peer manager', async () => {
      const { result } = renderProvider();
      await joined(result);
      const data = { fromUserId: 2, type: 'OFFER', payload: { sid: 's1' } };

      emit('signal', data);

      expect(manager.handleSignal).toHaveBeenCalledWith(data);
    });
  });

  describe('host and mute events', () => {
    it('host:changed moves the host role, announces it and recomputes isHost', async () => {
      const { result } = renderProvider();
      await joined(result);
      expect(result.current.isHost).toBe(true);

      emit('host:changed', { hostId: 2 });

      expect(result.current.room.hostId).toBe(2);
      expect(result.current.peers[1].role).toBe('GUEST');
      expect(result.current.peers[2].role).toBe('HOST');
      expect(result.current.notice).toEqual({ type: 'hostChanged', userId: 2 });
      expect(result.current.isHost).toBe(false);
    });

    it('clears the notice automatically after 4 seconds', async () => {
      const { result } = renderProvider();
      await joined(result);
      emit('host:changed', { hostId: 2 });

      advance(3999);
      expect(result.current.notice).not.toBeNull();

      advance(1);
      expect(result.current.notice).toBeNull();
    });

    it('lets the page dismiss the notice manually', async () => {
      const { result } = renderProvider();
      await joined(result);
      emit('host:changed', { hostId: 2 });

      act(() => {
        result.current.setNotice(null);
      });

      expect(result.current.notice).toBeNull();
    });

    it('mute:requested mutes the local microphone and tells who did it', async () => {
      const { result } = renderProvider();
      await joined(result);
      sendMic.mockClear();

      emit('mute:requested', { byUserId: 1 });

      expect(result.current.muted).toBe(true);
      expect(track.enabled).toBe(false);
      expect(sendMic).toHaveBeenCalledWith(ROOM_CODE, true);
      expect(result.current.notice).toEqual({ type: 'mutedByHost', userId: 1 });
    });
  });

  describe('toggleMute and requestMute', () => {
    it('toggleMute flips the state, the local track and notifies the room', async () => {
      const { result } = renderProvider();
      await joined(result);
      sendMic.mockClear();

      act(() => {
        result.current.toggleMute();
      });

      expect(result.current.muted).toBe(true);
      expect(track.enabled).toBe(false);
      expect(sendMic).toHaveBeenLastCalledWith(ROOM_CODE, true);

      act(() => {
        result.current.toggleMute();
      });

      expect(result.current.muted).toBe(false);
      expect(track.enabled).toBe(true);
      expect(sendMic).toHaveBeenLastCalledWith(ROOM_CODE, false);
    });

    it('toggleMute only changes local state before a session exists', () => {
      const { result } = renderProvider();

      act(() => {
        result.current.toggleMute();
      });

      expect(result.current.muted).toBe(true);
      expect(sendMic).not.toHaveBeenCalled();
    });

    it('requestMute asks the server to mute another member', async () => {
      const { result } = renderProvider();
      await joined(result);

      act(() => {
        result.current.requestMute(2);
      });

      expect(sendMute).toHaveBeenCalledWith(ROOM_CODE, 2);
    });

    it('requestMute does nothing before a session exists', () => {
      const { result } = renderProvider();

      act(() => {
        result.current.requestMute(2);
      });

      expect(sendMute).not.toHaveBeenCalled();
    });
  });

  describe('peer manager callbacks', () => {
    it('onLinkState updates the link of a known peer', async () => {
      const { result } = renderProvider();
      await joined(result);

      act(() => {
        peerOptions().onLinkState(2, 'connected');
      });

      expect(result.current.peers[2].link).toBe('connected');
    });

    it('onLinkState ignores unknown peers', async () => {
      const { result } = renderProvider();
      await joined(result);

      act(() => {
        peerOptions().onLinkState(99, 'connected');
      });

      expect(result.current.peers[99]).toBeUndefined();
    });

    it('onRemoteStream and onRemoteRemoved maintain the remote streams', async () => {
      const { result } = renderProvider();
      await joined(result);
      const remote = { id: 'remote' };

      act(() => {
        peerOptions().onRemoteStream(2, remote);
      });
      expect(result.current.streams).toEqual({ 2: remote });

      act(() => {
        peerOptions().onRemoteRemoved(2);
      });
      expect(result.current.streams).toEqual({});
    });

    it('sendSignal forwards signals to the room', async () => {
      const { result } = renderProvider();
      await joined(result);

      peerOptions().sendSignal(2, 'OFFER', { sid: 's1' });

      expect(sendSignal).toHaveBeenCalledWith(ROOM_CODE, 2, 'OFFER', { sid: 's1' });
    });

    describe('failed links', () => {
      const failLink = async (userId) => {
        act(() => {
          peerOptions().onLinkState(userId, 'failed');
        });
        await advanceAsync(2000);
      };

      it('restarts a failed link after 2 seconds, at most twice', async () => {
        const { result } = renderProvider();
        await joined(result);

        act(() => {
          peerOptions().onLinkState(2, 'failed');
        });
        await advanceAsync(1999);
        expect(manager.restartPeer).not.toHaveBeenCalled();

        await advanceAsync(1);
        expect(manager.restartPeer).toHaveBeenCalledTimes(1);
        expect(manager.restartPeer).toHaveBeenCalledWith(2);

        await failLink(2);
        expect(manager.restartPeer).toHaveBeenCalledTimes(2);

        await failLink(2);
        expect(manager.restartPeer).toHaveBeenCalledTimes(2);
      });

      it('allows new retries after the link connects successfully', async () => {
        const { result } = renderProvider();
        await joined(result);

        await failLink(2);
        await failLink(2);
        expect(manager.restartPeer).toHaveBeenCalledTimes(2);

        act(() => {
          peerOptions().onLinkState(2, 'connected');
        });
        await failLink(2);

        expect(manager.restartPeer).toHaveBeenCalledTimes(3);
      });

      it('does not restart a peer that is waiting to reconnect', async () => {
        const { result } = renderProvider();
        await joined(result);
        emit('peer:disconnected', { userId: 2 });

        await failLink(2);

        expect(manager.restartPeer).not.toHaveBeenCalled();
      });

      it('does not restart a peer that has left', async () => {
        const { result } = renderProvider();
        await joined(result);
        act(() => {
          peerOptions().onLinkState(2, 'failed');
        });

        emit('peer:left', { userId: 2 });
        await advanceAsync(2000);

        expect(manager.restartPeer).not.toHaveBeenCalled();
      });

      it('does not restart anything after the session was torn down', async () => {
        const { result } = renderProvider();
        await joined(result);
        act(() => {
          peerOptions().onLinkState(2, 'failed');
        });

        emit('room:closed', { reason: 'EMPTY' });
        await advanceAsync(2000);

        expect(manager.restartPeer).not.toHaveBeenCalled();
      });
    });
  });

  describe('closing events', () => {
    it('room:kicked ends the session as kicked and cleans up', async () => {
      const { result } = renderProvider();
      await joined(result);

      emit('room:kicked', {});

      expect(result.current.kicked).toBe(true);
      expect(result.current.phase).toBe('closed');
      expect(result.current.closedReason).toBe('KICKED');
      expect(manager.destroy).toHaveBeenCalledTimes(1);
      expect(track.stop).toHaveBeenCalledTimes(1);
      expect(unsubscribeSpeaking).toHaveBeenCalledTimes(1);
    });

    it('room:closed ends the session with the reason and cleans up', async () => {
      const { result } = renderProvider();
      await joined(result);

      emit('room:closed', { reason: 'ENDED_BY_HOST' });

      expect(result.current.phase).toBe('closed');
      expect(result.current.closedReason).toBe('ENDED_BY_HOST');
      expect(result.current.kicked).toBe(false);
      expect(manager.destroy).toHaveBeenCalledTimes(1);
      expect(track.stop).toHaveBeenCalledTimes(1);
      expect(unsubscribeSpeaking).toHaveBeenCalledTimes(1);
    });

    it('keeps the session running on a non-fatal error', async () => {
      const { result } = renderProvider();
      await joined(result);

      emit('error', { code: 'SPEAKING_NOT_HOST' });

      expect(result.current.error).toBe('SPEAKING_NOT_HOST');
      expect(result.current.phase).toBe('in');
      expect(manager.destroy).not.toHaveBeenCalled();
    });

    it.each([
      'SPEAKING_ROOM_ACCESS_DENIED',
      'SPEAKING_ROOM_CLOSED',
      'SPEAKING_ROOM_NOT_FOUND',
      'SPEAKING_ROOM_FULL',
      'SPEAKING_BANNED',
      'SPEAKING_ALREADY_IN_ROOM',
    ])('ends the session on the fatal error %s', async (code) => {
      const { result } = renderProvider();
      await joined(result);

      emit('error', { code });

      expect(result.current.error).toBe(code);
      expect(result.current.phase).toBe('error');
      expect(manager.destroy).toHaveBeenCalledTimes(1);
      expect(track.stop).toHaveBeenCalledTimes(1);
    });

    it('ignores malformed envelopes and unknown events', async () => {
      const { result } = renderProvider();
      await joined(result);

      act(() => {
        eventHandler(null);
        eventHandler({});
      });
      emit('unknown:event', { foo: 'bar' });

      expect(result.current.phase).toBe('in');
      expect(result.current.error).toBeNull();
    });

    it('teardown cleans up everything on the client without leaving the room', async () => {
      const { result } = renderProvider();
      await joined(result);
      emit('chat:message', chatMessage());
      emit('host:changed', { hostId: 2 });
      act(() => {
        peerOptions().onRemoteStream(2, { id: 'remote' });
        result.current.setChatOpen(true);
      });

      act(() => {
        result.current.teardown();
      });

      expect(manager.destroy).toHaveBeenCalledTimes(1);
      expect(track.stop).toHaveBeenCalledTimes(1);
      expect(unsubscribeSpeaking).toHaveBeenCalledTimes(1);
      expect(result.current.streams).toEqual({});
      expect(result.current.notice).toBeNull();
      expect(result.current.isPrivate).toBe(false);
      expect(result.current.chatMessages).toEqual([]);
      expect(result.current.chatUnread).toBe(0);
      expect(result.current.chatOpen).toBe(false);
      expect(sendLeave).not.toHaveBeenCalled();
    });
  });

  describe('reconnection', () => {
    it('re-sends join when the socket reconnects while in a room', async () => {
      const { result } = renderProvider();
      await joined(result);
      expect(sendJoin).toHaveBeenCalledTimes(1);

      changeConnection('disconnected');
      changeConnection('connected');

      expect(sendJoin).toHaveBeenCalledTimes(2);
      expect(sendJoin).toHaveBeenLastCalledWith(ROOM_CODE);
      expect(subscribeSpeaking).toHaveBeenCalledTimes(1);
    });

    it('does not join anything when the socket reconnects without a session', () => {
      renderProvider();

      changeConnection('disconnected');
      changeConnection('connected');

      expect(sendJoin).not.toHaveBeenCalled();
    });

    it('does not re-send join while the socket stays disconnected', async () => {
      const { result } = renderProvider();
      await joined(result);
      sendJoin.mockClear();

      changeConnection('disconnected');
      changeConnection('connecting');

      expect(sendJoin).not.toHaveBeenCalled();
    });
  });

  describe('leaveRoom', () => {
    const summary = { roomCode: ROOM_CODE, topic: 'Travel', durationSeconds: 90 };

    it('sends leave, waits for room:left, stores the summary and cleans up', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.leaveRoom();
      });
      expect(result.current.phase).toBe('leaving');
      expect(sendLeave).toHaveBeenCalledTimes(1);

      emit('room:left', summary);
      let returned;
      await act(async () => {
        returned = await promise;
      });

      expect(returned).toEqual(summary);
      expect(result.current.summary).toEqual(summary);
      expect(result.current.phase).toBe('leaving');
      expect(manager.destroy).toHaveBeenCalledTimes(1);
      expect(track.stop).toHaveBeenCalledTimes(1);
    });

    it('gives up after 3 seconds without a reply but still cleans up', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.leaveRoom();
      });
      let returned;
      await act(async () => {
        await vi.advanceTimersByTimeAsync(3000);
        returned = await promise;
      });

      expect(returned).toBeNull();
      expect(result.current.summary).toBeNull();
      expect(manager.destroy).toHaveBeenCalledTimes(1);
    });

    it('does not wait when the leave command cannot be sent', async () => {
      sendLeave.mockReturnValue(false);
      const { result } = renderProvider();
      await joined(result);

      let returned;
      await act(async () => {
        returned = await result.current.leaveRoom();
      });

      expect(returned).toBeNull();
      expect(manager.destroy).toHaveBeenCalledTimes(1);
    });

    it('does not send anything when there is no room', async () => {
      const { result } = renderProvider();

      let returned;
      await act(async () => {
        returned = await result.current.leaveRoom();
      });

      expect(returned).toBeNull();
      expect(sendLeave).not.toHaveBeenCalled();
      expect(result.current.phase).toBe('leaving');
    });

    it('clearSummary removes the stored summary', async () => {
      const { result } = renderProvider();
      await joined(result);
      let promise;
      act(() => {
        promise = result.current.leaveRoom();
      });
      emit('room:left', summary);
      await act(async () => {
        await promise;
      });
      expect(result.current.summary).toEqual(summary);

      act(() => {
        result.current.clearSummary();
      });

      expect(result.current.summary).toBeNull();
    });
  });

  describe('transferHostAndLeave', () => {
    const summary = { roomCode: ROOM_CODE, durationSeconds: 45 };

    it('transfers the host role, then leaves and returns the summary', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.transferHostAndLeave(2);
      });
      expect(sendTransferHost).toHaveBeenCalledWith(ROOM_CODE, 2);
      expect(sendLeave).not.toHaveBeenCalled();

      emit('host:changed', { hostId: 2 });
      await act(async () => {});
      expect(sendLeave).toHaveBeenCalledTimes(1);

      emit('room:left', summary);
      let returned;
      await act(async () => {
        returned = await promise;
      });

      expect(returned).toEqual(summary);
      expect(result.current.summary).toEqual(summary);
    });

    it('accepts the new host id as a string', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.transferHostAndLeave('2');
      });
      emit('host:changed', { hostId: 2 });
      await act(async () => {});

      expect(sendLeave).toHaveBeenCalledTimes(1);
      emit('room:left', summary);
      await act(async () => {
        await promise;
      });
    });

    it('rejects with the server error code and does not leave', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.transferHostAndLeave(2);
      });
      const rejection = expect(promise).rejects.toEqual({ code: 'SPEAKING_NOT_HOST' });
      emit('error', { code: 'SPEAKING_NOT_HOST' });

      await rejection;
      expect(sendLeave).not.toHaveBeenCalled();
    });

    it('rejects with TIMEOUT when the server does not answer within 4 seconds', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.transferHostAndLeave(2);
      });
      const rejection = expect(promise).rejects.toEqual({ code: 'TIMEOUT' });
      await advanceAsync(4000);

      await rejection;
      expect(sendLeave).not.toHaveBeenCalled();
    });

    it('rejects with SOCKET_NOT_CONNECTED when the command cannot be sent', async () => {
      sendTransferHost.mockReturnValue(false);
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.transferHostAndLeave(2);
      });

      await expect(promise).rejects.toEqual({ code: 'SOCKET_NOT_CONNECTED' });
      expect(sendLeave).not.toHaveBeenCalled();
    });
  });

  describe('endRoom', () => {
    const summary = { roomCode: ROOM_CODE, durationSeconds: 300 };

    it('ends the room, stores the summary and cleans up', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.endRoom();
      });
      expect(sendEnd).toHaveBeenCalledWith(ROOM_CODE);

      emit('room:left', summary);
      let returned;
      await act(async () => {
        returned = await promise;
      });

      expect(returned).toEqual(summary);
      expect(result.current.summary).toEqual(summary);
      expect(result.current.phase).toBe('leaving');
      expect(manager.destroy).toHaveBeenCalledTimes(1);
      expect(track.stop).toHaveBeenCalledTimes(1);
    });

    it('rejects with the server error code and keeps the session', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.endRoom();
      });
      const rejection = expect(promise).rejects.toEqual({ code: 'SPEAKING_NOT_HOST' });
      emit('error', { code: 'SPEAKING_NOT_HOST' });

      await rejection;
      expect(result.current.phase).toBe('in');
      expect(manager.destroy).not.toHaveBeenCalled();
    });

    it('rejects with TIMEOUT when the server does not answer', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.endRoom();
      });
      const rejection = expect(promise).rejects.toEqual({ code: 'TIMEOUT' });
      await advanceAsync(4000);

      await rejection;
    });

    it('rejects with SOCKET_NOT_CONNECTED when the command cannot be sent', async () => {
      sendEnd.mockReturnValue(false);
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.endRoom();
      });

      await expect(promise).rejects.toEqual({ code: 'SOCKET_NOT_CONNECTED' });
    });
  });

  describe('kickMember', () => {
    it('resolves once the server confirms the kick', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.kickMember(2);
      });
      expect(sendKick).toHaveBeenCalledWith(ROOM_CODE, 2);
      let settled = false;
      promise.then(() => {
        settled = true;
      });

      emit('peer:left', { userId: 2, reason: 'LEFT' });
      await act(async () => {});
      expect(settled).toBe(false);

      emit('peer:left', { userId: 3, reason: 'KICKED' });
      await act(async () => {});
      expect(settled).toBe(false);

      emit('peer:left', { userId: 2, reason: 'KICKED' });
      await act(async () => {
        await promise;
      });
      expect(settled).toBe(true);
    });

    it('rejects with the server error code', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.kickMember(2);
      });
      const rejection = expect(promise).rejects.toEqual({ code: 'SPEAKING_NOT_HOST' });
      emit('error', { code: 'SPEAKING_NOT_HOST' });

      await rejection;
    });

    it('rejects with SOCKET_NOT_CONNECTED when the command cannot be sent', async () => {
      sendKick.mockReturnValue(false);
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.kickMember(2);
      });

      await expect(promise).rejects.toEqual({ code: 'SOCKET_NOT_CONNECTED' });
    });
  });

  describe('unbanMember', () => {
    it('resolves once the server confirms the ban was removed', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.unbanMember(7);
      });
      expect(sendUnban).toHaveBeenCalledWith(ROOM_CODE, 7);
      let settled = false;
      promise.then(() => {
        settled = true;
      });

      emit('ban:removed', { userId: 8 });
      await act(async () => {});
      expect(settled).toBe(false);

      emit('ban:removed', { userId: '7' });
      await act(async () => {
        await promise;
      });
      expect(settled).toBe(true);
    });

    it('rejects with the server error code', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.unbanMember(7);
      });
      const rejection = expect(promise).rejects.toEqual({ code: 'SPEAKING_NOT_HOST' });
      emit('error', { code: 'SPEAKING_NOT_HOST' });

      await rejection;
    });

    it('rejects with TIMEOUT when the server does not answer', async () => {
      const { result } = renderProvider();
      await joined(result);

      let promise;
      act(() => {
        promise = result.current.unbanMember(7);
      });
      const rejection = expect(promise).rejects.toEqual({ code: 'TIMEOUT' });
      await advanceAsync(4000);

      await rejection;
    });
  });

  describe('chat', () => {
    it('appends incoming messages with incremental ids', async () => {
      const { result } = renderProvider();
      await joined(result);

      emit('chat:message', chatMessage({ text: 'first' }));
      emit('chat:message', chatMessage({ text: 'second' }));

      expect(result.current.chatMessages).toEqual([
        { id: 1, ...chatMessage({ text: 'first' }) },
        { id: 2, ...chatMessage({ text: 'second' }) },
      ]);
    });

    it('counts unread messages from others while the chat is closed', async () => {
      const { result } = renderProvider();
      await joined(result);

      emit('chat:message', chatMessage());
      emit('chat:message', chatMessage());

      expect(result.current.chatUnread).toBe(2);
    });

    it.each([[1], ['1']])('does not count own messages (fromUserId %j) as unread', async (fromUserId) => {
      const { result } = renderProvider();
      await joined(result);

      emit('chat:message', chatMessage({ fromUserId }));

      expect(result.current.chatMessages).toHaveLength(1);
      expect(result.current.chatUnread).toBe(0);
    });

    it('does not count messages as unread while the chat is open', async () => {
      const { result } = renderProvider();
      await joined(result);
      act(() => {
        result.current.setChatOpen(true);
      });

      emit('chat:message', chatMessage());

      expect(result.current.chatOpen).toBe(true);
      expect(result.current.chatUnread).toBe(0);
    });

    it('opening the chat clears the unread counter and closing keeps counting again', async () => {
      const { result } = renderProvider();
      await joined(result);
      emit('chat:message', chatMessage());
      expect(result.current.chatUnread).toBe(1);

      act(() => {
        result.current.setChatOpen(true);
      });
      expect(result.current.chatUnread).toBe(0);

      act(() => {
        result.current.setChatOpen(false);
      });
      emit('chat:message', chatMessage());

      expect(result.current.chatOpen).toBe(false);
      expect(result.current.chatUnread).toBe(1);
    });

    it('keeps only the latest 200 messages', async () => {
      const { result } = renderProvider();
      await joined(result);

      act(() => {
        for (let i = 1; i <= 205; i += 1) {
          eventHandler({ event: 'chat:message', data: chatMessage({ text: `m${i}` }) });
        }
      });

      const { chatMessages } = result.current;
      expect(chatMessages).toHaveLength(200);
      expect(chatMessages[0]).toMatchObject({ id: 6, text: 'm6' });
      expect(chatMessages.at(-1)).toMatchObject({ id: 205, text: 'm205' });
    });

    describe('sendChatMessage', () => {
      it('sends the trimmed text to the room', async () => {
        const { result } = renderProvider();
        await joined(result);

        let sent;
        act(() => {
          sent = result.current.sendChatMessage('  hello there  ');
        });

        expect(sent).toBe(true);
        expect(sendChat).toHaveBeenCalledWith(ROOM_CODE, 'hello there');
      });

      it('truncates the text to 500 characters', async () => {
        const { result } = renderProvider();
        await joined(result);

        act(() => {
          result.current.sendChatMessage('a'.repeat(600));
        });

        expect(sendChat).toHaveBeenCalledWith(ROOM_CODE, 'a'.repeat(500));
      });

      it.each([[''], ['   '], [null], [undefined]])('refuses the empty message %j', async (text) => {
        const { result } = renderProvider();
        await joined(result);

        let sent;
        act(() => {
          sent = result.current.sendChatMessage(text);
        });

        expect(sent).toBe(false);
        expect(sendChat).not.toHaveBeenCalled();
      });

      it('refuses to send when there is no room', () => {
        const { result } = renderProvider();

        let sent;
        act(() => {
          sent = result.current.sendChatMessage('hello');
        });

        expect(sent).toBe(false);
        expect(sendChat).not.toHaveBeenCalled();
      });
    });
  });
});
