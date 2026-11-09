import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BattleHistoryPage from '@/modules/battle/pages/BattleHistoryPage';
import { getBattleHistoryApi, getBattleStatsApi } from '@/modules/battle/battleApi';
import { useAuth } from '@/context/AuthContext';
import { tr, trRx } from '../../../../utils/i18n';

vi.mock('@/modules/battle/battleApi', () => ({
  getBattleHistoryApi: vi.fn(),
  getBattleStatsApi: vi.fn(),
}));

vi.mock('@/context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

describe('BattleHistoryPage page component', () => {
  let onNavigate;

  const buildItem = (overrides = {}) => ({
    matchId: 100,
    mode: 'MULTIPLE_CHOICE',
    matchType: 'RANDOM',
    opponent: { userId: 2, displayName: 'Bob', avatarUrl: null },
    myScore: 300,
    opponentScore: 150,
    result: 'WIN',
    finishedAt: '2026-01-02T10:30:00',
    ...overrides,
  });

  const historyResponse = (content, totalPages = 1) => ({
    success: true,
    data: { content, totalPages, totalElements: content.length },
  });

  const statsResponse = {
    success: true,
    data: { totalMatches: 10, wins: 6, losses: 3, draws: 1, winRate: 60 },
  };

  const renderPage = () => render(<BattleHistoryPage onNavigate={onNavigate} />);

  const lastHistoryParams = () => getBattleHistoryApi.mock.calls.at(-1)[0];

  const radio = (label) => screen.getByRole('radio', { name: label });

  beforeEach(() => {
    onNavigate = vi.fn();
    useAuth.mockReturnValue({ user: { id: 1 } });
    getBattleStatsApi.mockResolvedValue(statsResponse);
    getBattleHistoryApi.mockResolvedValue(historyResponse([buildItem()]));
    Element.prototype.scrollIntoView = vi.fn();
  });

  describe('loading and listing', () => {
    it('shows the loading spinner then the match list', async () => {
      const { container } = renderPage();

      expect(container.querySelector('.battle-history-loading')).toBeInTheDocument();

      expect(await screen.findByText('Bob')).toBeInTheDocument();
      expect(container.querySelector('.battle-history-loading')).not.toBeInTheDocument();
    });

    it('requests the first page of 10 matches without filters', async () => {
      renderPage();
      await screen.findByText('Bob');

      expect(getBattleHistoryApi).toHaveBeenCalledWith({
        page: 1,
        size: 10,
        result: undefined,
        mode: undefined,
        matchType: undefined,
      });
    });

    it('renders the match information', async () => {
      const { container } = renderPage();
      await screen.findByText('Bob');

      expect(screen.getByRole('heading', { name: tr('battle:history.title') })).toBeInTheDocument();
      expect(container.querySelector('.battle-history-item__my-score')).toHaveTextContent('300');
      expect(container.querySelector('.battle-history-item__opp-score')).toHaveTextContent('150');
      const item = screen.getByRole('button', { name: `${tr('battle:history.title')} #100` });
      expect(within(item).getByText(tr('battle:mode.multipleChoice'))).toBeInTheDocument();
      expect(within(item).getByText(tr('battle:history.matchType.RANDOM'))).toBeInTheDocument();
      expect(within(item).getByText(tr('battle:history.result.WIN'))).toBeInTheDocument();
    });

    it('renders win, lose and draw badges with the right style', async () => {
      getBattleHistoryApi.mockResolvedValue(
        historyResponse([
          buildItem({ matchId: 1, result: 'WIN' }),
          buildItem({ matchId: 2, result: 'LOSE' }),
          buildItem({ matchId: 3, result: 'DRAW' }),
        ])
      );
      const { container } = renderPage();
      await screen.findAllByText('Bob');

      expect(container.querySelector('.battle-history-badge--win')).toHaveTextContent(tr('battle:history.result.WIN'));
      expect(container.querySelector('.battle-history-badge--lose')).toHaveTextContent(tr('battle:history.result.LOSE'));
      expect(container.querySelector('.battle-history-badge--draw')).toHaveTextContent(tr('battle:history.result.DRAW'));
    });

    it('renders typing mode and room match labels', async () => {
      getBattleHistoryApi.mockResolvedValue(
        historyResponse([buildItem({ mode: 'TYPING', matchType: 'ROOM' })])
      );
      renderPage();
      await screen.findByText('Bob');

      const item = screen.getByRole('button', { name: `${tr('battle:history.title')} #100` });
      expect(within(item).getByText(tr('battle:mode.typing'))).toBeInTheDocument();
      expect(within(item).getByText(tr('battle:history.matchType.ROOM'))).toBeInTheDocument();
    });

    it('uses the opponent fallback name and no date when data is missing', async () => {
      getBattleHistoryApi.mockResolvedValue(
        historyResponse([buildItem({ opponent: null, finishedAt: null })])
      );
      renderPage();

      expect(await screen.findByText(tr('battle:play.opponentFallback'))).toBeInTheDocument();
    });

    it('navigates to the match detail when an item is clicked', async () => {
      const user = userEvent.setup();
      renderPage();
      await screen.findByText('Bob');

      await user.click(screen.getByRole('button', { name: `${tr('battle:history.title')} #100` }));

      expect(onNavigate).toHaveBeenCalledWith('/battle/history/100');
    });

    it('navigates back to the lobby', async () => {
      const user = userEvent.setup();
      renderPage();
      await screen.findByText('Bob');

      await user.click(screen.getByRole('button', { name: trRx('battle:history.backToLobby') }));

      expect(onNavigate).toHaveBeenCalledWith('/battle');
    });
  });

  describe('stats', () => {
    it('shows the stats card', async () => {
      const { container } = renderPage();
      await screen.findByText('Bob');

      expect(container.querySelector('.battle-stat-item__value')).toHaveTextContent('10');
      expect(container.querySelector('.battle-stat-item--win .battle-stat-item__value')).toHaveTextContent('6');
      expect(container.querySelector('.battle-stat-item--lose .battle-stat-item__value')).toHaveTextContent('3');
      expect(container.querySelector('.battle-stat-item--draw .battle-stat-item__value')).toHaveTextContent('1');
      expect(container.querySelector('.battle-stat-item--rate .battle-stat-item__value')).toHaveTextContent('60.0%');
      expect(screen.getByText(tr('battle:history.stats.winRate'))).toBeInTheDocument();
    });

    it('formats the win rate with one decimal place', async () => {
      getBattleStatsApi.mockResolvedValue({
        success: true,
        data: { totalMatches: 7, wins: 2, losses: 5, draws: 0, winRate: 28.571 },
      });
      const { container } = renderPage();
      await screen.findByText('Bob');

      await waitFor(() =>
        expect(container.querySelector('.battle-stat-item--rate .battle-stat-item__value')).toHaveTextContent('28.6%')
      );
    });

    it('hides the stats card when loading stats fails but still shows the list', async () => {
      getBattleStatsApi.mockRejectedValue(new Error('Network error'));
      const { container } = renderPage();

      expect(await screen.findByText('Bob')).toBeInTheDocument();
      expect(container.querySelector('.battle-stats-card')).not.toBeInTheDocument();
    });

    it('hides the stats card when the stats response is unsuccessful', async () => {
      getBattleStatsApi.mockResolvedValue({ success: false });
      const { container } = renderPage();
      await screen.findByText('Bob');

      expect(container.querySelector('.battle-stats-card')).not.toBeInTheDocument();
    });

    it('loads stats only once', async () => {
      const user = userEvent.setup();
      renderPage();
      await screen.findByText('Bob');

      await user.click(radio(tr('battle:history.result.WIN')));
      await waitFor(() => expect(getBattleHistoryApi).toHaveBeenCalledTimes(2));

      expect(getBattleStatsApi).toHaveBeenCalledTimes(1);
    });
  });

  describe('filters', () => {
    it('selects all filters by default', async () => {
      renderPage();
      await screen.findByText('Bob');

      screen.getAllByRole('radio', { name: tr('battle:history.filter.all') }).forEach((r) => {
        expect(r).toHaveAttribute('aria-checked', 'true');
      });
    });

    it('filters by result and requests the first page', async () => {
      const user = userEvent.setup();
      renderPage();
      await screen.findByText('Bob');

      await user.click(radio(tr('battle:history.result.WIN')));

      await waitFor(() => expect(lastHistoryParams()).toMatchObject({ page: 1, result: 'WIN' }));
      expect(radio(tr('battle:history.result.WIN'))).toHaveAttribute('aria-checked', 'true');
    });

    it('filters by mode', async () => {
      const user = userEvent.setup();
      renderPage();
      await screen.findByText('Bob');

      await user.click(radio(tr('battle:mode.typing')));

      await waitFor(() => expect(lastHistoryParams()).toMatchObject({ mode: 'TYPING', result: undefined }));
    });

    it('filters by match type', async () => {
      const user = userEvent.setup();
      renderPage();
      await screen.findByText('Bob');

      await user.click(radio(tr('battle:history.matchType.ROOM')));

      await waitFor(() => expect(lastHistoryParams()).toMatchObject({ matchType: 'ROOM' }));
    });

    it('combines several filters', async () => {
      const user = userEvent.setup();
      renderPage();
      await screen.findByText('Bob');

      await user.click(radio(tr('battle:history.result.LOSE')));
      await user.click(radio(tr('battle:mode.typing')));
      await user.click(radio(tr('battle:history.matchType.RANDOM')));

      await waitFor(() =>
        expect(lastHistoryParams()).toMatchObject({ result: 'LOSE', mode: 'TYPING', matchType: 'RANDOM' })
      );
    });

    it('removes a filter when ALL is selected again', async () => {
      const user = userEvent.setup();
      renderPage();
      await screen.findByText('Bob');
      await user.click(radio(tr('battle:history.result.WIN')));
      await waitFor(() => expect(lastHistoryParams().result).toBe('WIN'));

      await user.click(within(screen.getAllByRole('radiogroup')[0]).getByRole('radio', { name: tr('battle:history.filter.all') }));

      await waitFor(() => expect(lastHistoryParams().result).toBeUndefined());
    });

    it('goes back to the first page when a filter changes', async () => {
      const user = userEvent.setup();
      getBattleHistoryApi.mockResolvedValue(historyResponse([buildItem()], 3));
      renderPage();
      await screen.findByText('Bob');
      await user.click(screen.getByRole('button', { name: '3' }));
      await waitFor(() => expect(lastHistoryParams().page).toBe(3));

      await user.click(radio(tr('battle:history.result.WIN')));

      await waitFor(() => expect(lastHistoryParams()).toMatchObject({ page: 1, result: 'WIN' }));
    });
  });

  describe('pagination', () => {
    it('does not render pagination for a single page', async () => {
      renderPage();
      await screen.findByText('Bob');

      expect(document.querySelector('.pagination')).not.toBeInTheDocument();
    });

    it('renders pagination and loads the selected page', async () => {
      const user = userEvent.setup();
      getBattleHistoryApi.mockResolvedValue(historyResponse([buildItem()], 3));
      renderPage();
      await screen.findByText('Bob');

      await user.click(screen.getByRole('button', { name: '2' }));

      await waitFor(() => expect(lastHistoryParams().page).toBe(2));
      expect(Element.prototype.scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth' });
    });
  });

  describe('empty states', () => {
    it('shows the empty state when the user has no matches', async () => {
      getBattleHistoryApi.mockResolvedValue(historyResponse([]));
      renderPage();

      expect(await screen.findByText(tr('battle:history.emptyTitle'))).toBeInTheDocument();
      expect(screen.getByText(tr('battle:history.empty'))).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: tr('battle:history.clearFilters') })).not.toBeInTheDocument();
    });

    it('shows the filtered empty state and clears filters', async () => {
      const user = userEvent.setup();
      renderPage();
      await screen.findByText('Bob');
      getBattleHistoryApi.mockResolvedValue(historyResponse([]));

      await user.click(radio(tr('battle:history.result.DRAW')));

      expect(await screen.findByText(tr('battle:history.emptyFiltered'))).toBeInTheDocument();

      getBattleHistoryApi.mockResolvedValue(historyResponse([buildItem()]));
      await user.click(screen.getByRole('button', { name: tr('battle:history.clearFilters') }));

      expect(await screen.findByText('Bob')).toBeInTheDocument();
      expect(lastHistoryParams()).toMatchObject({ page: 1, result: undefined, mode: undefined, matchType: undefined });
      expect(radio(tr('battle:history.result.DRAW'))).toHaveAttribute('aria-checked', 'false');
    });

    it('handles a response without content', async () => {
      getBattleHistoryApi.mockResolvedValue({ success: true, data: {} });
      renderPage();

      expect(await screen.findByText(tr('battle:history.emptyTitle'))).toBeInTheDocument();
    });
  });

  describe('errors', () => {
    it('shows an error and retries loading the history', async () => {
      const user = userEvent.setup();
      getBattleHistoryApi.mockRejectedValueOnce(new Error('Network error'));
      renderPage();

      expect(await screen.findByText(tr('battle:history.loadError'))).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: tr('battle:history.retry') }));

      expect(await screen.findByText('Bob')).toBeInTheDocument();
      expect(screen.queryByText(tr('battle:history.loadError'))).not.toBeInTheDocument();
      expect(getBattleHistoryApi).toHaveBeenCalledTimes(2);
    });

    it('shows an error when the response is unsuccessful', async () => {
      getBattleHistoryApi.mockResolvedValue({ success: false });
      renderPage();

      expect(await screen.findByText(tr('battle:history.loadError'))).toBeInTheDocument();
    });
  });
});
