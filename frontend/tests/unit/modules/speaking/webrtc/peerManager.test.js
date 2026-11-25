import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { createPeerManager, shouldOffer } from '@/modules/speaking/webrtc/peerManager';

// RTCPeerConnection giả: ghi lại mọi lời gọi, cho phép ép một thao tác bất đồng bộ thất bại
class FakePC {
  static instances = [];
  static failOn = new Set();

  static maybeFail(operation) {
    if (FakePC.failOn.has(operation)) throw new Error(`${operation} failed`);
  }

  constructor(config) {
    this.config = config;
    this.connectionState = 'new';
    this.localDescription = null;
    this.remoteDescription = null;
    this.onicecandidate = null;
    this.ontrack = null;
    this.onconnectionstatechange = null;

    this.addTrack = vi.fn();
    this.close = vi.fn();
    this.createOffer = vi.fn(async () => {
      FakePC.maybeFail('createOffer');
      return { type: 'offer', sdp: 'offer-sdp' };
    });
    this.createAnswer = vi.fn(async () => {
      FakePC.maybeFail('createAnswer');
      return { type: 'answer', sdp: 'answer-sdp' };
    });
    this.setLocalDescription = vi.fn(async (description) => {
      FakePC.maybeFail('setLocalDescription');
      this.localDescription = {
        ...description,
        toJSON: () => ({ type: description.type, sdp: description.sdp }),
      };
    });
    this.setRemoteDescription = vi.fn(async (description) => {
      FakePC.maybeFail('setRemoteDescription');
      this.remoteDescription = description;
    });
    this.addIceCandidate = vi.fn(async () => {
      FakePC.maybeFail('addIceCandidate');
    });

    FakePC.instances.push(this);
  }
}

// Các thao tác của peerManager chạy qua chuỗi promise nên cần nhường một macrotask để chúng hoàn tất
const flush = () => new Promise((resolve) => setTimeout(resolve, 0));

const ICE_SERVERS = [{ urls: 'stun:stun.example.org:3478' }];

const buildManager = ({ selfId = 1 } = {}) => {
  const track = { id: 'audio-track' };
  const localStream = { getTracks: vi.fn(() => [track]) };
  const callbacks = {
    sendSignal: vi.fn(),
    onLinkState: vi.fn(),
    onRemoteStream: vi.fn(),
    onRemoteRemoved: vi.fn(),
  };
  const manager = createPeerManager({ selfId, iceServers: ICE_SERVERS, localStream, ...callbacks });
  return { manager, track, localStream, ...callbacks };
};

const signalsOf = (sendSignal, type) => sendSignal.mock.calls.filter(([, t]) => t === type);

// Tạo kết nối phía OFFER (selfId = 1, peer = 2) và trả về pc cùng sid của OFFER đã gửi
const startAsOfferer = async (ctx, peerId = 2) => {
  ctx.manager.addPeer(peerId);
  await flush();
  const pc = FakePC.instances.at(-1);
  const [, , payload] = signalsOf(ctx.sendSignal, 'OFFER').at(-1);
  return { pc, sid: payload.sid };
};

const offerFrom = (fromUserId, sid, desc = { type: 'offer', sdp: 'remote-offer' }) => ({
  fromUserId,
  type: 'OFFER',
  payload: { sid, desc },
});

