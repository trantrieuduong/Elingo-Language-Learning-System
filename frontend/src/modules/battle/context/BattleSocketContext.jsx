import { createContext, useContext, useEffect, useRef, useState } from 'react';
import {
  connect,
  disconnect,
  onMessage,
  onStatus,
  send,
} from '../battleSocket';

const BattleSocketContext = createContext(null);

export const useBattleSocket = () => {
  const context = useContext(BattleSocketContext);
  if (!context) {
    throw new Error('useBattleSocket must be used within a BattleSocketProvider');
  }
  return context;
};

export const BattleSocketProvider = ({ children }) => {
  const [connection, setConnection] = useState('connecting');
  const [status, setStatus] = useState('idle'); // 'idle' | 'searching' | 'starting' | 'playing' | 'roundResult' | 'finished' | 'cancelled'
  const [mode, setMode] = useState('MULTIPLE_CHOICE');
  const [match, setMatch] = useState(null);
  const [error, setError] = useState(null);

  // State cho trận đấu
  const [question, setQuestion] = useState(null);
  const [serverOffsetMs, setServerOffsetMs] = useState(0);
  const [answered, setAnswered] = useState(false);
  const [opponentAnswered, setOpponentAnswered] = useState(false);
  const [roundResult, setRoundResult] = useState(null);
  const [scores, setScores] = useState({});
  const [finished, setFinished] = useState(null);
  const [cancelReason, setCancelReason] = useState(null);

  // Lưu giá trị mới nhất để handler luôn đọc đúng state hiện tại
  const matchRef = useRef(null);
  const questionRef = useRef(null);
  const answeredRef = useRef(false);

  useEffect(() => {
    matchRef.current = match;
  }, [match]);

  useEffect(() => {
    questionRef.current = question;
  }, [question]);

  useEffect(() => {
    answeredRef.current = answered;
  }, [answered]);

  useEffect(() => {
    const handleEvent = (envelope) => {
      if (!envelope || !envelope.event) return;
      const { event, data } = envelope;

      switch (event) {
        case 'queue:joined':
          setStatus('searching');
          break;
        case 'queue:left':
          setStatus('idle');
          break;
        case 'queue:timeout':
          setStatus('idle');
          setError('QUEUE_TIMEOUT');
          break;
        case 'match:found': {
          const foundMatch = { ...data, foundAt: Date.now() };
          matchRef.current = foundMatch;
          setMatch(foundMatch);
          if (data?.mode) {
            setMode(data.mode);
          }
          // Khởi tạo điểm ban đầu = 0 cho các players
          if (Array.isArray(data?.players)) {
            const initialScores = {};
            data.players.forEach((p) => {
              initialScores[p.userId] = 0;
            });
            setScores(initialScores);
          } else {
            setScores({});
          }
          setRoundResult(null);
          setQuestion(null);
          setFinished(null);
          setCancelReason(null);
          setAnswered(false);
          setOpponentAnswered(false);
          setStatus('starting');
          break;
        }
        case 'match:question': {
          if (matchRef.current && data?.matchId && String(data.matchId) !== String(matchRef.current.matchId)) {
            return;
          }
          questionRef.current = data;
          answeredRef.current = false;
          setQuestion(data);
          setServerOffsetMs(data.serverNow - Date.now());
          setRoundResult(null);
          setAnswered(false);
          setOpponentAnswered(false);
          setError(null);
          setStatus('playing');
          break;
        }
        case 'match:opponent-answered': {
          if (matchRef.current && data?.matchId && String(data.matchId) !== String(matchRef.current.matchId)) {
            return;
          }
          setOpponentAnswered(true);
          break;
        }
        case 'match:round-result': {
          if (matchRef.current && data?.matchId && String(data.matchId) !== String(matchRef.current.matchId)) {
            return;
          }
          setRoundResult(data);
          if (Array.isArray(data?.results)) {
            setScores(Object.fromEntries(data.results.map((r) => [r.userId, r.score])));
          }
          setStatus('roundResult');
          break;
        }
        case 'match:finished': {
          if (matchRef.current && data?.matchId && String(data.matchId) !== String(matchRef.current.matchId)) {
            return;
          }
          setFinished(data);
          if (Array.isArray(data?.players)) {
            setScores(Object.fromEntries(data.players.map((p) => [p.userId, p.score])));
          }
          setStatus('finished');
          break;
        }
        case 'match:cancelled': {
          if (matchRef.current && data?.matchId && String(data.matchId) !== String(matchRef.current.matchId)) {
            return;
          }
          setCancelReason(data?.reason || null);
          setStatus('cancelled');
          break;
        }
        case 'error': {
          if (data?.code) {
            setError(data.code);
          }
          setStatus((prev) => (prev === 'searching' ? 'idle' : prev));
          break;
        }
        default:
          break;
      }
    };

    const offStatus = onStatus(setConnection);
    const offMsg = onMessage(handleEvent);
    connect();

    return () => {
      offMsg();
      offStatus();
      disconnect();
    };
  }, []);

  const joinQueue = (newMode) => {
    setError(null);
    if (newMode) {
      setMode(newMode);
    }
    const success = send('/app/battle/queue/join', { mode: newMode || mode });
    if (!success) {
      setError('BATTLE_NOT_CONNECTED');
    }
  };

  const leaveQueue = () => {
    send('/app/battle/queue/leave');
  };

  // Gửi đáp án lên server, trả true nếu gửi thành công
  const sendAnswer = (payload) => {
    const m = matchRef.current;
    const q = questionRef.current;
    if (!m || !q || answeredRef.current) return false;
    const ok = send('/app/battle/answer', {
      matchId: m.matchId,
      questionId: q.questionId,
      ...payload,
    });
    if (ok) {
      answeredRef.current = true;
      setAnswered(true);
    }
    return ok;
  };

  const exitBattle = () => {
    setStatus('idle');
    setMatch(null);
    setError(null);
    setQuestion(null);
    setServerOffsetMs(0);
    setAnswered(false);
    setOpponentAnswered(false);
    setRoundResult(null);
    setScores({});
    setFinished(null);
    setCancelReason(null);
    matchRef.current = null;
    questionRef.current = null;
    answeredRef.current = false;
  };

  const clearError = () => {
    setError(null);
  };

  const value = {
    connection,
    status,
    mode,
    match,
    error,
    question,
    serverOffsetMs,
    answered,
    opponentAnswered,
    roundResult,
    scores,
    finished,
    cancelReason,
    joinQueue,
    leaveQueue,
    sendAnswer,
    exitBattle,
    clearError,
  };

  return (
    <BattleSocketContext.Provider value={value}>
      {children}
    </BattleSocketContext.Provider>
  );
};
