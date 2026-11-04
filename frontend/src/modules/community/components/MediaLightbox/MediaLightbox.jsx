import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import './MediaLightbox.css';

/**
 * Trình xem ảnh hoặc video phóng to
 */
function MediaLightbox({ media, initialIndex, onClose }) {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const currentMedia = media[currentIndex];
  const hasPrevious = currentIndex > 0;
  const hasNext = currentIndex < media.length - 1;

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = previousOverflow; // Khôi phục trạng thái cuộn ban đầu
    };
  }, []);

  return createPortal(
    <div className="community-media-lightbox">
      <button type="button" className="btn-icon community-media-lightbox__close" onClick={onClose}>
        <span className="material-symbols-outlined">close</span>
      </button>
      {media.length > 1 && (
        <button
          type="button"
          className="btn-icon community-media-lightbox__navigation community-media-lightbox__navigation--previous"
          onClick={() => setCurrentIndex((index) => index - 1)}
          disabled={!hasPrevious}
        >
          <span className="material-symbols-outlined">chevron_left</span>
        </button>
      )}
      {currentMedia.type === 'video' ? (
        <video
          key={currentIndex}
          className="community-media-lightbox__asset"
          src={currentMedia.url}
          controls
          autoPlay
          playsInline
          preload="auto"
        />
      ) : (
        <img
          key={currentIndex}
          className="community-media-lightbox__asset"
          src={currentMedia.url}
          alt={currentMedia.alt || ''}
        />
      )}
      {media.length > 1 && (
        <button
          type="button"
          className="btn-icon community-media-lightbox__navigation community-media-lightbox__navigation--next"
          onClick={() => setCurrentIndex((index) => index + 1)}
          disabled={!hasNext}
        >
          <span className="material-symbols-outlined">chevron_right</span>
        </button>
      )}
    </div>,
    document.body
  );
}

export default MediaLightbox;
