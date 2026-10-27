import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import React, { createRef } from 'react';
import LessonYoutubePlayer from '@/modules/learning/lesson/components/LessonYoutubePlayer/LessonYoutubePlayer';

describe('LessonYoutubePlayer component', () => {
  let mockPlayerInstance;
  let onReadyCallback;
  let onErrorCallback;

  beforeEach(() => {
    mockPlayerInstance = {
      seekTo: vi.fn(),
      playVideo: vi.fn(),
      pauseVideo: vi.fn(),
      getCurrentTime: vi.fn().mockReturnValue(0),
      destroy: vi.fn(),
    };

    function MockPlayer(element, config) {
      if (config.events) {
        onReadyCallback = config.events.onReady;
        onErrorCallback = config.events.onError;
      }
      return mockPlayerInstance;
    }

    window.YT = {
      Player: vi.fn().mockImplementation(MockPlayer),
    };
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders fallback when videoId is not provided', () => {
    render(
      <LessonYoutubePlayer
        videoId=""
        title="Sample Video"
        thumbnailUrl="https://example.com/thumb.jpg"
        sourceUrl="https://youtube.com/watch?v=123"
      />
    );

    expect(screen.getByText('Sample Video')).toBeInTheDocument();
    expect(screen.getByText('smart_display')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /open/i })).toHaveAttribute(
      'href',
      'https://youtube.com/watch?v=123'
    );
  });

  it('initializes YT.Player when videoId is provided and handles controls via ref', async () => {
    const playerRef = createRef();

    render(
      <LessonYoutubePlayer
        ref={playerRef}
        videoId="dQw4w9WgXcQ"
        startMs={1000}
        endMs={5000}
        title="Rick Roll"
      />
    );

    await act(async () => {
      await Promise.resolve();
    });

    expect(window.YT.Player).toHaveBeenCalled();

    // Trigger onReady event
    act(() => {
      if (onReadyCallback) {
        onReadyCallback({ target: mockPlayerInstance });
      }
    });

    expect(playerRef.current.isReady()).toBe(true);

    // Test imperative controls
    act(() => {
      playerRef.current.replay();
    });
    expect(mockPlayerInstance.seekTo).toHaveBeenCalledWith(1, true);
    expect(mockPlayerInstance.playVideo).toHaveBeenCalled();

    act(() => {
      playerRef.current.pause();
    });
    expect(mockPlayerInstance.pauseVideo).toHaveBeenCalled();

    act(() => {
      playerRef.current.seekTo(3, false);
    });
    expect(mockPlayerInstance.seekTo).toHaveBeenCalledWith(3, false);

    act(() => {
      playerRef.current.play();
    });
    expect(mockPlayerInstance.playVideo).toHaveBeenCalled();
  });

  it('renders error overlay when player encounters onError', async () => {
    render(<LessonYoutubePlayer videoId="error-video" title="Error Video" />);

    await act(async () => {
      await Promise.resolve();
    });

    act(() => {
      if (onErrorCallback) {
        onErrorCallback();
      }
    });

    expect(screen.getByText('error')).toBeInTheDocument();
  });

  it('stops video and seeks back to startSec when currentTime exceeds endMs', async () => {
    vi.useFakeTimers();

    render(
      <LessonYoutubePlayer
        videoId="dQw4w9WgXcQ"
        startMs={1000}
        endMs={3000}
        title="Interval Test"
      />
    );

    await act(async () => {
      await Promise.resolve();
    });

    act(() => {
      if (onReadyCallback) {
        onReadyCallback({ target: mockPlayerInstance });
      }
    });

    mockPlayerInstance.getCurrentTime.mockReturnValue(3.5);

    act(() => {
      vi.advanceTimersByTime(250);
    });

    expect(mockPlayerInstance.pauseVideo).toHaveBeenCalled();
    expect(mockPlayerInstance.seekTo).toHaveBeenCalledWith(1, true);

    vi.useRealTimers();
  });

  it('triggers seekTo and playVideo when startMs changes while player is ready', async () => {
    const { rerender } = render(
      <LessonYoutubePlayer
        videoId="dQw4w9WgXcQ"
        startMs={1000}
        endMs={5000}
        title="StartMs Change Test"
      />
    );

    await act(async () => {
      await Promise.resolve();
    });

    act(() => {
      if (onReadyCallback) {
        onReadyCallback({ target: mockPlayerInstance });
      }
    });

    mockPlayerInstance.seekTo.mockClear();
    mockPlayerInstance.playVideo.mockClear();

    // Rerender with different startMs
    rerender(
      <LessonYoutubePlayer
        videoId="dQw4w9WgXcQ"
        startMs={3000}
        endMs={5000}
        title="StartMs Change Test"
      />
    );

    expect(mockPlayerInstance.seekTo).toHaveBeenCalledWith(3, true);
    expect(mockPlayerInstance.playVideo).toHaveBeenCalled();
  });

  it('destroys previous player instance when re-initializing or unmounting', async () => {
    const { rerender, unmount } = render(
      <LessonYoutubePlayer
        videoId="video-1"
        title="First Video"
      />
    );

    await act(async () => {
      await Promise.resolve();
    });

    act(() => {
      if (onReadyCallback) {
        onReadyCallback({ target: mockPlayerInstance });
      }
    });

    // Rerender with another videoId to trigger re-init cleanup
    rerender(
      <LessonYoutubePlayer
        videoId="video-2"
        title="Second Video"
      />
    );

    await act(async () => {
      await Promise.resolve();
    });

    expect(mockPlayerInstance.destroy).toHaveBeenCalled();

    // Unmount to test cleanup
    unmount();
    expect(mockPlayerInstance.destroy).toHaveBeenCalledTimes(2);
  });

  it('handles exception when new YT.Player throws', async () => {
    window.YT.Player.mockImplementationOnce(() => {
      throw new Error('YT Player init failed');
    });

    render(
      <LessonYoutubePlayer
        videoId="throw-video"
        title="Throw Video"
      />
    );

    await act(async () => {
      await Promise.resolve();
    });

    expect(screen.getByText('error')).toBeInTheDocument();
  });

  it('loads script tag when window.YT is not immediately present', async () => {
    const originalYT = window.YT;
    delete window.YT;

    render(
      <LessonYoutubePlayer
        videoId="script-load-video"
        title="Script Load Video"
      />
    );

    // Verify script tag was created in DOM
    const script = document.querySelector('script[src="https://www.youtube.com/iframe_api"]');
    expect(script).toBeTruthy();

    // Simulate script loading and onYouTubeIframeAPIReady callback
    window.YT = originalYT;
    act(() => {
      if (window.onYouTubeIframeAPIReady) {
        window.onYouTubeIframeAPIReady();
      }
    });

    await act(async () => {
      await Promise.resolve();
    });

    expect(window.YT.Player).toHaveBeenCalled();
  });
});

