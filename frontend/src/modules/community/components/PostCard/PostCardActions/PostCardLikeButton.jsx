import { useTranslation } from 'react-i18next';
import './PostCardLikeButton.css';

function PostCardLikeButton({ isLiked, onLike, isPending = false, likeCount, title }) {
  const { t } = useTranslation();

  return (
    <button
      type="button"
      disabled={isPending}
      onClick={onLike}
      title={title}
      className={`post-card-actions__btn ${isLiked ? 'post-card-actions__btn--liked' : ''} ${
        isPending ? 'post-card-actions__btn--disabled' : ''
      }`}
    >
      <span
        className={`material-symbols-outlined post-card-actions__icon ${isLiked ? 'filled' : ''}`}
      >
        thumb_up
      </span>
      <span>{isLiked ? t('community:postCard.liked') : t('community:postCard.like')}</span>
      {likeCount != null && <span className="post-card-like-button__count">{likeCount}</span>}
    </button>
  );
}

export default PostCardLikeButton;
