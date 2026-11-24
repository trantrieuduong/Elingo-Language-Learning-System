import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CreateRoomModal from '@/modules/speaking/components/CreateRoomModal/CreateRoomModal';
import { LANGS, tr, trRx, useLang } from '../../../../utils/i18n';

describe('CreateRoomModal component', () => {
  let onClose;
  let onSubmit;

  const renderModal = (props = {}) =>
    render(<CreateRoomModal open onClose={onClose} onSubmit={onSubmit} busy={false} {...props} />);

  const capacity = (count) =>
    screen.getByRole('radio', { name: trRx('speaking:lobby.capacityOption', { count }) });
  const topicInput = () => screen.getByLabelText(trRx('speaking:create.topic'));
  const submitButton = () => screen.getByRole('button', { name: trRx('speaking:create.submit') });
  const cancelButtons = () => screen.getAllByRole('button', { name: tr('speaking:create.cancel') });

  beforeEach(() => {
    onClose = vi.fn();
    onSubmit = vi.fn();
  });

  describe('visibility', () => {
    it('renders nothing when closed', () => {
      renderModal({ open: false });

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('renders an accessible modal dialog in the document body', () => {
      renderModal();

      const dialog = screen.getByRole('dialog');
      expect(dialog).toHaveAttribute('aria-modal', 'true');
      expect(dialog.parentElement).toBe(document.body);
    });
  });

  describe.each(LANGS)('content [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('shows the title, topic field, capacity options and privacy switch', () => {
      renderModal();

      expect(screen.getByRole('heading', { name: tr('speaking:create.title') })).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:create.subtitle'))).toBeInTheDocument();
      expect(topicInput()).toBeInTheDocument();
      [2, 3, 4].forEach((count) => expect(capacity(count)).toBeInTheDocument());
      expect(screen.getByText(tr('speaking:create.private'))).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:create.privateHint'))).toBeInTheDocument();
      expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'false');
    });
  });

  describe('defaults', () => {
    it('selects a capacity of 4 and a public room', () => {
      renderModal();

      expect(capacity(4)).toHaveAttribute('aria-checked', 'true');
      expect(capacity(2)).toHaveAttribute('aria-checked', 'false');
      expect(capacity(3)).toHaveAttribute('aria-checked', 'false');
      expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'false');
    });

    it('limits the topic to 255 characters', () => {
      renderModal();

      expect(topicInput()).toHaveAttribute('maxlength', '255');
    });

    it('focuses the topic field shortly after opening', async () => {
      renderModal();

      await waitFor(() => expect(topicInput()).toHaveFocus());
    });

    it('resets the form every time it is reopened', async () => {
      const user = userEvent.setup();
      const { rerender } = renderModal();
      await user.type(topicInput(), 'Old topic');
      await user.click(capacity(2));
      await user.click(screen.getByRole('switch'));

      rerender(<CreateRoomModal open={false} onClose={onClose} onSubmit={onSubmit} />);
      rerender(<CreateRoomModal open onClose={onClose} onSubmit={onSubmit} />);

      expect(topicInput()).toHaveValue('');
      expect(capacity(4)).toHaveAttribute('aria-checked', 'true');
      expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'false');
    });
  });

  describe('submitting', () => {
    it('submits the defaults with an empty topic as null', async () => {
      const user = userEvent.setup();
      renderModal();

      await user.click(submitButton());

      expect(onSubmit).toHaveBeenCalledWith({ topic: null, maxParticipants: 4, isPrivate: false });
    });

    it('submits the trimmed topic, chosen capacity and privacy', async () => {
      const user = userEvent.setup();
      renderModal();

      await user.type(topicInput(), '  Daily English  ');
      await user.click(capacity(2));
      await user.click(screen.getByRole('switch'));
      await user.click(submitButton());

      expect(onSubmit).toHaveBeenCalledWith({
        topic: 'Daily English',
        maxParticipants: 2,
        isPrivate: true,
      });
    });

    it('treats a blank topic as no topic', async () => {
      const user = userEvent.setup();
      renderModal();

      await user.type(topicInput(), '    ');
      await user.click(submitButton());

      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ topic: null }));
    });

    it('submits with Enter from the topic field', async () => {
      const user = userEvent.setup();
      renderModal();

      await user.type(topicInput(), 'Quick chat{Enter}');

      expect(onSubmit).toHaveBeenCalledTimes(1);
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ topic: 'Quick chat' }));
    });

    it('switches the selected capacity and can toggle privacy back', async () => {
      const user = userEvent.setup();
      renderModal();

      await user.click(capacity(3));
      expect(capacity(3)).toHaveAttribute('aria-checked', 'true');
      expect(capacity(4)).toHaveAttribute('aria-checked', 'false');

      await user.click(screen.getByRole('switch'));
      expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'true');
      await user.click(screen.getByRole('switch'));
      expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'false');
    });
  });

  describe('busy', () => {
    it('disables every control', () => {
      renderModal({ busy: true });

      expect(topicInput()).toBeDisabled();
      [2, 3, 4].forEach((count) => expect(capacity(count)).toBeDisabled());
      expect(screen.getByRole('switch')).toBeDisabled();
      expect(submitButton()).toBeDisabled();
      expect(cancelButtons()).toHaveLength(2);
      cancelButtons().forEach((button) => expect(button).toBeDisabled());
    });

    it('shows a spinner in the submit button', () => {
      renderModal({ busy: true });

      expect(submitButton().querySelector('.app-loading-spinner')).toBeInTheDocument();
    });

    it('does not submit', () => {
      renderModal({ busy: true });

      fireEvent.submit(topicInput().closest('form'));

      expect(onSubmit).not.toHaveBeenCalled();
    });

    it('cannot be closed with Escape or the backdrop', () => {
      renderModal({ busy: true });

      fireEvent.keyDown(document, { key: 'Escape' });
      fireEvent.click(screen.getByRole('dialog'));

      expect(onClose).not.toHaveBeenCalled();
    });
  });

  describe('closing', () => {
    it('closes through the cancel button and the header close button', async () => {
      const user = userEvent.setup();
      renderModal();

      const buttons = screen.getAllByRole('button', { name: tr('speaking:create.cancel') });
      expect(buttons).toHaveLength(2);
      await user.click(buttons[0]);
      await user.click(buttons[1]);

      expect(onClose).toHaveBeenCalledTimes(2);
    });

    it('closes on Escape', () => {
      renderModal();

      fireEvent.keyDown(document, { key: 'Escape' });

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('closes when the backdrop is clicked but not when the content is clicked', () => {
      renderModal();

      fireEvent.click(screen.getByRole('heading', { name: tr('speaking:create.title') }));
      expect(onClose).not.toHaveBeenCalled();

      fireEvent.click(screen.getByRole('dialog'));
      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('locks the page scroll while open and restores it afterwards', () => {
      const { unmount } = renderModal();
      expect(document.body.style.overflow).toBe('hidden');

      unmount();

      expect(document.body.style.overflow).toBe('');
    });
  });

  describe('errors', () => {
    it('shows no alert without an error', () => {
      const { container } = renderModal();

      expect(container.ownerDocument.querySelector('.form-alert')).not.toBeInTheDocument();
    });

    it('explains a missing socket connection', () => {
      renderModal({ error: 'SOCKET_NOT_CONNECTED' });

      expect(screen.getByText(tr('speaking:error.socketNotConnected'))).toBeInTheDocument();
    });

    it('translates a server error code', () => {
      renderModal({ error: 'SPEAKING_ROOM_FULL' });

      expect(screen.getByText(tr('api:error.SPEAKING_ROOM_FULL'))).toBeInTheDocument();
    });

    it('falls back to a generic message for an unknown code', () => {
      renderModal({ error: 'SOMETHING_NEW' });

      expect(screen.getByText(tr('api:common.fallback'))).toBeInTheDocument();
    });
  });
});
