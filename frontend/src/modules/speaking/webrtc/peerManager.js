// Server gửi userId dạng số (Long). So sánh số nếu cả hai là số, ngược lại so chuỗi.
export const shouldOffer = (selfId, otherId) => {
  const a = Number(selfId);
  const b = Number(otherId);
  if (!Number.isNaN(a) && !Number.isNaN(b)) return a < b;
  return String(selfId) < String(otherId);
};

const mapLinkState = (connectionState) => {
  if (connectionState === 'connected') return 'connected';
  if (connectionState === 'failed') return 'failed';
  return 'connecting'; // new | connecting | disconnected
};

const generateSid = () => {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return Math.random().toString(36).substring(2) + Date.now().toString(36);
};

/**
 * @param {object} opts
 * @param {number|string} opts.selfId
 * @param {RTCIceServer[]} opts.iceServers
 * @param {MediaStream} opts.localStream
 * @param {(toUserId, type, payload) => void} opts.sendSignal // type: 'OFFER' | 'ANSWER' | 'ICE' | 'READY'
 * @param {(userId, state) => void} opts.onLinkState // 'connecting' | 'connected' | 'failed'
 * @param {(userId, stream) => void} opts.onRemoteStream
 * @param {(userId) => void} opts.onRemoteRemoved
 */
