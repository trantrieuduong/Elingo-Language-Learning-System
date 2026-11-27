import React, {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
} from 'react';
import PropTypes from 'prop-types';
import { PlayIcon } from '../../../components/icons/AdminIcons';
import { loadYouTubeIframeApi } from '../utils/youtube';
import styles from './YouTubePlayer.module.css';

const POLL_INTERVAL_MS = 250;
const MAX_POLL_ATTEMPTS = 40;

const YouTubePlayer = forwardRef(function YouTubePlayer(
  { videoId, onReady, onError, onStateChange, className = '' },
  ref
) {
  const containerRef = useRef(null);
  const playerRef = useRef(null);
  const pollTimerRef = useRef(null);
  const currentVideoIdRef = useRef(videoId);

  const callbacksRef = useRef({ onReady, onError, onStateChange });
  callbacksRef.current = { onReady, onError, onStateChange };

  const stopPolling = () => {
    if (pollTimerRef.current) {
      clearInterval(pollTimerRef.current);
      pollTimerRef.current = null;
    }
  };

  const pollDuration = (player, isCancelled) => {
    stopPolling();
    let attempts = 0;

    const check = () => {
      if (isCancelled()) {
        stopPolling();
        return;
      }
      attempts += 1;
      try {
        const d = typeof player.getDuration === 'function' ? player.getDuration() : 0;
        if (d > 0) {
          stopPolling();
          callbacksRef.current.onReady?.({ durationMs: Math.round(d * 1000) });
          return;
        }
      } catch {
        // Player chưa sẵn sàng
      }
      if (attempts >= MAX_POLL_ATTEMPTS) {
        stopPolling();
        callbacksRef.current.onError?.('DURATION_UNAVAILABLE');
      }
    };

    check();
    if (attempts < MAX_POLL_ATTEMPTS && !pollTimerRef.current) {
      pollTimerRef.current = setInterval(check, POLL_INTERVAL_MS);
    }
  };

  useImperativeHandle(ref, () => ({
    seekTo: (seconds) => {
      try {
        playerRef.current?.seekTo?.(seconds, true);
      } catch {
        // ignore
      }
    },
    getCurrentTime: () => {
      try {
        return playerRef.current?.getCurrentTime?.() || 0;
      } catch {
        return 0;
      }
    },
    play: () => {
      try {
        playerRef.current?.playVideo?.();
      } catch {
        // ignore
      }
    },
    pause: () => {
      try {
        playerRef.current?.pauseVideo?.();
      } catch {
        // ignore
      }
    },
    getDurationMs: () => {
      try {
        const d = playerRef.current?.getDuration?.() || 0;
        return d > 0 ? Math.round(d * 1000) : 0;
      } catch {
        return 0;
      }
    },
  }));

  useEffect(() => {
    currentVideoIdRef.current = videoId;
  }, [videoId]);

  useEffect(() => {
    if (!videoId) {
      stopPolling();
      if (playerRef.current) {
        try {
          playerRef.current.destroy();
        } catch {
          // ignore
        }
        playerRef.current = null;
      }
      return undefined;
    }

    let cancelled = false;

    // Nếu player đã tồn tại -> cue video mới, không tạo player mới
    if (playerRef.current && typeof playerRef.current.cueVideoById === 'function') {
      try {
        playerRef.current.cueVideoById(videoId);
        pollDuration(playerRef.current, () => cancelled);
      } catch {
        callbacksRef.current.onError?.('DURATION_UNAVAILABLE');
      }
      return () => {
        cancelled = true;
        stopPolling();
      };
    }

    loadYouTubeIframeApi()
      .then((YT) => {
        if (cancelled || !containerRef.current) return;

        playerRef.current = new YT.Player(containerRef.current, {
          videoId,
          playerVars: {
            rel: 0,
            modestbranding: 1,
            playsinline: 1,
          },
          events: {
            onReady: (event) => {
              if (cancelled) return;
              pollDuration(event.target, () => cancelled);
            },
            onError: (event) => {
              if (cancelled) return;
              stopPolling();
              callbacksRef.current.onError?.(event.data);
            },
            onStateChange: (event) => {
              if (cancelled) return;
              callbacksRef.current.onStateChange?.(event.data);
            },
          },
        });
      })
      .catch(() => {
        if (!cancelled) {
          callbacksRef.current.onError?.('YT_API_LOAD_FAILED');
        }
      });

    return () => {
      cancelled = true;
      stopPolling();
    };
  }, [videoId]);

  // Cleanup khi unmount hoàn toàn
  useEffect(() => {
    return () => {
      stopPolling();
      if (playerRef.current) {
        try {
          playerRef.current.destroy();
        } catch {
          // ignore
        }
        playerRef.current = null;
      }
    };
  }, []);

  if (!videoId) {
    return (
      <div className={`${styles.playerWrapper} ${styles.placeholder} ${className}`}>
        <PlayIcon size={32} className={styles.placeholderIcon} />
      </div>
    );
  }

  return (
    <div className={`${styles.playerWrapper} ${className}`}>
      <div ref={containerRef} className={styles.playerMount} />
    </div>
  );
});

YouTubePlayer.propTypes = {
  videoId: PropTypes.string,
  onReady: PropTypes.func,
  onError: PropTypes.func,
  onStateChange: PropTypes.func,
  className: PropTypes.string,
};

export default YouTubePlayer;
