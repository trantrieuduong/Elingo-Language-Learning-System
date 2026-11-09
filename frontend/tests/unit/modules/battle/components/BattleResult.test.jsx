import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BattleResult from '@/modules/battle/components/BattleResult/BattleResult';
import { tr, trRx } from '../../../../utils/i18n';

describe('BattleResult component', () => {
  const me = { userId: 1, name: 'Alice', avatarUrl: null };
  const opponent = { userId: 2, name: 'Bob', avatarUrl: null };

  const buildFinished = (overrides = {}) => ({
    matchId: 100,
    winnerId: 1,
    reason: 'COMPLETED',
    players: [
      { userId: 1, score: 450, correctCount: 4, isWinner: true },
      { userId: 2, score: 300, correctCount: 3, isWinner: false },
    ],
    ...overrides,
  });

  const renderResult = (props = {}) =>
    render(
      <BattleResult
        me={me}
        opponent={opponent}
        finished={buildFinished()}
        totalRounds={10}
        onExit={vi.fn()}
        {...props}
      />
    );

  describe('outcome', () => {
    it('shows the win state when the current user is the winner', () => {
      const { container } = renderResult();

      expect(screen.getByRole('heading', { name: tr('battle:result.win') })).toBeInTheDocument();
      expect(screen.getByText(tr('battle:result.winDesc'))).toBeInTheDocument();
      expect(container.firstChild).toHaveClass('battle-result--win');
    });

    it('shows the lose state when the opponent is the winner', () => {
      const { container } = renderResult({ finished: buildFinished({ winnerId: 2 }) });

      expect(screen.getByRole('heading', { name: tr('battle:result.lose') })).toBeInTheDocument();
      expect(screen.getByText(tr('battle:result.loseDesc'))).toBeInTheDocument();
      expect(container.firstChild).toHaveClass('battle-result--lose');
    });

    it('shows the draw state when there is no winner', () => {
      const { container } = renderResult({ finished: buildFinished({ winnerId: null }) });

      expect(screen.getByRole('heading', { name: tr('battle:result.draw') })).toBeInTheDocument();
      expect(screen.getByText(tr('battle:result.drawDesc'))).toBeInTheDocument();
      expect(container.firstChild).toHaveClass('battle-result--draw');
    });

    it('compares user ids as strings', () => {
      renderResult({ me: { ...me, userId: '1' }, finished: buildFinished({ winnerId: 1 }) });

      expect(screen.getByRole('heading', { name: tr('battle:result.win') })).toBeInTheDocument();
    });
  });

  describe('forfeit', () => {
    it('shows the forfeit win message for the winner', () => {
      renderResult({ finished: buildFinished({ reason: 'FORFEIT' }) });

      expect(screen.getByText(tr('battle:result.forfeitWin'))).toBeInTheDocument();
    });

    it('shows the forfeit lose message for the loser', () => {
      renderResult({ finished: buildFinished({ reason: 'FORFEIT', winnerId: 2 }) });

      expect(screen.getByText(tr('battle:result.forfeitLose'))).toBeInTheDocument();
    });

    it('does not show a forfeit message for a completed match', () => {
      renderResult();

      expect(screen.queryByText(tr('battle:result.forfeitWin'))).not.toBeInTheDocument();
      expect(screen.queryByText(tr('battle:result.forfeitLose'))).not.toBeInTheDocument();
    });
  });

  describe('players', () => {
    it('shows names, scores and correct counts of both players', () => {
      renderResult();

      expect(screen.getByText('Alice')).toBeInTheDocument();
      expect(screen.getByText('Bob')).toBeInTheDocument();
      expect(screen.getByText('450')).toBeInTheDocument();
      expect(screen.getByText('300')).toBeInTheDocument();
      expect(screen.getByText(tr('battle:result.correctCount', { count: 4, total: 10 }))).toBeInTheDocument();
      expect(screen.getByText(tr('battle:result.correctCount', { count: 3, total: 10 }))).toBeInTheDocument();
    });

    it('highlights only the winning row', () => {
      const { container } = renderResult();

      const rows = container.querySelectorAll('.battle-result-row');
      expect(rows).toHaveLength(2);
      expect(rows[0]).toHaveClass('battle-result-row--winner');
      expect(rows[1]).not.toHaveClass('battle-result-row--winner');
    });

    it('defaults to zero when stats of a player are missing', () => {
      renderResult({ finished: buildFinished({ players: [] }) });

      expect(screen.getAllByText('0')).toHaveLength(2);
      expect(screen.getAllByText(tr('battle:result.correctCount', { count: 0, total: 10 }))).toHaveLength(2);
    });

    it('falls back to default names', () => {
      renderResult({ me: { userId: 1 }, opponent: { userId: 2 } });

      expect(screen.getByText(tr('battle:play.you'))).toBeInTheDocument();
      expect(screen.getByText(tr('battle:play.opponentFallback'))).toBeInTheDocument();
    });
  });

  describe('XP', () => {
    it('shows the gained XP when provided', () => {
      renderResult({ matchType: 'RANDOM', xpGained: 25 });

      expect(screen.getByText(tr('battle:result.xpGained', { xp: 25 }))).toBeInTheDocument();
      expect(screen.queryByText(tr('battle:result.xpHint'))).not.toBeInTheDocument();
    });

    it('shows the XP hint for a random match with fewer than 3 correct answers', () => {
      renderResult({
        matchType: 'RANDOM',
        finished: buildFinished({
          players: [
            { userId: 1, score: 100, correctCount: 2, isWinner: true },
            { userId: 2, score: 50, correctCount: 1, isWinner: false },
          ],
        }),
      });

      expect(screen.getByText(tr('battle:result.xpHint'))).toBeInTheDocument();
    });

    it('does not show the XP hint when the user has at least 3 correct answers', () => {
      renderResult({ matchType: 'RANDOM' });

      expect(screen.queryByText(tr('battle:result.xpHint'))).not.toBeInTheDocument();
    });

    it('does not show the XP hint for room matches', () => {
      renderResult({
        matchType: 'ROOM',
        finished: buildFinished({
          players: [
            { userId: 1, score: 100, correctCount: 0, isWinner: true },
            { userId: 2, score: 50, correctCount: 0, isWinner: false },
          ],
        }),
      });

      expect(screen.queryByText(tr('battle:result.xpHint'))).not.toBeInTheDocument();
    });

    it('shows XP gained of zero instead of the hint', () => {
      renderResult({ matchType: 'RANDOM', xpGained: 0 });

      expect(screen.getByText(tr('battle:result.xpGained', { xp: 0 }))).toBeInTheDocument();
    });
  });

  describe('actions', () => {
    it('calls onExit when clicking play again', async () => {
      const user = userEvent.setup();
      const onExit = vi.fn();
      renderResult({ onExit });

      await user.click(screen.getByRole('button', { name: trRx('battle:result.playAgain') }));

      expect(onExit).toHaveBeenCalledTimes(1);
    });

    it('calls onViewHistory when clicking view history', async () => {
      const user = userEvent.setup();
      const onViewHistory = vi.fn();
      renderResult({ onViewHistory });

      await user.click(screen.getByRole('button', { name: trRx('battle:result.viewHistory') }));

      expect(onViewHistory).toHaveBeenCalledTimes(1);
    });

    it('hides the view history button when no handler is provided', () => {
      renderResult();

      expect(screen.queryByRole('button', { name: trRx('battle:result.viewHistory') })).not.toBeInTheDocument();
    });
  });
});
