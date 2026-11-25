import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from '@/i18n';
import SpeakingHistoryPage from '@/modules/speaking/pages/SpeakingHistoryPage';
import { useSpeakingSocket } from '@/modules/speaking/context/SpeakingSocketContext';
import {
  getSpeakingHistoryApi,
  getSpeakingHistorySummaryApi,
} from '@/modules/speaking/speakingApi';
import { LANGS, tr, trRx, useLang } from '../../../../utils/i18n';

vi.mock('@/modules/speaking/context/SpeakingSocketContext', () => ({
  useSpeakingSocket: vi.fn(),
}));

vi.mock('@/modules/speaking/speakingApi', () => ({
  getSpeakingHistoryApi: vi.fn(),
  getSpeakingHistorySummaryApi: vi.fn(),
}));

describe('SpeakingHistoryPage page component', () => {
  let socket;
  let onNavigate;

  const LEFT_AT = '2026-03-05T10:30:00.000Z';

  const buildItem = (overrides = {}) => ({
    roomCode: 'ABC234',
    topic: 'Travel plans',
    maxParticipants: 4,
    role: 'HOST',
    roomStatus: 'ACTIVE',
    leftAt: LEFT_AT,
    durationSeconds: 125,
    visitCount: 1,
    canRejoin: true,
    ...overrides,
  });

  const historyResponse = (content = [buildItem()], totalPages = 1) => ({
    data: { content, totalPages },
  });

  const summaryResponse = {
    data: { totalSeconds: 3725, roomCount: 4, sessionCount: 6 },
  };

  const buildSocket = (overrides = {}) => ({
    connection: 'connected',
    activeRoomCode: null,
    checkActiveRoom: vi.fn(),
    ...overrides,
  });

  const page = () => <SpeakingHistoryPage onNavigate={onNavigate} />;

  const renderPage = (overrides = {}) => {
    socket = buildSocket(overrides);
    useSpeakingSocket.mockReturnValue(socket);
    return render(page());
  };

  const renderLoaded = async (overrides) => {
    const view = renderPage(overrides);
    await screen.findByText('Travel plans');
    return view;
  };

  const deferred = () => {
    let resolve;
    const promise = new Promise((res) => {
      resolve = res;
    });
    return { promise, resolve };
  };

  const rejoinButton = (name = trRx('speaking:history.rejoin')) =>
    screen.getByRole('button', { name });
  const lastParams = () => getSpeakingHistoryApi.mock.calls.at(-1)[0];
  const capacityPill = (count) =>
    screen.getByRole('button', { name: tr('speaking:lobby.capacityOption', { count }) });

  beforeEach(() => {
    onNavigate = vi.fn();
    getSpeakingHistoryApi.mockResolvedValue(historyResponse());
    getSpeakingHistorySummaryApi.mockResolvedValue(summaryResponse);
  });

  describe.each(LANGS)('layout [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('shows the title, subtitle and a back link', async () => {
      await renderLoaded();

      expect(screen.getByRole('heading', { level: 1, name: tr('speaking:history.title') })).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:history.subtitle'))).toBeInTheDocument();
      expect(screen.getByRole('button', { name: trRx('speaking:history.backToLobby') })).toBeInTheDocument();
    });
  });

  it('goes back to the lobby', async () => {
    const user = userEvent.setup();
    await renderLoaded();

    await user.click(screen.getByRole('button', { name: trRx('speaking:history.backToLobby') }));

    expect(onNavigate).toHaveBeenCalledWith('/speaking');
  });

  describe('summary', () => {
    it('shows the total time, room count and session count', async () => {
      await renderLoaded();

      expect(await screen.findByText('1:02:05')).toBeInTheDocument();
      expect(screen.getByText('4')).toBeInTheDocument();
      expect(screen.getByText('6')).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:history.summary.totalTime'))).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:history.summary.rooms'))).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:history.summary.sessions'))).toBeInTheDocument();
    });

    it('does not block the list when the summary fails to load', async () => {
      getSpeakingHistorySummaryApi.mockRejectedValue(new Error('boom'));

      await renderLoaded();

      expect(screen.queryByText(tr('speaking:history.summary.totalTime'))).not.toBeInTheDocument();
      expect(screen.queryByText(tr('speaking:history.loadError'))).not.toBeInTheDocument();
    });

    it('hides the summary when the response has no data', async () => {
      getSpeakingHistorySummaryApi.mockResolvedValue({});

      await renderLoaded();

      expect(screen.queryByText(tr('speaking:history.summary.totalTime'))).not.toBeInTheDocument();
    });
  });

  describe('history list', () => {
    it('requests the first page of 10 sessions without filters', async () => {
      await renderLoaded();

      expect(getSpeakingHistoryApi).toHaveBeenCalledTimes(1);
      expect(getSpeakingHistoryApi).toHaveBeenCalledWith({
        page: 1,
        size: 10,
        keyword: '',
        maxParticipants: null,
      });
    });

    it('shows a spinner while loading', () => {
      getSpeakingHistoryApi.mockReturnValue(new Promise(() => {}));

      const { container } = renderPage();

      expect(container.querySelector('.page-loading')).toBeInTheDocument();
    });

    it('shows the details of each session', async () => {
      const { container } = await renderLoaded();

      expect(screen.getByRole('heading', { name: 'Travel plans' })).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:lobby.capacityOption', { count: 4 }), { selector: 'span' })).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:history.role.HOST'))).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:history.status.ACTIVE'))).toBeInTheDocument();
      expect(screen.getByText('ABC234')).toBeInTheDocument();
      expect(screen.getByText(new Date(LEFT_AT).toLocaleString(i18n.language))).toBeInTheDocument();
      expect(screen.getByText('2:05')).toBeInTheDocument();
      expect(container.querySelector('.speaking-history-status--ACTIVE')).toBeInTheDocument();
    });

    it('uses a placeholder for a session without topic and tolerates a missing leave time', async () => {
      getSpeakingHistoryApi.mockResolvedValue(historyResponse([buildItem({ topic: null, leftAt: null })]));

      renderPage();

      expect(await screen.findByRole('heading', { name: tr('speaking:lobby.noTopic') })).toBeInTheDocument();
    });

    it('shows the translated role and status of a closed guest session', async () => {
      getSpeakingHistoryApi.mockResolvedValue(
        historyResponse([buildItem({ role: 'GUEST', roomStatus: 'CLOSED', canRejoin: false })])
      );

      renderPage();

      expect(await screen.findByText(tr('speaking:history.role.GUEST'))).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:history.status.CLOSED'), { selector: 'span.speaking-history-status' })).toBeInTheDocument();
    });

    it('shows the number of visits only when the room was visited more than once', async () => {
      getSpeakingHistoryApi.mockResolvedValue(
        historyResponse([
          buildItem({ roomCode: 'ABC234', visitCount: 3 }),
          buildItem({ roomCode: 'DEF567', topic: 'Single visit', visitCount: 1 }),
        ])
      );

      renderPage();

      expect(await screen.findByText(tr('speaking:history.visits', { count: 3 }))).toBeInTheDocument();
      expect(screen.queryByText(tr('speaking:history.visits', { count: 1 }))).not.toBeInTheDocument();
    });

    it('paginates the sessions', async () => {
      const user = userEvent.setup();
      getSpeakingHistoryApi.mockResolvedValue(historyResponse([buildItem()], 3));
      renderPage();
      await screen.findByText('Travel plans');

      await user.click(screen.getByRole('button', { name: '2' }));

      await waitFor(() => expect(lastParams()).toMatchObject({ page: 2 }));
    });

    it('ignores a stale response that arrives after a newer one', async () => {
      const user = userEvent.setup();
      const first = deferred();
      const second = deferred();
      getSpeakingHistoryApi.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
      renderPage();

      await user.click(capacityPill(3));
      await act(async () => {
        second.resolve(historyResponse([buildItem({ roomCode: 'NEW234', topic: 'Newer result' })]));
      });
      await act(async () => {
        first.resolve(historyResponse([buildItem({ roomCode: 'OLD234', topic: 'Older result' })]));
      });

      expect(screen.getByText('Newer result')).toBeInTheDocument();
      expect(screen.queryByText('Older result')).not.toBeInTheDocument();
    });
  });

  describe('empty and error states', () => {
    it('invites the user to join a room when there is no history', async () => {
      getSpeakingHistoryApi.mockResolvedValue(historyResponse([]));

      renderPage();

      expect(await screen.findByText(tr('speaking:history.empty'))).toBeInTheDocument();
    });

    it.each([
      ['a missing data field', {}],
      ['a rejected request', null],
    ])('shows an error with a retry for %s', async (_label, response) => {
      const user = userEvent.setup();
      if (response) getSpeakingHistoryApi.mockResolvedValueOnce(response);
      else getSpeakingHistoryApi.mockRejectedValueOnce(new Error('boom'));
      renderPage();

      expect(await screen.findByText(tr('speaking:history.loadError'))).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: tr('speaking:history.retry') }));

      expect(await screen.findByText('Travel plans')).toBeInTheDocument();
      expect(getSpeakingHistoryApi).toHaveBeenCalledTimes(2);
    });
  });

  describe('filters', () => {
    it('filters by capacity and clears the filter', async () => {
      const user = userEvent.setup();
      await renderLoaded();

      await user.click(capacityPill(2));
      await waitFor(() => expect(lastParams()).toMatchObject({ maxParticipants: 2 }));

      await user.click(screen.getByRole('button', { name: tr('speaking:lobby.clearFilters') }));

      await waitFor(() => expect(lastParams().maxParticipants).toBeNull());
    });

    it('shows the filtered empty state with an action to clear the filters', async () => {
      const user = userEvent.setup();
      await renderLoaded();
      getSpeakingHistoryApi.mockResolvedValue(historyResponse([]));

      await user.click(capacityPill(3));

      expect(await screen.findByText(tr('speaking:history.emptyFiltered'))).toBeInTheDocument();

      getSpeakingHistoryApi.mockResolvedValue(historyResponse());
      const clearButtons = screen.getAllByRole('button', { name: tr('speaking:lobby.clearFilters') });
      await user.click(clearButtons[clearButtons.length - 1]);

      expect(await screen.findByText('Travel plans')).toBeInTheDocument();
    });

    describe('keyword', () => {
      beforeEach(() => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
      });

      afterEach(() => {
        vi.useRealTimers();
      });

      it('searches only after the user stops typing', async () => {
        const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
        await renderLoaded();
        getSpeakingHistoryApi.mockClear();

        await user.type(screen.getByRole('searchbox'), 'ie');
        expect(getSpeakingHistoryApi).not.toHaveBeenCalled();

        act(() => {
          vi.advanceTimersByTime(500);
        });

        await waitFor(() =>
          expect(getSpeakingHistoryApi).toHaveBeenCalledWith({
            page: 1,
            size: 10,
            keyword: 'ie',
            maxParticipants: null,
          })
        );
      });
    });
  });

  describe('rejoining', () => {
    it('opens the room when the session can be rejoined', async () => {
      const user = userEvent.setup();
      await renderLoaded();

      expect(rejoinButton()).toBeEnabled();
      await user.click(rejoinButton());

      expect(onNavigate).toHaveBeenCalledWith('/speaking/room/ABC234');
    });

    it('labels the button "back to room" for the room the user is currently in', async () => {
      const user = userEvent.setup();
      await renderLoaded({ activeRoomCode: 'ABC234' });

      const button = rejoinButton(tr('speaking:history.backToRoom'));
      expect(button).toBeEnabled();
      await user.click(button);

      expect(onNavigate).toHaveBeenCalledWith('/speaking/room/ABC234');
    });

    it('is disabled with an explanation when the user is in another room', async () => {
      await renderLoaded({ activeRoomCode: 'OTHER9' });

      expect(rejoinButton()).toBeDisabled();
      expect(rejoinButton()).toHaveAttribute('title', tr('speaking:history.cannotRejoin.inOtherRoom'));
      expect(screen.getByText(tr('speaking:history.cannotRejoin.inOtherRoom'))).toBeInTheDocument();
    });

    it('is disabled when the room has been closed', async () => {
      getSpeakingHistoryApi.mockResolvedValue(
        historyResponse([buildItem({ canRejoin: false, roomStatus: 'CLOSED' })])
      );

      renderPage();

      expect(await screen.findByText(tr('speaking:history.cannotRejoin.closed'))).toBeInTheDocument();
      expect(rejoinButton()).toBeDisabled();
    });

    it('is disabled when the user was kicked from the room', async () => {
      getSpeakingHistoryApi.mockResolvedValue(
        historyResponse([buildItem({ canRejoin: false, roomStatus: 'ACTIVE' })])
      );

      renderPage();

      expect(await screen.findByText(tr('speaking:history.cannotRejoin.kicked'))).toBeInTheDocument();
      expect(rejoinButton()).toBeDisabled();
    });

    it('is disabled without an explanation while the socket is disconnected', async () => {
      await renderLoaded({ connection: 'disconnected' });

      expect(rejoinButton()).toBeDisabled();
      expect(rejoinButton()).not.toHaveAttribute('title');
    });
  });

  describe('active room check', () => {
    it('asks the server once the socket is connected', async () => {
      await renderLoaded();

      expect(socket.checkActiveRoom).toHaveBeenCalledTimes(1);
    });

    it('does not ask while the socket is not connected', async () => {
      await renderLoaded({ connection: 'connecting' });

      expect(socket.checkActiveRoom).not.toHaveBeenCalled();
    });
  });
});