export const createPeerManager = ({
  selfId,
  iceServers,
  localStream,
  sendSignal,
  onLinkState,
  onRemoteStream,
  onRemoteRemoved,
}) => {
  const peers = new Map(); // userId -> { pc, sid, pendingIce, remoteSet, chain }
  let closed = false;

  const enqueue = (entry, task) => {
    entry.chain = entry.chain.then(task).catch(() => {
      // lỗi được báo qua onLinkState
    });
    return entry.chain;
  };

  const flushIce = async (entry) => {
    const queued = entry.pendingIce.splice(0);
    for (const candidate of queued) {
      try {
        await entry.pc.addIceCandidate(candidate);
      } catch {
        // candidate cũ/không hợp lệ: bỏ qua
      }
    }
  };

  const removePeerInternal = (userId) => {
    const entry = peers.get(userId);
    if (!entry) return;
    peers.delete(userId);
    entry.pc.onicecandidate = null;
    entry.pc.ontrack = null;
    entry.pc.onconnectionstatechange = null;
    entry.pc.close();
    onRemoteRemoved(userId);
  };

  const create = (userId, sid) => {
    // Nếu đã có entry cũ, đóng kết nối cũ trước
    if (peers.has(userId)) {
      removePeerInternal(userId);
    }

    const pc = new RTCPeerConnection({ iceServers });
    const entry = {
      pc,
      sid: sid || generateSid(),
      pendingIce: [],
      remoteSet: false,
      chain: Promise.resolve(),
    };
    localStream.getTracks().forEach((track) => pc.addTrack(track, localStream));

    pc.onicecandidate = (event) => {
      if (event.candidate && !closed && peers.get(userId) === entry) {
        sendSignal(userId, 'ICE', {
          sid: entry.sid,
          candidate: event.candidate.toJSON(),
        });
      }
    };
    pc.ontrack = (event) => {
      const [stream] = event.streams;
      if (stream && peers.get(userId) === entry) onRemoteStream(userId, stream);
    };
    pc.onconnectionstatechange = () => {
      if (closed || peers.get(userId) !== entry) return;
      onLinkState(userId, mapLinkState(pc.connectionState));
    };
    peers.set(userId, entry);
    onLinkState(userId, 'connecting');
    return entry;
  };

  const ensure = (userId, sid) => peers.get(userId) ?? create(userId, sid);

  const makeOffer = (userId, entry) =>
    enqueue(entry, async () => {
      if (closed || peers.get(userId) !== entry) return;
      try {
        entry.sid = generateSid(); // Bên OFFER sinh sid mới mỗi lần tạo OFFER
        const offer = await entry.pc.createOffer();
        await entry.pc.setLocalDescription(offer);
        sendSignal(userId, 'OFFER', {
          sid: entry.sid,
          desc: entry.pc.localDescription.toJSON(),
        });
      } catch {
        onLinkState(userId, 'failed');
      }
    });

  // Có người trong phòng (room:state / peer:joined)
  const addPeer = (userId) => {
    if (closed || String(userId) === String(selfId)) return;
    const existed = peers.has(userId);
    const isOfferer = shouldOffer(selfId, userId);

    if (isOfferer) {
      const entry = ensure(userId);
      if (!existed) {
        makeOffer(userId, entry);
      }
    } else {
      // Bên id lớn hơn (không tạo OFFER): tạo mới entry (nếu chưa có) và gửi READY
      if (!existed) {
        create(userId);
        sendSignal(userId, 'READY', {});
      }
    }
  };

  const handleReadySignal = (fromUserId) => {
    if (!shouldOffer(selfId, fromUserId)) return;

    removePeerInternal(fromUserId);
    const newEntry = create(fromUserId);
    makeOffer(fromUserId, newEntry);
  };

  const handleOfferSignal = (fromUserId, sid, desc) => {
    let entry = peers.get(fromUserId);
    if (!entry || entry.sid !== sid || entry.remoteSet) {
      removePeerInternal(fromUserId);
      entry = create(fromUserId, sid);
    }

    enqueue(entry, async () => {
      if (closed || peers.get(fromUserId) !== entry) return;
      try {
        entry.sid = sid;
        await entry.pc.setRemoteDescription(desc);
        entry.remoteSet = true;
        await flushIce(entry);
        const answer = await entry.pc.createAnswer();
        await entry.pc.setLocalDescription(answer);
        sendSignal(fromUserId, 'ANSWER', {
          sid: entry.sid,
          desc: entry.pc.localDescription.toJSON(),
        });
      } catch {
        onLinkState(fromUserId, 'failed');
      }
    });
  };

  const handleAnswerSignal = (fromUserId, sid, desc) => {
    const entry = peers.get(fromUserId);
    if (!entry || (sid && entry.sid !== sid)) return;

    enqueue(entry, async () => {
      if (closed || peers.get(fromUserId) !== entry) return;
      try {
        await entry.pc.setRemoteDescription(desc);
        entry.remoteSet = true;
        await flushIce(entry);
      } catch {
        onLinkState(fromUserId, 'failed');
      }
    });
  };

  const handleIceSignal = (fromUserId, sid, candidate) => {
    const entry = peers.get(fromUserId);
    if (!entry || (sid && entry.sid !== sid)) return;

    enqueue(entry, async () => {
      if (closed || peers.get(fromUserId) !== entry) return;
      if (!entry.remoteSet) {
        entry.pendingIce.push(candidate);
        return;
      }

      try {
        await entry.pc.addIceCandidate(candidate);
      } catch {
        // bỏ qua candidate không hợp lệ
      }
    });
  };

  const handleSignal = ({ fromUserId, type, payload }) => {
    if (closed || String(fromUserId) === String(selfId)) return;

    const { sid, desc, candidate } = payload || {};
    const signalHandlers = new Map([
      ['READY', () => handleReadySignal(fromUserId)],
      ['OFFER', () => handleOfferSignal(fromUserId, sid, desc)],
      ['ANSWER', () => handleAnswerSignal(fromUserId, sid, desc)],
      ['ICE', () => handleIceSignal(fromUserId, sid, candidate)],
    ]);

    signalHandlers.get(type)?.();
  };

  const removePeer = (userId) => {
    removePeerInternal(userId);
  };

  // Dựng lại kết nối với 1 người (dùng khi họ quay lại hoặc liên kết hỏng) theo quy tắc glare
  const restartPeer = (userId) => {
    if (closed || String(userId) === String(selfId)) return;
    removePeer(userId);
    addPeer(userId);
  };

  const has = (userId) => peers.has(userId);

  const closeAll = () => {
    [...peers.keys()].forEach(removePeerInternal);
  };

  const destroy = () => {
    closeAll();
    closed = true;
  };

  return { addPeer, handleSignal, removePeer, closeAll, destroy, restartPeer, has };
};
