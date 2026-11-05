import { useTranslation } from 'react-i18next';
import './PostCardStats.css';

function PostCardStats({ likeCount = 0, commentCount = 0, onCommentClick }) {
  const { t } = useTranslation();

  return (
    <div className="post-card-stats">
      <div className="post-card-stats__item post-card-stats__likes">
        <span className="post-card-stats__badge-group">
          <span className="post-card-stats__mini-badge post-card-stats__mini-badge--like">
            <span className="material-symbols-outlined">thumb_up</span>
          </span>
        </span>
        <span>
          {t('community:postCard.likeCount', {
            count: likeCount.toLocaleString(),
          })}
        </span>
      </div>

      <button type="button" onClick={onCommentClick} className="post-card-stats__btn">
        <span>
          {t('community:postCard.commentCount', {
            count: commentCount.toLocaleString(),
          })}
        </span>
      </button>
    </div>
  );
}

export default PostCardStats;
