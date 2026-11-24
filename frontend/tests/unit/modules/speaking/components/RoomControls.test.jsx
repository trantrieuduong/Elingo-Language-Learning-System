import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RoomControls from '@/modules/speaking/components/RoomControls/RoomControls';
import { LANGS, tr, useLang } from '../../../../utils/i18n';

describe('RoomControls component', () => {
  let onToggleMute;
  let onLeave;
  let onToggleChat;

  const renderControls = (props = {}) =>
    render(
      <RoomControls
        onToggleMute={onToggleMute}
        onLeave={onLeave}
        onToggleChat={onToggleChat}
        {...props}
      />
    );

  const micButton = (muted) =>
    screen.getByRole('button', { name: tr(muted ? 'speaking:room.unmute' : 'speaking:room.mute') });
  const leaveButton = () => screen.getByRole('button', { name: tr('speaking:room.leave') });
  const chatButton = (name) => screen.getByRole('button', { name });

  beforeEach(() => {
    onToggleMute = vi.fn();
    onLeave = vi.fn();
    onToggleChat = vi.fn();
  });

  it('renders a labelled toolbar', () => {
    renderControls();

    expect(screen.getByRole('toolbar', { name: 'Controls' })).toBeInTheDocument();
  });

  describe.each(LANGS)('labels [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('offers to mute while the mic is on', () => {
      renderControls({ muted: false });

      expect(micButton(false)).toHaveAttribute('aria-pressed', 'false');
    });

    it('offers to unmute while the mic is off', () => {
      renderControls({ muted: true });

      expect(micButton(true)).toHaveAttribute('aria-pressed', 'true');
    });

    it('labels the chat button according to its state', () => {
      const { rerender } = renderControls({ chatOpen: false });
      expect(chatButton(tr('speaking:chat.toggle'))).toHaveAttribute('aria-pressed', 'false');

      rerender(<RoomControls chatOpen onToggleChat={onToggleChat} />);
      expect(chatButton(tr('speaking:chat.close'))).toHaveAttribute('aria-pressed', 'true');
    });

    it('announces the unread count on the chat button', () => {
      renderControls({ chatUnread: 3 });

      expect(chatButton(tr('speaking:chat.unreadLabel', { count: 3 }))).toBeInTheDocument();
    });
  });

  describe('actions', () => {
    it('toggles the mic', async () => {
      const user = userEvent.setup();
      renderControls();

      await user.click(micButton(false));

      expect(onToggleMute).toHaveBeenCalledTimes(1);
    });

    it('leaves the room', async () => {
      const user = userEvent.setup();
      renderControls();

      await user.click(leaveButton());

      expect(onLeave).toHaveBeenCalledTimes(1);
    });

    it('toggles the chat', async () => {
      const user = userEvent.setup();
      renderControls();

      await user.click(chatButton(tr('speaking:chat.toggle')));

      expect(onToggleChat).toHaveBeenCalledTimes(1);
    });
  });

  describe('chat button', () => {
    it('is not rendered without a toggle handler', () => {
      renderControls({ onToggleChat: undefined });

      expect(screen.queryByRole('button', { name: tr('speaking:chat.toggle') })).not.toBeInTheDocument();
      expect(screen.getAllByRole('button')).toHaveLength(2);
    });

    it('prefers the unread label over the open label', () => {
      renderControls({ chatOpen: true, chatUnread: 2 });

      expect(chatButton(tr('speaking:chat.unreadLabel', { count: 2 }))).toBeInTheDocument();
    });
  });

  describe('unread badge', () => {
    const badge = (container) => container.querySelector('.speaking-chat-unread-badge');

    it('is hidden when there are no unread messages', () => {
      const { container } = renderControls({ chatUnread: 0 });

      expect(badge(container)).not.toBeInTheDocument();
    });

    it('shows the unread count', () => {
      const { container } = renderControls({ chatUnread: 4 });

      expect(badge(container)).toHaveTextContent('4');
    });

    it('caps the count at 9+', () => {
      const { container } = renderControls({ chatUnread: 12 });

      expect(badge(container)).toHaveTextContent('9+');
    });

    it('shows 9 as is', () => {
      const { container } = renderControls({ chatUnread: 9 });

      expect(badge(container)).toHaveTextContent('9');
      expect(badge(container)).not.toHaveTextContent('9+');
    });

    it('is hidden while the chat is open', () => {
      const { container } = renderControls({ chatOpen: true, chatUnread: 5 });

      expect(badge(container)).not.toBeInTheDocument();
    });
  });

  describe('leaving', () => {
    it('disables every control and shows a spinner instead of the hang-up icon', () => {
      const { container } = renderControls({ leaving: true });

      screen.getAllByRole('button').forEach((button) => expect(button).toBeDisabled());
      expect(container.querySelector('.app-loading-spinner')).toBeInTheDocument();
      expect(screen.queryByText('call_end')).not.toBeInTheDocument();
    });

    it('shows the hang-up icon when not leaving', () => {
      const { container } = renderControls();

      expect(screen.getByText('call_end')).toBeInTheDocument();
      expect(container.querySelector('.app-loading-spinner')).not.toBeInTheDocument();
    });
  });
});
