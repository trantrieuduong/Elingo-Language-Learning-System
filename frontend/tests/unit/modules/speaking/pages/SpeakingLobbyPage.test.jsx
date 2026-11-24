import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SpeakingLobbyPage from '@/modules/speaking/pages/SpeakingLobbyPage';
import { useSpeakingSocket } from '@/modules/speaking/context/SpeakingSocketContext';
import { getSpeakingRoomsApi } from '@/modules/speaking/speakingApi';
import { LANGS, tr, trRx, useLang } from '../../../../utils/i18n';

vi.mock('@/modules/speaking/context/SpeakingSocketContext', () => ({
  useSpeakingSocket: vi.fn(),
}));

vi.mock('@/modules/speaking/speakingApi', () => ({
  getSpeakingRoomsApi: vi.fn(),
}));

describe('SpeakingLobbyPage page component', () => {
  let socket;
  let onNavigate;

  const buildRoom = (overrides = {}) => ({
    roomCode: 'ABC234',
    topic: 'Travel plans',
    status: 'WAITING',
    host: { displayName: 'Alice', avatarUrl: null },
    participantCount: 1,
    maxParticipants: 4,
    ...overrides,
  });

  const roomsResponse = (content = [buildRoom()], totalPages = 1) => ({
    data: { content, totalPages },
  });

  const buildSocket = (overrides = {}) => ({
    connection: 'connected',
    busy: false,
    error: null,
    roomState: null,
    createdRoomCode: null,
    activeRoomCode: null,
    activeRoomChecked: false,
    createRoom: vi.fn(),
    joinRoom: vi.fn(),
    checkActiveRoom: vi.fn(),
    clearError: vi.fn(),
    clearCreated: vi.fn(),
    ...overrides,
  });

  const page = () => <SpeakingLobbyPage onNavigate={onNavigate} />;

  const renderLobby = (overrides = {}) => {
    socket = buildSocket(overrides);
    useSpeakingSocket.mockReturnValue(socket);
    return render(page());
  };

  // Đổi state của socket context giữa các lần render (giữ nguyên các hàm mock)
  const updateSocket = (view, overrides) => {
    socket = { ...socket, ...overrides };
    useSpeakingSocket.mockReturnValue(socket);
    view.rerender(page());
  };

  const renderLoaded = async (overrides) => {
    const view = renderLobby(overrides);
    await screen.findByText('Travel plans');
    return view;
  };

  const deferred = () => {
    let resolve;
    let reject;
    const promise = new Promise((res, rej) => {
      resolve = res;
      reject = rej;
    });
    return { promise, resolve, reject };
  };

  const codeInput = () => screen.getByPlaceholderText(tr('speaking:lobby.codePlaceholder'));
  const codeJoinButton = () =>
    within(codeInput().closest('form')).getByRole('button', { name: trRx('speaking:lobby.join') });
  const createButton = () =>
    within(
      screen.getByRole('heading', { name: tr('speaking:lobby.createRoom') }).closest('.speaking-lobby-action-card')
    ).getByRole('button', { name: tr('speaking:lobby.createRoom') });
  const capacityPill = (count) =>
    screen.getByRole('button', { name: tr('speaking:lobby.capacityOption', { count }) });
  const lastRoomsParams = () => getSpeakingRoomsApi.mock.calls.at(-1)[0];

  beforeEach(() => {
    onNavigate = vi.fn();
    getSpeakingRoomsApi.mockResolvedValue(roomsResponse());
  });

  describe.each(LANGS)('layout [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('shows the title, the two entry cards and the history link', async () => {
      await renderLoaded();

      expect(screen.getByRole('heading', { level: 1, name: tr('speaking:lobby.title') })).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:lobby.subtitle'))).toBeInTheDocument();
      expect(createButton()).toBeEnabled();
      expect(screen.getByRole('heading', { name: tr('speaking:lobby.joinByCode') })).toBeInTheDocument();
      expect(codeInput()).toBeInTheDocument();
      expect(screen.getByRole('button', { name: tr('speaking:lobby.history') })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: tr('speaking:lobby.publicRooms') })).toBeInTheDocument();
    });
  });

  it('navigates to the history page', async () => {
    const user = userEvent.setup();
    await renderLoaded();

    await user.click(screen.getByRole('button', { name: tr('speaking:lobby.history') }));

    expect(onNavigate).toHaveBeenCalledWith('/speaking/history');
  });

  describe('room list', () => {
    it('requests the first page of 10 public rooms without filters', async () => {
      await renderLoaded();

      expect(getSpeakingRoomsApi).toHaveBeenCalledTimes(1);
      expect(getSpeakingRoomsApi).toHaveBeenCalledWith({
        page: 1,
        size: 10,
        keyword: '',
        maxParticipants: undefined,
      });
    });

    it('shows a spinner while loading, then the rooms', async () => {
      const request = deferred();
      getSpeakingRoomsApi.mockReturnValue(request.promise);

      const { container } = renderLobby();
      expect(container.querySelector('.page-loading')).toBeInTheDocument();

      await act(async () => {
        request.resolve(
          roomsResponse([buildRoom(), buildRoom({ roomCode: 'DEF567', topic: 'Job interview' })])
        );
      });

      expect(container.querySelector('.page-loading')).not.toBeInTheDocument();
      expect(screen.getByText('Travel plans')).toBeInTheDocument();
      expect(screen.getByText('Job interview')).toBeInTheDocument();
    });

    it('shows the empty state with a create action when there are no rooms', async () => {
      const user = userEvent.setup();
      getSpeakingRoomsApi.mockResolvedValue(roomsResponse([]));
      renderLobby();

      expect(await screen.findByText(tr('speaking:lobby.empty'))).toBeInTheDocument();

      const buttons = screen.getAllByRole('button', { name: tr('speaking:lobby.createRoom') });
      await user.click(buttons[buttons.length - 1]);

      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('shows an error state with a retry that reloads the rooms', async () => {
      const user = userEvent.setup();
      getSpeakingRoomsApi.mockRejectedValueOnce(new Error('boom'));
      renderLobby();

      expect(await screen.findByText(tr('speaking:lobby.loadError'))).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: tr('speaking:lobby.retry') }));

      expect(await screen.findByText('Travel plans')).toBeInTheDocument();
      expect(getSpeakingRoomsApi).toHaveBeenCalledTimes(2);
    });

    it('reloads the rooms with the refresh button', async () => {
      const user = userEvent.setup();
      await renderLoaded();

      await user.click(screen.getByRole('button', { name: trRx('speaking:lobby.refresh') }));

      await waitFor(() => expect(getSpeakingRoomsApi).toHaveBeenCalledTimes(2));
      await screen.findByText('Travel plans');
    });

    it('disables the refresh button while loading', () => {
      getSpeakingRoomsApi.mockReturnValue(new Promise(() => {}));
      renderLobby();

      expect(screen.getByRole('button', { name: trRx('speaking:lobby.refresh') })).toBeDisabled();
    });

    it('paginates the rooms', async () => {
      const user = userEvent.setup();
      getSpeakingRoomsApi.mockResolvedValue(roomsResponse([buildRoom()], 3));
      renderLobby();
      await screen.findByText('Travel plans');

      await user.click(screen.getByRole('button', { name: '2' }));

      await waitFor(() => expect(lastRoomsParams()).toMatchObject({ page: 2 }));
    });

    it('ignores a stale response that arrives after a newer one', async () => {
      const user = userEvent.setup();
      const first = deferred();
      const second = deferred();
      getSpeakingRoomsApi.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
      renderLobby();

      await user.click(capacityPill(3));
      await act(async () => {
        second.resolve(roomsResponse([buildRoom({ roomCode: 'NEW234', topic: 'Newer result' })]));
      });
      await act(async () => {
        first.resolve(roomsResponse([buildRoom({ roomCode: 'OLD234', topic: 'Older result' })]));
      });

      expect(screen.getByText('Newer result')).toBeInTheDocument();
      expect(screen.queryByText('Older result')).not.toBeInTheDocument();
    });
  });

  describe('search and filters', () => {
    it('filters by capacity and can clear the filter', async () => {
      const user = userEvent.setup();
      await renderLoaded();

      await user.click(capacityPill(3));

      await waitFor(() => expect(lastRoomsParams()).toMatchObject({ maxParticipants: 3 }));

      await user.click(screen.getByRole('button', { name: trRx('speaking:lobby.clearFilters') }));

      await waitFor(() => expect(lastRoomsParams().maxParticipants).toBeUndefined());
    });

    it('goes back to all capacities with the "all" pill', async () => {
      const user = userEvent.setup();
      await renderLoaded();
      await user.click(capacityPill(2));
      await waitFor(() => expect(lastRoomsParams()).toMatchObject({ maxParticipants: 2 }));

      await user.click(screen.getByRole('button', { name: tr('speaking:lobby.capacityAll') }));

      await waitFor(() => expect(lastRoomsParams().maxParticipants).toBeUndefined());
    });

    it('explains that nothing matches the filters and offers to clear them', async () => {
      const user = userEvent.setup();
      await renderLoaded();
      getSpeakingRoomsApi.mockResolvedValue(roomsResponse([]));

      await user.click(capacityPill(2));

      expect(await screen.findByText(tr('speaking:lobby.emptyFiltered'))).toBeInTheDocument();

      getSpeakingRoomsApi.mockResolvedValue(roomsResponse());
      const clearButtons = screen.getAllByRole('button', { name: trRx('speaking:lobby.clearFilters') });
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
        getSpeakingRoomsApi.mockClear();

        await user.type(screen.getByRole('searchbox'), 'ie');
        expect(getSpeakingRoomsApi).not.toHaveBeenCalled();

        act(() => {
          vi.advanceTimersByTime(500);
        });

        await waitFor(() =>
          expect(getSpeakingRoomsApi).toHaveBeenCalledWith({
            page: 1,
            size: 10,
            keyword: 'ie',
            maxParticipants: undefined,
          })
        );
      });

      it('clears the keyword from the search box and from the filters button', async () => {
        const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
        await renderLoaded();
        await user.type(screen.getByRole('searchbox'), 'ie');
        act(() => {
          vi.advanceTimersByTime(500);
        });
        await waitFor(() => expect(lastRoomsParams()).toMatchObject({ keyword: 'ie' }));

        await user.click(screen.getByRole('button', { name: trRx('speaking:lobby.clearFilters') }));

        expect(screen.getByRole('searchbox')).toHaveValue('');
        act(() => {
          vi.advanceTimersByTime(500);
        });
        await waitFor(() => expect(lastRoomsParams()).toMatchObject({ keyword: '' }));
      });
    });
  });

  describe('connection', () => {
    it('warns and disables every action while disconnected', async () => {
      await renderLoaded({ connection: 'disconnected' });

      expect(screen.getByText(tr('speaking:error.socketNotConnected'))).toBeInTheDocument();
      expect(createButton()).toBeDisabled();
      expect(codeInput()).toBeDisabled();
      expect(codeJoinButton()).toBeDisabled();
    });

    it('shows no warning while connected', async () => {
      await renderLoaded();

      expect(screen.queryByText(tr('speaking:error.socketNotConnected'))).not.toBeInTheDocument();
    });

    it('checks for an active room once connected', async () => {
      await renderLoaded();

      expect(socket.checkActiveRoom).toHaveBeenCalledTimes(1);
    });

    it('does not check for an active room while disconnected', async () => {
      await renderLoaded({ connection: 'connecting' });

      expect(socket.checkActiveRoom).not.toHaveBeenCalled();
    });

    it('disables every action while a request is in progress', async () => {
      await renderLoaded({ busy: true });

      expect(createButton()).toBeDisabled();
      expect(codeInput()).toBeDisabled();
    });
  });

  describe('active room', () => {
    it('shows no banner before the check completes or when there is no active room', async () => {
      await renderLoaded({ activeRoomChecked: false, activeRoomCode: 'ABC234' });

      expect(screen.queryByText(tr('speaking:lobby.activeRoom.back'))).not.toBeInTheDocument();
    });

    it('offers to go back to the room the user is already in', async () => {
      const user = userEvent.setup();
      await renderLoaded({ activeRoomChecked: true, activeRoomCode: 'XYZ789' });

      expect(
        screen.getByText(tr('speaking:lobby.activeRoom.title', { code: 'XYZ789' }))
      ).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:lobby.activeRoom.desc'))).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: trRx('speaking:lobby.activeRoom.back') }));

      expect(socket.clearError).toHaveBeenCalled();
      expect(onNavigate).toHaveBeenCalledWith('/speaking/room/XYZ789');
    });

    it('blocks creating and joining other rooms', async () => {
      await renderLoaded({ activeRoomChecked: true, activeRoomCode: 'XYZ789' });

      expect(createButton()).toBeDisabled();
      expect(createButton()).toHaveAttribute('title', tr('speaking:lobby.activeRoom.blocked'));
      expect(codeInput()).toBeDisabled();
      expect(codeJoinButton()).toBeDisabled();
    });

    it('blocks joining a listed room and explains why', async () => {
      await renderLoaded({ activeRoomChecked: true, activeRoomCode: 'XYZ789' });

      const cardButton = screen
        .getAllByRole('button', { name: trRx('speaking:lobby.join') })
        .find((button) => button.classList.contains('speaking-room-card__btn'));
      expect(cardButton).toBeDisabled();
      expect(cardButton).toHaveAttribute('title', tr('speaking:lobby.activeRoom.blocked'));
    });
  });

  describe('join by code', () => {
    it('keeps the join button disabled until a code is typed', async () => {
      const user = userEvent.setup();
      await renderLoaded();
      expect(codeJoinButton()).toBeDisabled();

      await user.type(codeInput(), 'a');

      expect(codeJoinButton()).toBeEnabled();
    });

    it('uppercases the code as the user types and limits it to 6 characters', async () => {
      const user = userEvent.setup();
      await renderLoaded();

      await user.type(codeInput(), 'abc234zz');

      expect(codeInput()).toHaveValue('ABC234');
    });

    it('joins the room with a valid code', async () => {
      const user = userEvent.setup();
      await renderLoaded();

      await user.type(codeInput(), 'abc234');
      await user.click(codeJoinButton());

      expect(socket.clearError).toHaveBeenCalled();
      expect(socket.joinRoom).toHaveBeenCalledTimes(1);
      expect(socket.joinRoom).toHaveBeenCalledWith('ABC234');
    });

    it('joins with Enter', async () => {
      const user = userEvent.setup();
      await renderLoaded();

      await user.type(codeInput(), 'abc234{Enter}');

      expect(socket.joinRoom).toHaveBeenCalledWith('ABC234');
    });

    it.each([
      ['too short', 'ABC23'],
      ['containing the letter O', 'ABCDEO'],
      ['containing the letter I', 'ABCDEI'],
      ['containing the digit 0', 'ABCDE0'],
      ['containing the digit 1', 'ABCDE1'],
      ['containing a symbol', 'ABC-23'],
    ])('rejects a code that is %s', async (_label, code) => {
      const user = userEvent.setup();
      await renderLoaded();

      await user.type(codeInput(), code);
      await user.click(codeJoinButton());

      expect(socket.joinRoom).not.toHaveBeenCalled();
      expect(await screen.findByRole('alert')).toHaveTextContent(tr('speaking:lobby.codeInvalid'));
    });

    it('hides the validation message once the user edits the code', async () => {
      const user = userEvent.setup();
      await renderLoaded();
      await user.type(codeInput(), 'ABC');
      await user.click(codeJoinButton());
      expect(await screen.findByRole('alert')).toBeInTheDocument();

      await user.type(codeInput(), '2');

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('joins a room from its card', async () => {
      const user = userEvent.setup();
      await renderLoaded();

      const card = screen.getByRole('heading', { name: 'Travel plans' }).closest('.speaking-room-card');
      await user.click(within(card).getByRole('button', { name: trRx('speaking:lobby.join') }));

      expect(socket.clearError).toHaveBeenCalled();
      expect(socket.joinRoom).toHaveBeenCalledWith('ABC234');
    });
  });

  describe('navigation after joining or creating', () => {
    it('opens the room once the server confirms the room the user asked for', async () => {
      const user = userEvent.setup();
      const view = await renderLoaded();
      await user.type(codeInput(), 'abc234');
      await user.click(codeJoinButton());
      expect(onNavigate).not.toHaveBeenCalled();

      updateSocket(view, { roomState: { roomCode: 'ABC234' } });

      expect(onNavigate).toHaveBeenCalledWith('/speaking/room/ABC234');
    });

    it('stays when the server confirms a different room', async () => {
      const user = userEvent.setup();
      const view = await renderLoaded();
      await user.type(codeInput(), 'abc234');
      await user.click(codeJoinButton());

      updateSocket(view, { roomState: { roomCode: 'OTHER9' } });

      expect(onNavigate).not.toHaveBeenCalled();
    });

    it('does not navigate for a room state the user did not ask to join', async () => {
      await renderLoaded({ roomState: { roomCode: 'ABC234' } });

      expect(onNavigate).not.toHaveBeenCalled();
    });

    it('forgets the pending join when the socket reports an error', async () => {
      const user = userEvent.setup();
      const view = await renderLoaded();
      await user.type(codeInput(), 'abc234');
      await user.click(codeJoinButton());

      updateSocket(view, { error: 'SPEAKING_ROOM_FULL' });
      updateSocket(view, { error: null, roomState: { roomCode: 'ABC234' } });

      expect(onNavigate).not.toHaveBeenCalled();
    });

    it('opens the room that was just created', async () => {
      await renderLoaded({ createdRoomCode: 'NEW234' });

      expect(socket.clearCreated).toHaveBeenCalledTimes(1);
      expect(onNavigate).toHaveBeenCalledWith('/speaking/room/NEW234');
    });
  });

  describe('create room modal', () => {
    it('opens from the create card after clearing previous errors', async () => {
      const user = userEvent.setup();
      await renderLoaded();

      await user.click(createButton());

      expect(socket.clearError).toHaveBeenCalled();
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('creates a room with the submitted settings', async () => {
      const user = userEvent.setup();
      await renderLoaded();
      await user.click(createButton());

      const dialog = screen.getByRole('dialog');
      await user.click(within(dialog).getByRole('button', { name: trRx('speaking:create.submit') }));

      expect(socket.createRoom).toHaveBeenCalledWith({
        topic: null,
        maxParticipants: 4,
        isPrivate: false,
      });
    });

    it('closes and clears the error when cancelled', async () => {
      const user = userEvent.setup();
      await renderLoaded();
      await user.click(createButton());
      socket.clearError.mockClear();

      const dialog = screen.getByRole('dialog');
      await user.click(within(dialog).getAllByRole('button', { name: tr('speaking:create.cancel') })[0]);

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(socket.clearError).toHaveBeenCalledTimes(1);
    });

    it('passes the busy state to the modal', async () => {
      const user = userEvent.setup();
      const view = await renderLoaded();
      await user.click(createButton());

      updateSocket(view, { busy: true });

      const dialog = screen.getByRole('dialog');
      expect(within(dialog).getByRole('button', { name: trRx('speaking:create.submit') })).toBeDisabled();
    });
  });

  describe('errors', () => {
    it('shows a socket error in a toast', async () => {
      await renderLoaded({ error: 'SPEAKING_ROOM_FULL' });

      expect(screen.getByRole('alert')).toHaveTextContent(tr('api:error.SPEAKING_ROOM_FULL'));
    });

    it('explains a missing socket connection in the toast', async () => {
      await renderLoaded({ error: 'SOCKET_NOT_CONNECTED' });

      expect(screen.getByRole('alert')).toHaveTextContent(tr('speaking:error.socketNotConnected'));
    });

    it('falls back to a generic message for an unknown error code', async () => {
      await renderLoaded({ error: 'SOMETHING_NEW' });

      expect(screen.getByRole('alert')).toHaveTextContent(tr('api:common.fallback'));
    });

    it('does not toast SPEAKING_ALREADY_IN_ROOM because the banner covers it', async () => {
      await renderLoaded({ error: 'SPEAKING_ALREADY_IN_ROOM' });

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('shows the error inside the open modal instead of a toast', async () => {
      const user = userEvent.setup();
      const view = await renderLoaded();
      await user.click(createButton());

      updateSocket(view, { error: 'SPEAKING_ROOM_FULL' });

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(
        within(screen.getByRole('dialog')).getByText(tr('api:error.SPEAKING_ROOM_FULL'))
      ).toBeInTheDocument();
    });

    it('dismisses the toast automatically and clears the error', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true });
      try {
        await renderLoaded({ error: 'SPEAKING_ROOM_FULL' });
        socket.clearError.mockClear();

        act(() => {
          vi.advanceTimersByTime(3000);
        });

        expect(socket.clearError).toHaveBeenCalled();
      } finally {
        vi.useRealTimers();
      }
    });
  });
});
