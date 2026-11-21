import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { useAuth } from '../../../context/AuthContext';
import { getSocketStatus } from '../../../services/stompClient';
import { getIceConfigApi } from '../speakingApi';
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
} from '../speakingSocket';
import { createPeerManager } from '../webrtc/peerManager';

const SpeakingRoomContext = createContext(null);

export const useSpeakingRoom = () => {
  const ctx = useContext(SpeakingRoomContext);
  if (!ctx) throw new Error('useSpeakingRoom must be used within a SpeakingRoomProvider');
  return ctx;
};

const FATAL_ERRORS = [
  'SPEAKING_ROOM_ACCESS_DENIED',
  'SPEAKING_ROOM_CLOSED',
  'SPEAKING_ROOM_NOT_FOUND',
  'SPEAKING_ROOM_FULL',
  'SPEAKING_BANNED',
  'SPEAKING_ALREADY_IN_ROOM',
];

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

export const SpeakingRoomProvider = ({ children }) => {
  const { user } = useAuth();
  const selfId = user?.id; // Tên trường id từ AuthContext

  const [connection, setConnection] = useState(getSocketStatus);
  const [phase, setPhase] = useState('idle');
  const [room, setRoom] = useState(null);
  const [isPrivate, setIsPrivate] = useState(false);
  const [peers, setPeers] = useState({});
  const [streams, setStreams] = useState({});
  const [muted, setMuted] = useState(false);
  const [micError, setMicError] = useState(null);
  const [error, setError] = useState(null);
  const [closedReason, setClosedReason] = useState(null);
  const [notice, setNotice] = useState(null); // { type: 'hostChanged', userId } | null
  const [kicked, setKicked] = useState(false);
  const [summary, setSummary] = useState(null);

  const MAX_CHAT_KEPT = 200;
  const [chatMessages, setChatMessages] = useState([]); // [{ id, fromUserId, displayName, text, timestamp }]
  const [chatUnread, setChatUnread] = useState(0);
  const [chatOpen, setChatOpenState] = useState(false);
  const chatOpenRef = useRef(false);
  const chatSeqRef = useRef(0);
  const selfIdRef = useRef(selfId);

  const sessionRef = useRef(0); // token phiên vào phòng
  const roomCodeRef = useRef(null);
  const localStreamRef = useRef(null);
  const managerRef = useRef(null);
  const unsubRef = useRef(null);
  const joinTimerRef = useRef(null);
  const leaveWaiterRef = useRef(null); // resolver của Promise chờ event 'room:left' khi bấm leave
  const replyWaiterRef = useRef(null); // resolver cho sendAndWait (transfer-host, end)
  const gotStateRef = useRef(false);
  const peersRef = useRef(peers);
  const retryRef = useRef({}); // userId -> số lần đã thử kết nối WebRTC
  const mutedRef = useRef(muted);
  const joinedAtMsRef = useRef(null);

  useEffect(() => {
    mutedRef.current = muted;
  }, [muted]);

  useEffect(() => {
    selfIdRef.current = selfId;
  }, [selfId]);

  useEffect(() => onStatus(setConnection), []);

  useEffect(() => {
    peersRef.current = peers;
  }, [peers]);

  // Tự tắt thông báo sau 4s
  useEffect(() => {
    if (!notice) return undefined;
    const id = setTimeout(() => setNotice(null), 4000);
    return () => clearTimeout(id);
  }, [notice]);

  // Resync khi STOMP rớt rồi nối lại
  const prevConnRef = useRef(connection);
  useEffect(() => {
    const was = prevConnRef.current;
    prevConnRef.current = connection;
    const code = roomCodeRef.current;
    if (
      connection === 'connected' &&
      was !== 'connected' &&
      code &&
      (phase === 'in' || phase === 'connecting')
    ) {
      // stompClient đã tự đăng ký lại subscription -> Không subscribe lại ở đây
      // Chỉ gửi lại join (idempotent); BE trả room:state
      gotStateRef.current = false;
      sendJoin(code);
    }
  }, [connection, phase]);

  // Helper gửi lệnh STOMP và chờ event tương ứng hoặc error
  const sendAndWait = useCallback((send, okPred, timeoutMs = 4000) => {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        replyWaiterRef.current = null;
        reject({ code: 'TIMEOUT' });
      }, timeoutMs);
      replyWaiterRef.current = (event, data) => {
        if (event === 'error') {
          clearTimeout(timer);
          replyWaiterRef.current = null;
          reject({ code: data.code });
        } else if (okPred(event, data)) {
          clearTimeout(timer);
          replyWaiterRef.current = null;
          resolve(data);
        }
      };
      if (!send()) {
        clearTimeout(timer);
        replyWaiterRef.current = null;
        reject({ code: 'SOCKET_NOT_CONNECTED' });
      }
    });
  }, []);

  const applyMuted = useCallback((next) => {
    setMuted(next);
    mutedRef.current = next;
    localStreamRef.current?.getAudioTracks().forEach((t) => {
      t.enabled = !next;
    });
    const code = roomCodeRef.current;
    if (code) sendMic(code, next);
  }, []);

  const toggleMute = useCallback(() => {
    applyMuted(!mutedRef.current);
  }, [applyMuted]);

  const stopLocal = () => {
    localStreamRef.current?.getTracks().forEach((t) => t.stop());
    localStreamRef.current = null;
  };

  // Dọn mọi thứ phía client (không gửi leave)
  const teardown = useCallback(() => {
    sessionRef.current += 1;
    clearTimeout(joinTimerRef.current);
    unsubRef.current?.();
    unsubRef.current = null;
    managerRef.current?.destroy();
    managerRef.current = null;
    stopLocal();
    roomCodeRef.current = null;
    gotStateRef.current = false;
    replyWaiterRef.current = null;
    retryRef.current = {};
    joinedAtMsRef.current = null;
    setNotice(null);
    setStreams({});
    setIsPrivate(false);
    setChatMessages([]);
    setChatUnread(0);
    setChatOpenState(false);
    chatOpenRef.current = false;
  }, []);

  const handleEvent = useCallback((envelope) => {
    if (!envelope?.event) return;
    const { event, data } = envelope;
    replyWaiterRef.current?.(event, data);
    const manager = managerRef.current;
    switch (event) {
      case 'room:state': {
        if (joinedAtMsRef.current == null) {
          joinedAtMsRef.current = Date.now();
        }
        gotStateRef.current = true;
        setIsPrivate(Boolean(data.isPrivate));
        clearTimeout(joinTimerRef.current);
        manager?.closeAll();
        setStreams({});
        setRoom({
          roomCode: data.roomCode,
          topic: data.topic,
          hostId: data.hostId,
          maxParticipants: data.maxParticipants,
          joinedAtMs: joinedAtMsRef.current,
        });
        const mutedIds = Array.isArray(data.mutedUserIds) ? data.mutedUserIds : [];
        setPeers(
          Object.fromEntries(
            data.peers.map((p) => [
              p.userId,
              {
                ...p,
                link: String(p.userId) === String(selfId) ? undefined : 'connecting',
                grace: false,
                micMuted: mutedIds.includes(p.userId),
              },
            ])
          )
        );
        data.peers.forEach((p) => manager?.addPeer(p.userId));
        setPhase('in');
        // Báo trạng thái mic của máy mình sau mỗi lần vào/nối lại
        sendMic(data.roomCode, mutedRef.current);
        break;
      }
      case 'peer:joined':
        setPeers((prev) => ({
          ...prev,
          [data.userId]: { ...data, link: 'connecting', grace: false, micMuted: false },
        }));
        manager?.addPeer(data.userId);
        break;
      case 'peer:left':
        manager?.removePeer(data.userId);
        setPeers((prev) =>
          Object.fromEntries(Object.entries(prev).filter(([uid]) => String(uid) !== String(data.userId)))
        );
        break;
      case 'peer:disconnected':
        setPeers((prev) =>
          prev[data.userId] ? { ...prev, [data.userId]: { ...prev[data.userId], grace: true } } : prev
        );
        break;
      case 'peer:reconnected':
        // Người đó đã vào lại: bỏ cờ grace và dựng lại kết nối WebRTC từ đầu
        setPeers((prev) => ({
          ...prev,
          [data.userId]: {
            ...prev[data.userId],
            ...data,
            link: 'connecting',
            grace: false,
            micMuted: false,
          },
        }));
        manager?.restartPeer(data.userId);
        break;
      case 'peer:mic':
        setPeers((prev) =>
          prev[data.userId]
            ? { ...prev, [data.userId]: { ...prev[data.userId], micMuted: data.muted } }
            : prev
        );
        break;
      case 'mute:requested':
        applyMuted(true);
        setNotice({ type: 'mutedByHost', userId: data.byUserId });
        break;
      case 'host:changed':
        setRoom((prev) => (prev ? { ...prev, hostId: data.hostId } : prev));
        setPeers((prev) =>
          Object.fromEntries(
            Object.entries(prev).map(([id, p]) => [
              id,
              { ...p, role: String(p.userId) === String(data.hostId) ? 'HOST' : 'GUEST' },
            ])
          )
        );
        setNotice({ type: 'hostChanged', userId: data.hostId });
        break;
      case 'chat:message': {
        const msg = { id: ++chatSeqRef.current, ...data };
        setChatMessages((prev) => {
          const next = [...prev, msg];
          return next.length > MAX_CHAT_KEPT ? next.slice(next.length - MAX_CHAT_KEPT) : next;
        });
        if (!chatOpenRef.current && String(data.fromUserId) !== String(selfIdRef.current)) {
          setChatUnread((n) => n + 1);
        }
        break;
      }
      case 'signal':
        manager?.handleSignal(data);
        break;
      case 'room:left':
        leaveWaiterRef.current?.(data);
        break;
      case 'room:kicked':
        // Chỉ người bị kick nhận event này
        teardown();
        setKicked(true);
        setPhase('closed');
        setClosedReason('KICKED');
        break;
      case 'room:closed':
        teardown();
        setClosedReason(data.reason);
        setPhase('closed');
        break;
      case 'error':
        setError(data.code);
        if (FATAL_ERRORS.includes(data.code)) {
          teardown();
          setPhase('error');
        }
        break;
      default:
        break;
    }
  }, [applyMuted, selfId, teardown]);

  const startSession = useCallback(async (roomCode) => {
    teardown();
    setSummary(null);
    const session = sessionRef.current;
    const alive = () => sessionRef.current === session;
    roomCodeRef.current = roomCode;
    joinedAtMsRef.current = null;
    setPhase('preparing');
    setRoom(null);
    setPeers({});
    setStreams({});
    setError(null);
    setMicError(null);
    setClosedReason(null);
    setMuted(false);
    setKicked(false);

    let stream;
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
      });
    } catch (e) {
      if (!alive()) return;
      setMicError(
        e?.name === 'NotAllowedError' || e?.name === 'SecurityError' ? 'MIC_DENIED' : 'MIC_UNAVAILABLE'
      );
      setPhase('error');
      return;
    }
    if (!alive()) {
      stream.getTracks().forEach((t) => t.stop());
      return;
    }
    localStreamRef.current = stream;

    try {
      const ice = await getIceConfigApi();
      if (!alive()) return;
      managerRef.current = createPeerManager({
        selfId,
        iceServers: ice?.data?.iceServers || [],
        localStream: stream,
        sendSignal: (to, type, payload) => sendSignal(roomCode, to, type, payload),
        onLinkState: (uid, link) => {
          setPeers((prev) => (prev[uid] ? { ...prev, [uid]: { ...prev[uid], link } } : prev));
          if (link === 'connected') {
            retryRef.current[uid] = 0;
            return;
          }
          if (link === 'failed' && (retryRef.current[uid] ?? 0) < 2) {
            retryRef.current[uid] = (retryRef.current[uid] ?? 0) + 1;
            setTimeout(() => {
              if (
                sessionRef.current === session &&
                managerRef.current &&
                peersRef.current[uid] &&
                !peersRef.current[uid].grace
              ) {
                managerRef.current.restartPeer(uid);
              }
            }, 2000);
          }
        },
        onRemoteStream: (uid, s) => setStreams((prev) => ({ ...prev, [uid]: s })),
        onRemoteRemoved: (uid) =>
          setStreams((prev) =>
            Object.fromEntries(Object.entries(prev).filter(([key]) => String(key) !== String(uid)))
          ),
      });
      unsubRef.current = subscribeSpeaking(handleEvent);
      setPhase('connecting');
      await sleep(200);
      if (!alive()) return;
      const tryJoin = (n) => {
        if (!alive() || gotStateRef.current) return;
        sendJoin(roomCode);
        if (n < 3) joinTimerRef.current = setTimeout(() => tryJoin(n + 1), 2000);
      };
      tryJoin(1);
    } catch (e) {
      if (!alive()) return;
      stopLocal();
      setError(e?.response?.data?.code ?? e?.code ?? 'UNKNOWN');
      setPhase('error');
    }
  }, [handleEvent, selfId, teardown]);

  const requestMute = useCallback((targetUserId) => {
    const code = roomCodeRef.current;
    if (code) sendMute(code, targetUserId);
  }, []);

  // Bấm "Rời phòng": gửi STOMP 'leave', chờ event 'room:left' (tối đa 3s), rồi dọn cục bộ
  const leaveRoom = useCallback(async () => {
    const code = roomCodeRef.current;
    setPhase('leaving');
    let summary = null;
    if (code) {
      summary = await new Promise((resolve) => {
        const timer = setTimeout(() => {
          leaveWaiterRef.current = null;
          resolve(null);
        }, 3000);
        leaveWaiterRef.current = (data) => {
          clearTimeout(timer);
          leaveWaiterRef.current = null;
          resolve(data);
        };
        if (!sendLeave()) {
          clearTimeout(timer);
          leaveWaiterRef.current = null;
          resolve(null);
        }
      });
    }
    teardown();
    if (summary) setSummary(summary);
    return summary;
  }, [teardown]);

  // Host chuyển quyền rồi rời. Trả summary hoặc null như leaveRoom
  const transferHostAndLeave = useCallback(
    async (newHostId) => {
      const code = roomCodeRef.current;
      await sendAndWait(
        () => sendTransferHost(code, newHostId),
        (ev, d) => ev === 'host:changed' && String(d.hostId) === String(newHostId)
      );
      return leaveRoom();
    },
    [leaveRoom, sendAndWait]
  );

  // Host kết thúc phòng cho tất cả: gửi 'end', host nhận 'room:left' (tóm tắt)
  const endRoom = useCallback(async () => {
    const code = roomCodeRef.current;
    const data = await sendAndWait(() => sendEnd(code), (ev) => ev === 'room:left', 4000);
    setPhase('leaving');
    teardown();
    if (data) setSummary(data);
    return data;
  }, [sendAndWait, teardown]);

  const kickMember = useCallback(
    async (targetUserId) => {
      const code = roomCodeRef.current;
      await sendAndWait(
        () => sendKick(code, targetUserId),
        (ev, d) =>
          ev === 'peer:left' &&
          d.reason === 'KICKED' &&
          String(d.userId) === String(targetUserId)
      );
    },
    [sendAndWait]
  );

  const unbanMember = useCallback(
    async (targetUserId) => {
      const code = roomCodeRef.current;
      await sendAndWait(
        () => sendUnban(code, targetUserId),
        (ev, d) => ev === 'ban:removed' && String(d.userId) === String(targetUserId)
      );
    },
    [sendAndWait]
  );

  const setChatOpen = useCallback((open) => {
    chatOpenRef.current = open;
    setChatOpenState(open);
    if (open) setChatUnread(0);
  }, []);

  const sendChatMessage = useCallback((rawText) => {
    const text = (rawText || '').trim();
    const code = roomCodeRef.current;
    if (!text || !code) return false;
    sendChat(code, text.slice(0, 500));
    return true;
  }, []);

  const isHost = Boolean(room && String(room.hostId) === String(selfId));

  const value = {
    selfId,
    connection,
    phase,
    room,
    peers,
    streams,
    muted,
    micError,
    error,
    closedReason,
    notice,
    setNotice,
    isHost,
    kicked,
    summary,
    clearSummary: () => setSummary(null),
    requestMute,
    kickMember,
    unbanMember,
    transferHostAndLeave,
    endRoom,
    startSession,
    leaveRoom,
    teardown,
    toggleMute,
    isPrivate,
    chatMessages,
    chatUnread,
    chatOpen,
    setChatOpen,
    sendChatMessage,
  };

  return (
    <SpeakingRoomContext.Provider value={value}>
      {children}
    </SpeakingRoomContext.Provider>
  );
};
