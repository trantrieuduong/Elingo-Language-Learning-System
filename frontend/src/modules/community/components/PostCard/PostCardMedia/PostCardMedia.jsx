import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import MediaLightbox from '../../MediaLightbox/MediaLightbox';
import './PostCardMedia.css';

const MAX_VISIBLE_ITEMS = 4;

/**
 * PostCardMedia — Lưới hiển thị danh sách tập tin đa phương tiện (ảnh/video) với tối đa 4 ô.
 *
 * @param {Array} media - Danh sách media [{ id, type, url, poster, alt }]
 */
function PostCardMedia({ media = [] }) {
  const [selectedMediaIndex, setSelectedMediaIndex] = useState(null);
  const { t } = useTranslation();

  const handleCloseLightbox = useCallback(() => {
    setSelectedMediaIndex(null);
  }, []);

  if (!media.length) return null;

  const gridClass =
    media.length === 1
      ? 'post-card-media--single'
      : media.length === 2
        ? 'post-card-media--double'
        : media.length === 3
          ? 'post-card-media--triple'
          : 'post-card-media--quad';

  const remainingCount = media.length - MAX_VISIBLE_ITEMS;

  const handleItemClick = (index) => setSelectedMediaIndex(index);

  return (
    <div className={`post-card-media ${gridClass}`}>
      {media.slice(0, MAX_VISIBLE_ITEMS).map((item, index) => {
        const isRemainingOverlay = index === MAX_VISIBLE_ITEMS - 1 && remainingCount > 0;

        return (
          <div
            key={item.id || index}
            onClick={() => handleItemClick(index)}
            className="post-card-media__item"
          >
            {item.type === 'video' ? (
              <div className="post-card-media__video-container">
                <video
                  src={item.url}
                  playsInline
                  preload="metadata"
                  className="post-card-media__asset"
                />
                <span className="post-card-media__type-badge">
                  <span className="material-symbols-outlined">videocam</span>
                  {t('community:postCard.videoBadge')}
                </span>
                <span className="post-card-media__play-btn">
                  <span className="material-symbols-outlined">play_arrow</span>
                </span>
              </div>
            ) : (
              <img src={item.url} alt={item.alt || ''} className="post-card-media__asset" />
            )}

            {isRemainingOverlay && (
              <span className="post-card-media__overlay">+{remainingCount}</span>
            )}
          </div>
        );
      })}
      {selectedMediaIndex !== null && (
        <MediaLightbox
          media={media}
          initialIndex={selectedMediaIndex}
          onClose={handleCloseLightbox}
        />
      )}
    </div>
  );
}

export default PostCardMedia;
