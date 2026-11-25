import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, waitFor } from '@testing-library/react';
import RemoteAudio from '@/modules/speaking/components/RemoteAudio/RemoteAudio';

describe('RemoteAudio component', () => {
  const originalPlay = window.HTMLMediaElement.prototype.play;
  let play;

  beforeEach(() => {
    play = vi.fn().mockResolvedValue(undefined);
    window.HTMLMediaElement.prototype.play = play;
  });

  afterEach(() => {
    window.HTMLMediaElement.prototype.play = originalPlay;
  });

  it('renders an inline autoplay audio element', () => {
    const { container } = render(<RemoteAudio stream={{ id: 's1' }} />);

    const audio = container.querySelector('audio');
    expect(audio).toBeInTheDocument();
    expect(audio).toHaveAttribute('autoplay');
    expect(audio).toHaveAttribute('playsinline');
  });

  it('attaches the stream and starts playback', () => {
    const stream = { id: 's1' };
    const { container } = render(<RemoteAudio stream={stream} />);

    expect(container.querySelector('audio').srcObject).toBe(stream);
    expect(play).toHaveBeenCalledTimes(1);
  });

  it('does nothing without a stream', () => {
    const { container } = render(<RemoteAudio stream={null} />);

    expect(container.querySelector('audio').srcObject).toBeFalsy();
    expect(play).not.toHaveBeenCalled();
  });

  it('attaches the new stream when it changes', () => {
    const first = { id: 's1' };
    const second = { id: 's2' };
    const { container, rerender } = render(<RemoteAudio stream={first} />);

    rerender(<RemoteAudio stream={second} />);

    expect(container.querySelector('audio').srcObject).toBe(second);
    expect(play).toHaveBeenCalledTimes(2);
  });

  it('detaches the stream on unmount', () => {
    const { container, unmount } = render(<RemoteAudio stream={{ id: 's1' }} />);
    const audio = container.querySelector('audio');

    unmount();

    expect(audio.srcObject).toBeNull();
  });

  it('calls onAutoplayBlocked when the browser blocks playback', async () => {
    play.mockRejectedValue(new Error('NotAllowedError'));
    const onAutoplayBlocked = vi.fn();

    render(<RemoteAudio stream={{ id: 's1' }} onAutoplayBlocked={onAutoplayBlocked} />);

    await waitFor(() => expect(onAutoplayBlocked).toHaveBeenCalledTimes(1));
  });

  it('does not throw when playback is blocked and no handler is given', async () => {
    play.mockRejectedValue(new Error('NotAllowedError'));

    expect(() => render(<RemoteAudio stream={{ id: 's1' }} />)).not.toThrow();
    await waitFor(() => expect(play).toHaveBeenCalled());
  });
});
