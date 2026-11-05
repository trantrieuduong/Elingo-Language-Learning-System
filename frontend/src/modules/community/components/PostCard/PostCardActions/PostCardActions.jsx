import { useTranslation } from 'react-i18next';
import PostCardLikeButton from './PostCardLikeButton';
import './PostCardActions.css';

function PostCardActions({
  onLike,
  isCommentOpen,
  onCommentToggle,
  isPostUnpublished = false,
  isLikePending = false,
  isLiked,
}) {
  const { t } = useTranslation();
  const disabledTitle = isPostUnpublished
    ? t('community:postCard.cannotInteractUnpublished')
    : undefined;

  return (
    <div className="post-card-actions">
      <PostCardLikeButton
        isLiked={isLiked}
        onLike={onLike}
        isPending={isPostUnpublished || isLikePending}
        title={disabledTitle}
      />

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
