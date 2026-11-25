import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RoomCard from '@/modules/speaking/components/RoomCard/RoomCard';
import { LANGS, tr, trRx, useLang } from '../../../../utils/i18n';

describe('RoomCard component', () => {
  let onJoin;

  const buildRoom = (overrides = {}) => ({
    roomCode: 'ABC234',
    topic: 'Travel plans',
    status: 'WAITING',
    host: { displayName: 'Alice', avatarUrl: null },
    participantCount: 2,
    maxParticipants: 4,
    ...overrides,
  });

  const renderCard = (props = {}) =>
    render(<RoomCard room={buildRoom()} onJoin={onJoin} {...props} />);

  beforeEach(() => {
    onJoin = vi.fn();
  });

  describe.each(LANGS)('rendering [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('shows the topic, host, capacity and status', () => {
      renderCard();

      expect(screen.getByRole('heading', { name: 'Travel plans' })).toBeInTheDocument();
      expect(screen.getByText('Alice', { selector: '.speaking-room-card__host-name' })).toBeInTheDocument();
      expect(
        screen.getByText(tr('speaking:lobby.participants', { count: 2, max: 4 }))
      ).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:lobby.status.WAITING'))).toBeInTheDocument();
      expect(screen.getByRole('button', { name: trRx('speaking:lobby.join') })).toBeEnabled();
    });

    it('shows the translated status of an active room', () => {
      renderCard({ room: buildRoom({ status: 'ACTIVE' }) });

      expect(screen.getByText(tr('speaking:lobby.status.ACTIVE'))).toBeInTheDocument();
    });

    it('shows a placeholder when the room has no topic', () => {
      renderCard({ room: buildRoom({ topic: null }) });

      expect(screen.getByRole('heading', { name: tr('speaking:lobby.noTopic') })).toBeInTheDocument();
    });

    it('shows the full label and disables joining when the room is full', () => {
      renderCard({ room: buildRoom({ participantCount: 4, maxParticipants: 4 }) });

      const button = screen.getByRole('button', { name: trRx('speaking:lobby.full') });
      expect(button).toBeDisabled();
      expect(screen.queryByRole('button', { name: trRx('speaking:lobby.join') })).not.toBeInTheDocument();
    });
  });

  it('falls back to the raw status when it has no translation', () => {
    const { container } = renderCard({ room: buildRoom({ status: 'PAUSED' }) });

    expect(screen.getByText('PAUSED')).toBeInTheDocument();
    expect(container.querySelector('.speaking-status-chip--paused')).toBeInTheDocument();
  });

  it('shows Unknown when the host is missing', () => {
    renderCard({ room: buildRoom({ host: undefined }) });

    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });

  it('calls onJoin with the room code when the join button is clicked', async () => {
    const user = userEvent.setup();
    renderCard();

    await user.click(screen.getByRole('button', { name: trRx('speaking:lobby.join') }));

    expect(onJoin).toHaveBeenCalledTimes(1);
    expect(onJoin).toHaveBeenCalledWith('ABC234');
  });

  it('does not call onJoin when the room is full', async () => {
    const user = userEvent.setup();
    renderCard({ room: buildRoom({ participantCount: 4, maxParticipants: 4 }) });

    await user.click(screen.getByRole('button', { name: trRx('speaking:lobby.full') }));

    expect(onJoin).not.toHaveBeenCalled();
  });

  it('disables joining while a join is in progress', async () => {
    const user = userEvent.setup();
    renderCard({ joining: true });

    const button = screen.getByRole('button', { name: trRx('speaking:lobby.join') });
    await user.click(button);

    expect(button).toBeDisabled();
    expect(onJoin).not.toHaveBeenCalled();
  });

  it('shows the disabled title only when provided', () => {
    const { rerender } = renderCard({ joining: true, disabledTitle: 'Bạn đang ở phòng khác' });

    expect(screen.getByRole('button')).toHaveAttribute('title', 'Bạn đang ở phòng khác');

    rerender(<RoomCard room={buildRoom()} onJoin={onJoin} joining />);

    expect(screen.getByRole('button')).not.toHaveAttribute('title');
  });
});
