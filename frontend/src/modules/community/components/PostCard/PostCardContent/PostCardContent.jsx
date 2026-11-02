import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import './PostCardContent.css';

const VISIBLE_CONTENT_LENGTH = 260;

function PostCardContent({ title, content = '', onOpenPost }) {
  const { t } = useTranslation();
  const [isExpanded, setIsExpanded] = useState(false);
  const canExpand = content.length > VISIBLE_CONTENT_LENGTH;

  const displayedContent = useMemo(() => {
    if (!canExpand || isExpanded) return content;
    return `${content.slice(0, VISIBLE_CONTENT_LENGTH).trim()}…`;
  }, [canExpand, content, isExpanded]);

  return (
    <div className="post-card-content">
      {title && (
        <button type="button" onClick={onOpenPost} className="post-card-content__title-btn">
          <h2 className="post-card-content__title">{title}</h2>
        </button>
      )}

      <p className="post-card-content__body">{displayedContent}</p>

      {canExpand && !isExpanded && (
        <button
          type="button"
          onClick={() => setIsExpanded(true)}
          className="post-card-content__toggle-btn"
        >
          {t('community:postCard.expand')}
        </button>
      )}
    </div>
  );
}

export default PostCardContent;
