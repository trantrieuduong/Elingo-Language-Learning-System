import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, waitFor, act } from '@testing-library/react';
import YouTubePlayer from '@/modules/admin/pages/LessonManagementPage/components/YouTubePlayer';
import { loadYouTubeIframeApi } from '@/modules/admin/pages/LessonManagementPage/utils/youtube';

// Mock util load API: component chỉ cần nhận về object YT, không cần đụng tới <script> thật.
// (Test util loadYouTubeIframeApi đã có riêng ở utils/youtube.test.js)
vi.mock('@/modules/admin/pages/LessonManagementPage/utils/youtube', () => ({
  loadYouTubeIframeApi: vi.fn(),
  extractVideoId: vi.fn(),
}));

describe('YouTubePlayer component', () => {
  let mockPlayer;
  let mockYT;

  // Lấy config mà component truyền vào `new YT.Player(el, config)`
  const getPlayerConfig = (callIndex = 0) => mockYT.Player.mock.calls[callIndex][1];

  // Chờ component tạo xong player (xảy ra sau khi promise của loadYouTubeIframeApi resolve)
  const waitForPlayer = () =>
    waitFor(() => {
      expect(mockYT.Player).toHaveBeenCalled();
    });

  beforeEach(() => {
    mockPlayer = {
      cueVideoById: vi.fn(),
      getDuration: vi.fn(() => 600),
      getCurrentTime: vi.fn(() => 0),
      seekTo: vi.fn(),
      playVideo: vi.fn(),
      pauseVideo: vi.fn(),
      destroy: vi.fn(),
    };

    // Phải dùng `function` (không dùng arrow) vì component gọi `new YT.Player(...)`.
    // Trả về object -> `new` sẽ trả đúng mockPlayer.
    mockYT = {
      Player: vi.fn(function Player() {
        return mockPlayer;
      }),
    };

    loadYouTubeIframeApi.mockImplementation(() => Promise.resolve(mockYT));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders placeholder when no videoId is provided', () => {
    const { container } = render(<YouTubePlayer videoId={null} />);

    // Placeholder là icon SVG (aria-hidden nên không query bằng role được)
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(loadYouTubeIframeApi).not.toHaveBeenCalled();
    expect(mockYT.Player).not.toHaveBeenCalled();
  });

  it('creates YouTube player when videoId is provided', async () => {
    render(<YouTubePlayer videoId="abc123" />);

    await waitForPlayer();

    expect(loadYouTubeIframeApi).toHaveBeenCalled();
    expect(mockYT.Player).toHaveBeenCalledWith(
      expect.any(HTMLElement),
      expect.objectContaining({ videoId: 'abc123' })
    );
  });

  it('calls onReady with duration when player is ready', async () => {
    const mockOnReady = vi.fn();
    render(<YouTubePlayer videoId="abc123" onReady={mockOnReady} />);
    await waitForPlayer();

    // Giả lập YouTube bắn sự kiện onReady
    act(() => {
      getPlayerConfig().events.onReady({ target: mockPlayer });
    });

    expect(mockOnReady).toHaveBeenCalledWith({ durationMs: 600000 });
  });

  it('calls onError when player errors', async () => {
    const mockOnError = vi.fn();
    render(<YouTubePlayer videoId="abc123" onError={mockOnError} />);
    await waitForPlayer();

    act(() => {
      getPlayerConfig().events.onError({ data: 101 });
    });

    expect(mockOnError).toHaveBeenCalledWith(101);
  });

  it('calls onStateChange when player state changes', async () => {
    const mockOnStateChange = vi.fn();
    render(<YouTubePlayer videoId="abc123" onStateChange={mockOnStateChange} />);
    await waitForPlayer();

    act(() => {
      getPlayerConfig().events.onStateChange({ data: 1 }); // Playing
    });

    expect(mockOnStateChange).toHaveBeenCalledWith(1);
  });

  it('provides seekTo method via ref', async () => {
    const ref = React.createRef();
    render(<YouTubePlayer ref={ref} videoId="abc123" />);
    await waitForPlayer();

    ref.current.seekTo(30);

    expect(mockPlayer.seekTo).toHaveBeenCalledWith(30, true);
  });

  it('provides getCurrentTime method via ref', async () => {
    const ref = React.createRef();
    mockPlayer.getCurrentTime = vi.fn(() => 45);

    render(<YouTubePlayer ref={ref} videoId="abc123" />);
    await waitForPlayer();

    expect(ref.current.getCurrentTime()).toBe(45);
  });

  it('provides play method via ref', async () => {
    const ref = React.createRef();
    render(<YouTubePlayer ref={ref} videoId="abc123" />);
    await waitForPlayer();

    ref.current.play();

    expect(mockPlayer.playVideo).toHaveBeenCalled();
  });

  it('provides pause method via ref', async () => {
    const ref = React.createRef();
    render(<YouTubePlayer ref={ref} videoId="abc123" />);
    await waitForPlayer();

    ref.current.pause();

    expect(mockPlayer.pauseVideo).toHaveBeenCalled();
  });

  it('provides getDurationMs method via ref', async () => {
    const ref = React.createRef();
    mockPlayer.getDuration = vi.fn(() => 300);

    render(<YouTubePlayer ref={ref} videoId="abc123" />);
    await waitForPlayer();

    expect(ref.current.getDurationMs()).toBe(300000);
  });

  it('cues new video when videoId changes', async () => {
    const { rerender } = render(<YouTubePlayer videoId="abc123" />);
    await waitForPlayer();

    rerender(<YouTubePlayer videoId="xyz789" />);

    expect(mockPlayer.cueVideoById).toHaveBeenCalledWith('xyz789');
    // Không tạo player mới, chỉ cue video mới trên player cũ
    expect(mockYT.Player).toHaveBeenCalledTimes(1);
  });

  it('destroys player when videoId is removed', async () => {
    const { rerender } = render(<YouTubePlayer videoId="abc123" />);
    await waitForPlayer();

    rerender(<YouTubePlayer videoId={null} />);

    expect(mockPlayer.destroy).toHaveBeenCalled();
  });

  it('reports DURATION_UNAVAILABLE after polling times out', async () => {
    const mockOnError = vi.fn();
    mockPlayer.getDuration = vi.fn(() => 0); // Duration chưa có

    render(<YouTubePlayer videoId="abc123" onError={mockOnError} />);
    await waitForPlayer();

    // Chỉ bật fake timers SAU khi player đã tạo (để waitFor ở trên vẫn dùng timer thật).
    // Polling: 40 lần x 250ms = 10s thật -> dùng fake timers để khỏi chờ.
    vi.useFakeTimers();
    act(() => {
      getPlayerConfig().events.onReady({ target: mockPlayer });
    });
    expect(mockOnError).not.toHaveBeenCalled();

    act(() => {
      vi.advanceTimersByTime(250 * 40);
    });

    expect(mockOnError).toHaveBeenCalledWith('DURATION_UNAVAILABLE');
    expect(mockOnError).toHaveBeenCalledTimes(1);
  });

  it('applies custom className', () => {
    const { container } = render(
      <YouTubePlayer videoId="abc123" className="custom-class" />
    );

    expect(container.firstChild).toHaveClass('custom-class');
  });

  it('applies custom className to the placeholder too', () => {
    const { container } = render(
      <YouTubePlayer videoId={null} className="custom-class" />
    );

    expect(container.firstChild).toHaveClass('custom-class');
  });

  it('handles errors gracefully in ref methods', async () => {
    const ref = React.createRef();
    mockPlayer.seekTo = vi.fn(() => {
      throw new Error('Player error');
    });

    render(<YouTubePlayer ref={ref} videoId="abc123" />);
    await waitForPlayer();

    expect(() => ref.current.seekTo(30)).not.toThrow();
  });

  it('returns default values when player is not ready', () => {
    const ref = React.createRef();
    render(<YouTubePlayer ref={ref} videoId={null} />);

    expect(ref.current.getCurrentTime()).toBe(0);
    expect(ref.current.getDurationMs()).toBe(0);
  });

  it('cleans up player on unmount', async () => {
    const { unmount } = render(<YouTubePlayer videoId="abc123" />);
    await waitForPlayer();

    unmount();

    expect(mockPlayer.destroy).toHaveBeenCalled();
  });

  it('does not create a player if unmounted before the API finishes loading', async () => {
    let resolveApi;
    loadYouTubeIframeApi.mockImplementation(
      () => new Promise((resolve) => { resolveApi = resolve; })
    );

    const { unmount } = render(<YouTubePlayer videoId="abc123" />);
    unmount();

    await act(async () => {
      resolveApi(mockYT);
    });

    expect(mockYT.Player).not.toHaveBeenCalled();
  });

  it('handles API load failure', async () => {
    const mockOnError = vi.fn();
    loadYouTubeIframeApi.mockImplementation(() =>
      Promise.reject(new Error('YT_API_LOAD_FAILED'))
    );

    render(<YouTubePlayer videoId="abc123" onError={mockOnError} />);

    await waitFor(() => {
      expect(mockOnError).toHaveBeenCalledWith('YT_API_LOAD_FAILED');
    });
    expect(mockYT.Player).not.toHaveBeenCalled();
  });

  it('configures player with correct playerVars', async () => {
    render(<YouTubePlayer videoId="abc123" />);
    await waitForPlayer();

    expect(mockYT.Player).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        playerVars: {
          rel: 0,
          modestbranding: 1,
          playsinline: 1,
        },
      })
    );
  });
});