describe('speaking/webrtc/peerManager.js', () => {
  beforeEach(() => {
    FakePC.instances = [];
    FakePC.failOn = new Set();
    vi.stubGlobal('RTCPeerConnection', FakePC);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe('shouldOffer', () => {
    it('lets the side with the lower numeric id create the OFFER', () => {
      expect(shouldOffer(1, 2)).toBe(true);
      expect(shouldOffer(2, 1)).toBe(false);
    });

    it('compares ids as numbers rather than as strings', () => {
      expect(shouldOffer(2, 10)).toBe(true);
      expect(shouldOffer(10, 2)).toBe(false);
    });

    it('treats numeric strings as numbers', () => {
      expect(shouldOffer('2', '10')).toBe(true);
      expect(shouldOffer(7, '3')).toBe(false);
    });

    it('never offers to itself', () => {
      expect(shouldOffer(5, 5)).toBe(false);
    });

    it('falls back to string comparison when an id is not numeric', () => {
      expect(shouldOffer('abc', 'abd')).toBe(true);
      expect(shouldOffer('b', 'a')).toBe(false);
      expect(shouldOffer(1, 'x')).toBe(true);
    });
  });

  describe('addPeer', () => {
    it('creates a connection and sends an OFFER when this side has the lower id', async () => {
      const { manager, track, localStream, sendSignal, onLinkState } = buildManager({ selfId: 1 });

      manager.addPeer(2);

      expect(FakePC.instances).toHaveLength(1);
      const pc = FakePC.instances[0];
      expect(pc.config).toEqual({ iceServers: ICE_SERVERS });
      expect(pc.addTrack).toHaveBeenCalledWith(track, localStream);
      expect(onLinkState).toHaveBeenCalledWith(2, 'connecting');
      expect(manager.has(2)).toBe(true);

      await flush();

      expect(sendSignal).toHaveBeenCalledTimes(1);
      expect(sendSignal).toHaveBeenCalledWith(2, 'OFFER', {
        sid: expect.any(String),
        desc: { type: 'offer', sdp: 'offer-sdp' },
      });
      expect(pc.setLocalDescription).toHaveBeenCalledTimes(1);
    });

    it('does not create a second connection or OFFER when the peer is added twice', async () => {
      const { manager, sendSignal } = buildManager({ selfId: 1 });

      manager.addPeer(2);
      manager.addPeer(2);
      await flush();

      expect(FakePC.instances).toHaveLength(1);
      expect(signalsOf(sendSignal, 'OFFER')).toHaveLength(1);
    });

    it('creates the connection and sends READY when this side has the higher id', async () => {
      const { manager, sendSignal, onLinkState } = buildManager({ selfId: 2 });

      manager.addPeer(1);

      expect(FakePC.instances).toHaveLength(1);
      expect(onLinkState).toHaveBeenCalledWith(1, 'connecting');
      expect(sendSignal).toHaveBeenCalledWith(1, 'READY', {});

      await flush();

      expect(signalsOf(sendSignal, 'OFFER')).toHaveLength(0);
      expect(FakePC.instances[0].createOffer).not.toHaveBeenCalled();
    });

    it('does not send READY twice when the peer is added again', () => {
      const { manager, sendSignal } = buildManager({ selfId: 2 });

      manager.addPeer(1);
      manager.addPeer(1);

      expect(FakePC.instances).toHaveLength(1);
      expect(signalsOf(sendSignal, 'READY')).toHaveLength(1);
    });

    it('ignores its own id, even when the type differs', () => {
      const { manager, sendSignal } = buildManager({ selfId: 1 });

      manager.addPeer(1);
      manager.addPeer('1');

      expect(FakePC.instances).toHaveLength(0);
      expect(sendSignal).not.toHaveBeenCalled();
      expect(manager.has(1)).toBe(false);
    });

    it('generates a distinct session id for every OFFER', async () => {
      const { manager, sendSignal } = buildManager({ selfId: 1 });

      manager.addPeer(2);
      await flush();
      manager.handleSignal({ fromUserId: 2, type: 'READY' });
      await flush();

      const offers = signalsOf(sendSignal, 'OFFER');
      expect(offers).toHaveLength(2);
      expect(offers[0][2].sid).not.toBe(offers[1][2].sid);
    });

    it('falls back to a random session id when crypto.randomUUID is unavailable', async () => {
      vi.stubGlobal('crypto', {});
      const { manager, sendSignal } = buildManager({ selfId: 1 });

      manager.addPeer(2);
      await flush();

      const { sid } = signalsOf(sendSignal, 'OFFER')[0][2];
      expect(sid).toMatch(/^[a-z0-9]+$/);
    });

    it('reports failed and sends no OFFER when createOffer rejects', async () => {
      FakePC.failOn.add('createOffer');
      const { manager, sendSignal, onLinkState } = buildManager({ selfId: 1 });

      manager.addPeer(2);
      await flush();

      expect(onLinkState).toHaveBeenLastCalledWith(2, 'failed');
      expect(signalsOf(sendSignal, 'OFFER')).toHaveLength(0);
    });
  });

  describe('handleSignal: READY', () => {
    it('rebuilds the connection and sends a fresh OFFER when this side offers', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc: first } = await startAsOfferer(ctx);

      ctx.manager.handleSignal({ fromUserId: 2, type: 'READY' });
      await flush();

      expect(first.close).toHaveBeenCalledTimes(1);
      expect(ctx.onRemoteRemoved).toHaveBeenCalledWith(2);
      expect(FakePC.instances).toHaveLength(2);
      expect(signalsOf(ctx.sendSignal, 'OFFER')).toHaveLength(2);
    });

    it('creates the connection and OFFERs when READY arrives from an unknown peer', async () => {
      const { manager, sendSignal } = buildManager({ selfId: 1 });

      manager.handleSignal({ fromUserId: 2, type: 'READY' });
      await flush();

      expect(FakePC.instances).toHaveLength(1);
      expect(signalsOf(sendSignal, 'OFFER')).toHaveLength(1);
    });

    it('ignores READY when this side is the answerer', async () => {
      const { manager, sendSignal } = buildManager({ selfId: 2 });

      manager.handleSignal({ fromUserId: 1, type: 'READY' });
      await flush();

      expect(FakePC.instances).toHaveLength(0);
      expect(sendSignal).not.toHaveBeenCalled();
    });
  });

  describe('handleSignal: OFFER', () => {
    it('applies the remote OFFER and replies with an ANSWER using the same sid', async () => {
      const { manager, sendSignal, onLinkState } = buildManager({ selfId: 2 });
      const remoteOffer = { type: 'offer', sdp: 'remote-offer' };

      manager.handleSignal(offerFrom(1, 'sid-1', remoteOffer));
      await flush();

      expect(FakePC.instances).toHaveLength(1);
      const pc = FakePC.instances[0];
      expect(onLinkState).toHaveBeenCalledWith(1, 'connecting');
      expect(pc.setRemoteDescription).toHaveBeenCalledWith(remoteOffer);
      expect(pc.createAnswer).toHaveBeenCalledTimes(1);
      expect(sendSignal).toHaveBeenCalledWith(1, 'ANSWER', {
        sid: 'sid-1',
        desc: { type: 'answer', sdp: 'answer-sdp' },
      });
      expect(pc.setRemoteDescription.mock.invocationCallOrder[0]).toBeLessThan(
        pc.setLocalDescription.mock.invocationCallOrder[0]
      );
    });

    it('replaces the connection when the same session is offered again after it was applied', async () => {
      const { manager, sendSignal, onRemoteRemoved } = buildManager({ selfId: 2 });

      manager.handleSignal(offerFrom(1, 'sid-1'));
      await flush();
      manager.handleSignal(offerFrom(1, 'sid-1'));
      await flush();

      expect(FakePC.instances).toHaveLength(2);
      expect(FakePC.instances[0].close).toHaveBeenCalledTimes(1);
      expect(onRemoteRemoved).toHaveBeenCalledWith(1);
      expect(signalsOf(sendSignal, 'ANSWER')).toHaveLength(2);
    });

    it('replaces the connection when the OFFER carries a different session id', async () => {
      const { manager, sendSignal } = buildManager({ selfId: 2 });

      manager.handleSignal(offerFrom(1, 'sid-1'));
      await flush();
      manager.handleSignal(offerFrom(1, 'sid-2'));
      await flush();

      expect(FakePC.instances).toHaveLength(2);
      expect(signalsOf(sendSignal, 'ANSWER').map(([, , payload]) => payload.sid)).toEqual([
        'sid-1',
        'sid-2',
      ]);
    });

    it('replaces the connection that addPeer created while waiting for the OFFER', async () => {
      const { manager, sendSignal } = buildManager({ selfId: 2 });

      manager.addPeer(1); // tạo entry chờ OFFER (đã gửi READY)
      const waitingPc = FakePC.instances[0];
      manager.handleSignal(offerFrom(1, 'sid-1'));
      await flush();

      // sid của entry chờ được sinh ngẫu nhiên nên khác 'sid-1' => thay bằng entry mới theo sid của OFFER
      expect(waitingPc.close).toHaveBeenCalledTimes(1);
      expect(FakePC.instances).toHaveLength(2);
      expect(signalsOf(sendSignal, 'ANSWER')[0][2].sid).toBe('sid-1');
    });

    it('reports failed and sends no ANSWER when applying the remote OFFER fails', async () => {
      FakePC.failOn.add('setRemoteDescription');
      const { manager, sendSignal, onLinkState } = buildManager({ selfId: 2 });

      manager.handleSignal(offerFrom(1, 'sid-1'));
      await flush();

      expect(onLinkState).toHaveBeenLastCalledWith(1, 'failed');
      expect(signalsOf(sendSignal, 'ANSWER')).toHaveLength(0);
    });

    it('reports failed when creating the ANSWER fails', async () => {
      FakePC.failOn.add('createAnswer');
      const { manager, sendSignal, onLinkState } = buildManager({ selfId: 2 });

      manager.handleSignal(offerFrom(1, 'sid-1'));
      await flush();

      expect(onLinkState).toHaveBeenLastCalledWith(1, 'failed');
      expect(signalsOf(sendSignal, 'ANSWER')).toHaveLength(0);
    });

    it('does not throw when an OFFER arrives without payload', async () => {
      const { manager } = buildManager({ selfId: 2 });

      expect(() => manager.handleSignal({ fromUserId: 1, type: 'OFFER' })).not.toThrow();
      await flush();
    });
  });

  describe('handleSignal: ANSWER', () => {
    const answerFrom = (sid, desc = { type: 'answer', sdp: 'remote-answer' }) => ({
      fromUserId: 2,
      type: 'ANSWER',
      payload: { sid, desc },
    });

    it('applies the remote ANSWER to the matching session', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc, sid } = await startAsOfferer(ctx);
      const desc = { type: 'answer', sdp: 'remote-answer' };

      ctx.manager.handleSignal(answerFrom(sid, desc));
      await flush();

      expect(pc.setRemoteDescription).toHaveBeenCalledWith(desc);
    });

    it('accepts an ANSWER that carries no sid', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc } = await startAsOfferer(ctx);

      ctx.manager.handleSignal(answerFrom(undefined));
      await flush();

      expect(pc.setRemoteDescription).toHaveBeenCalledTimes(1);
    });

    it('ignores an ANSWER that belongs to an older session', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc } = await startAsOfferer(ctx);

      ctx.manager.handleSignal(answerFrom('stale-sid'));
      await flush();

      expect(pc.setRemoteDescription).not.toHaveBeenCalled();
    });

    it('ignores an ANSWER from an unknown peer', async () => {
      const { manager, onLinkState } = buildManager({ selfId: 1 });

      expect(() => manager.handleSignal(answerFrom('sid'))).not.toThrow();
      await flush();

      expect(FakePC.instances).toHaveLength(0);
      expect(onLinkState).not.toHaveBeenCalled();
    });

    it('reports failed when applying the ANSWER fails', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { sid } = await startAsOfferer(ctx);
      FakePC.failOn.add('setRemoteDescription');

      ctx.manager.handleSignal(answerFrom(sid));
      await flush();

      expect(ctx.onLinkState).toHaveBeenLastCalledWith(2, 'failed');
    });
  });

  describe('handleSignal: ICE', () => {
    const iceFrom = (sid, candidate) => ({
      fromUserId: 2,
      type: 'ICE',
      payload: { sid, candidate },
    });

    const applyAnswer = async (ctx, sid) => {
      ctx.manager.handleSignal({
        fromUserId: 2,
        type: 'ANSWER',
        payload: { sid, desc: { type: 'answer', sdp: 'remote-answer' } },
      });
      await flush();
    };

    it('queues candidates until the remote description is set, then flushes them in order', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc, sid } = await startAsOfferer(ctx);
      const first = { candidate: 'c1' };
      const second = { candidate: 'c2' };

      ctx.manager.handleSignal(iceFrom(sid, first));
      ctx.manager.handleSignal(iceFrom(sid, second));
      await flush();

      expect(pc.addIceCandidate).not.toHaveBeenCalled();

      await applyAnswer(ctx, sid);

      expect(pc.addIceCandidate.mock.calls.map(([candidate]) => candidate)).toEqual([
        first,
        second,
      ]);
      expect(pc.setRemoteDescription.mock.invocationCallOrder[0]).toBeLessThan(
        pc.addIceCandidate.mock.invocationCallOrder[0]
      );
    });

    it('adds candidates immediately once the remote description is set', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc, sid } = await startAsOfferer(ctx);
      await applyAnswer(ctx, sid);
      const candidate = { candidate: 'late' };

      ctx.manager.handleSignal(iceFrom(sid, candidate));
      await flush();

      expect(pc.addIceCandidate).toHaveBeenCalledTimes(1);
      expect(pc.addIceCandidate).toHaveBeenCalledWith(candidate);
    });

    it('queues candidates that carry no sid', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc, sid } = await startAsOfferer(ctx);
      const candidate = { candidate: 'no-sid' };

      ctx.manager.handleSignal(iceFrom(undefined, candidate));
      await flush();
      expect(pc.addIceCandidate).not.toHaveBeenCalled();

      await applyAnswer(ctx, sid);

      expect(pc.addIceCandidate).toHaveBeenCalledWith(candidate);
    });

    it('ignores candidates that belong to an older session', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc, sid } = await startAsOfferer(ctx);

      ctx.manager.handleSignal(iceFrom('stale-sid', { candidate: 'old' }));
      await flush();
      await applyAnswer(ctx, sid);

      expect(pc.addIceCandidate).not.toHaveBeenCalled();
    });

    it('ignores candidates from an unknown peer', async () => {
      const { manager } = buildManager({ selfId: 1 });

      expect(() => manager.handleSignal(iceFrom('sid', { candidate: 'x' }))).not.toThrow();
      await flush();

      expect(FakePC.instances).toHaveLength(0);
    });

    it('swallows invalid candidates without marking the link as failed', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc, sid } = await startAsOfferer(ctx);
      await applyAnswer(ctx, sid);
      FakePC.failOn.add('addIceCandidate');

      ctx.manager.handleSignal(iceFrom(sid, { candidate: 'broken' }));
      await flush();

      expect(pc.addIceCandidate).toHaveBeenCalledTimes(1);
      expect(ctx.onLinkState).not.toHaveBeenCalledWith(2, 'failed');
    });
  });

  describe('handleSignal: general', () => {
    it('ignores every signal that comes from itself', async () => {
      const { manager, sendSignal } = buildManager({ selfId: 1 });

      manager.handleSignal(offerFrom(1, 'sid-1'));
      manager.handleSignal(offerFrom('1', 'sid-1'));
      manager.handleSignal({ fromUserId: 1, type: 'READY' });
      await flush();

      expect(FakePC.instances).toHaveLength(0);
      expect(sendSignal).not.toHaveBeenCalled();
    });

    it('ignores unknown signal types', async () => {
      const { manager, sendSignal } = buildManager({ selfId: 1 });

      manager.handleSignal({ fromUserId: 2, type: 'WHATEVER', payload: {} });
      await flush();

      expect(FakePC.instances).toHaveLength(0);
      expect(sendSignal).not.toHaveBeenCalled();
    });
  });

  describe('peer connection events', () => {
    it('forwards local ICE candidates with the current session id', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc, sid } = await startAsOfferer(ctx);

      pc.onicecandidate({
        candidate: { toJSON: () => ({ candidate: 'cand-1', sdpMid: '0' }) },
      });

      expect(ctx.sendSignal).toHaveBeenLastCalledWith(2, 'ICE', {
        sid,
        candidate: { candidate: 'cand-1', sdpMid: '0' },
      });
    });

    it('does not forward the end-of-candidates marker', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc } = await startAsOfferer(ctx);

      pc.onicecandidate({ candidate: null });

      expect(signalsOf(ctx.sendSignal, 'ICE')).toHaveLength(0);
    });

    it('does not forward candidates from a connection that was removed', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc } = await startAsOfferer(ctx);
      const handler = pc.onicecandidate;

      ctx.manager.removePeer(2);
      handler({ candidate: { toJSON: () => ({ candidate: 'late' }) } });

      expect(signalsOf(ctx.sendSignal, 'ICE')).toHaveLength(0);
    });

    it('exposes the first remote stream through onRemoteStream', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc } = await startAsOfferer(ctx);
      const stream = { id: 'remote-stream' };

      pc.ontrack({ streams: [stream, { id: 'other' }] });

      expect(ctx.onRemoteStream).toHaveBeenCalledWith(2, stream);
    });

    it('ignores track events without a stream', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc } = await startAsOfferer(ctx);

      pc.ontrack({ streams: [] });

      expect(ctx.onRemoteStream).not.toHaveBeenCalled();
    });

    it('ignores tracks from a connection that was removed', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc } = await startAsOfferer(ctx);
      const handler = pc.ontrack;

      ctx.manager.removePeer(2);
      handler({ streams: [{ id: 'late' }] });

      expect(ctx.onRemoteStream).not.toHaveBeenCalled();
    });

    it.each([
      ['connected', 'connected'],
      ['failed', 'failed'],
      ['new', 'connecting'],
      ['connecting', 'connecting'],
      ['disconnected', 'connecting'],
      ['closed', 'connecting'],
    ])('maps the connection state %s to the link state %s', async (connectionState, expected) => {
      const ctx = buildManager({ selfId: 1 });
      const { pc } = await startAsOfferer(ctx);

      pc.connectionState = connectionState;
      pc.onconnectionstatechange();

      expect(ctx.onLinkState).toHaveBeenLastCalledWith(2, expected);
    });

    it('ignores state changes after the manager is destroyed', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc } = await startAsOfferer(ctx);
      const handler = pc.onconnectionstatechange;

      ctx.manager.destroy();
      ctx.onLinkState.mockClear();
      pc.connectionState = 'connected';
      handler();

      expect(ctx.onLinkState).not.toHaveBeenCalled();
    });
  });

  describe('removePeer', () => {
    it('closes the connection, detaches its handlers and notifies the listener', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc } = await startAsOfferer(ctx);

      ctx.manager.removePeer(2);

      expect(pc.close).toHaveBeenCalledTimes(1);
      expect(pc.onicecandidate).toBeNull();
      expect(pc.ontrack).toBeNull();
      expect(pc.onconnectionstatechange).toBeNull();
      expect(ctx.onRemoteRemoved).toHaveBeenCalledWith(2);
      expect(ctx.manager.has(2)).toBe(false);
    });

    it('does nothing for an unknown peer', () => {
      const { manager, onRemoteRemoved } = buildManager({ selfId: 1 });

      manager.removePeer(99);

      expect(onRemoteRemoved).not.toHaveBeenCalled();
    });
  });

  describe('closeAll', () => {
    it('closes every connection but keeps the manager usable', () => {
      const { manager, onRemoteRemoved } = buildManager({ selfId: 1 });
      manager.addPeer(2);
      manager.addPeer(3);

      manager.closeAll();

      FakePC.instances.forEach((pc) => expect(pc.close).toHaveBeenCalledTimes(1));
      expect(onRemoteRemoved).toHaveBeenCalledWith(2);
      expect(onRemoteRemoved).toHaveBeenCalledWith(3);
      expect(manager.has(2)).toBe(false);
      expect(manager.has(3)).toBe(false);

      manager.addPeer(4);

      expect(FakePC.instances).toHaveLength(3);
      expect(manager.has(4)).toBe(true);
    });
  });

  describe('destroy', () => {
    it('closes every connection', () => {
      const { manager } = buildManager({ selfId: 1 });
      manager.addPeer(2);
      manager.addPeer(3);

      manager.destroy();

      FakePC.instances.forEach((pc) => expect(pc.close).toHaveBeenCalledTimes(1));
      expect(manager.has(2)).toBe(false);
      expect(manager.has(3)).toBe(false);
    });

    it('rejects new work afterwards', async () => {
      const { manager, sendSignal } = buildManager({ selfId: 1 });
      manager.destroy();

      manager.addPeer(2);
      manager.handleSignal(offerFrom(2, 'sid-1'));
      manager.handleSignal({ fromUserId: 2, type: 'READY' });
      manager.restartPeer(2);
      await flush();

      expect(FakePC.instances).toHaveLength(0);
      expect(sendSignal).not.toHaveBeenCalled();
    });

    it('cancels an OFFER that has not been sent yet', async () => {
      const { manager, sendSignal } = buildManager({ selfId: 1 });

      manager.addPeer(2);
      manager.destroy();
      await flush();

      expect(signalsOf(sendSignal, 'OFFER')).toHaveLength(0);
    });
  });

  describe('restartPeer', () => {
    it('drops the old connection and OFFERs again when this side offers', async () => {
      const ctx = buildManager({ selfId: 1 });
      const { pc: first } = await startAsOfferer(ctx);

      ctx.manager.restartPeer(2);
      await flush();

      expect(first.close).toHaveBeenCalledTimes(1);
      expect(FakePC.instances).toHaveLength(2);
      expect(signalsOf(ctx.sendSignal, 'OFFER')).toHaveLength(2);
    });

    it('drops the old connection and sends READY again when this side answers', () => {
      const { manager, sendSignal } = buildManager({ selfId: 2 });
      manager.addPeer(1);
      const first = FakePC.instances[0];

      manager.restartPeer(1);

      expect(first.close).toHaveBeenCalledTimes(1);
      expect(FakePC.instances).toHaveLength(2);
      expect(signalsOf(sendSignal, 'READY')).toHaveLength(2);
    });

    it('ignores its own id', () => {
      const { manager } = buildManager({ selfId: 1 });

      manager.restartPeer(1);

      expect(FakePC.instances).toHaveLength(0);
    });
  });

  describe('has', () => {
    it('reports whether a connection exists for the peer', () => {
      const { manager } = buildManager({ selfId: 1 });

      expect(manager.has(2)).toBe(false);

      manager.addPeer(2);

      expect(manager.has(2)).toBe(true);
    });
  });
});
