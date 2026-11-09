import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BattlePlayPage from '@/modules/battle/pages/BattlePlayPage';
import { useBattleSocket } from '@/modules/battle/context/BattleSocketContext';
import { useAuth } from '@/context/AuthContext';
import { useXp } from '@/modules/gamification/context/XpContext';
import { tr, trRx } from '../../../../utils/i18n';

vi.mock('@/modules/battle/context/BattleSocketContext', () => ({
  useBattleSocket: vi.fn(),
}));

vi.mock('@/context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/modules/gamification/context/XpContext', () => ({
  useXp: vi.fn(),
}));

describe('BattlePlayPage page component', () => {
  const MATCH_ID = 100;
  const players = [
    { userId: 1, displayName: 'Alice', avatarUrl: null },
    { userId: 2, displayName: 'Bob', avatarUrl: null },
  ];

  let socket;
  let onNavigate;

  const buildMatch = (overrides = {}) => ({
    matchId: MATCH_ID,
    mode: 'MULTIPLE_CHOICE',
    matchType: 'RANDOM',
    totalRounds: 10,
    countdownMs: 0,
    foundAt: Date.now(),
    players,
    ...overrides,
  });

  const buildQuestion = (overrides = {}) => ({
    questionId: 1001,
    roundNo: 2,
    mode: 'MULTIPLE_CHOICE',
    term: 'apple',
    timeLimitMs: 15000,
    serverNow: Date.now(),
    deadlineAt: Date.now() + 15000,
    options: [
      { id: 1, text: 'táo' },
      { id: 2, text: 'cam' },
    ],
    ...overrides,
  });

  const buildFinished = (overrides = {}) => ({
    matchId: MATCH_ID,
    winnerId: 1,
    reason: 'COMPLETED',
    players: [
      { userId: 1, score: 450, correctCount: 4, isWinner: true },
      { userId: 2, score: 300, correctCount: 3, isWinner: false },
    ],
    ...overrides,
  });

  const buildSocket = (overrides = {}) => ({
    connection: 'connected',
    status: 'starting',
    match: buildMatch(),
    error: null,
    activeMatchId: null,
    activeMatchChecked: true,
    opponentDisconnected: null,
    question: null,
    serverOffsetMs: 0,
    answered: false,
    opponentAnswered: false,
    roundResult: null,
    scores: { 1: 0, 2: 0 },
    finished: null,
    cancelReason: null,
    rejoinMatch: vi.fn(),
    sendAnswer: vi.fn(),
    exitBattle: vi.fn(),
    clearError: vi.fn(),
    ...overrides,
  });

  const renderPlay = (overrides = {}, props = {}) => {
    socket = buildSocket(overrides);
    useBattleSocket.mockReturnValue(socket);
    return render(<BattlePlayPage matchId={MATCH_ID} onNavigate={onNavigate} {...props} />);
  };

  beforeEach(() => {
    onNavigate = vi.fn();
    useAuth.mockReturnValue({ user: { id: 1, fullName: 'Alice', avatarUrl: null } });
    useXp.mockReturnValue({ lastXp: null });
  });

  describe('restoring the match', () => {
    it('shows the restoring screen while waiting for the active match check', () => {
      renderPlay({ match: null, activeMatchChecked: false });

      expect(screen.getByText(tr('battle:play.restoring'))).toBeInTheDocument();
      expect(onNavigate).not.toHaveBeenCalled();
      expect(socket.rejoinMatch).not.toHaveBeenCalled();
    });

    it('rejoins the match when the active match equals the one in the URL', () => {
      renderPlay({ match: null, activeMatchChecked: true, activeMatchId: MATCH_ID });

      expect(socket.rejoinMatch).toHaveBeenCalledTimes(1);
      expect(socket.rejoinMatch).toHaveBeenCalledWith(MATCH_ID);
      expect(onNavigate).not.toHaveBeenCalled();
      expect(screen.getByText(tr('battle:play.restoring'))).toBeInTheDocument();
    });

    it('compares the active match id and the URL match id as strings', () => {
      renderPlay({ match: null, activeMatchChecked: true, activeMatchId: '100' });

      expect(socket.rejoinMatch).toHaveBeenCalledTimes(1);
    });

    it('rejoins only once even when the component re-renders', () => {
      const { rerender } = renderPlay({ match: null, activeMatchChecked: true, activeMatchId: MATCH_ID });

      rerender(<BattlePlayPage matchId={MATCH_ID} onNavigate={onNavigate} />);

      expect(socket.rejoinMatch).toHaveBeenCalledTimes(1);
    });

    it('goes back to the lobby when there is no active match', () => {
      renderPlay({ match: null, activeMatchChecked: true, activeMatchId: null });

      expect(onNavigate).toHaveBeenCalledWith('/battle');
      expect(socket.rejoinMatch).not.toHaveBeenCalled();
    });

    it('goes back to the lobby when the active match is a different match', () => {
      renderPlay({ match: null, activeMatchChecked: true, activeMatchId: 999 });

      expect(onNavigate).toHaveBeenCalledWith('/battle');
    });

    it('treats a match with another id as invalid', () => {
      renderPlay({ match: buildMatch({ matchId: 999 }), activeMatchChecked: true, activeMatchId: null });

      expect(onNavigate).toHaveBeenCalledWith('/battle');
      expect(screen.getByText(tr('battle:play.restoring'))).toBeInTheDocument();
    });

    it('does not navigate or rejoin when the match is valid', () => {
      renderPlay();

      expect(onNavigate).not.toHaveBeenCalled();
      expect(socket.rejoinMatch).not.toHaveBeenCalled();
    });
  });

  describe('starting', () => {
    it('shows the mode, the players and the countdown', () => {
      renderPlay({ match: buildMatch({ countdownMs: 3000, foundAt: Date.now() }) });

      expect(screen.getByText(tr('battle:mode.multipleChoice'))).toBeInTheDocument();
      expect(screen.getByText(tr('battle:play.preparing'))).toBeInTheDocument();
      expect(screen.getAllByText('Alice').length).toBeGreaterThan(0);
      expect(screen.getByText('Bob')).toBeInTheDocument();
      expect(screen.getByText(tr('battle:play.vs'))).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
    });

    it('shows GO when the countdown is over', () => {
      renderPlay({ match: buildMatch({ countdownMs: 0 }) });

      expect(screen.getByText('GO!')).toBeInTheDocument();
    });

    it('shows the typing mode label', () => {
      renderPlay({ match: buildMatch({ mode: 'TYPING' }) });

      expect(screen.getByText(tr('battle:mode.typing'))).toBeInTheDocument();
    });

    it('uses the opponent fallback name when the opponent has no display name', () => {
      renderPlay({
        match: buildMatch({ players: [players[0], { userId: 2, displayName: null }] }),
      });

      expect(screen.getByText(tr('battle:play.opponentFallback'))).toBeInTheDocument();
    });

    it('shows the reconnecting banner when the connection is lost', () => {
      renderPlay({ connection: 'disconnected' });

      expect(screen.getByText(tr('battle:connection.reconnecting'))).toBeInTheDocument();
    });

    it('does not show the reconnecting banner when connected', () => {
      renderPlay();

      expect(screen.queryByText(tr('battle:connection.reconnecting'))).not.toBeInTheDocument();
    });
  });

  describe('playing', () => {
    it('shows the scoreboard and the question', () => {
      renderPlay({
        status: 'playing',
        question: buildQuestion(),
        scores: { 1: 140, 2: 60 },
      });

      expect(screen.getByRole('heading', { name: 'apple' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'táo' })).toBeInTheDocument();
      expect(screen.getByText('140')).toBeInTheDocument();
      expect(screen.getByText('60')).toBeInTheDocument();
      expect(screen.getByText(tr('battle:play.round', { current: 2, total: 10 }))).toBeInTheDocument();
    });

    it('uses 10 rounds when the match has no total rounds', () => {
      renderPlay({
        status: 'playing',
        match: buildMatch({ totalRounds: undefined }),
        question: buildQuestion({ roundNo: 1 }),
      });

      expect(screen.getByText(tr('battle:play.round', { current: 1, total: 10 }))).toBeInTheDocument();
    });

    it('sends the selected option', async () => {
      const user = userEvent.setup();
      renderPlay({ status: 'playing', question: buildQuestion() });

      await user.click(screen.getByRole('button', { name: 'cam' }));

      expect(socket.sendAnswer).toHaveBeenCalledWith({ selectedOptionId: 2 });
    });

    it('shows the answered chips for both players', () => {
      renderPlay({
        status: 'playing',
        question: buildQuestion(),
        answered: true,
        opponentAnswered: true,
      });

      expect(screen.getAllByText(tr('battle:play.answered'))).toHaveLength(2);
      expect(screen.getByText(tr('battle:play.waitingOpponent'))).toBeInTheDocument();
    });

    it('shows the opponent disconnected banner', () => {
      renderPlay({
        status: 'playing',
        question: buildQuestion(),
        opponentDisconnected: { graceMs: 10000, at: Date.now() },
      });

      expect(
        screen.getByText(tr('battle:play.opponentDisconnected', { name: 'Bob', seconds: 10 }))
      ).toBeInTheDocument();
    });

    it('does not show the opponent disconnected banner by default', () => {
      renderPlay({ status: 'playing', question: buildQuestion() });

      expect(screen.queryByText(/mất kết nối - còn/)).not.toBeInTheDocument();
    });

    it('shows a spinner when there is no question and no round result yet', () => {
      const { container } = renderPlay({ status: 'playing', question: null, roundResult: null });

      expect(container.querySelector('.battle-play-loading .app-loading-spinner')).toBeInTheDocument();
    });

    it('renders a new question component for each question', async () => {
      const user = userEvent.setup();
      const { rerender } = renderPlay({ status: 'playing', question: buildQuestion() });
      await user.click(screen.getByRole('button', { name: 'táo' }));
      expect(screen.getByRole('button', { name: 'táo' })).toHaveClass('battle-option--selected');

      useBattleSocket.mockReturnValue(
        buildSocket({
          status: 'playing',
          question: buildQuestion({ questionId: 1002, term: 'banana', options: [{ id: 1, text: 'táo' }] }),
        })
      );
      rerender(<BattlePlayPage matchId={MATCH_ID} onNavigate={onNavigate} />);

      expect(screen.getByRole('heading', { name: 'banana' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'táo' })).not.toHaveClass('battle-option--selected');
    });
  });

  describe('round result', () => {
    const roundResult = {
      matchId: MATCH_ID,
      roundNo: 2,
      correctAnswer: 'táo',
      results: [
        { userId: 1, answered: true, isCorrect: true, pointsGained: 140, score: 140 },
        { userId: 2, answered: true, isCorrect: false, pointsGained: 0, score: 0 },
      ],
    };

    it('shows the correct answer and my points together with the question', () => {
      renderPlay({
        status: 'roundResult',
        question: buildQuestion(),
        roundResult,
        answered: true,
      });

      expect(screen.getByText(`+140 ${tr('battle:play.points')}`)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /táo/ })).toHaveClass('battle-option--correct');
    });

    it('shows the rejoin round result screen when there is no question', () => {
      renderPlay({ status: 'roundResult', question: null, roundResult });

      // Một ở bảng điểm, một ở thẻ kết quả vòng
      expect(screen.getAllByText(tr('battle:play.round', { current: 2, total: 10 }))).toHaveLength(2);
      expect(screen.getByText('táo')).toBeInTheDocument();
      expect(screen.getByText(`${tr('battle:play.correctAnswer')}:`)).toBeInTheDocument();
      expect(screen.getByText(/\+140/)).toBeInTheDocument();
      expect(screen.getByText(tr('battle:play.nextQuestionSoon'))).toBeInTheDocument();
    });

    it('shows zero gained points on the rejoin screen when the user has no points', () => {
      renderPlay({
        status: 'roundResult',
        question: null,
        roundResult: {
          ...roundResult,
          results: [{ userId: 1, answered: false, isCorrect: false, pointsGained: 0, score: 0 }],
        },
      });

      expect(screen.getByText(/\+0/)).toBeInTheDocument();
    });

    it('hides my points on the rejoin screen when the user has no result', () => {
      renderPlay({
        status: 'roundResult',
        question: null,
        roundResult: { ...roundResult, results: [{ userId: 2, pointsGained: 50, score: 50 }] },
      });

      expect(screen.queryByText(/\+50/)).not.toBeInTheDocument();
      expect(screen.getByText(tr('battle:play.nextQuestionSoon'))).toBeInTheDocument();
    });
  });

  describe('finished', () => {
    it('shows the result and exits to the lobby', async () => {
      const user = userEvent.setup();
      renderPlay({ status: 'finished', finished: buildFinished() });

      expect(screen.getByRole('heading', { name: tr('battle:result.win') })).toBeInTheDocument();
      expect(screen.getByText('450')).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: trRx('battle:result.playAgain') }));

      expect(socket.exitBattle).toHaveBeenCalledTimes(1);
      expect(onNavigate).toHaveBeenCalledWith('/battle');
    });

    it('exits to the history page', async () => {
      const user = userEvent.setup();
      renderPlay({ status: 'finished', finished: buildFinished() });

      await user.click(screen.getByRole('button', { name: trRx('battle:result.viewHistory') }));

      expect(socket.exitBattle).toHaveBeenCalledTimes(1);
      expect(onNavigate).toHaveBeenCalledWith('/battle/history');
    });

    it('shows the lose result when the opponent won', () => {
      renderPlay({ status: 'finished', finished: buildFinished({ winnerId: 2 }) });

      expect(screen.getByRole('heading', { name: tr('battle:result.lose') })).toBeInTheDocument();
    });

    it('does not render the result before the finished data arrives', () => {
      renderPlay({ status: 'finished', finished: null });

      expect(screen.queryByRole('heading', { name: tr('battle:result.win') })).not.toBeInTheDocument();
    });

    it('shows the XP gained for a random match when XP arrives after the match ended', () => {
      useXp.mockReturnValue({ lastXp: { amount: 30, at: Date.now() + 60000 } });
      const { rerender } = renderPlay({ status: 'finished', finished: buildFinished() });

      // finishedAt được ghi nhận trong effect nên cần render lại để so sánh với lastXp
      rerender(<BattlePlayPage matchId={MATCH_ID} onNavigate={onNavigate} />);

      expect(screen.getByText(tr('battle:result.xpGained', { xp: 30 }))).toBeInTheDocument();
    });

    it('does not show XP that was gained before the match ended', () => {
      useXp.mockReturnValue({ lastXp: { amount: 30, at: Date.now() - 60000 } });
      const { rerender } = renderPlay({ status: 'finished', finished: buildFinished() });

      rerender(<BattlePlayPage matchId={MATCH_ID} onNavigate={onNavigate} />);

      expect(screen.queryByText(tr('battle:result.xpGained', { xp: 30 }))).not.toBeInTheDocument();
    });

    it('does not show XP for a room match', () => {
      useXp.mockReturnValue({ lastXp: { amount: 30, at: Date.now() + 60000 } });
      const { rerender } = renderPlay({
        status: 'finished',
        match: buildMatch({ matchType: 'ROOM' }),
        finished: buildFinished(),
      });

      rerender(<BattlePlayPage matchId={MATCH_ID} onNavigate={onNavigate} />);

      expect(screen.queryByText(tr('battle:result.xpGained', { xp: 30 }))).not.toBeInTheDocument();
    });

    it('shows the static XP hint for a random match without XP and few correct answers', () => {
      renderPlay({
        status: 'finished',
        finished: buildFinished({
          players: [
            { userId: 1, score: 100, correctCount: 1, isWinner: true },
            { userId: 2, score: 0, correctCount: 0, isWinner: false },
          ],
        }),
      });

      expect(screen.getByText(tr('battle:result.xpHint'))).toBeInTheDocument();
    });
  });

  describe('cancelled', () => {
    it.each([
      ['BOTH_DISCONNECTED', 'battle:result.cancelled.BOTH_DISCONNECTED'],
      ['INTERNAL_ERROR', 'battle:result.cancelled.INTERNAL_ERROR'],
      ['CONNECTION_LOST', 'battle:result.cancelled.CONNECTION_LOST'],
      ['SOMETHING_ELSE', 'battle:result.cancelled.unknown'],
      [null, 'battle:result.cancelled.unknown'],
    ])('shows the message for cancel reason %s', (cancelReason, key) => {
      renderPlay({ status: 'cancelled', cancelReason });

      expect(screen.getByRole('heading', { name: tr('battle:result.cancelled.title') })).toBeInTheDocument();
      expect(screen.getByText(tr(key))).toBeInTheDocument();
    });

    it('goes back to the lobby', async () => {
      const user = userEvent.setup();
      renderPlay({ status: 'cancelled', cancelReason: 'INTERNAL_ERROR' });

      await user.click(screen.getByRole('button', { name: tr('battle:result.backToLobby') }));

      expect(socket.exitBattle).toHaveBeenCalledTimes(1);
      expect(onNavigate).toHaveBeenCalledWith('/battle');
    });

    it('goes to the history page', async () => {
      const user = userEvent.setup();
      renderPlay({ status: 'cancelled', cancelReason: 'INTERNAL_ERROR' });

      await user.click(screen.getByRole('button', { name: tr('battle:result.viewHistory') }));

      expect(socket.exitBattle).toHaveBeenCalledTimes(1);
      expect(onNavigate).toHaveBeenCalledWith('/battle/history');
    });
  });

  describe('errors', () => {
    it('shows the translated battle error', () => {
      renderPlay({ error: 'BATTLE_NOT_CONNECTED' });

      expect(screen.getByRole('alert')).toHaveTextContent(tr('battle:error.BATTLE_NOT_CONNECTED'));
    });

    it('falls back to the API error message', () => {
      renderPlay({ error: 'BATTLE_QUESTION_TIMEOUT' });

      expect(screen.getByRole('alert')).toHaveTextContent(tr('api:error.BATTLE_QUESTION_TIMEOUT'));
    });

    it('falls back to the generic error message', () => {
      renderPlay({ error: 'SOMETHING_NEW' });

      expect(screen.getByRole('alert')).toHaveTextContent(tr('api:common.UNKNOWN_ERROR'));
    });

    it('does not show an alert without an error', () => {
      renderPlay();

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('clears the error after the toast timeout', () => {
      vi.useFakeTimers();
      try {
        renderPlay({ error: 'BATTLE_NOT_CONNECTED' });

        act(() => {
          vi.advanceTimersByTime(3500);
        });

        expect(socket.clearError).toHaveBeenCalled();
      } finally {
        vi.useRealTimers();
      }
    });
  });
});
