import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BattleMatchDetailPage from '@/modules/battle/pages/BattleMatchDetailPage';
import { getBattleDetailApi } from '@/modules/battle/battleApi';
import { useAuth } from '@/context/AuthContext';
import { tr, trRx } from '../../../../utils/i18n';

vi.mock('@/modules/battle/battleApi', () => ({
  getBattleDetailApi: vi.fn(),
}));

vi.mock('@/context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

describe('BattleMatchDetailPage page component', () => {
  const MATCH_ID = 100;
  let onNavigate;

  const buildDetail = (overrides = {}) => ({
    matchId: MATCH_ID,
    mode: 'MULTIPLE_CHOICE',
    matchType: 'RANDOM',
    totalRounds: 2,
    winnerId: 1,
    startedAt: '2026-01-02T10:00:00',
    finishedAt: '2026-01-02T10:05:00',
    players: [
      { userId: 1, displayName: 'Alice', avatarUrl: null },
      { userId: 2, displayName: 'Bob', avatarUrl: null },
    ],
    results: [
      { userId: 1, score: 300, correctCount: 2, isWinner: true },
      { userId: 2, score: 150, correctCount: 1, isWinner: false },
    ],
    rounds: [
      {
        roundNo: 1,
        term: 'apple',
        correctAnswer: 'táo',
        answers: [
          { userId: 1, answered: true, answerText: 'táo', isCorrect: true, responseTimeMs: 2000 },
          { userId: 2, answered: true, answerText: 'cam', isCorrect: false, responseTimeMs: 3500 },
        ],
      },
      {
        roundNo: 2,
        term: 'banana',
        correctAnswer: 'chuối',
        answers: [
          { userId: 1, answered: false, answerText: null, isCorrect: false, responseTimeMs: null },
          { userId: 2, answered: true, answerText: 'chuối', isCorrect: true, responseTimeMs: 1000 },
        ],
      },
    ],
    ...overrides,
  });

  const renderPage = (props = {}) =>
    render(<BattleMatchDetailPage matchId={MATCH_ID} onNavigate={onNavigate} {...props} />);

  const roundHeader = (term) => screen.getByText(term).closest('button');

  beforeEach(() => {
    onNavigate = vi.fn();
    useAuth.mockReturnValue({ user: { id: 1 } });
    getBattleDetailApi.mockResolvedValue({ success: true, data: buildDetail() });
  });

  describe('loading', () => {
    it('shows the loading spinner then the detail', async () => {
      const { container } = renderPage();

      expect(container.querySelector('.battle-detail-loading')).toBeInTheDocument();

      expect(await screen.findByRole('heading', { name: /#100/ })).toBeInTheDocument();
      expect(container.querySelector('.battle-detail-loading')).not.toBeInTheDocument();
      expect(getBattleDetailApi).toHaveBeenCalledWith(MATCH_ID);
    });

    it('reloads the detail when the match id changes', async () => {
      const { rerender } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      getBattleDetailApi.mockResolvedValue({ success: true, data: buildDetail({ matchId: 200 }) });
      rerender(<BattleMatchDetailPage matchId={200} onNavigate={onNavigate} />);

      expect(await screen.findByRole('heading', { name: /#200/ })).toBeInTheDocument();
      expect(getBattleDetailApi).toHaveBeenLastCalledWith(200);
    });
  });

  describe('summary', () => {
    it('shows the title, the mode and the match type', async () => {
      renderPage();

      expect(await screen.findByRole('heading', { name: `${tr('battle:detail.title')} #100` })).toBeInTheDocument();
      expect(screen.getByText(tr('battle:mode.multipleChoice'))).toBeInTheDocument();
      expect(screen.getByText(tr('battle:history.matchType.RANDOM'))).toBeInTheDocument();
    });

    it('shows typing mode and room match labels', async () => {
      getBattleDetailApi.mockResolvedValue({
        success: true,
        data: buildDetail({ mode: 'TYPING', matchType: 'ROOM' }),
      });
      renderPage();

      expect(await screen.findByText(tr('battle:mode.typing'))).toBeInTheDocument();
      expect(screen.getByText(tr('battle:history.matchType.ROOM'))).toBeInTheDocument();
    });

    it('shows both players with their scores and correct counts', async () => {
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      const scores = [...container.querySelectorAll('.battle-detail-player__score')].map((el) => el.textContent);
      expect(scores).toEqual(['300', '150']);
      expect(screen.getByText('Alice')).toBeInTheDocument();
      expect(screen.getByText('Bob')).toBeInTheDocument();
      expect(screen.getByText(tr('battle:result.correctCount', { count: 2, total: 2 }))).toBeInTheDocument();
      expect(screen.getByText(tr('battle:result.correctCount', { count: 1, total: 2 }))).toBeInTheDocument();
    });

    it('identifies the current user even when it is the second player', async () => {
      useAuth.mockReturnValue({ user: { id: 2 } });
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      const scores = [...container.querySelectorAll('.battle-detail-player__score')].map((el) => el.textContent);
      expect(scores).toEqual(['150', '300']);
    });

    it('falls back to default names and zero scores when player data is missing', async () => {
      getBattleDetailApi.mockResolvedValue({
        success: true,
        data: buildDetail({ players: [], results: [] }),
      });
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      expect(screen.getAllByText(tr('battle:detail.you')).length).toBeGreaterThan(0);
      expect(screen.getAllByText(tr('battle:detail.opponent')).length).toBeGreaterThan(0);
      const scores = [...container.querySelectorAll('.battle-detail-player__score')].map((el) => el.textContent);
      expect(scores).toEqual(['0', '0']);
    });

    it('shows the win badge when the current user won', async () => {
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      expect(container.querySelector('.battle-detail-badge--win')).toHaveTextContent(tr('battle:history.result.WIN'));
    });

    it('shows the lose badge when the opponent won', async () => {
      getBattleDetailApi.mockResolvedValue({ success: true, data: buildDetail({ winnerId: 2 }) });
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      expect(container.querySelector('.battle-detail-badge--lose')).toHaveTextContent(tr('battle:history.result.LOSE'));
    });

    it('shows the draw badge when there is no winner', async () => {
      getBattleDetailApi.mockResolvedValue({ success: true, data: buildDetail({ winnerId: null }) });
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      expect(container.querySelector('.battle-detail-badge--draw')).toHaveTextContent(tr('battle:history.result.DRAW'));
    });

    it('navigates back to the history page', async () => {
      const user = userEvent.setup();
      renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      await user.click(screen.getByRole('button', { name: trRx('battle:detail.back') }));

      expect(onNavigate).toHaveBeenCalledWith('/battle/history');
    });
  });

  describe('rounds', () => {
    it('lists the rounds collapsed with a preview of my answer', async () => {
      renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      expect(screen.getByText(tr('battle:detail.roundsTitle'))).toBeInTheDocument();
      expect(screen.getByText('apple')).toBeInTheDocument();
      expect(screen.getByText('banana')).toBeInTheDocument();
      expect(roundHeader('apple')).toHaveAttribute('aria-expanded', 'false');
      expect(within(roundHeader('apple')).getByText(tr('battle:detail.correct'))).toBeInTheDocument();
      expect(within(roundHeader('apple')).getByText(new RegExp(`${tr('battle:detail.youAnswered')}: táo`))).toBeInTheDocument();
      expect(within(roundHeader('banana')).getByText(tr('battle:detail.wrong'))).toBeInTheDocument();
      expect(
        within(roundHeader('banana')).getByText(new RegExp(`${tr('battle:detail.youAnswered')}: ${tr('battle:detail.noAnswer')}`))
      ).toBeInTheDocument();
      expect(screen.queryByText(tr('battle:detail.correctAnswer') + ':', { exact: false })).not.toBeInTheDocument();
    });

    it('expands a round to show the correct answer and both answers', async () => {
      const user = userEvent.setup();
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      await user.click(roundHeader('apple'));

      expect(roundHeader('apple')).toHaveAttribute('aria-expanded', 'true');
      const body = container.querySelector('.battle-detail-round-body');
      expect(within(body).getByText('táo', { selector: 'strong' })).toBeInTheDocument();
      const answers = [...body.querySelectorAll('.battle-detail-answer-text')].map((el) => el.textContent);
      expect(answers).toEqual(['táo', 'cam']);
      expect(within(body).getByText(tr('battle:detail.seconds', { sec: '2.0' }))).toBeInTheDocument();
      expect(within(body).getByText(tr('battle:detail.seconds', { sec: '3.5' }))).toBeInTheDocument();
    });

    it('collapses an expanded round when clicked again', async () => {
      const user = userEvent.setup();
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      await user.click(roundHeader('apple'));
      await user.click(roundHeader('apple'));

      expect(roundHeader('apple')).toHaveAttribute('aria-expanded', 'false');
      expect(container.querySelector('.battle-detail-round-body')).not.toBeInTheDocument();
    });

    it('expands rounds independently', async () => {
      const user = userEvent.setup();
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      await user.click(roundHeader('apple'));
      await user.click(roundHeader('banana'));

      expect(container.querySelectorAll('.battle-detail-round-body')).toHaveLength(2);
    });

    it('shows no answer and no time for a round I did not answer', async () => {
      const user = userEvent.setup();
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      await user.click(roundHeader('banana'));

      const body = container.querySelector('.battle-detail-round-body');
      const answers = [...body.querySelectorAll('.battle-detail-answer-text')].map((el) => el.textContent);
      expect(answers).toEqual([tr('battle:detail.noAnswer'), 'chuối']);
      // Chỉ đối thủ có thời gian trả lời
      expect(within(body).getAllByText(/\ds$/)).toHaveLength(1);
      expect(within(body).getByText(tr('battle:detail.seconds', { sec: '1.0' }))).toBeInTheDocument();
    });

    it('shows status icons only for answered rounds', async () => {
      const user = userEvent.setup();
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      await user.click(roundHeader('apple'));
      await user.click(roundHeader('banana'));

      const icons = [...container.querySelectorAll('.battle-detail-status-icon')].map((el) => el.textContent);
      // Round 1: mình đúng, đối thủ sai; Round 2: chỉ đối thủ có trả lời (đúng)
      expect(icons).toEqual(['check_circle', 'cancel', 'check_circle']);
    });

    it('renders no round when the match has no rounds', async () => {
      getBattleDetailApi.mockResolvedValue({ success: true, data: buildDetail({ rounds: [] }) });
      const { container } = renderPage();
      await screen.findByRole('heading', { name: /#100/ });

      expect(container.querySelectorAll('.battle-detail-round-item')).toHaveLength(0);
    });
  });

  describe('errors', () => {
    it('shows a generic error and goes back to the history', async () => {
      const user = userEvent.setup();
      getBattleDetailApi.mockRejectedValue(new Error('Network error'));
      renderPage();

      expect(await screen.findByText(tr('battle:detail.loadError'))).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: tr('battle:detail.back') }));

      expect(onNavigate).toHaveBeenCalledWith('/battle/history');
    });

    it('retries loading the detail', async () => {
      const user = userEvent.setup();
      getBattleDetailApi.mockRejectedValueOnce(new Error('Network error'));
      renderPage();
      await screen.findByText(tr('battle:detail.loadError'));

      await user.click(screen.getByRole('button', { name: tr('battle:history.retry') }));

      expect(await screen.findByRole('heading', { name: /#100/ })).toBeInTheDocument();
      expect(screen.queryByText(tr('battle:detail.loadError'))).not.toBeInTheDocument();
      expect(getBattleDetailApi).toHaveBeenCalledTimes(2);
    });

    it('shows an error when the response is unsuccessful', async () => {
      getBattleDetailApi.mockResolvedValue({ success: false });
      renderPage();

      expect(await screen.findByText(tr('battle:detail.loadError'))).toBeInTheDocument();
    });

    it.each(['BATTLE_PARTICIPANT_NOT_FOUND', 'BATTLE_MATCH_NOT_FOUND'])(
      'shows the API error message for %s',
      async (code) => {
        getBattleDetailApi.mockRejectedValue({ response: { data: { code } } });
        renderPage();

        expect(await screen.findByText(tr(`api:error.${code}`))).toBeInTheDocument();
      }
    );

    it('shows the generic error for other error codes', async () => {
      getBattleDetailApi.mockRejectedValue({ response: { data: { code: 'SOMETHING_ELSE' } } });
      renderPage();

      expect(await screen.findByText(tr('battle:detail.loadError'))).toBeInTheDocument();
    });
  });
});
