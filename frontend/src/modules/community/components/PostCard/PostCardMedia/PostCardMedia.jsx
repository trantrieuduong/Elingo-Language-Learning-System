import { useRef, useState } from 'react'
import './PostCardMedia.css'

const MAX_VISIBLE_ITEMS = 4

/**
 * PostCardMedia — Lưới hiển thị danh sách tập tin đa phương tiện (ảnh/video) với tối đa 4 ô.
 *
 * @param {Array} media - Danh sách media [{ id, type, url, poster, alt }]
 * @param {Function} onOpenMedia - Callback khi nhấn vào một media
 */
function PostCardMedia({ media = [], onOpenMedia }) {
  const [playingIndex, setPlayingIndex] = useState(null)
  const videoRefs = useRef({})

  if (!media.length) return null

  const gridClass =
    media.length === 1
      ? 'post-card-media--single'
      : media.length === 2
        ? 'post-card-media--double'
        : media.length === 3
          ? 'post-card-media--triple'
          : 'post-card-media--quad'

  const remainingCount = media.length - MAX_VISIBLE_ITEMS

  const handlePlayVideo = (e, index) => {
    e.stopPropagation()
    const videoEl = videoRefs.current[index]
    if (!videoEl) return

    if (playingIndex === index) {
      if (videoEl.paused) {
        videoEl.play()
      } else {
        videoEl.pause()
      }
    } else {
      // Pause any previously playing video
      if (playingIndex !== null && videoRefs.current[playingIndex]) {
        videoRefs.current[playingIndex].pause()
      }
      setPlayingIndex(index)
      videoEl.play().catch(() => {})
    }
  }

  const handleItemClick = (e, item, index) => {
    // Nếu đang phát video và click vào container, không trigger onOpenMedia
    if (item.type === 'video' && playingIndex === index) {
      return
    }
    if (onOpenMedia) {
      onOpenMedia(item, index)
    }
  }

  return (
    <div className={`post-card-media ${gridClass}`}>
      {media.slice(0, MAX_VISIBLE_ITEMS).map((item, index) => {
        const isRemainingOverlay = index === MAX_VISIBLE_ITEMS - 1 && remainingCount > 0
        const isPlaying = playingIndex === index

        return (
          <div
            key={item.id || index}
            role="button"
            tabIndex={0}
            onClick={(e) => handleItemClick(e, item, index)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                handleItemClick(e, item, index)
              }
            }}
            className="post-card-media__item"
          >
            {item.type === 'video' ? (
              <div className={`post-card-media__video-container ${isPlaying ? 'post-card-media__video-container--playing' : ''}`}>
                <video
                  ref={(el) => {
                    videoRefs.current[index] = el
                  }}
                  src={item.url}
                  poster={item.poster}
                  controls={isPlaying}
                  playsInline
                  preload="metadata"
                  onEnded={() => setPlayingIndex(null)}
                  className="post-card-media__asset"
                />
                {!isPlaying && (
                  <>
                    <span className="post-card-media__type-badge">
                      <span className="material-symbols-outlined">videocam</span>
                      Video
                    </span>
                    <button
                      type="button"
                      aria-label="Phát video"
                      onClick={(e) => handlePlayVideo(e, index)}
                      className="post-card-media__play-btn"
                    >
                      <span className="material-symbols-outlined">play_arrow</span>
                    </button>
                  </>
                )}
              </div>
            ) : (
              <img
                src={item.url}
                alt={item.alt || ''}
                className="post-card-media__asset"
              />
            )}

            {isRemainingOverlay && !isPlaying && (
              <span className="post-card-media__overlay">
                +{remainingCount}
              </span>
            )}
          </div>
        )
      })}
    </div>
  )
}

export default PostCardMedia
