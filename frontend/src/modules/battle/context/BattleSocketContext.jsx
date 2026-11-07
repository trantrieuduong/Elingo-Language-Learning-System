import { createContext, useContext, useEffect, useRef, useState } from 'react';
import { subscribeBattle, onStatus, send } from '../battleSocket';
import { getSocketStatus } from '../../../services/stompClient';

const BattleSocketContext = createContext(null);

export const useBattleSocket = () => {
  const context = useContext(BattleSocketContext);
  if (!context) {
    throw new Error('useBattleSocket must be used within a BattleSocketProvider');
  }
  return context;
};

export const BattleSocketProvider = ({ children }) => {
  const [connection, setConnection] = useState(getSocketStatus);
  const [status, setStatus] = useState('idle'); // 'idle' | 'searching' | 'roomWaiting' | 'starting' | 'playing' | 'roundResult' | 'finished' | 'cancelled'
  const [mode, setMode] = useState('MULTIPLE_CHOICE');
  const [match, setMatch] = useState(null);
  const [error, setError] = useState(null);

  // State cho phòng & khôi phục trận & mất kết nối
  const [roomCode, setRoomCode] = useState(null);
  const [activeMatchId, setActiveMatchId] = useState(null);
  const [activeMatchChecked, setActiveMatchChecked] = useState(false);
  const [opponentDisconnected, setOpponentDisconnected] = useState(null); // null | { graceMs, at }

  // State cho diễn biến trận đấu
  const [question, setQuestion] = useState(null);
  const [serverOffsetMs, setServerOffsetMs] = useState(0);
  const [answered, setAnswered] = useState(false);
  const [opponentAnswered, setOpponentAnswered] = useState(false);
  const [roundResult, setRoundResult] = useState(null);
  const [scores, setScores] = useState({});
  const [finished, setFinished] = useState(null);
  const [cancelReason, setCancelReason] = useState(null);

  // Refs tránh stale closure và lưu trạng thái tức thì
  const matchRef = useRef(null);
  const questionRef = useRef(null);
  const answeredRef = useRef(false);
  const statusRef = useRef('idle');
  const finishedRef = useRef(false);
  const rejoiningRef = useRef(false);

  const activeMatchIdRef = useRef(null);

  useEffect(() => {
    matchRef.current = match;
  }, [match]);

  useEffect(() => {
    activeMatchIdRef.current = activeMatchId;
  }, [activeMatchId]);

  useEffect(() => {
    questionRef.current = question;
  }, [question]);

  useEffect(() => {
    answeredRef.current = answered;
  }, [answered]);

  useEffect(() => {
    statusRef.current = status;
  }, [status]);

  useEffect(() => {
    const handleEvent = (envelope) => {
      if (!envelope || !envelope.event) return;
      const { event, data } = envelope;

      // Bỏ qua event match không thuộc phiên đấu hiện tại, tránh xử lý event mồ côi
      if (
        event.startsWith('match:') &&
        event !== 'match:found' &&
        event !== 'match:rejoined'
      ) {
        if (!matchRef.current) {
          if (
            (event === 'match:finished' || event === 'match:cancelled') &&
            data?.matchId &&
            String(data.matchId) === String(activeMatchIdRef.current)
          ) {
            setActiveMatchId(null);
          }
          return;
        }

        // Nếu matchRef.current đã có và data có matchId nhưng khác match hiện tại -> bỏ qua
        if (data?.matchId && String(data.matchId) !== String(matchRef.current.matchId)) {
          return;
        }
      }

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

        case 'room:created':
          setRoomCode(data?.roomCode || null);
          if (data?.mode) setMode(data.mode);
          setError(null);
          setStatus('roomWaiting');
          break;

        case 'room:left':
          setRoomCode(null);
          if (statusRef.current === 'roomWaiting') {
            setStatus('idle');
          }
          break;

        case 'match:found': {
          setRoomCode(null);
          setOpponentDisconnected(null);
          setActiveMatchId(null);
          finishedRef.current = false;

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

        case 'active-match': {
          setActiveMatchId(data?.matchId ?? null);
          setActiveMatchChecked(true);
          break;
        }

        case 'match:rejoined': {
          rejoiningRef.current = false;
          const s = data?.snapshot;
          if (!s) break;

          const rejoined = {
            matchId: s.matchId,
            mode: s.mode,
            matchType: s.matchType,
            totalRounds: s.totalRounds,
            countdownMs: 0,
            players: s.players,
            foundAt: Date.now(),
          };
          matchRef.current = rejoined;
          setMatch(rejoined);
          setMode(s.mode);

          const newScores = {};
          if (Array.isArray(s.scores)) {
            s.scores.forEach((x) => {
              newScores[x.userId] = x.score;
            });
          }
          setScores(newScores);

          setFinished(null);
          setCancelReason(null);
          setOpponentDisconnected(null);
          setError(null);
          setOpponentAnswered(false);

          if (s.question) {
            questionRef.current = s.question;
            answeredRef.current = s.answered;
            setQuestion(s.question);
            setServerOffsetMs(s.question.serverNow - Date.now());
            setAnswered(s.answered);
            setRoundResult(null);
            setStatus('playing');
          } else if (s.lastRoundResult) {
            setRoundResult(s.lastRoundResult);
            setQuestion(null);
            questionRef.current = null;
            setAnswered(false);
            setStatus('roundResult');
          } else {
            setQuestion(null);
            setRoundResult(null);
            setStatus('starting');
          }
          break;
        }

        case 'match:opponent-disconnected': {
          setOpponentDisconnected({
            graceMs: data?.graceMs,
            at: Date.now(),
          });
          break;
        }

        case 'match:opponent-reconnected': {
          setOpponentDisconnected(null);
          break;
        }

        case 'match:question': {
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
          setOpponentAnswered(true);
          break;
        }

        case 'match:round-result': {
          setRoundResult(data);
          if (Array.isArray(data?.results)) {
            setScores(Object.fromEntries(data.results.map((r) => [r.userId, r.score])));
          }
          setStatus('roundResult');
          break;
        }

        case 'match:finished': {
          finishedRef.current = true;
          setOpponentDisconnected(null);
          setFinished(data);
          if (Array.isArray(data?.players)) {
            setScores(Object.fromEntries(data.players.map((p) => [p.userId, p.score])));
          }
          setStatus('finished');
          break;
        }

        case 'match:cancelled': {
          setOpponentDisconnected(null);
          setCancelReason(data?.reason || null);
          setStatus('cancelled');
          break;
        }

        case 'error': {
          if (rejoiningRef.current && data?.code === 'BATTLE_MATCH_NOT_FOUND') {
            rejoiningRef.current = false;
            setCancelReason('CONNECTION_LOST');
            setStatus('cancelled');
            setActiveMatchId(null);
            // không setError
            break;
          }

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
    const offBattle = subscribeBattle(handleEvent);
    return () => {
      offBattle();
      offStatus();
    };
  }, []);

  // Tự khôi phục khi (re)connect
  useEffect(() => {
    if (connection === 'connected') {
      const m = matchRef.current;
      if (m && !finishedRef.current && statusRef.current !== 'cancelled') {
        rejoinMatch(m.matchId);
      } else if (!m) {
        send('/app/battle/active-match');
      }
    } else if (connection === 'disconnected') {
      if (['searching', 'roomWaiting'].includes(statusRef.current)) {
        setStatus('idle');
        setRoomCode(null);
      }
    }
  }, [connection]);

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

  const createRoom = (newMode) => {
    setError(null);
    setMode(newMode);
    if (!send('/app/battle/room/create', { mode: newMode })) {
      setError('BATTLE_NOT_CONNECTED');
    }
  };

  const joinRoom = (code) => {
    setError(null);
    if (!send('/app/battle/room/join', { roomCode: code })) {
      setError('BATTLE_NOT_CONNECTED');
    }
  };

  const leaveRoom = () => {
    send('/app/battle/room/leave');
  };

  const rejoinMatch = (targetMatchId) => {
    rejoiningRef.current = true;
    if (!send('/app/battle/rejoin', { matchId: Number(targetMatchId) })) {
      rejoiningRef.current = false;
    }
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
    setRoomCode(null);
    setActiveMatchId(null);
    setOpponentDisconnected(null);
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
    finishedRef.current = false;
    rejoiningRef.current = false;
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
    roomCode,
    activeMatchId,
    activeMatchChecked,
    opponentDisconnected,
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
    createRoom,
    joinRoom,
    leaveRoom,
    rejoinMatch,
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
