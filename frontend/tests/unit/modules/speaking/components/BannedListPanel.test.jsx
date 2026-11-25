import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from '@/i18n';
import BannedListPanel from '@/modules/speaking/components/BannedListPanel/BannedListPanel';
import { getBansApi } from '@/modules/speaking/speakingApi';
import { LANGS, tr, trRx, useLang } from '../../../../utils/i18n';

vi.mock('@/modules/speaking/speakingApi', () => ({
  getBansApi: vi.fn(),
}));

describe('BannedListPanel component', () => {
  let onClose;
  let onUnban;
  let onBansCountChange;

  const KICKED_AT = '2026-03-05T10:30:00.000Z';
  const bans = [
    { userId: 7, displayName: 'Eve', avatarUrl: null, kickedAt: KICKED_AT },
    { userId: 8, displayName: 'Mallory', avatarUrl: null, kickedAt: null },
  ];

  const renderPanel = (props = {}) =>
    render(
      <BannedListPanel
        open
        roomCode="ABC234"
        onClose={onClose}
        onUnban={onUnban}
        onBansCountChange={onBansCountChange}
        {...props}
      />
    );

  const unbanButtons = () => screen.getAllByRole('button', { name: trRx('speaking:room.bans.unban') });
  const closeButtons = () => screen.getAllByRole('button', { name: tr('speaking:room.bans.close') });

  beforeEach(() => {
    onClose = vi.fn();
    onUnban = vi.fn().mockResolvedValue(undefined);
    onBansCountChange = vi.fn();
    getBansApi.mockResolvedValue({ data: bans });
  });

  describe('visibility', () => {
    it('renders nothing and loads nothing when closed', () => {
      renderPanel({ open: false });

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(getBansApi).not.toHaveBeenCalled();
    });

    it('renders an accessible modal dialog in the document body', async () => {
      renderPanel();

      const dialog = screen.getByRole('dialog');
      expect(dialog).toHaveAttribute('aria-modal', 'true');
      expect(dialog.parentElement).toBe(document.body);
      await screen.findByText('Eve');
    });

    it('does not load anything without a room code', () => {
      renderPanel({ roomCode: undefined });

      expect(getBansApi).not.toHaveBeenCalled();
      expect(screen.getByText(tr('speaking:room.bans.empty'))).toBeInTheDocument();
    });
  });

  describe('loading the list', () => {
    it('shows a loading state, then the banned members', async () => {
      renderPanel();

      expect(screen.getByText(tr('speaking:room.preparing'))).toBeInTheDocument();

      expect(await screen.findByText('Eve')).toBeInTheDocument();
      expect(screen.getByText('Mallory')).toBeInTheDocument();
      expect(screen.queryByText(tr('speaking:room.preparing'))).not.toBeInTheDocument();
      expect(getBansApi).toHaveBeenCalledWith('ABC234');
    });

    it('reports the number of bans to the parent', async () => {
      renderPanel();

      await screen.findByText('Eve');

      expect(onBansCountChange).toHaveBeenCalledWith(2);
    });

    it('accepts a response that is a plain array', async () => {
      getBansApi.mockResolvedValue(bans);

      renderPanel();

      expect(await screen.findByText('Eve')).toBeInTheDocument();
      expect(onBansCountChange).toHaveBeenCalledWith(2);
    });

    it('treats a response without data as an empty list', async () => {
      getBansApi.mockResolvedValue({});

      renderPanel();

      expect(await screen.findByText(tr('speaking:room.bans.empty'))).toBeInTheDocument();
      expect(onBansCountChange).toHaveBeenCalledWith(0);
    });

    it.each(LANGS)('shows the title with the number of bans [%s]', async (lng) => {
      await useLang(lng);
      renderPanel();

      await screen.findByText('Eve');

      expect(
        screen.getByRole('heading', { name: tr('speaking:room.bans.titleWithCount', { count: 2 }) })
      ).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:room.bans.hint'))).toBeInTheDocument();
    });

    it('shows the empty state when nobody is banned', async () => {
      getBansApi.mockResolvedValue({ data: [] });

      renderPanel();

      expect(await screen.findByText(tr('speaking:room.bans.empty'))).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: trRx('speaking:room.bans.unban') })).not.toBeInTheDocument();
    });

    it('shows the kick time only when it is valid', async () => {
      getBansApi.mockResolvedValue({
        data: [
          { userId: 7, displayName: 'Eve', kickedAt: KICKED_AT },
          { userId: 8, displayName: 'Mallory', kickedAt: null },
          { userId: 9, displayName: 'Trent', kickedAt: 'not-a-date' },
        ],
      });

      const { baseElement } = renderPanel();
      await screen.findByText('Eve');

      const times = baseElement.querySelectorAll('.speaking-banned-modal__user-time');
      expect(times).toHaveLength(1);
      expect(times[0]).toHaveTextContent(new Date(KICKED_AT).toLocaleString(i18n.language));
    });
  });

  describe('load errors', () => {
    it('shows an error with a retry button that reloads the list', async () => {
      const user = userEvent.setup();
      getBansApi.mockRejectedValueOnce(new Error('boom'));

      renderPanel();

      expect(await screen.findByText(tr('speaking:room.bans.loadError'))).toBeInTheDocument();
      expect(screen.queryByText('Eve')).not.toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: tr('speaking:room.bans.retry') }));

      expect(await screen.findByText('Eve')).toBeInTheDocument();
      expect(screen.queryByText(tr('speaking:room.bans.loadError'))).not.toBeInTheDocument();
      expect(getBansApi).toHaveBeenCalledTimes(2);
    });
  });

  describe('unbanning', () => {
    it('unbans the member and removes it from the list', async () => {
      const user = userEvent.setup();
      renderPanel();
      await screen.findByText('Eve');

      await user.click(unbanButtons()[0]);

      expect(onUnban).toHaveBeenCalledWith(7);
      await waitFor(() => expect(screen.queryByText('Eve')).not.toBeInTheDocument());
      expect(screen.getByText('Mallory')).toBeInTheDocument();
      expect(onBansCountChange).toHaveBeenLastCalledWith(1);
    });

    it('locks every action while the unban is pending', async () => {
      let resolveUnban;
      onUnban.mockReturnValue(
        new Promise((resolve) => {
          resolveUnban = resolve;
        })
      );
      const user = userEvent.setup();
      renderPanel();
      await screen.findByText('Eve');

      await user.click(unbanButtons()[0]);

      await waitFor(() =>
        screen.getAllByRole('button', { name: /lock_open/ }).forEach((button) => expect(button).toBeDisabled())
      );
      expect(screen.getByText(tr('speaking:room.preparing'))).toBeInTheDocument();
      closeButtons().forEach((button) => expect(button).toBeDisabled());

      fireEvent.keyDown(document, { key: 'Escape' });
      expect(onClose).not.toHaveBeenCalled();

      await act(async () => {
        resolveUnban();
      });
      await waitFor(() => expect(screen.queryByText('Eve')).not.toBeInTheDocument());
    });

    it('shows the translated error and keeps the member when the unban fails', async () => {
      onUnban.mockRejectedValue({ code: 'SPEAKING_NOT_HOST' });
      const user = userEvent.setup();
      renderPanel();
      await screen.findByText('Eve');

      await user.click(unbanButtons()[0]);

      expect(await screen.findByText(tr('api:error.SPEAKING_NOT_HOST'))).toBeInTheDocument();
      expect(screen.getByText('Eve')).toBeInTheDocument();
      expect(unbanButtons()[0]).toBeEnabled();
    });

    it('shows a generic error when the failure has no code', async () => {
      onUnban.mockRejectedValue(new Error('network'));
      const user = userEvent.setup();
      renderPanel();
      await screen.findByText('Eve');

      await user.click(unbanButtons()[0]);

      expect(await screen.findByText(tr('api:common.fallback'))).toBeInTheDocument();
    });

    it('does nothing when no unban handler is given', async () => {
      const user = userEvent.setup();
      renderPanel({ onUnban: undefined });
      await screen.findByText('Eve');

      await user.click(unbanButtons()[0]);

      expect(screen.getByText('Eve')).toBeInTheDocument();
    });
  });

  describe('closing', () => {
    it('closes through the footer and header buttons', async () => {
      const user = userEvent.setup();
      renderPanel();
      await screen.findByText('Eve');

      const buttons = closeButtons();
      expect(buttons).toHaveLength(2);
      await user.click(buttons[0]);
      await user.click(buttons[1]);

      expect(onClose).toHaveBeenCalledTimes(2);
    });

    it('closes on Escape', async () => {
      renderPanel();
      await screen.findByText('Eve');

      fireEvent.keyDown(document, { key: 'Escape' });

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('closes when the backdrop is clicked but not when the content is clicked', async () => {
      renderPanel();
      await screen.findByText('Eve');

      fireEvent.click(screen.getByText('Eve'));
      expect(onClose).not.toHaveBeenCalled();

      fireEvent.click(screen.getByRole('dialog'));
      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('locks the page scroll while open and restores it afterwards', async () => {
      const { unmount } = renderPanel();
      await screen.findByText('Eve');
      expect(document.body.style.overflow).toBe('hidden');

      unmount();

      expect(document.body.style.overflow).toBe('');
    });

    it('loads the list again every time it is reopened', async () => {
      const { rerender } = renderPanel();
      await screen.findByText('Eve');

      rerender(<BannedListPanel open={false} roomCode="ABC234" onBansCountChange={onBansCountChange} />);
      expect(screen.queryByText('Eve')).not.toBeInTheDocument();

      rerender(<BannedListPanel open roomCode="ABC234" onBansCountChange={onBansCountChange} />);

      expect(await screen.findByText('Eve')).toBeInTheDocument();
      expect(getBansApi).toHaveBeenCalledTimes(2);
    });
  });
});
