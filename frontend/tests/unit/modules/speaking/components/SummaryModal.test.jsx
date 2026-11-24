import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SummaryModal from '@/modules/speaking/components/SummaryModal/SummaryModal';
import { LANGS, tr, useLang } from '../../../../utils/i18n';

describe('SummaryModal component', () => {
  let onClose;
  let onNavigate;

  const summary = { topic: 'Travel plans', roomCode: 'ABC234', durationSeconds: 300 };

  const renderModal = (props = {}) =>
    render(
      <SummaryModal open summary={summary} onClose={onClose} onNavigate={onNavigate} {...props} />
    );

  const closeButtons = () => screen.getAllByRole('button', { name: tr('speaking:room.summary.toLobby') });

  beforeEach(() => {
    onClose = vi.fn();
    onNavigate = vi.fn();
  });

  describe('visibility', () => {
    it('renders nothing when closed', () => {
      renderModal({ open: false });

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('renders nothing when there is no summary', () => {
      renderModal({ summary: null });

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

    it('shows the topic, room code and formatted duration', () => {
      renderModal();

      expect(screen.getByRole('heading', { name: tr('speaking:room.summary.title') })).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:room.summary.topic'))).toBeInTheDocument();
      expect(screen.getByText('Travel plans')).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:room.summary.roomCode'))).toBeInTheDocument();
      expect(screen.getByText('ABC234')).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:room.summary.duration'))).toBeInTheDocument();
      expect(screen.getByText('5:00')).toBeInTheDocument();
    });

    it('shows a placeholder when the room had no topic', () => {
      renderModal({ summary: { ...summary, topic: null } });

      expect(screen.getByText(tr('speaking:lobby.noTopic'))).toBeInTheDocument();
    });
  });

  it('formats durations over one hour with hours', () => {
    renderModal({ summary: { ...summary, durationSeconds: 3725 } });

    expect(screen.getByText('1:02:05')).toBeInTheDocument();
  });

  describe('actions', () => {
    it('closes through both the header and the footer buttons', async () => {
      const user = userEvent.setup();
      renderModal();

      const buttons = closeButtons();
      expect(buttons).toHaveLength(2);
      await user.click(buttons[0]);
      await user.click(buttons[1]);

      expect(onClose).toHaveBeenCalledTimes(2);
    });

    it('navigates to the history', async () => {
      const user = userEvent.setup();
      renderModal();

      await user.click(screen.getByRole('button', { name: tr('speaking:room.summary.toHistory') }));

      expect(onNavigate).toHaveBeenCalledTimes(1);
      expect(onClose).not.toHaveBeenCalled();
    });

    it('closes when the backdrop is clicked', () => {
      renderModal();

      fireEvent.click(screen.getByRole('dialog'));

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('does not close when the content is clicked', () => {
      renderModal();

      fireEvent.click(screen.getByText('Travel plans'));

      expect(onClose).not.toHaveBeenCalled();
    });
  });

  describe('keyboard and focus', () => {
    it('focuses the close button when opened', () => {
      renderModal();

      expect(closeButtons()[0]).toHaveFocus();
    });

    it('closes on Escape', () => {
      renderModal();

      fireEvent.keyDown(document, { key: 'Escape' });

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('keeps the focus on the close button when Tab is pressed', () => {
      renderModal();
      screen.getByRole('button', { name: tr('speaking:room.summary.toHistory') }).focus();

      fireEvent.keyDown(document, { key: 'Tab' });

      expect(closeButtons()[0]).toHaveFocus();
    });

    it('ignores other keys', () => {
      renderModal();

      fireEvent.keyDown(document, { key: 'a' });

      expect(onClose).not.toHaveBeenCalled();
    });

    it('stops listening to the keyboard when closed', () => {
      const { rerender } = renderModal();

      rerender(<SummaryModal open={false} summary={summary} onClose={onClose} onNavigate={onNavigate} />);
      fireEvent.keyDown(document, { key: 'Escape' });

      expect(onClose).not.toHaveBeenCalled();
    });
  });

  describe('scroll lock', () => {
    it('locks the page scroll while open and restores it on close', () => {
      const { rerender } = renderModal();
      expect(document.body.style.overflow).toBe('hidden');

      rerender(<SummaryModal open={false} summary={summary} onClose={onClose} onNavigate={onNavigate} />);

      expect(document.body.style.overflow).toBe('');
    });

    it('restores the page scroll on unmount', () => {
      const { unmount } = renderModal();

      unmount();

      expect(document.body.style.overflow).toBe('');
    });
  });
});
