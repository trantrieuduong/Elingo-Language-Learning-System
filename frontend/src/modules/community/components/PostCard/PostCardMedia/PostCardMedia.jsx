import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import MediaLightbox from '../../MediaLightbox/MediaLightbox';
import './PostCardMedia.css';

const MAX_VISIBLE_ITEMS_DESKTOP = 3;
const MAX_VISIBLE_ITEMS_MOBILE = 4;

/**
 * PostCardMedia — Hiển thị danh sách tập tin đa phương tiện (ảnh/video).
 * Desktop: 1 hàng 3 ô (nếu có 3+ item), Mobile: lưới 2 hàng 2x2 tối đa 4 ô.
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

  const remainingDesktop = media.length - MAX_VISIBLE_ITEMS_DESKTOP;
  const remainingMobile = media.length - MAX_VISIBLE_ITEMS_MOBILE;

  const handleItemClick = (index) => setSelectedMediaIndex(index);

  return (
    <div className={`post-card-media ${gridClass}`}>
      {media.slice(0, MAX_VISIBLE_ITEMS_MOBILE).map((item, index) => {
        const isThirdItem = index === 2;
        const isFourthItem = index === 3;

        return (
          <div
            key={item.id || index}
            onClick={() => handleItemClick(index)}
            className={`post-card-media__item ${isFourthItem ? 'post-card-media__item--mobile-only' : ''}`}
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

            {isThirdItem && remainingDesktop > 0 && (
              <span className="post-card-media__overlay post-card-media__overlay--desktop-only">
                +{remainingDesktop}
              </span>
            )}

            {isFourthItem && remainingMobile > 0 && (
              <span className="post-card-media__overlay post-card-media__overlay--mobile-only">
                +{remainingMobile}
              </span>
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
