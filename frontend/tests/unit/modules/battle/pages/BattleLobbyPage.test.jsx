import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BattleLobbyPage from '@/modules/battle/pages/BattleLobbyPage';
import { useBattleSocket } from '@/modules/battle/context/BattleSocketContext';
import { tr, trRx } from '../../../../utils/i18n';

vi.mock('@/modules/battle/context/BattleSocketContext', () => ({
  useBattleSocket: vi.fn(),
}));

describe('BattleLobbyPage page component', () => {
  let socket;
  let onNavigate;

  const buildSocket = (overrides = {}) => ({
    connection: 'connected',
    status: 'idle',
    match: null,
    error: null,
    roomCode: null,
    activeMatchId: null,
    joinQueue: vi.fn(),
    leaveQueue: vi.fn(),
    createRoom: vi.fn(),
    joinRoom: vi.fn(),
    leaveRoom: vi.fn(),
    clearError: vi.fn(),
    ...overrides,
  });

  const renderLobby = (overrides = {}) => {
    socket = buildSocket(overrides);
    useBattleSocket.mockReturnValue(socket);
    return render(<BattleLobbyPage onNavigate={onNavigate} />);
  };

  const modeCard = (key) =>
    screen.getByRole('radio', { name: trRx(`battle:mode.${key}`) });

  beforeEach(() => {
    onNavigate = vi.fn();
    localStorage.clear();
  });

  describe('layout', () => {
    it('renders the title, mode options and room section', () => {
      renderLobby();

      expect(screen.getByRole('heading', { name: tr('battle:title') })).toBeInTheDocument();
      expect(modeCard('multipleChoice')).toBeInTheDocument();
      expect(modeCard('typing')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: trRx('battle:findMatch') })).toBeEnabled();
      expect(screen.getByRole('button', { name: tr('battle:room.create') })).toBeEnabled();
      expect(screen.getByRole('button', { name: tr('battle:room.join') })).toBeEnabled();
    });

    it('selects multiple choice by default', () => {
      renderLobby();

      expect(modeCard('multipleChoice')).toHaveAttribute('aria-checked', 'true');
      expect(modeCard('typing')).toHaveAttribute('aria-checked', 'false');
    });

    it('restores the saved mode from localStorage', () => {
      localStorage.setItem('battle.mode', 'TYPING');

      renderLobby();

      expect(modeCard('typing')).toHaveAttribute('aria-checked', 'true');
    });

    it('ignores an invalid saved mode', () => {
      localStorage.setItem('battle.mode', 'INVALID');

      renderLobby();

      expect(modeCard('multipleChoice')).toHaveAttribute('aria-checked', 'true');
    });

    it('navigates to the history page', async () => {
      const user = userEvent.setup();
      renderLobby();

      await user.click(screen.getByRole('button', { name: tr('battle:history.link') }));

      expect(onNavigate).toHaveBeenCalledWith('/battle/history');
    });
  });

  describe('mode selection', () => {
    it('changes the selected mode and saves it', async () => {
      const user = userEvent.setup();
      renderLobby();

      await user.click(modeCard('typing'));

      expect(modeCard('typing')).toHaveAttribute('aria-checked', 'true');
      expect(localStorage.getItem('battle.mode')).toBe('TYPING');
    });
  });

  describe('connection', () => {
    it('shows the reconnecting banner and disables actions when disconnected', () => {
      renderLobby({ connection: 'disconnected' });

      expect(screen.getByText(tr('battle:connection.reconnecting'))).toBeInTheDocument();
      expect(screen.getByRole('button', { name: trRx('battle:findMatch') })).toBeDisabled();
      expect(screen.getByRole('button', { name: tr('battle:room.create') })).toBeDisabled();
      expect(screen.getByRole('button', { name: tr('battle:room.join') })).toBeDisabled();
    });

    it('does not show the reconnecting banner when connected', () => {
      renderLobby();

      expect(screen.queryByText(tr('battle:connection.reconnecting'))).not.toBeInTheDocument();
    });
  });

  describe('random match', () => {
    it('joins the queue with the selected mode', async () => {
      const user = userEvent.setup();
      renderLobby();

      await user.click(modeCard('typing'));
      await user.click(screen.getByRole('button', { name: trRx('battle:findMatch') }));

      expect(socket.clearError).toHaveBeenCalled();
      expect(socket.joinQueue).toHaveBeenCalledWith('TYPING');
    });

    it('shows the searching dialog with the selected mode and cancels the search', async () => {
      const user = userEvent.setup();
      renderLobby({ status: 'searching' });

      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByText(tr('battle:searching'))).toBeInTheDocument();
      expect(screen.getByText(tr('battle:mode.multipleChoice'), { selector: 'strong' })).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: tr('battle:cancel') }));

      expect(socket.leaveQueue).toHaveBeenCalledTimes(1);
    });

    it('does not show the searching dialog when idle', () => {
      renderLobby();

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('counts the searching time', () => {
      vi.useFakeTimers();
      try {
        renderLobby({ status: 'searching' });
        expect(screen.getByText(tr('battle:searchingTime', { time: '0:00' }))).toBeInTheDocument();

        act(() => {
          vi.advanceTimersByTime(65000);
        });

        expect(screen.getByText(tr('battle:searchingTime', { time: '1:05' }))).toBeInTheDocument();
      } finally {
        vi.useRealTimers();
      }
    });

    it('navigates to the play page when a match is found', () => {
      renderLobby({ status: 'starting', match: { matchId: 100 } });

      expect(onNavigate).toHaveBeenCalledWith('/battle/play/100');
    });

    it('does not navigate when starting without a match id', () => {
      renderLobby({ status: 'starting', match: null });

      expect(onNavigate).not.toHaveBeenCalled();
    });
  });

  describe('active match', () => {
    it('shows the resume banner and blocks starting a new match', () => {
      renderLobby({ activeMatchId: 55 });

      expect(screen.getByText(tr('battle:resume.title'))).toBeInTheDocument();
      expect(screen.getByText(tr('battle:resume.blocked'))).toBeInTheDocument();
      expect(screen.getByRole('button', { name: trRx('battle:findMatch') })).toBeDisabled();
      expect(screen.getByRole('button', { name: tr('battle:room.create') })).toBeDisabled();
      expect(screen.getByRole('button', { name: tr('battle:room.join') })).toBeDisabled();
      expect(screen.getByPlaceholderText(tr('battle:room.codePlaceholder'))).toBeDisabled();
      expect(modeCard('typing')).toBeDisabled();
    });

    it('navigates to the active match when clicking continue', async () => {
      const user = userEvent.setup();
      renderLobby({ activeMatchId: 55 });

      await user.click(screen.getByRole('button', { name: trRx('battle:resume.continue') }));

      expect(onNavigate).toHaveBeenCalledWith('/battle/play/55');
    });

    it('does not show the resume banner without an active match', () => {
      renderLobby();

      expect(screen.queryByText(tr('battle:resume.title'))).not.toBeInTheDocument();
    });

    it('does not show the resume banner while searching', () => {
      renderLobby({ status: 'searching', activeMatchId: 55 });

      expect(screen.queryByText(tr('battle:resume.title'))).not.toBeInTheDocument();
    });
  });

  describe('private room', () => {
    it('creates a room with the selected mode', async () => {
      const user = userEvent.setup();
      renderLobby();

      await user.click(modeCard('typing'));
      await user.click(screen.getByRole('button', { name: tr('battle:room.create') }));

      expect(socket.clearError).toHaveBeenCalled();
      expect(socket.createRoom).toHaveBeenCalledWith('TYPING');
    });

    it('joins a room with an upper-cased code', async () => {
      const user = userEvent.setup();
      renderLobby();
      const input = screen.getByPlaceholderText(tr('battle:room.codePlaceholder'));

      await user.type(input, 'abc234');
      await user.click(screen.getByRole('button', { name: tr('battle:room.join') }));

      expect(input).toHaveValue('ABC234');
      expect(socket.joinRoom).toHaveBeenCalledWith('ABC234');
    });

    it('joins a room when pressing Enter in the code input', async () => {
      const user = userEvent.setup();
      renderLobby();

      await user.type(screen.getByPlaceholderText(tr('battle:room.codePlaceholder')), 'abc234{Enter}');

      expect(socket.joinRoom).toHaveBeenCalledWith('ABC234');
    });

    it('does not join with Enter when disconnected', () => {
      renderLobby({ connection: 'disconnected' });
      const input = screen.getByPlaceholderText(tr('battle:room.codePlaceholder'));

      fireEvent.change(input, { target: { value: 'abc234' } });
      fireEvent.keyDown(input, { key: 'Enter' });

      expect(socket.joinRoom).not.toHaveBeenCalled();
    });

    it('limits the code input to 6 characters', () => {
      renderLobby();

      expect(screen.getByPlaceholderText(tr('battle:room.codePlaceholder'))).toHaveAttribute('maxlength', '6');
    });

    it('shows a validation error and does not join when the code is not 6 characters', async () => {
      const user = userEvent.setup();
      renderLobby();

      await user.type(screen.getByPlaceholderText(tr('battle:room.codePlaceholder')), 'abc');
      await user.click(screen.getByRole('button', { name: tr('battle:room.join') }));

      expect(screen.getByRole('alert')).toHaveTextContent(tr('battle:room.codeInvalid'));
      expect(socket.joinRoom).not.toHaveBeenCalled();
    });

    it('trims the entered code before validating', () => {
      renderLobby();
      const input = screen.getByPlaceholderText(tr('battle:room.codePlaceholder'));

      fireEvent.change(input, { target: { value: 'ab c' } });
      fireEvent.click(screen.getByRole('button', { name: tr('battle:room.join') }));

      // "AB C" có 4 ký tự -> không hợp lệ
      expect(screen.getByRole('alert')).toHaveTextContent(tr('battle:room.codeInvalid'));
      expect(socket.joinRoom).not.toHaveBeenCalled();
    });

    it('clears the validation error when the user edits the code', async () => {
      const user = userEvent.setup();
      renderLobby();
      const input = screen.getByPlaceholderText(tr('battle:room.codePlaceholder'));
      await user.type(input, 'abc');
      await user.click(screen.getByRole('button', { name: tr('battle:room.join') }));
      expect(screen.getByRole('alert')).toBeInTheDocument();

      await user.type(input, 'd');

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  describe('room waiting dialog', () => {
    afterEach(() => {
      delete navigator.clipboard;
    });

    it('shows the room code and the selected mode', () => {
      renderLobby({ status: 'roomWaiting', roomCode: 'ABC234' });

      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByText(tr('battle:room.waitingTitle'))).toBeInTheDocument();
      expect(screen.getByText('ABC234')).toBeInTheDocument();
      expect(screen.getByText(tr('battle:mode.multipleChoice'), { selector: 'strong' })).toBeInTheDocument();
    });

    it('leaves the room when cancelling', async () => {
      const user = userEvent.setup();
      renderLobby({ status: 'roomWaiting', roomCode: 'ABC234' });

      await user.click(screen.getByRole('button', { name: tr('battle:room.cancel') }));

      expect(socket.leaveRoom).toHaveBeenCalledTimes(1);
    });

    it('copies the room code to the clipboard and shows the copied state', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
      renderLobby({ status: 'roomWaiting', roomCode: 'ABC234' });

      fireEvent.click(screen.getByRole('button', { name: trRx('battle:room.copy') }));

      expect(writeText).toHaveBeenCalledWith('ABC234');
      expect(await screen.findByText(tr('battle:room.copied'))).toBeInTheDocument();
    });

    it('shows the copied state even when the clipboard API fails', async () => {
      const writeText = vi.fn().mockRejectedValue(new Error('denied'));
      Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
      renderLobby({ status: 'roomWaiting', roomCode: 'ABC234' });

      fireEvent.click(screen.getByRole('button', { name: trRx('battle:room.copy') }));

      await waitFor(() => expect(screen.getByText(tr('battle:room.copied'))).toBeInTheDocument());
    });
  });

  describe('errors', () => {
    it('shows the translated battle error and clears it when dismissed', () => {
      vi.useFakeTimers();
      try {
        renderLobby({ error: 'QUEUE_TIMEOUT' });

        expect(screen.getByRole('alert')).toHaveTextContent(tr('battle:error.QUEUE_TIMEOUT'));

        act(() => {
          vi.advanceTimersByTime(3500);
        });

        expect(socket.clearError).toHaveBeenCalled();
      } finally {
        vi.useRealTimers();
      }
    });

    it('falls back to the API error message for unknown battle errors', () => {
      renderLobby({ error: 'BATTLE_ROOM_NOT_FOUND' });

      expect(screen.getByRole('alert')).toHaveTextContent(tr('api:error.BATTLE_ROOM_NOT_FOUND'));
    });

    it('falls back to the generic error message for unknown error codes', () => {
      renderLobby({ error: 'SOMETHING_NEW' });

      expect(screen.getByRole('alert')).toHaveTextContent(tr('api:common.UNKNOWN_ERROR'));
    });

    it('does not show an alert when there is no error', () => {
      renderLobby();

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  describe('additional behaviors', () => {
    afterEach(() => {
      delete navigator.clipboard;
    });

    it('hides the copied state again after two seconds', async () => {
      vi.useFakeTimers();
      try {
        const writeText = vi.fn().mockResolvedValue(undefined);
        Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
        renderLobby({ status: 'roomWaiting', roomCode: 'ABC234' });

        await act(async () => {
          fireEvent.click(screen.getByRole('button', { name: trRx('battle:room.copy') }));
        });
        expect(screen.getByText(tr('battle:room.copied'))).toBeInTheDocument();

        act(() => {
          vi.advanceTimersByTime(2100);
        });

        expect(screen.queryByText(tr('battle:room.copied'))).not.toBeInTheDocument();
      } finally {
        vi.useRealTimers();
      }
    });

    it('hides the copied state again after two seconds when the clipboard API fails', async () => {
      vi.useFakeTimers();
      try {
        const writeText = vi.fn().mockRejectedValue(new Error('denied'));
        Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
        renderLobby({ status: 'roomWaiting', roomCode: 'ABC234' });

        await act(async () => {
          fireEvent.click(screen.getByRole('button', { name: trRx('battle:room.copy') }));
        });
        expect(screen.getByText(tr('battle:room.copied'))).toBeInTheDocument();

        act(() => {
          vi.advanceTimersByTime(2100);
        });

        expect(screen.queryByText(tr('battle:room.copied'))).not.toBeInTheDocument();
      } finally {
        vi.useRealTimers();
      }
    });

    it('does not copy anything when there is no room code', () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
      renderLobby({ status: 'roomWaiting', roomCode: null });

      fireEvent.click(screen.getByRole('button', { name: trRx('battle:room.copy') }));

      expect(writeText).not.toHaveBeenCalled();
    });

    it('formats the searching time with two digit seconds', () => {
      vi.useFakeTimers();
      try {
        renderLobby({ status: 'searching' });

        act(() => {
          vi.advanceTimersByTime(75000);
        });

        expect(screen.getByText(tr('battle:searchingTime', { time: '1:15' }))).toBeInTheDocument();
      } finally {
        vi.useRealTimers();
      }
    });

    it('shows the typing mode in the searching dialog', () => {
      localStorage.setItem('battle.mode', 'TYPING');

      renderLobby({ status: 'searching' });

      expect(screen.getByText(tr('battle:mode.typing'), { selector: 'strong' })).toBeInTheDocument();
    });

    it('shows the typing mode in the room waiting dialog', () => {
      localStorage.setItem('battle.mode', 'TYPING');

      renderLobby({ status: 'roomWaiting', roomCode: 'ABC234' });

      expect(screen.getByText(tr('battle:mode.typing'), { selector: 'strong' })).toBeInTheDocument();
    });
  });
});
