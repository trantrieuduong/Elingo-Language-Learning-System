import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { BattleSocketProvider, useBattleSocket } from '@/modules/battle/context/BattleSocketContext';
import { subscribeBattle, onStatus, send } from '@/modules/battle/battleSocket';
import { getSocketStatus } from '@/services/stompClient';

vi.mock('@/modules/battle/battleSocket', () => ({
  subscribeBattle: vi.fn(),
  onStatus: vi.fn(),
  send: vi.fn(),
}));

vi.mock('@/services/stompClient', () => ({
  getSocketStatus: vi.fn(),
}));

describe('BattleSocketContext', () => {
  let eventHandler;
  let statusHandler;
  let unsubscribeBattle;
  let unsubscribeStatus;

  const players = [
    { userId: 1, displayName: 'Alice', avatarUrl: null },
    { userId: 2, displayName: 'Bob', avatarUrl: null },
  ];

  const foundData = {
    matchId: 100,
    mode: 'TYPING',
    matchType: 'RANDOM',
    totalRounds: 10,
    countdownMs: 3000,
    players,
  };

  const questionData = (overrides = {}) => ({
    matchId: 100,
    questionId: 1001,
    roundNo: 1,
    mode: 'MULTIPLE_CHOICE',
    term: 'apple',
    timeLimitMs: 15000,
    serverNow: Date.now(),
    deadlineAt: Date.now() + 15000,
    options: [{ id: 1, text: 'táo' }],
    ...overrides,
  });

  const renderProvider = () => renderHook(() => useBattleSocket(), { wrapper: BattleSocketProvider });

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

  const startMatch = (result) => {
    emit('match:found', foundData);
    expect(result.current.status).toBe('starting');
  };

  beforeEach(() => {
    vi.clearAllMocks();
    unsubscribeBattle = vi.fn();
    unsubscribeStatus = vi.fn();
    getSocketStatus.mockReturnValue('disconnected');
    send.mockReturnValue(true);
    subscribeBattle.mockImplementation((handler) => {
      eventHandler = handler;
      return unsubscribeBattle;
    });
    onStatus.mockImplementation((handler) => {
      statusHandler = handler;
      return unsubscribeStatus;
    });
  });

  describe('provider', () => {
    it('throws when useBattleSocket is used outside the provider', () => {
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});

      expect(() => renderHook(() => useBattleSocket())).toThrow(
        'useBattleSocket must be used within a BattleSocketProvider'
      );

      spy.mockRestore();
    });

    it('provides the idle initial state', () => {
      const { result } = renderProvider();

      expect(result.current).toMatchObject({
        connection: 'disconnected',
        status: 'idle',
        mode: 'MULTIPLE_CHOICE',
        match: null,
        error: null,
        roomCode: null,
        activeMatchId: null,
        activeMatchChecked: false,
        opponentDisconnected: null,
        question: null,
        serverOffsetMs: 0,
        answered: false,
        opponentAnswered: false,
        roundResult: null,
        scores: {},
        finished: null,
        cancelReason: null,
      });
    });

    it('subscribes to battle events and socket status, and cleans up on unmount', () => {
      const { unmount } = renderProvider();

      expect(subscribeBattle).toHaveBeenCalledTimes(1);
      expect(onStatus).toHaveBeenCalledTimes(1);

      unmount();

      expect(unsubscribeBattle).toHaveBeenCalledTimes(1);
      expect(unsubscribeStatus).toHaveBeenCalledTimes(1);
    });

    it('ignores malformed envelopes and unknown events', () => {
      const { result } = renderProvider();

      act(() => {
        eventHandler(null);
        eventHandler({});
        eventHandler({ data: { foo: 'bar' } });
      });
      emit('unknown:event', { foo: 'bar' });

      expect(result.current.status).toBe('idle');
    });
  });

  describe('connection', () => {
    it('updates the connection status', () => {
      const { result } = renderProvider();

      changeConnection('connecting');

      expect(result.current.connection).toBe('connecting');
    });

    it('asks the server for the active match when connected without a local match', () => {
      renderProvider();

      changeConnection('connected');

      expect(send).toHaveBeenCalledWith('/app/battle/active-match');
    });

    it('rejoins the current match when reconnected during a match', () => {
      const { result } = renderProvider();
      startMatch(result);

      changeConnection('connected');

      expect(send).toHaveBeenCalledWith('/app/battle/rejoin', { matchId: 100 });
      expect(send).not.toHaveBeenCalledWith('/app/battle/active-match');
    });

    it('does not rejoin a match that has already finished', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:finished', { matchId: 100, winnerId: 1, players: [] });

      changeConnection('connected');

      expect(send).not.toHaveBeenCalledWith('/app/battle/rejoin', expect.anything());
    });

    it('does not rejoin a cancelled match', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:cancelled', { matchId: 100, reason: 'INTERNAL_ERROR' });

      changeConnection('connected');

      expect(send).not.toHaveBeenCalledWith('/app/battle/rejoin', expect.anything());
    });

    it('resets room waiting state when the connection is lost', () => {
      const { result } = renderProvider();
      changeConnection('connected');
      emit('room:created', { roomCode: 'ABC234', mode: 'TYPING' });
      expect(result.current.status).toBe('roomWaiting');

      changeConnection('disconnected');

      expect(result.current.status).toBe('idle');
      expect(result.current.roomCode).toBeNull();
    });

    it('resets searching state when the connection is lost', () => {
      const { result } = renderProvider();
      changeConnection('connected');
      emit('queue:joined', {});
      expect(result.current.status).toBe('searching');

      changeConnection('disconnected');

      expect(result.current.status).toBe('idle');
    });

    it('keeps the playing state when the connection is lost during a match', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:question', questionData());

      changeConnection('disconnected');

      expect(result.current.status).toBe('playing');
      expect(result.current.match).not.toBeNull();
    });
  });

  describe('queue events', () => {
    it('switches to searching on queue:joined', () => {
      const { result } = renderProvider();

      emit('queue:joined', { mode: 'TYPING' });

      expect(result.current.status).toBe('searching');
    });

    it('goes back to idle on queue:left', () => {
      const { result } = renderProvider();
      emit('queue:joined', {});

      emit('queue:left', {});

      expect(result.current.status).toBe('idle');
    });

    it('goes back to idle with an error on queue:timeout', () => {
      const { result } = renderProvider();
      emit('queue:joined', {});

      emit('queue:timeout', {});

      expect(result.current.status).toBe('idle');
      expect(result.current.error).toBe('QUEUE_TIMEOUT');
    });
  });

  describe('room events', () => {
    it('stores the room code and mode on room:created', () => {
      const { result } = renderProvider();

      emit('room:created', { roomCode: 'ABC234', mode: 'TYPING' });

      expect(result.current.status).toBe('roomWaiting');
      expect(result.current.roomCode).toBe('ABC234');
      expect(result.current.mode).toBe('TYPING');
      expect(result.current.error).toBeNull();
    });

    it('keeps the current mode when room:created has no mode', () => {
      const { result } = renderProvider();

      emit('room:created', { roomCode: 'ABC234' });

      expect(result.current.mode).toBe('MULTIPLE_CHOICE');
    });

    it('resets to idle on room:left while waiting', () => {
      const { result } = renderProvider();
      emit('room:created', { roomCode: 'ABC234', mode: 'TYPING' });

      emit('room:left', {});

      expect(result.current.status).toBe('idle');
      expect(result.current.roomCode).toBeNull();
    });

    it('does not change the status on room:left when not waiting for a room', () => {
      const { result } = renderProvider();
      emit('queue:joined', {});

      emit('room:left', {});

      expect(result.current.status).toBe('searching');
    });
  });

  describe('match:found', () => {
    it('creates the match, resets the previous state and initializes scores to zero', () => {
      const { result } = renderProvider();
      emit('room:created', { roomCode: 'ABC234', mode: 'TYPING' });

      emit('match:found', foundData);

      expect(result.current.status).toBe('starting');
      expect(result.current.match).toMatchObject({ matchId: 100, mode: 'TYPING', totalRounds: 10, players });
      expect(typeof result.current.match.foundAt).toBe('number');
      expect(result.current.mode).toBe('TYPING');
      expect(result.current.roomCode).toBeNull();
      expect(result.current.scores).toEqual({ 1: 0, 2: 0 });
      expect(result.current.question).toBeNull();
      expect(result.current.roundResult).toBeNull();
      expect(result.current.finished).toBeNull();
      expect(result.current.cancelReason).toBeNull();
      expect(result.current.answered).toBe(false);
      expect(result.current.opponentAnswered).toBe(false);
    });

    it('uses empty scores when players are missing', () => {
      const { result } = renderProvider();

      emit('match:found', { matchId: 100, mode: 'TYPING' });

      expect(result.current.scores).toEqual({});
    });

    it('clears the active match id', () => {
      const { result } = renderProvider();
      emit('active-match', { matchId: 55 });
      expect(result.current.activeMatchId).toBe(55);

      emit('match:found', foundData);

      expect(result.current.activeMatchId).toBeNull();
    });

    it('starts a fresh game after a previous finished match', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:finished', { matchId: 100, winnerId: 1, players: [{ userId: 1, score: 300 }] });

      emit('match:found', { ...foundData, matchId: 101 });

      expect(result.current.status).toBe('starting');
      expect(result.current.match.matchId).toBe(101);
      expect(result.current.finished).toBeNull();
      expect(result.current.scores).toEqual({ 1: 0, 2: 0 });
      // Có thể rejoin lại trận mới khi reconnect
      changeConnection('connected');
      expect(send).toHaveBeenCalledWith('/app/battle/rejoin', { matchId: 101 });
    });
  });

  describe('active-match', () => {
    it('stores the active match id and marks it as checked', () => {
      const { result } = renderProvider();

      emit('active-match', { matchId: 55, status: 'IN_PROGRESS' });

      expect(result.current.activeMatchId).toBe(55);
      expect(result.current.activeMatchChecked).toBe(true);
    });

    it('stores null when the user has no active match', () => {
      const { result } = renderProvider();

      emit('active-match', { matchId: null, status: 'NONE' });

      expect(result.current.activeMatchId).toBeNull();
      expect(result.current.activeMatchChecked).toBe(true);
    });

    it('clears the active match id when an orphan match:finished for it arrives', () => {
      const { result } = renderProvider();
      emit('active-match', { matchId: 55 });

      emit('match:finished', { matchId: 55 });

      expect(result.current.activeMatchId).toBeNull();
    });

    it('clears the active match id when an orphan match:cancelled for it arrives', () => {
      const { result } = renderProvider();
      emit('active-match', { matchId: 55 });

      emit('match:cancelled', { matchId: 55, reason: 'BOTH_DISCONNECTED' });

      expect(result.current.activeMatchId).toBeNull();
    });

    it('keeps the active match id when an orphan event is for another match', () => {
      const { result } = renderProvider();
      emit('active-match', { matchId: 55 });

      emit('match:finished', { matchId: 77 });

      expect(result.current.activeMatchId).toBe(55);
    });
  });

  describe('orphan match events', () => {
    it('ignores match events when there is no current match', () => {
      const { result } = renderProvider();

      emit('match:question', questionData());
      emit('match:round-result', { matchId: 100, results: [] });
      emit('match:opponent-disconnected', { matchId: 100, graceMs: 10000 });

      expect(result.current.status).toBe('idle');
      expect(result.current.question).toBeNull();
      expect(result.current.opponentDisconnected).toBeNull();
    });

    it('ignores match events that belong to another match', () => {
      const { result } = renderProvider();
      startMatch(result);

      emit('match:question', questionData({ matchId: 999 }));
      emit('match:finished', { matchId: 999, winnerId: 1, players: [] });

      expect(result.current.status).toBe('starting');
      expect(result.current.question).toBeNull();
      expect(result.current.finished).toBeNull();
    });

    it('compares match ids as strings', () => {
      const { result } = renderProvider();
      startMatch(result);

      emit('match:question', questionData({ matchId: '100' }));

      expect(result.current.status).toBe('playing');
    });
  });

  describe('match:question', () => {
    it('stores the question, computes the server offset and starts playing', () => {
      const { result } = renderProvider();
      startMatch(result);
      const question = questionData({ serverNow: Date.now() + 5000 });

      emit('match:question', question);

      expect(result.current.status).toBe('playing');
      expect(result.current.question).toEqual(question);
      expect(result.current.serverOffsetMs).toBeGreaterThan(4500);
      expect(result.current.serverOffsetMs).toBeLessThanOrEqual(5000);
      expect(result.current.answered).toBe(false);
      expect(result.current.opponentAnswered).toBe(false);
      expect(result.current.roundResult).toBeNull();
    });

    it('resets the answered flags and round result for the next question', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:question', questionData());
      act(() => {
        result.current.sendAnswer({ selectedOptionId: 1 });
      });
      emit('match:opponent-answered', { matchId: 100 });
      emit('match:round-result', { matchId: 100, roundNo: 1, results: [{ userId: 1, score: 140 }] });

      emit('match:question', questionData({ questionId: 1002, roundNo: 2 }));

      expect(result.current.question.questionId).toBe(1002);
      expect(result.current.answered).toBe(false);
      expect(result.current.opponentAnswered).toBe(false);
      expect(result.current.roundResult).toBeNull();
      expect(result.current.status).toBe('playing');
    });

    it('clears a previous error', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('error', { code: 'SOMETHING' });
      expect(result.current.error).toBe('SOMETHING');

      emit('match:question', questionData());

      expect(result.current.error).toBeNull();
    });
  });

  describe('match progress events', () => {
    it('marks the opponent as answered', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:question', questionData());

      emit('match:opponent-answered', { matchId: 100, roundNo: 1 });

      expect(result.current.opponentAnswered).toBe(true);
    });

    it('stores the round result and updates the scores', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:question', questionData());
      const roundResult = {
        matchId: 100,
        roundNo: 1,
        correctAnswer: 'táo',
        results: [
          { userId: 1, score: 140, pointsGained: 140, isCorrect: true },
          { userId: 2, score: 0, pointsGained: 0, isCorrect: false },
        ],
      };

      emit('match:round-result', roundResult);

      expect(result.current.status).toBe('roundResult');
      expect(result.current.roundResult).toEqual(roundResult);
      expect(result.current.scores).toEqual({ 1: 140, 2: 0 });
    });

    it('keeps the scores when the round result has no results array', () => {
      const { result } = renderProvider();
      startMatch(result);

      emit('match:round-result', { matchId: 100, roundNo: 1 });

      expect(result.current.status).toBe('roundResult');
      expect(result.current.scores).toEqual({ 1: 0, 2: 0 });
    });

    it('stores the final result and final scores on match:finished', () => {
      const { result } = renderProvider();
      startMatch(result);
      const finished = {
        matchId: 100,
        winnerId: 1,
        reason: 'COMPLETED',
        players: [
          { userId: 1, score: 450, correctCount: 4, isWinner: true },
          { userId: 2, score: 300, correctCount: 3, isWinner: false },
        ],
      };

      emit('match:finished', finished);

      expect(result.current.status).toBe('finished');
      expect(result.current.finished).toEqual(finished);
      expect(result.current.scores).toEqual({ 1: 450, 2: 300 });
      expect(result.current.opponentDisconnected).toBeNull();
    });

    it('stores the cancel reason on match:cancelled', () => {
      const { result } = renderProvider();
      startMatch(result);

      emit('match:cancelled', { matchId: 100, reason: 'BOTH_DISCONNECTED' });

      expect(result.current.status).toBe('cancelled');
      expect(result.current.cancelReason).toBe('BOTH_DISCONNECTED');
    });

    it('stores a null cancel reason when none is provided', () => {
      const { result } = renderProvider();
      startMatch(result);

      emit('match:cancelled', { matchId: 100 });

      expect(result.current.status).toBe('cancelled');
      expect(result.current.cancelReason).toBeNull();
    });
  });

  describe('opponent connection', () => {
    it('stores the disconnect info and clears it on reconnect', () => {
      const { result } = renderProvider();
      startMatch(result);

      emit('match:opponent-disconnected', { matchId: 100, graceMs: 10000 });
      expect(result.current.opponentDisconnected).toMatchObject({ graceMs: 10000 });
      expect(typeof result.current.opponentDisconnected.at).toBe('number');

      emit('match:opponent-reconnected', { matchId: 100 });
      expect(result.current.opponentDisconnected).toBeNull();
    });

    it('clears the disconnect info when the match finishes or is cancelled', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:opponent-disconnected', { matchId: 100, graceMs: 10000 });

      emit('match:finished', { matchId: 100, winnerId: 1, players: [] });
      expect(result.current.opponentDisconnected).toBeNull();

      emit('match:found', { ...foundData, matchId: 101 });
      emit('match:opponent-disconnected', { matchId: 101, graceMs: 10000 });
      emit('match:cancelled', { matchId: 101, reason: 'INTERNAL_ERROR' });
      expect(result.current.opponentDisconnected).toBeNull();
    });
  });

  describe('match:rejoined', () => {
    const baseSnapshot = {
      matchId: 100,
      mode: 'MULTIPLE_CHOICE',
      matchType: 'RANDOM',
      totalRounds: 10,
      players,
      scores: [
        { userId: 1, score: 140, correctCount: 1 },
        { userId: 2, score: 0, correctCount: 0 },
      ],
      roundNo: 1,
      question: null,
      answered: false,
      lastRoundResult: null,
    };

    it('ignores a rejoin event without snapshot', () => {
      const { result } = renderProvider();

      emit('match:rejoined', {});

      expect(result.current.match).toBeNull();
      expect(result.current.status).toBe('idle');
    });

    it('restores a match that is in the middle of a question', () => {
      const { result } = renderProvider();
      const question = questionData({ serverNow: Date.now() + 2000 });

      emit('match:rejoined', { snapshot: { ...baseSnapshot, question, answered: true } });

      expect(result.current.status).toBe('playing');
      expect(result.current.match).toMatchObject({ matchId: 100, mode: 'MULTIPLE_CHOICE', countdownMs: 0, players });
      expect(result.current.mode).toBe('MULTIPLE_CHOICE');
      expect(result.current.scores).toEqual({ 1: 140, 2: 0 });
      expect(result.current.question).toEqual(question);
      expect(result.current.answered).toBe(true);
      expect(result.current.serverOffsetMs).toBeGreaterThan(1500);
      expect(result.current.roundResult).toBeNull();
      expect(result.current.opponentAnswered).toBe(false);
    });

    it('restores a match between rounds with the last round result', () => {
      const { result } = renderProvider();
      const lastRoundResult = { matchId: 100, roundNo: 1, correctAnswer: 'táo', results: [] };

      emit('match:rejoined', { snapshot: { ...baseSnapshot, lastRoundResult } });

      expect(result.current.status).toBe('roundResult');
      expect(result.current.roundResult).toEqual(lastRoundResult);
      expect(result.current.question).toBeNull();
      expect(result.current.answered).toBe(false);
    });

    it('restores a match that is still in the countdown', () => {
      const { result } = renderProvider();

      emit('match:rejoined', { snapshot: baseSnapshot });

      expect(result.current.status).toBe('starting');
      expect(result.current.question).toBeNull();
      expect(result.current.roundResult).toBeNull();
    });

    it('uses empty scores when the snapshot has no scores', () => {
      const { result } = renderProvider();

      emit('match:rejoined', { snapshot: { ...baseSnapshot, scores: undefined } });

      expect(result.current.scores).toEqual({});
    });

    it('clears disconnect info, finished state and errors', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:opponent-disconnected', { matchId: 100, graceMs: 10000 });
      emit('error', { code: 'SOMETHING' });

      emit('match:rejoined', { snapshot: baseSnapshot });

      expect(result.current.opponentDisconnected).toBeNull();
      expect(result.current.error).toBeNull();
      expect(result.current.finished).toBeNull();
      expect(result.current.cancelReason).toBeNull();
    });

    it('allows answering the restored question', () => {
      const { result } = renderProvider();
      emit('match:rejoined', { snapshot: { ...baseSnapshot, question: questionData() } });

      let ok;
      act(() => {
        ok = result.current.sendAnswer({ selectedOptionId: 1 });
      });

      expect(ok).toBe(true);
      expect(send).toHaveBeenCalledWith('/app/battle/answer', {
        matchId: 100,
        questionId: 1001,
        selectedOptionId: 1,
      });
    });

    it('does not allow answering again when the snapshot says the question is answered', () => {
      const { result } = renderProvider();
      emit('match:rejoined', { snapshot: { ...baseSnapshot, question: questionData(), answered: true } });

      let ok;
      act(() => {
        ok = result.current.sendAnswer({ selectedOptionId: 1 });
      });

      expect(ok).toBe(false);
      expect(send).not.toHaveBeenCalledWith('/app/battle/answer', expect.anything());
    });
  });

  describe('error event', () => {
    it('stores the error code', () => {
      const { result } = renderProvider();

      emit('error', { code: 'BATTLE_ALREADY_PARTICIPATED' });

      expect(result.current.error).toBe('BATTLE_ALREADY_PARTICIPATED');
    });

    it('returns from searching to idle on error', () => {
      const { result } = renderProvider();
      emit('queue:joined', {});

      emit('error', { code: 'BATTLE_NOT_ENOUGH_CARDS' });

      expect(result.current.status).toBe('idle');
      expect(result.current.error).toBe('BATTLE_NOT_ENOUGH_CARDS');
    });

    it('keeps other statuses on error', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:question', questionData());

      emit('error', { code: 'BATTLE_QUESTION_TIMEOUT' });

      expect(result.current.status).toBe('playing');
      expect(result.current.error).toBe('BATTLE_QUESTION_TIMEOUT');
    });

    it('does not set an error when the payload has no code', () => {
      const { result } = renderProvider();

      emit('error', {});

      expect(result.current.error).toBeNull();
    });

    it('cancels the match with CONNECTION_LOST when rejoin fails because the match no longer exists', () => {
      const { result } = renderProvider();
      emit('active-match', { matchId: 100 });
      act(() => {
        result.current.rejoinMatch(100);
      });

      emit('error', { code: 'BATTLE_MATCH_NOT_FOUND' });

      expect(result.current.status).toBe('cancelled');
      expect(result.current.cancelReason).toBe('CONNECTION_LOST');
      expect(result.current.activeMatchId).toBeNull();
      expect(result.current.error).toBeNull();
    });

    it('treats BATTLE_MATCH_NOT_FOUND as a normal error when not rejoining', () => {
      const { result } = renderProvider();

      emit('error', { code: 'BATTLE_MATCH_NOT_FOUND' });

      expect(result.current.status).toBe('idle');
      expect(result.current.error).toBe('BATTLE_MATCH_NOT_FOUND');
    });

    it('stops treating errors as rejoin failures after a successful rejoin', () => {
      const { result } = renderProvider();
      act(() => {
        result.current.rejoinMatch(100);
      });
      emit('match:rejoined', {
        snapshot: { matchId: 100, mode: 'TYPING', matchType: 'RANDOM', totalRounds: 10, players, scores: [] },
      });

      emit('error', { code: 'BATTLE_MATCH_NOT_FOUND' });

      expect(result.current.status).toBe('starting');
      expect(result.current.error).toBe('BATTLE_MATCH_NOT_FOUND');
    });
  });

  describe('actions', () => {
    it('joinQueue sends the selected mode and remembers it', () => {
      const { result } = renderProvider();

      act(() => {
        result.current.joinQueue('TYPING');
      });

      expect(send).toHaveBeenCalledWith('/app/battle/queue/join', { mode: 'TYPING' });
      expect(result.current.mode).toBe('TYPING');
      expect(result.current.error).toBeNull();
    });

    it('joinQueue uses the current mode when none is given', () => {
      const { result } = renderProvider();

      act(() => {
        result.current.joinQueue();
      });

      expect(send).toHaveBeenCalledWith('/app/battle/queue/join', { mode: 'MULTIPLE_CHOICE' });
    });

    it('joinQueue reports BATTLE_NOT_CONNECTED when sending fails', () => {
      send.mockReturnValue(false);
      const { result } = renderProvider();

      act(() => {
        result.current.joinQueue('TYPING');
      });

      expect(result.current.error).toBe('BATTLE_NOT_CONNECTED');
    });

    it('leaveQueue sends the leave message', () => {
      const { result } = renderProvider();

      act(() => {
        result.current.leaveQueue();
      });

      expect(send).toHaveBeenCalledWith('/app/battle/queue/leave');
    });

    it('createRoom sends the mode and remembers it', () => {
      const { result } = renderProvider();

      act(() => {
        result.current.createRoom('TYPING');
      });

      expect(send).toHaveBeenCalledWith('/app/battle/room/create', { mode: 'TYPING' });
      expect(result.current.mode).toBe('TYPING');
    });

    it('createRoom reports BATTLE_NOT_CONNECTED when sending fails', () => {
      send.mockReturnValue(false);
      const { result } = renderProvider();

      act(() => {
        result.current.createRoom('TYPING');
      });

      expect(result.current.error).toBe('BATTLE_NOT_CONNECTED');
    });

    it('joinRoom sends the room code', () => {
      const { result } = renderProvider();

      act(() => {
        result.current.joinRoom('ABC234');
      });

      expect(send).toHaveBeenCalledWith('/app/battle/room/join', { roomCode: 'ABC234' });
      expect(result.current.error).toBeNull();
    });

    it('joinRoom reports BATTLE_NOT_CONNECTED when sending fails', () => {
      send.mockReturnValue(false);
      const { result } = renderProvider();

      act(() => {
        result.current.joinRoom('ABC234');
      });

      expect(result.current.error).toBe('BATTLE_NOT_CONNECTED');
    });

    it('leaveRoom sends the leave message', () => {
      const { result } = renderProvider();

      act(() => {
        result.current.leaveRoom();
      });

      expect(send).toHaveBeenCalledWith('/app/battle/room/leave');
    });

    it('rejoinMatch sends the match id as a number', () => {
      const { result } = renderProvider();

      act(() => {
        result.current.rejoinMatch('100');
      });

      expect(send).toHaveBeenCalledWith('/app/battle/rejoin', { matchId: 100 });
    });

    it('clearError clears the current error', () => {
      const { result } = renderProvider();
      emit('error', { code: 'SOMETHING' });

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('sendAnswer', () => {
    it('returns false when there is no match or question', () => {
      const { result } = renderProvider();

      let ok;
      act(() => {
        ok = result.current.sendAnswer({ selectedOptionId: 1 });
      });

      expect(ok).toBe(false);
      expect(send).not.toHaveBeenCalled();
    });

    it('returns false when the match has no question yet', () => {
      const { result } = renderProvider();
      startMatch(result);

      let ok;
      act(() => {
        ok = result.current.sendAnswer({ selectedOptionId: 1 });
      });

      expect(ok).toBe(false);
    });

    it('sends the answer with the match and question ids and marks as answered', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:question', questionData());

      let ok;
      act(() => {
        ok = result.current.sendAnswer({ selectedOptionId: 2 });
      });

      expect(ok).toBe(true);
      expect(send).toHaveBeenCalledWith('/app/battle/answer', {
        matchId: 100,
        questionId: 1001,
        selectedOptionId: 2,
      });
      expect(result.current.answered).toBe(true);
    });

    it('sends typed answers', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:question', questionData({ mode: 'TYPING' }));

      act(() => {
        result.current.sendAnswer({ typedAnswer: 'ice cream' });
      });

      expect(send).toHaveBeenCalledWith('/app/battle/answer', {
        matchId: 100,
        questionId: 1001,
        typedAnswer: 'ice cream',
      });
    });

    it('only sends the first answer of a question', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:question', questionData());

      let first;
      let second;
      act(() => {
        first = result.current.sendAnswer({ selectedOptionId: 1 });
        second = result.current.sendAnswer({ selectedOptionId: 2 });
      });

      expect(first).toBe(true);
      expect(second).toBe(false);
      expect(send).toHaveBeenCalledTimes(1);
    });

    it('does not mark as answered when sending fails and allows a retry', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:question', questionData());
      send.mockReturnValueOnce(false);

      let first;
      act(() => {
        first = result.current.sendAnswer({ selectedOptionId: 1 });
      });
      expect(first).toBe(false);
      expect(result.current.answered).toBe(false);

      let retry;
      act(() => {
        retry = result.current.sendAnswer({ selectedOptionId: 1 });
      });
      expect(retry).toBe(true);
      expect(result.current.answered).toBe(true);
    });
  });

  describe('exitBattle', () => {
    it('resets the whole battle state', () => {
      const { result } = renderProvider();
      startMatch(result);
      emit('match:question', questionData());
      emit('match:opponent-disconnected', { matchId: 100, graceMs: 10000 });
      emit('match:finished', {
        matchId: 100,
        winnerId: 1,
        players: [{ userId: 1, score: 300 }],
      });

      act(() => {
        result.current.exitBattle();
      });

      expect(result.current).toMatchObject({
        status: 'idle',
        match: null,
        error: null,
        roomCode: null,
        activeMatchId: null,
        opponentDisconnected: null,
        question: null,
        serverOffsetMs: 0,
        answered: false,
        opponentAnswered: false,
        roundResult: null,
        scores: {},
        finished: null,
        cancelReason: null,
      });
    });

    it('does not try to rejoin the previous match after exiting', () => {
      const { result } = renderProvider();
      startMatch(result);
      act(() => {
        result.current.exitBattle();
      });
      vi.clearAllMocks();

      changeConnection('connected');

      expect(send).not.toHaveBeenCalledWith('/app/battle/rejoin', expect.anything());
      expect(send).toHaveBeenCalledWith('/app/battle/active-match');
    });

    it('ignores events of the previous match after exiting', () => {
      const { result } = renderProvider();
      startMatch(result);
      act(() => {
        result.current.exitBattle();
      });

      emit('match:question', questionData());

      expect(result.current.status).toBe('idle');
      expect(result.current.question).toBeNull();
    });
  });

  describe('payloads with missing fields', () => {
    it('stores a null room code when room:created has no room code', () => {
      const { result } = renderProvider();

      emit('room:created', {});

      expect(result.current.status).toBe('roomWaiting');
      expect(result.current.roomCode).toBeNull();
    });

    it('keeps the current mode when match:found has no mode', () => {
      const { result } = renderProvider();

      emit('match:found', { matchId: 100, players });

      expect(result.current.status).toBe('starting');
      expect(result.current.mode).toBe('MULTIPLE_CHOICE');
    });

    it('keeps the scores when match:finished has no players list', () => {
      const { result } = renderProvider();
      startMatch(result);

      emit('match:finished', { matchId: 100, winnerId: 1 });

      expect(result.current.status).toBe('finished');
      expect(result.current.finished).toEqual({ matchId: 100, winnerId: 1 });
      expect(result.current.scores).toEqual({ 1: 0, 2: 0 });
    });
  });

  describe('rejoin failure to send', () => {
    it('does not treat later match-not-found errors as a failed rejoin when the rejoin message was not sent', () => {
      send.mockReturnValue(false);
      const { result } = renderProvider();

      act(() => {
        result.current.rejoinMatch(100);
      });
      emit('error', { code: 'BATTLE_MATCH_NOT_FOUND' });

      expect(send).toHaveBeenCalledWith('/app/battle/rejoin', { matchId: 100 });
      expect(result.current.status).toBe('idle');
      expect(result.current.error).toBe('BATTLE_MATCH_NOT_FOUND');
      expect(result.current.cancelReason).toBeNull();
    });
  });
});
