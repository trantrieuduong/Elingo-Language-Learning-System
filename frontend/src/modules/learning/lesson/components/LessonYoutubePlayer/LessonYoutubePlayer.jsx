import { useEffect, useRef, useState, useImperativeHandle } from 'react';
import { useTranslation } from 'react-i18next';
import './LessonYoutubePlayer.css';

// Singleton promise nạp script YouTube IFrame API đúng 1 lần duy nhất trong toàn app
let ytApiPromise = null;

function loadYouTubeIframeApi() {
  if (typeof window !== 'undefined' && window.YT && window.YT.Player) {
    return Promise.resolve(window.YT);
  }

  if (!ytApiPromise) {
    ytApiPromise = new Promise((resolve) => {
      const prevCallback = window.onYouTubeIframeAPIReady;
      window.onYouTubeIframeAPIReady = () => {
        if (typeof prevCallback === 'function') prevCallback();
        resolve(window.YT);
      };

      const script = document.createElement('script');
      script.src = 'https://www.youtube.com/iframe_api';
      script.async = true;
      const firstScript = document.getElementsByTagName('script')[0];
      if (firstScript && firstScript.parentNode) {
        firstScript.parentNode.insertBefore(script, firstScript);
      } else {
        document.head.appendChild(script);
      }
    });
  }

  return ytApiPromise;
}

