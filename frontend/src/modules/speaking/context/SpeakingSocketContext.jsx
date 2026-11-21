import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { getSocketStatus } from '../../../services/stompClient';
import {
  subscribeSpeaking,
  onStatus,
  sendCreate,
  sendJoin,
  sendLeave,
  sendActiveRoom,
} from '../speakingSocket';

const SpeakingSocketContext = createContext(null);

export const useSpeakingSocket = () => {
  const ctx = useContext(SpeakingSocketContext);
  if (!ctx) throw new Error('useSpeakingSocket must be used within a SpeakingSocketProvider');
  return ctx;
};

export const SpeakingSocketProvider = ({ children }) => {
  const [connection, setConnection] = useState(getSocketStatus);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [roomState, setRoomState] = useState(null);
  const [leftSummary, setLeftSummary] = useState(null);
  const [createdRoomCode, setCreatedRoomCode] = useState(null);
  const [activeRoomCode, setActiveRoomCode] = useState(null);
  const [activeRoomChecked, setActiveRoomChecked] = useState(false);

  useEffect(() => onStatus(setConnection), []);

  // Khi connection rời khỏi 'connected' thì giải phóng busy để tránh kẹt nút ở UI
  useEffect(() => {
    if (connection !== 'connected') {
      setBusy(false);
    }
  }, [connection]);

  useEffect(() => {
    return subscribeSpeaking((envelope) => {
      if (!envelope?.event) return;
      const { event, data } = envelope;
      switch (event) {
        case 'room:active':
          setActiveRoomCode(data?.roomCode ?? null);
          setActiveRoomChecked(true);
          break;
        case 'room:created':
          setBusy(false);
          setCreatedRoomCode(data.roomCode);
          setActiveRoomCode(data.roomCode);
          break;
        case 'room:state':
          setBusy(false);
          setRoomState(data);
          setActiveRoomCode(data.roomCode);
          break;
        case 'room:left':
          setBusy(false);
          setRoomState(null);
          setLeftSummary(data);
          setActiveRoomCode(null);
          break;
        case 'room:closed':
        case 'room:kicked':
          setActiveRoomCode(null);
          break;
        case 'peer:joined':
          setRoomState((prev) =>
            prev ? { ...prev, peers: [...prev.peers.filter((p) => p.userId !== data.userId), data] } : prev
          );
          break;
        case 'peer:left':
          setRoomState((prev) =>
            prev ? { ...prev, peers: prev.peers.filter((p) => p.userId !== data.userId) } : prev
          );
          break;
        case 'error':
          setBusy(false);
          setError(data.code);
          break;
        default:
          break;
      }
    });
  }, []);

  const createRoom = useCallback((payload) => {
    setError(null);
    setLeftSummary(null);
    setBusy(true);
    if (!sendCreate(payload)) {
      setBusy(false);
      setError('SOCKET_NOT_CONNECTED');
    }
  }, []);

  const joinRoom = useCallback((code) => {
    setError(null);
    setLeftSummary(null);
    setBusy(true);
    if (!sendJoin(code)) {
      setBusy(false);
      setError('SOCKET_NOT_CONNECTED');
    }
  }, []);

  const leaveRoom = useCallback(() => {
    setError(null);
    setBusy(true);
    if (!sendLeave()) {
      setBusy(false);
      setError('SOCKET_NOT_CONNECTED');
    }
  }, []);

  const checkActiveRoom = useCallback(() => {
    sendActiveRoom();
  }, []);

  const clearError = useCallback(() => setError(null), []);
  const clearCreated = useCallback(() => setCreatedRoomCode(null), []);

  const value = {
    connection,
    busy,
    error,
    roomState,
    leftSummary,
    createdRoomCode,
    activeRoomCode,
    activeRoomChecked,
    createRoom,
    joinRoom,
    leaveRoom,
    checkActiveRoom,
    clearError,
    clearCreated,
  };

  return (
    <SpeakingSocketContext.Provider value={value}>
      {children}
    </SpeakingSocketContext.Provider >
  );
};
