import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SpeakingRoomPage from '@/modules/speaking/pages/SpeakingRoomPage';
import { useSpeakingRoom } from '@/modules/speaking/context/SpeakingRoomContext';
import { getBansApi } from '@/modules/speaking/speakingApi';
import { LANGS, tr, trRx, useLang } from '../../../../utils/i18n';

vi.mock('@/modules/speaking/context/SpeakingRoomContext', () => ({
  useSpeakingRoom: vi.fn(),
}));

vi.mock('@/modules/speaking/speakingApi', () => ({
  getBansApi: vi.fn(),
}));

describe('SpeakingRoomPage page component', () => {
  const ROOM_CODE = 'ABC234';
  const originalScrollTo = HTMLElement.prototype.scrollTo;
  const originalPlay = window.HTMLMediaElement.prototype.play;

  let ctx;
  let onNavigate;

  const buildPeer = (userId, overrides = {}) => ({
    userId,
    displayName: `User ${userId}`,
    avatarUrl: null,
    role: 'GUEST',
    link: 'connected',
    grace: false,
    micMuted: false,
    ...overrides,
  });

  const SELF = buildPeer(1, { displayName: 'Me', role: 'HOST', link: undefined });
  const BOB = buildPeer(2, { displayName: 'Bob' });

  const buildCtx = (overrides = {}) => ({
    selfId: 1,
    connection: 'connected',
    phase: 'in',
    room: {
      roomCode: ROOM_CODE,
      topic: 'Travel plans',
      hostId: 1,
      maxParticipants: 4,
      joinedAtMs: Date.now(),
    },
    peers: { 1: SELF, 2: BOB },
    streams: {},
    muted: false,
    micError: null,
    error: null,
    closedReason: null,
    notice: null,
    setNotice: vi.fn(),
    isHost: true,
    requestMute: vi.fn(),
    kickMember: vi.fn().mockResolvedValue(undefined),
    unbanMember: vi.fn().mockResolvedValue(undefined),
    kicked: false,
    summary: null,
    clearSummary: vi.fn(),
    transferHostAndLeave: vi.fn().mockResolvedValue(null),
    endRoom: vi.fn().mockResolvedValue(null),
    startSession: vi.fn(),
    leaveRoom: vi.fn().mockResolvedValue(null),
    teardown: vi.fn(),
    toggleMute: vi.fn(),
    isPrivate: false,
    chatMessages: [],
    chatUnread: 0,
    chatOpen: false,
    setChatOpen: vi.fn(),
    sendChatMessage: vi.fn().mockReturnValue(true),
    ...overrides,
  });

  const page = (roomCode = ROOM_CODE) => <SpeakingRoomPage roomCode={roomCode} onNavigate={onNavigate} />;

  const renderRoom = (overrides = {}) => {
    ctx = buildCtx(overrides);
    useSpeakingRoom.mockReturnValue(ctx);
    return render(page());
  };

  const updateCtx = (view, overrides) => {
    ctx = { ...ctx, ...overrides };
    useSpeakingRoom.mockReturnValue(ctx);
    view.rerender(page());
  };

  const guestCtx = (overrides = {}) => ({
    isHost: false,
    selfId: 2,
    room: { ...buildCtx().room, hostId: 1 },
    peers: { 1: buildPeer(1, { displayName: 'Host', role: 'HOST' }), 2: buildPeer(2, { displayName: 'Me', link: undefined }) },
    ...overrides,
  });

  const button = (name) => screen.getByRole('button', { name });
  const leaveButton = () => button(tr('speaking:room.leave'));
  const muteBobButton = () => button(tr('speaking:room.muteAction', { name: 'Bob' }));
  const kickBobButton = () => button(tr('speaking:room.kickAction', { name: 'Bob' }));

  beforeEach(() => {
    onNavigate = vi.fn();
    getBansApi.mockResolvedValue({ data: [] });
    HTMLElement.prototype.scrollTo = vi.fn();
    window.HTMLMediaElement.prototype.play = vi.fn().mockResolvedValue(undefined);
  });

  afterEach(() => {
    HTMLElement.prototype.scrollTo = originalScrollTo;
    window.HTMLMediaElement.prototype.play = originalPlay;
    Reflect.deleteProperty(navigator, 'clipboard');
  });

  describe('session lifecycle', () => {
    it('starts the session for the room on mount and tears it down on unmount', () => {
      const { unmount } = renderRoom();

      expect(ctx.startSession).toHaveBeenCalledTimes(1);
      expect(ctx.startSession).toHaveBeenCalledWith(ROOM_CODE);
      expect(ctx.teardown).not.toHaveBeenCalled();

      unmount();

      expect(ctx.teardown).toHaveBeenCalledTimes(1);
    });

    it('restarts the session when the room code changes', () => {
      const view = renderRoom();

      view.rerender(page('XYZ789'));

      expect(ctx.teardown).toHaveBeenCalledTimes(1);
      expect(ctx.startSession).toHaveBeenLastCalledWith('XYZ789');
    });
  });

  describe('preparing phase', () => {
    it('shows only a loading spinner', () => {
      const { container } = renderRoom({ phase: 'preparing', room: null, peers: {} });

      expect(container.querySelector('.page-loading')).toBeInTheDocument();
      expect(screen.queryByRole('heading', { level: 1 })).not.toBeInTheDocument();
    });
  });

  describe('error phase', () => {
    it.each([
      ['MIC_DENIED', 'speaking:room.micDenied'],
      ['MIC_UNAVAILABLE', 'speaking:room.micUnavailable'],
    ])('explains the microphone problem %s and lets the user retry or leave', async (micError, key) => {
      const user = userEvent.setup();
      renderRoom({ phase: 'error', micError, room: null, peers: {} });

      expect(screen.getByText(tr(key))).toBeInTheDocument();

      await user.click(button(tr('speaking:room.retry')));
      expect(ctx.startSession).toHaveBeenCalledTimes(2);
      expect(ctx.startSession).toHaveBeenLastCalledWith(ROOM_CODE);

      await user.click(button(tr('speaking:room.backToLobby')));
      expect(onNavigate).toHaveBeenCalledWith('/speaking');
    });

    it('tells the user they were removed after being disconnected for too long', async () => {
      const user = userEvent.setup();
      renderRoom({ phase: 'error', error: 'SPEAKING_ROOM_ACCESS_DENIED', room: null, peers: {} });

      expect(screen.getByText(tr('speaking:room.removed'))).toBeInTheDocument();

      await user.click(button(tr('speaking:room.backToLobby')));
      expect(onNavigate).toHaveBeenCalledWith('/speaking');
    });

    it('translates other server errors', () => {
      renderRoom({ phase: 'error', error: 'SPEAKING_ROOM_FULL', room: null, peers: {} });

      expect(screen.getByText(tr('api:error.SPEAKING_ROOM_FULL'))).toBeInTheDocument();
    });

    it('falls back to a generic message for an unknown error', () => {
      renderRoom({ phase: 'error', error: 'SOMETHING_NEW', room: null, peers: {} });

      expect(screen.getByText(tr('api:common.fallback'))).toBeInTheDocument();
    });

    it('falls back to a generic message when there is no error code', () => {
      renderRoom({ phase: 'error', error: null, room: null, peers: {} });

      expect(screen.getByText(tr('api:common.fallback'))).toBeInTheDocument();
    });
  });

  describe('closed phase', () => {
    it.each([
      ['the KICKED reason', { closedReason: 'KICKED' }],
      ['the kicked flag', { kicked: true, closedReason: 'ENDED_BY_HOST' }],
    ])('tells the user they were kicked (%s)', async (_label, overrides) => {
      const user = userEvent.setup();
      renderRoom({ phase: 'closed', room: null, peers: {}, ...overrides });

      expect(screen.getByText(tr('speaking:room.kicked.title'))).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:room.kicked.body'))).toBeInTheDocument();

      await user.click(button(tr('speaking:room.kicked.backToLobby')));
      expect(onNavigate).toHaveBeenCalledWith('/speaking');
    });

    it.each(['EMPTY', 'EXPIRED', 'SERVER_RESTART', 'ENDED_BY_HOST'])(
      'explains why the room closed (%s)',
      async (closedReason) => {
        const user = userEvent.setup();
        renderRoom({ phase: 'closed', closedReason, room: null, peers: {} });

        expect(screen.getByText(tr('speaking:room.closedTitle'))).toBeInTheDocument();
        expect(screen.getByText(tr(`speaking:room.closed.${closedReason}`))).toBeInTheDocument();

        await user.click(button(tr('speaking:room.backToLobby')));
        expect(onNavigate).toHaveBeenCalledWith('/speaking');
      }
    );

    it.each([
      ['an unknown reason', 'WHATEVER'],
      ['no reason', null],
    ])('falls back to the empty-room message for %s', (_label, closedReason) => {
      renderRoom({ phase: 'closed', closedReason, room: null, peers: {} });

      expect(screen.getByText(tr('speaking:room.closed.EMPTY'))).toBeInTheDocument();
    });
  });

  describe('room header', () => {
    describe.each(LANGS)('[%s]', (lng) => {
      beforeEach(() => {
        useLang(lng);
      });

      it('shows the room code, topic, visibility and participant count', () => {
        renderRoom();

        expect(screen.getByTitle(ROOM_CODE)).toBeInTheDocument();
        expect(screen.getByRole('heading', { level: 1, name: 'Travel plans' })).toBeInTheDocument();
        expect(screen.getByText(tr('speaking:room.public'))).toBeInTheDocument();
        expect(
          screen.getByRole('heading', { name: tr('speaking:room.participantsWithCount', { count: 2 }) })
        ).toBeInTheDocument();
        expect(screen.getByRole('region', { name: tr('speaking:room.rules.title') })).toBeInTheDocument();
      });
    });

    it('shows a placeholder when the room has no topic', () => {
      renderRoom({ room: { ...buildCtx().room, topic: null } });

      expect(screen.getByRole('heading', { level: 1, name: tr('speaking:lobby.noTopic') })).toBeInTheDocument();
    });

    it('uses the room code from the route until the room state arrives', () => {
      renderRoom({ room: null, peers: {} });

      expect(screen.getByTitle(ROOM_CODE)).toBeInTheDocument();
      expect(screen.getByRole('heading', { level: 1, name: tr('speaking:lobby.noTopic') })).toBeInTheDocument();
    });

    it('shows the private badge and hint for a private room', () => {
      renderRoom({ isPrivate: true });

      expect(screen.getByText(tr('speaking:room.private'))).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:room.privateHint'))).toBeInTheDocument();
      expect(screen.queryByText(tr('speaking:room.public'))).not.toBeInTheDocument();
    });

    it('shows no private hint for a public room', () => {
      renderRoom();

      expect(screen.queryByText(tr('speaking:room.privateHint'))).not.toBeInTheDocument();
    });

    it('shows the elapsed time since the user joined', () => {
      renderRoom({ room: { ...buildCtx().room, joinedAtMs: Date.now() - 65000 } });

      expect(screen.getByLabelText(tr('speaking:room.clock.label'))).toHaveTextContent('1:05');
    });

    it('warns while the socket is reconnecting', () => {
      renderRoom({ connection: 'disconnected' });

      expect(screen.getByRole('alert')).toHaveTextContent(tr('speaking:room.reconnecting'));
    });

    it('shows no warning while connected', () => {
      renderRoom();

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  describe('participants', () => {
    it('invites others to join when the user is alone', () => {
      renderRoom({ peers: { 1: SELF } });

      expect(screen.getByText(tr('speaking:room.waitingOthers'))).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:room.shareCode'))).toBeInTheDocument();
      expect(screen.getByText('Me', { selector: '.speaking-peer-tile__name' })).toBeInTheDocument();
    });

    it('shows a tile for every participant once others have joined', () => {
      const { container } = renderRoom();

      expect(screen.queryByText(tr('speaking:room.waitingOthers'))).not.toBeInTheDocument();
      expect(screen.getByText('Me', { selector: '.speaking-peer-tile__name' })).toBeInTheDocument();
      expect(screen.getByText('Bob', { selector: '.speaking-peer-tile__name' })).toBeInTheDocument();
      expect(container.querySelector('.speaking-peer-grid--count-2')).toBeInTheDocument();
    });

    it('caps the grid layout at four columns', () => {
      const peers = Object.fromEntries([1, 2, 3, 4, 5].map((id) => [id, buildPeer(id)]));
      const { container } = renderRoom({ peers });

      expect(container.querySelector('.speaking-peer-grid--count-4')).toBeInTheDocument();
      expect(container.querySelectorAll('.speaking-peer-tile')).toHaveLength(5);
    });

    it('marks the host and the current user', () => {
      const { container } = renderRoom();

      expect(container.querySelectorAll('.speaking-peer-badge--host')).toHaveLength(1);
      expect(container.querySelectorAll('.speaking-peer-badge--self')).toHaveLength(1);
    });

    it('renders one hidden audio element per remote stream', () => {
      const { container } = renderRoom({ streams: { 2: { id: 'a' }, 3: { id: 'b' } } });

      expect(container.querySelectorAll('audio')).toHaveLength(2);
    });
  });

  describe('copying the room code', () => {
    const installClipboard = (writeText) => {
      Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    };
    const copyButton = () =>
      screen.getByRole('button', { name: tr('speaking:room.copyAria', { code: ROOM_CODE }) });

    it('copies the code and confirms for two seconds', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true });
      try {
        const writeText = vi.fn().mockResolvedValue(undefined);
        installClipboard(writeText);
        renderRoom();

        fireEvent.click(copyButton());

        expect(writeText).toHaveBeenCalledWith(ROOM_CODE);
        expect(await screen.findByText(tr('speaking:room.copied'))).toBeInTheDocument();

        act(() => {
          vi.advanceTimersByTime(2000);
        });

        expect(screen.queryByText(tr('speaking:room.copied'))).not.toBeInTheDocument();
      } finally {
        vi.useRealTimers();
      }
    });

    it('copies from the waiting card too', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      installClipboard(writeText);
      renderRoom({ peers: { 1: SELF } });

      const waitingCard = screen.getByText(tr('speaking:room.waitingOthers')).closest('.speaking-room-waiting-card');
      fireEvent.click(within(waitingCard).getByRole('button', { name: trRx('speaking:room.copy') }));

      expect(writeText).toHaveBeenCalledWith(ROOM_CODE);
      await waitFor(() => expect(screen.getAllByText(tr('speaking:room.copied')).length).toBeGreaterThan(0));
    });

    it('does not claim success when the clipboard rejects the write', async () => {
      const writeText = vi.fn().mockRejectedValue(new Error('denied'));
      installClipboard(writeText);
      renderRoom();

      fireEvent.click(copyButton());

      await waitFor(() => expect(writeText).toHaveBeenCalled());
      expect(screen.queryByText(tr('speaking:room.copied'))).not.toBeInTheDocument();
    });

    it('does nothing when the clipboard API is unavailable', () => {
      Reflect.deleteProperty(navigator, 'clipboard');
      renderRoom();

      expect(() => fireEvent.click(copyButton())).not.toThrow();
      expect(screen.queryByText(tr('speaking:room.copied'))).not.toBeInTheDocument();
    });
  });

  describe('notices', () => {
    it.each([
      [
        'the user becomes host',
        { type: 'hostChanged', userId: 1 },
        () => tr('speaking:room.youAreHost'),
      ],
      [
        'another member becomes host',
        { type: 'hostChanged', userId: 2 },
        () => tr('speaking:room.hostChanged', { name: 'Bob' }),
      ],
      [
        'an unknown member becomes host',
        { type: 'hostChanged', userId: 99 },
        () => tr('speaking:room.hostChanged', { name: tr('speaking:room.host') }),
      ],
      [
        'the host muted the user',
        { type: 'mutedByHost', userId: 2 },
        () => tr('speaking:room.mutedByHost', { name: 'Bob' }),
      ],
      [
        'a member was kicked',
        { type: 'peerKicked', userId: 2 },
        () => tr('speaking:room.peerKicked', { name: 'Bob' }),
      ],
    ])('announces when %s', (_label, notice, expected) => {
      renderRoom({ notice });

      expect(screen.getByRole('alert')).toHaveTextContent(expected());
    });

    it('shows nothing for an unknown notice type', () => {
      renderRoom({ notice: { type: 'whatever', userId: 2 } });

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('dismisses the notice after 4 seconds', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true });
      try {
        renderRoom({ notice: { type: 'hostChanged', userId: 2 } });

        act(() => {
          vi.advanceTimersByTime(4000);
        });

        expect(ctx.setNotice).toHaveBeenCalledWith(null);
      } finally {
        vi.useRealTimers();
      }
    });
  });

  describe('controls', () => {
    it('toggles the microphone', async () => {
      const user = userEvent.setup();
      renderRoom();

      await user.click(button(tr('speaking:room.mute')));

      expect(ctx.toggleMute).toHaveBeenCalledTimes(1);
    });

    it('offers to unmute while muted', () => {
      renderRoom({ muted: true });

      expect(button(tr('speaking:room.unmute'))).toHaveAttribute('aria-pressed', 'true');
    });

    it('opens the chat', async () => {
      const user = userEvent.setup();
      renderRoom({ chatOpen: false });

      await user.click(button(tr('speaking:chat.toggle')));

      expect(ctx.setChatOpen).toHaveBeenCalledWith(true);
    });

    it('closes the chat from the controls when it is open', async () => {
      const user = userEvent.setup();
      renderRoom({ chatOpen: true });

      await user.click(screen.getAllByRole('button', { name: tr('speaking:chat.close') })[1]);

      expect(ctx.setChatOpen).toHaveBeenCalledWith(false);
    });

    it('shows the number of unread chat messages', () => {
      renderRoom({ chatUnread: 3 });

      expect(button(tr('speaking:chat.unreadLabel', { count: 3 }))).toBeInTheDocument();
    });

    it('disables the controls while leaving', () => {
      renderRoom({ phase: 'leaving' });

      expect(leaveButton()).toBeDisabled();
      expect(button(tr('speaking:room.mute'))).toBeDisabled();
    });
  });

  describe('chat', () => {
    it('does not render the chat panel while closed', () => {
      const { container } = renderRoom({ chatOpen: false });

      expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
      expect(container.querySelector('.speaking-room-body--with-chat')).not.toBeInTheDocument();
    });

    it('renders the chat panel with the messages when open', () => {
      const { container } = renderRoom({
        chatOpen: true,
        chatMessages: [{ id: 1, fromUserId: 2, displayName: 'Bob', text: 'Hello everyone', timestamp: 1700000000000 }],
      });

      expect(screen.getByRole('complementary', { name: tr('speaking:chat.title') })).toBeInTheDocument();
      expect(screen.getByText('Hello everyone')).toBeInTheDocument();
      expect(container.querySelector('.speaking-room-body--with-chat')).toBeInTheDocument();
    });

    it('sends messages through the room context', async () => {
      const user = userEvent.setup();
      renderRoom({ chatOpen: true });

      await user.type(screen.getByPlaceholderText(tr('speaking:chat.placeholder')), 'hi all{Enter}');

      expect(ctx.sendChatMessage).toHaveBeenCalledWith('hi all');
    });

    it('closes the chat from the panel header', async () => {
      const user = userEvent.setup();
      renderRoom({ chatOpen: true });

      const panel = screen.getByRole('complementary');
      await user.click(within(panel).getByRole('button', { name: tr('speaking:chat.close') }));

      expect(ctx.setChatOpen).toHaveBeenCalledWith(false);
    });
  });

  describe('leaving', () => {
    it('lets a guest leave straight away and returns to the lobby when there is no summary', async () => {
      const user = userEvent.setup();
      renderRoom(guestCtx());

      await user.click(leaveButton());

      expect(ctx.leaveRoom).toHaveBeenCalledTimes(1);
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      await waitFor(() => expect(onNavigate).toHaveBeenCalledWith('/speaking'));
    });

    it('stays on the page to show the summary when leaving returns one', async () => {
      const user = userEvent.setup();
      renderRoom(guestCtx({ leaveRoom: vi.fn().mockResolvedValue({ roomCode: ROOM_CODE }) }));

      await user.click(leaveButton());

      await waitFor(() => expect(ctx.leaveRoom).toHaveBeenCalled());
      expect(onNavigate).not.toHaveBeenCalled();
    });

    it('lets a host who is alone leave straight away', async () => {
      const user = userEvent.setup();
      renderRoom({ peers: { 1: SELF } });

      await user.click(leaveButton());

      expect(ctx.leaveRoom).toHaveBeenCalledTimes(1);
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('asks a host with other members to transfer the role or end the room', async () => {
      const user = userEvent.setup();
      renderRoom();

      await user.click(leaveButton());

      expect(ctx.leaveRoom).not.toHaveBeenCalled();
      const dialog = screen.getByRole('dialog');
      expect(within(dialog).getByRole('heading', { name: tr('speaking:room.leaveDialog.title') })).toBeInTheDocument();
      expect(within(dialog).getByRole('radio', { name: 'Bob' })).toBeChecked();
    });

    it('closes the dialog when the host decides to stay', async () => {
      const user = userEvent.setup();
      renderRoom();
      await user.click(leaveButton());

      const dialog = screen.getByRole('dialog');
      await user.click(within(dialog).getAllByRole('button', { name: tr('speaking:room.leaveDialog.cancel') })[0]);

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(ctx.leaveRoom).not.toHaveBeenCalled();
    });

    describe('transferring the host role', () => {
      const openAndTransfer = async (user) => {
        await user.click(leaveButton());
        const dialog = screen.getByRole('dialog');
        await user.click(
          within(dialog).getByRole('button', { name: trRx('speaking:room.leaveDialog.transferConfirm') })
        );
      };

      it('transfers to the selected member then returns to the lobby', async () => {
        const user = userEvent.setup();
        renderRoom();

        await openAndTransfer(user);

        expect(ctx.transferHostAndLeave).toHaveBeenCalledWith(2);
        await waitFor(() => expect(onNavigate).toHaveBeenCalledWith('/speaking'));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });

      it('stays for the summary when the transfer returns one', async () => {
        const user = userEvent.setup();
        renderRoom({ transferHostAndLeave: vi.fn().mockResolvedValue({ roomCode: ROOM_CODE }) });

        await openAndTransfer(user);

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(onNavigate).not.toHaveBeenCalled();
      });

      it('keeps the dialog open and shows the translated error when the transfer fails', async () => {
        const user = userEvent.setup();
        renderRoom({ transferHostAndLeave: vi.fn().mockRejectedValue({ code: 'SPEAKING_NOT_HOST' }) });

        await openAndTransfer(user);

        expect(await screen.findByText(tr('api:error.SPEAKING_NOT_HOST'))).toBeInTheDocument();
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(onNavigate).not.toHaveBeenCalled();
      });

      it('shows a generic error when the failure has no code', async () => {
        const user = userEvent.setup();
        renderRoom({ transferHostAndLeave: vi.fn().mockRejectedValue(new Error('boom')) });

        await openAndTransfer(user);

        expect(
          await screen.findByText(tr('api:error.UNKNOWN', { defaultValue: tr('api:common.fallback') }))
        ).toBeInTheDocument();
      });
    });

    describe('ending the room', () => {
      const openAndEnd = async (user) => {
        await user.click(leaveButton());
        const dialog = screen.getByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: trRx('speaking:room.leaveDialog.endConfirm') }));
      };

      it('ends the room for everyone then returns to the lobby', async () => {
        const user = userEvent.setup();
        renderRoom();

        await openAndEnd(user);

        expect(ctx.endRoom).toHaveBeenCalledTimes(1);
        await waitFor(() => expect(onNavigate).toHaveBeenCalledWith('/speaking'));
      });

      it('stays for the summary when ending returns one', async () => {
        const user = userEvent.setup();
        renderRoom({ endRoom: vi.fn().mockResolvedValue({ roomCode: ROOM_CODE }) });

        await openAndEnd(user);

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(onNavigate).not.toHaveBeenCalled();
      });

      it('keeps the dialog open and shows the error when ending fails', async () => {
        const user = userEvent.setup();
        renderRoom({ endRoom: vi.fn().mockRejectedValue({ code: 'SPEAKING_NOT_HOST' }) });

        await openAndEnd(user);

        expect(await screen.findByText(tr('api:error.SPEAKING_NOT_HOST'))).toBeInTheDocument();
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
    });
  });

  describe('host moderation', () => {
    it('lets the host mute a connected member', async () => {
      const user = userEvent.setup();
      renderRoom();

      await user.click(muteBobButton());

      expect(ctx.requestMute).toHaveBeenCalledWith(2);
    });

    it('does not offer moderation to a guest', () => {
      renderRoom(guestCtx());

      expect(screen.queryByRole('button', { name: trRx('speaking:room.bans.open') })).not.toBeInTheDocument();
      expect(
        screen.queryByRole('button', { name: tr('speaking:room.kickAction', { name: 'Host' }) })
      ).not.toBeInTheDocument();
      expect(
        screen.queryByRole('button', { name: tr('speaking:room.muteAction', { name: 'Host' }) })
      ).not.toBeInTheDocument();
      expect(getBansApi).not.toHaveBeenCalled();
    });

    describe('kicking a member', () => {
      const kickTitle = () => tr('speaking:room.kickConfirm.title', { name: 'Bob' });

      it('asks for confirmation before kicking', async () => {
        const user = userEvent.setup();
        renderRoom();

        await user.click(kickBobButton());

        expect(screen.getByRole('heading', { name: kickTitle() })).toBeInTheDocument();
        expect(screen.getByText(tr('speaking:room.kickConfirm.body'))).toBeInTheDocument();
        expect(ctx.kickMember).not.toHaveBeenCalled();
      });

      it('kicks the member, closes the confirmation and counts the new ban', async () => {
        const user = userEvent.setup();
        renderRoom();
        expect(
          await screen.findByRole('button', { name: tr('speaking:room.bans.openWithCount', { count: 0 }) })
        ).toBeInTheDocument();

        await user.click(kickBobButton());
        await user.click(button(tr('speaking:room.kickConfirm.confirm')));

        expect(ctx.kickMember).toHaveBeenCalledWith(2);
        await waitFor(() => expect(screen.queryByRole('heading', { name: kickTitle() })).not.toBeInTheDocument());
        expect(
          screen.getByRole('button', { name: tr('speaking:room.bans.openWithCount', { count: 1 }) })
        ).toBeInTheDocument();
      });

      it('cancels without kicking', async () => {
        const user = userEvent.setup();
        renderRoom();
        await user.click(kickBobButton());

        await user.click(button(tr('speaking:room.kickConfirm.cancel')));

        expect(screen.queryByRole('heading', { name: kickTitle() })).not.toBeInTheDocument();
        expect(ctx.kickMember).not.toHaveBeenCalled();
      });

      it('keeps the confirmation open and shows the error when kicking fails', async () => {
        const user = userEvent.setup();
        renderRoom({ kickMember: vi.fn().mockRejectedValue({ code: 'SPEAKING_NOT_HOST' }) });

        await user.click(kickBobButton());
        await user.click(button(tr('speaking:room.kickConfirm.confirm')));

        expect(
          await screen.findByText(
            `${tr('speaking:room.kickConfirm.body')} (${tr('api:error.SPEAKING_NOT_HOST')})`
          )
        ).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: kickTitle() })).toBeInTheDocument();
      });

      it('forgets a previous kick error when the confirmation is opened again', async () => {
        const user = userEvent.setup();
        renderRoom({ kickMember: vi.fn().mockRejectedValue({ code: 'SPEAKING_NOT_HOST' }) });
        await user.click(kickBobButton());
        await user.click(button(tr('speaking:room.kickConfirm.confirm')));
        const withError = `${tr('speaking:room.kickConfirm.body')} (${tr('api:error.SPEAKING_NOT_HOST')})`;
        expect(await screen.findByText(withError)).toBeInTheDocument();

        await user.click(button(tr('speaking:room.kickConfirm.cancel')));
        await user.click(kickBobButton());

        expect(screen.getByText(tr('speaking:room.kickConfirm.body'))).toBeInTheDocument();
        expect(screen.queryByText(withError)).not.toBeInTheDocument();
      });
    });

    describe('banned list', () => {
      it('shows the number of banned members on the button', async () => {
        getBansApi.mockResolvedValue({ data: [{ userId: 7 }, { userId: 8 }] });
        renderRoom();

        expect(getBansApi).toHaveBeenCalledWith(ROOM_CODE);
        expect(
          await screen.findByRole('button', { name: tr('speaking:room.bans.openWithCount', { count: 2 }) })
        ).toBeInTheDocument();
      });

      it('accepts a plain array response', async () => {
        getBansApi.mockResolvedValue([{ userId: 7 }]);
        renderRoom();

        expect(
          await screen.findByRole('button', { name: tr('speaking:room.bans.openWithCount', { count: 1 }) })
        ).toBeInTheDocument();
      });

      it('keeps working when the count cannot be loaded', async () => {
        getBansApi.mockRejectedValue(new Error('boom'));
        renderRoom();

        expect(
          await screen.findByRole('button', { name: tr('speaking:room.bans.openWithCount', { count: 0 }) })
        ).toBeInTheDocument();
      });

      it('opens the banned list with the members and lets the host close it', async () => {
        const user = userEvent.setup();
        getBansApi.mockResolvedValue({
          data: [{ userId: 7, displayName: 'Eve', avatarUrl: null, kickedAt: null }],
        });
        renderRoom();

        await user.click(await screen.findByRole('button', { name: tr('speaking:room.bans.openWithCount', { count: 1 }) }));

        const dialog = await screen.findByRole('dialog');
        expect(await within(dialog).findByText('Eve')).toBeInTheDocument();

        await user.click(within(dialog).getAllByRole('button', { name: tr('speaking:room.bans.close') })[0]);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });

      it('unbans a member through the room context', async () => {
        const user = userEvent.setup();
        getBansApi.mockResolvedValue({
          data: [{ userId: 7, displayName: 'Eve', avatarUrl: null, kickedAt: null }],
        });
        renderRoom();
        await user.click(await screen.findByRole('button', { name: tr('speaking:room.bans.openWithCount', { count: 1 }) }));
        const dialog = await screen.findByRole('dialog');
        await within(dialog).findByText('Eve');

        await user.click(within(dialog).getByRole('button', { name: trRx('speaking:room.bans.unban') }));

        expect(ctx.unbanMember).toHaveBeenCalledWith(7);
        await waitFor(() => expect(within(dialog).queryByText('Eve')).not.toBeInTheDocument());
        expect(
          screen.getByRole('button', { name: tr('speaking:room.bans.openWithCount', { count: 0 }) })
        ).toBeInTheDocument();
      });
    });
  });

  describe('summary', () => {
    const summary = { topic: 'Travel plans', roomCode: ROOM_CODE, durationSeconds: 300 };

    it('does not show the summary while there is none', () => {
      renderRoom();

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('shows the summary of the finished session', () => {
      renderRoom({ summary });

      const dialog = screen.getByRole('dialog');
      expect(within(dialog).getByText('5:00')).toBeInTheDocument();
      expect(within(dialog).getByRole('heading', { name: tr('speaking:room.summary.title') })).toBeInTheDocument();
    });

    it('clears the summary and returns to the lobby', async () => {
      const user = userEvent.setup();
      renderRoom({ summary });

      const dialog = screen.getByRole('dialog');
      await user.click(within(dialog).getAllByRole('button', { name: tr('speaking:room.summary.toLobby') })[1]);

      expect(ctx.clearSummary).toHaveBeenCalledTimes(1);
      expect(onNavigate).toHaveBeenCalledWith('/speaking');
    });

    it('clears the summary and opens the history', async () => {
      const user = userEvent.setup();
      renderRoom({ summary });

      await user.click(button(tr('speaking:room.summary.toHistory')));

      expect(ctx.clearSummary).toHaveBeenCalledTimes(1);
      expect(onNavigate).toHaveBeenCalledWith('/speaking/history');
    });
  });
});