function LessonYoutubePlayer({
  videoId,
  startMs = 0,
  endMs = 0,
  title = '',
  description = '',
  thumbnailUrl = '',
  sourceUrl = '',
  ref,
}) {
  const { t } = useTranslation(['learning']);
  const containerRef = useRef(null);
  const playerRef = useRef(null);
  const [isPlayerReady, setIsPlayerReady] = useState(false);
  const [hasError, setHasError] = useState(false);

  // Lưu startMs và endMs mới nhất vào ref để timer loop truy cập không bị closure cũ
  const timeRangeRef = useRef({ startMs, endMs });
  timeRangeRef.current = { startMs, endMs };

  const getStartSec = () => Math.floor((timeRangeRef.current.startMs || 0) / 1000);
  const getEndSec = () => Math.ceil((timeRangeRef.current.endMs || 0) / 1000);

  // Cung cấp các phương thức điều khiển cho component cha
  useImperativeHandle(ref, () => ({
    replay: () => {
      if (playerRef.current && typeof playerRef.current.seekTo === 'function') {
        const startSec = getStartSec();
        playerRef.current.seekTo(startSec, true);
        if (typeof playerRef.current.playVideo === 'function') {
          playerRef.current.playVideo();
        }
      }
    },
    play: () => {
      if (playerRef.current && typeof playerRef.current.playVideo === 'function') {
        playerRef.current.playVideo();
      }
    },
    pause: () => {
      if (playerRef.current && typeof playerRef.current.pauseVideo === 'function') {
        playerRef.current.pauseVideo();
      }
    },
    seekTo: (seconds, allowSeekAhead = true) => {
      if (playerRef.current && typeof playerRef.current.seekTo === 'function') {
        playerRef.current.seekTo(seconds, allowSeekAhead);
      }
    },
    isReady: () => isPlayerReady,
  }));

  // 1. Khởi tạo YT.Player khi có videoId và containerRef
  useEffect(() => {
    if (!videoId) {
      setHasError(false);
      return;
    }

    let isSubscribed = true;
    setHasError(false);

    loadYouTubeIframeApi().then((YT) => {
      if (!isSubscribed || !containerRef.current) return;

      // Dọn player cũ nếu có
      if (playerRef.current && typeof playerRef.current.destroy === 'function') {
        try {
          playerRef.current.destroy();
        } catch {
          // ignore
        }
        playerRef.current = null;
      }

      try {
        const player = new YT.Player(containerRef.current, {
          videoId,
          playerVars: {
            autoplay: 1,
            controls: 1,
            rel: 0,
            modestbranding: 1,
            enablejsapi: 1,
            playsinline: 1,
          },
          events: {
            onReady: (event) => {
              if (!isSubscribed) return;
              playerRef.current = event.target;
              setIsPlayerReady(true);
              const startSec = getStartSec();
              event.target.seekTo(startSec, true);
              // Thử tự động phát, nếu trình duyệt chặn autoplay thì controls=1 cho phép user tự ấn
              try {
                event.target.playVideo();
              } catch {
                // ignore
              }
            },
            onError: () => {
              if (!isSubscribed) return;
              setHasError(true);
            },
          },
        });
        playerRef.current = player;
      } catch {
        if (isSubscribed) setHasError(true);
      }
    });

    return () => {
      isSubscribed = false;
      setIsPlayerReady(false);
      if (playerRef.current && typeof playerRef.current.destroy === 'function') {
        try {
          playerRef.current.destroy();
        } catch {
          // ignore
        }
        playerRef.current = null;
      }
    };
  }, [videoId]);

  // 2. Tự động tua và phát lại khi startMs/endMs đổi (chuyển segment)
  useEffect(() => {
    if (!isPlayerReady || !playerRef.current) return;
    const startSec = Math.floor((startMs || 0) / 1000);
    if (typeof playerRef.current.seekTo === 'function') {
      playerRef.current.seekTo(startSec, true);
    }
    if (typeof playerRef.current.playVideo === 'function') {
      try {
        playerRef.current.playVideo();
      } catch {
        // ignore
      }
    }
  }, [startMs, isPlayerReady]);

  // 3. Vòng lặp kiểm tra thời gian video: đến endMs thì dừng và quay về đầu segment
  useEffect(() => {
    if (!isPlayerReady || !playerRef.current) return;

    const intervalId = setInterval(() => {
      if (playerRef.current && typeof playerRef.current.getCurrentTime === 'function') {
        try {
          const currentTime = playerRef.current.getCurrentTime();
          const endSec = getEndSec();
          const startSec = getStartSec();

          if (currentTime >= endSec) {
            if (typeof playerRef.current.pauseVideo === 'function') {
              playerRef.current.pauseVideo();
            }
            if (typeof playerRef.current.seekTo === 'function') {
              playerRef.current.seekTo(startSec, true);
            }
          }
        } catch {
          // ignore
        }
      }
    }, 200);

    return () => clearInterval(intervalId);
  }, [isPlayerReady]);

  const youtubeExternalUrl =
    sourceUrl || (videoId ? `https://www.youtube.com/watch?v=${videoId}` : '');

  return (
    <aside className="lesson-yt-player-card glass-card">
      <div className="lesson-yt-player__media-container">
        {videoId && !hasError ? (
          <div ref={containerRef} className="lesson-yt-player__iframe" />
        ) : (
          <div className="lesson-yt-player__fallback">
            {thumbnailUrl && (
              <img src={thumbnailUrl} alt={title} className="lesson-yt-player__thumbnail" />
            )}
            <div className="lesson-yt-player__fallback-overlay">
              <span className="material-symbols-outlined lesson-yt-player__fallback-icon">
                {hasError ? 'error' : 'smart_display'}
              </span>
              <p className="lesson-yt-player__fallback-text">
                {hasError ? t('studyPlayer.videoLoadError') : t('studyPlayer.noVideo')}
              </p>
              {youtubeExternalUrl && (
                <a
                  href={youtubeExternalUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn-secondary lesson-yt-player__external-link"
                >
                  <span className="material-symbols-outlined">open_in_new</span>
                  {t('studyPlayer.openOnYoutube')}
                </a>
              )}
            </div>
          </div>
        )}
      </div>

      <div className="lesson-yt-player__info">
        <h2 className="lesson-yt-player__title">{title}</h2>
        {description && <p className="lesson-yt-player__desc">{description}</p>}
      </div>
    </aside>
  );
}

export default LessonYoutubePlayer;
