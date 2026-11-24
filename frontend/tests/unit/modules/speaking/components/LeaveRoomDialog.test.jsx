import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LeaveRoomDialog from '@/modules/speaking/components/LeaveRoomDialog/LeaveRoomDialog';
import { LANGS, tr, trRx, useLang } from '../../../../utils/i18n';

describe('LeaveRoomDialog component', () => {
  let onClose;
  let onTransfer;
  let onEnd;

  const members = [
    { userId: 2, displayName: 'Bob', avatarUrl: null },
    { userId: 3, displayName: 'Cara', avatarUrl: null },
  ];

  const renderDialog = (props = {}) =>
    render(
      <LeaveRoomDialog
        open
        members={members}
        onClose={onClose}
        onTransfer={onTransfer}
        onEnd={onEnd}
        {...props}
      />
    );

  const member = (name) => screen.getByRole('radio', { name });
  const transferButton = () =>
    screen.getByRole('button', { name: trRx('speaking:room.leaveDialog.transferConfirm') });
  const endButton = () =>
    screen.getByRole('button', { name: trRx('speaking:room.leaveDialog.endConfirm') });
  const stayButtons = () =>
    screen.getAllByRole('button', { name: tr('speaking:room.leaveDialog.cancel') });

  beforeEach(() => {
    onClose = vi.fn();
    onTransfer = vi.fn();
    onEnd = vi.fn();
  });

  describe('visibility', () => {
    it('renders nothing when closed', () => {
      renderDialog({ open: false });

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('renders an accessible modal dialog in the document body', () => {
      renderDialog();

      const dialog = screen.getByRole('dialog');
      expect(dialog).toHaveAttribute('aria-modal', 'true');
      expect(dialog.parentElement).toBe(document.body);
    });
  });

  describe.each(LANGS)('content [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('offers to transfer the host role or end the room', () => {
      renderDialog();

      expect(screen.getByRole('heading', { name: tr('speaking:room.leaveDialog.title') })).toBeInTheDocument();
      expect(
        screen.getByRole('heading', { name: tr('speaking:room.leaveDialog.transferTitle') })
      ).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: tr('speaking:room.leaveDialog.endTitle') })).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:room.leaveDialog.endWarning'))).toBeInTheDocument();
      expect(transferButton()).toBeEnabled();
      expect(endButton()).toBeEnabled();
    });
  });

  describe('transferring', () => {
    it('lists every member as a candidate and selects the first one', () => {
      renderDialog();

      expect(screen.getAllByRole('radio')).toHaveLength(2);
      expect(member('Bob')).toBeChecked();
      expect(member('Cara')).not.toBeChecked();
    });

    it('transfers to the first member by default', async () => {
      const user = userEvent.setup();
      renderDialog();

      await user.click(transferButton());

      expect(onTransfer).toHaveBeenCalledWith(2);
    });

    it('transfers to the member that was picked', async () => {
      const user = userEvent.setup();
      renderDialog();

      await user.click(member('Cara'));
      await user.click(transferButton());

      expect(member('Cara')).toBeChecked();
      expect(onTransfer).toHaveBeenCalledWith(3);
    });

    it('selects the first member again when reopened', () => {
      const { rerender } = renderDialog();
      fireEvent.click(member('Cara'));

      rerender(<LeaveRoomDialog open={false} members={members} onTransfer={onTransfer} />);
      rerender(<LeaveRoomDialog open members={members} onTransfer={onTransfer} />);

      expect(member('Bob')).toBeChecked();
    });

    it('disables transferring when nobody can take over', () => {
      renderDialog({ members: [] });

      expect(screen.queryAllByRole('radio')).toHaveLength(0);
      expect(transferButton()).toBeDisabled();
    });
  });

  describe('ending the room', () => {
    it('ends the room for everyone', async () => {
      const user = userEvent.setup();
      renderDialog();

      await user.click(endButton());

      expect(onEnd).toHaveBeenCalledTimes(1);
      expect(onTransfer).not.toHaveBeenCalled();
    });
  });

  describe('busy', () => {
    it('disables every action', () => {
      renderDialog({ busy: true });

      expect(transferButton()).toBeDisabled();
      expect(endButton()).toBeDisabled();
      stayButtons().forEach((button) => expect(button).toBeDisabled());
      screen.getAllByRole('radio').forEach((radio) => expect(radio).toBeDisabled());
    });

    it('cannot be closed with Escape or the backdrop', () => {
      renderDialog({ busy: true });

      fireEvent.keyDown(document, { key: 'Escape' });
      fireEvent.click(screen.getByRole('dialog'));

      expect(onClose).not.toHaveBeenCalled();
    });
  });

  describe('closing', () => {
    it('closes through the stay button and the header close button', async () => {
      const user = userEvent.setup();
      renderDialog();

      const buttons = stayButtons();
      expect(buttons).toHaveLength(2);
      await user.click(buttons[0]);
      await user.click(buttons[1]);

      expect(onClose).toHaveBeenCalledTimes(2);
    });

    it('closes on Escape', () => {
      renderDialog();

      fireEvent.keyDown(document, { key: 'Escape' });

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('closes when the backdrop is clicked but not when the content is clicked', () => {
      renderDialog();

      fireEvent.click(screen.getByRole('heading', { name: tr('speaking:room.leaveDialog.title') }));
      expect(onClose).not.toHaveBeenCalled();

      fireEvent.click(screen.getByRole('dialog'));
      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('locks the page scroll while open and restores it afterwards', () => {
      const { unmount } = renderDialog();
      expect(document.body.style.overflow).toBe('hidden');

      unmount();

      expect(document.body.style.overflow).toBe('');
    });
  });

  describe('errors', () => {
    it('shows no alert without an error', () => {
      renderDialog();

      expect(document.querySelector('.form-alert')).not.toBeInTheDocument();
    });

    it('explains a missing socket connection', () => {
      renderDialog({ error: 'SOCKET_NOT_CONNECTED' });

      expect(screen.getByText(tr('speaking:error.socketNotConnected'))).toBeInTheDocument();
    });

    it('translates a server error code', () => {
      renderDialog({ error: 'SPEAKING_NOT_HOST' });

      expect(screen.getByText(tr('api:error.SPEAKING_NOT_HOST'))).toBeInTheDocument();
    });

    it('falls back to a generic message for an unknown code', () => {
      renderDialog({ error: 'UNKNOWN' });

      expect(screen.getByText(tr('api:common.fallback'))).toBeInTheDocument();
    });
  });
});
