import { useTranslation } from 'react-i18next';
import './PostCardActions.css';

function PostCardActions({
  isLiked,
  onLike,
  isCommentOpen,
  onCommentToggle,
  isPostUnpublished = false,
  isLikePending = false,
}) {
  const { t } = useTranslation();
  const disabledTitle = isPostUnpublished
    ? t('community:postCard.cannotInteractUnpublished')
    : undefined;

  return (
    <div className="post-card-actions">
      <button
        type="button"
        disabled={isPostUnpublished || isLikePending}
        onClick={onLike}
        title={disabledTitle}
        className={`post-card-actions__btn ${isLiked ? 'post-card-actions__btn--liked' : ''} ${
          isPostUnpublished || isLikePending ? 'post-card-actions__btn--disabled' : ''
        }`}
      >
        <span
          className={`material-symbols-outlined post-card-actions__icon ${isLiked ? 'filled' : ''}`}
        >
          thumb_up
        </span>
        <span>{isLiked ? t('community:postCard.liked') : t('community:postCard.like')}</span>
      </button>

      <button
        type="button"
        disabled={isPostUnpublished}
        onClick={onCommentToggle}
        title={disabledTitle}
        className={`post-card-actions__btn ${
          isPostUnpublished ? 'post-card-actions__btn--disabled' : ''
        }`}
      >
        <span className="material-symbols-outlined post-card-actions__icon">chat_bubble</span>
        <span>{t('community:postCard.comment')}</span>
      </button>
    </div>
  );
}

export default PostCardActions;
