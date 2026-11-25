import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import PeerTile from '@/modules/speaking/components/PeerTile/PeerTile';
import { LANGS, tr, useLang } from '../../../../utils/i18n';

describe('PeerTile component', () => {
  let onMute;
  let onKick;

  const buildPeer = (overrides = {}) => ({
    userId: 2,
    displayName: 'Bob',
    avatarUrl: null,
    role: 'GUEST',
    link: 'connected',
    grace: false,
    micMuted: false,
    ...overrides,
  });

  const renderTile = (props = {}) =>
    render(<PeerTile peer={buildPeer()} onMute={onMute} onKick={onKick} {...props} />);

  const muteButton = () =>
    screen.queryByRole('button', { name: tr('speaking:room.muteAction', { name: 'Bob' }) });
  const kickButton = () =>
    screen.queryByRole('button', { name: tr('speaking:room.kickAction', { name: 'Bob' }) });
  const statusIcon = (container) =>
    container.querySelector('.speaking-peer-tile__status-icon .material-symbols-outlined');

  beforeEach(() => {
    onMute = vi.fn();
    onKick = vi.fn();
  });

  describe('identity', () => {
    it('shows the display name', () => {
      renderTile();

      expect(screen.getByText('Bob', { selector: '.speaking-peer-tile__name' })).toBeInTheDocument();
    });

    it('falls back to the "you" label when the peer has no name', () => {
      renderTile({ peer: buildPeer({ displayName: '' }) });

      expect(
        screen.getByText(tr('speaking:room.you'), { selector: '.speaking-peer-tile__name' })
      ).toBeInTheDocument();
    });

    it('marks the tile of the current user', () => {
      const { container } = renderTile({ isSelf: true });

      expect(container.querySelector('article')).toHaveClass('speaking-peer-tile--self');
      expect(
        screen.getByText(tr('speaking:room.you'), { selector: '.speaking-peer-badge--self' })
      ).toBeInTheDocument();
    });

    it.each([
      ['the isHost prop', { isHost: true }, buildPeer()],
      ['the HOST role', {}, buildPeer({ role: 'HOST' })],
    ])('shows the host badge from %s', (_label, props, peer) => {
      renderTile({ peer, ...props });

      expect(
        screen.getByText(tr('speaking:room.host'), { selector: '.speaking-peer-badge--host' })
      ).toBeInTheDocument();
    });

    it('shows no host badge for a regular guest', () => {
      const { container } = renderTile();

      expect(container.querySelector('.speaking-peer-badge--host')).not.toBeInTheDocument();
    });
  });

  describe.each(LANGS)('link state [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it.each(['connecting', 'connected', 'failed'])('shows the %s link state', (link) => {
      const { container } = renderTile({ peer: buildPeer({ link }) });

      expect(screen.getByText(tr(`speaking:room.link.${link}`))).toBeInTheDocument();
      expect(container.querySelector(`.speaking-peer-tile__link--${link}`)).toBeInTheDocument();
    });

    it('shows the grace message for a peer waiting to come back', () => {
      const { container } = renderTile({ grace: true });

      expect(screen.getByText(tr('speaking:room.link.grace'))).toBeInTheDocument();
      expect(container.querySelector('article')).toHaveClass('speaking-peer-tile--grace');
    });

    it('shows the host grace message when the disconnected peer is the host', () => {
      renderTile({ peer: buildPeer({ role: 'HOST', grace: true }) });

      expect(screen.getByText(tr('speaking:room.link.hostGrace'))).toBeInTheDocument();
    });
  });

  it('shows no link text when the link state is unknown', () => {
    const { container } = renderTile({ peer: buildPeer({ link: undefined }) });

    expect(container.querySelector('.speaking-peer-tile__link-text')).toHaveTextContent('');
  });

  it('shows no link state for the current user', () => {
    const { container } = renderTile({ isSelf: true, peer: buildPeer({ link: undefined }) });

    expect(container.querySelector('.speaking-peer-tile__link')).not.toBeInTheDocument();
  });

  describe('mic status', () => {
    it('shows an active mic for another peer who is not muted', () => {
      const { container } = renderTile();

      expect(statusIcon(container)).toHaveTextContent('mic');
      expect(statusIcon(container)).toHaveClass('speaking-peer-tile__status-icon--active');
    });

    it.each([
      ['the micMuted prop', { micMuted: true }, buildPeer()],
      ['the peer micMuted flag', {}, buildPeer({ micMuted: true })],
    ])('shows a muted mic from %s', (_label, props, peer) => {
      const { container } = renderTile({ peer, ...props });

      expect(statusIcon(container)).toHaveTextContent('mic_off');
      expect(statusIcon(container)).toHaveClass('speaking-peer-tile__status-icon--muted');
    });

    it('uses the muted prop for the current user', () => {
      const { container, rerender } = renderTile({ isSelf: true, muted: true });
      expect(statusIcon(container)).toHaveTextContent('mic_off');

      rerender(<PeerTile peer={buildPeer()} isSelf muted={false} />);

      expect(statusIcon(container)).toHaveTextContent('mic');
    });

    it('ignores the muted prop for another peer', () => {
      const { container } = renderTile({ muted: true });

      expect(statusIcon(container)).toHaveTextContent('mic');
    });
  });

  describe('mute action', () => {
    it('is available to a host for a connected, unmuted peer', () => {
      renderTile({ canMute: true });

      expect(muteButton()).toBeInTheDocument();
    });

    it.each([
      ['the viewer cannot mute', { canMute: false }, buildPeer()],
      ['the tile is the current user', { canMute: true, isSelf: true }, buildPeer()],
      ['the link is still connecting', { canMute: true }, buildPeer({ link: 'connecting' })],
      ['the link failed', { canMute: true }, buildPeer({ link: 'failed' })],
      ['the peer is waiting to come back', { canMute: true, grace: true }, buildPeer()],
      ['the peer is already muted (prop)', { canMute: true, micMuted: true }, buildPeer()],
      ['the peer is already muted (peer flag)', { canMute: true }, buildPeer({ micMuted: true })],
    ])('is hidden when %s', (_label, props, peer) => {
      renderTile({ peer, ...props });

      expect(muteButton()).not.toBeInTheDocument();
    });

    describe('cooldown', () => {
      beforeEach(() => {
        vi.useFakeTimers();
      });

      afterEach(() => {
        vi.useRealTimers();
      });

      it('requests the mute then disables the button for 2 seconds', () => {
        renderTile({ canMute: true });

        fireEvent.click(muteButton());

        expect(onMute).toHaveBeenCalledTimes(1);
        expect(onMute).toHaveBeenCalledWith(2);
        expect(muteButton()).toBeDisabled();

        fireEvent.click(muteButton());
        expect(onMute).toHaveBeenCalledTimes(1);

        act(() => {
          vi.advanceTimersByTime(1999);
        });
        expect(muteButton()).toBeDisabled();

        act(() => {
          vi.advanceTimersByTime(1);
        });
        expect(muteButton()).toBeEnabled();
      });
    });

    it('does not throw when onMute is missing', () => {
      renderTile({ canMute: true, onMute: undefined });

      expect(() => fireEvent.click(muteButton())).not.toThrow();
    });
  });

  describe('kick action', () => {
    it('is available when the viewer can kick', () => {
      renderTile({ canKick: true });

      expect(kickButton()).toBeInTheDocument();
    });

    it('is available even when the peer link is not connected', () => {
      renderTile({ canKick: true, peer: buildPeer({ link: 'failed' }) });

      expect(kickButton()).toBeInTheDocument();
    });

    it('is hidden when the viewer cannot kick', () => {
      renderTile({ canKick: false });

      expect(kickButton()).not.toBeInTheDocument();
    });

    it('is hidden on the tile of the current user', () => {
      renderTile({ canKick: true, isSelf: true });

      expect(kickButton()).not.toBeInTheDocument();
    });

    it('calls onKick with the user id and display name', () => {
      renderTile({ canKick: true });

      fireEvent.click(kickButton());

      expect(onKick).toHaveBeenCalledWith(2, 'Bob');
    });

    it('does not throw when onKick is missing', () => {
      renderTile({ canKick: true, onKick: undefined });

      expect(() => fireEvent.click(kickButton())).not.toThrow();
    });
  });
});
