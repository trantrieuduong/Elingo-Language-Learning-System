import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import OptionsModal from '../../../../../components/OptionsModal/OptionsModal';
import OptionsMenu from '../../../../../components/OptionsMenu/OptionsMenu';
import ConfirmModal from '../../../../../components/ConfirmModal/ConfirmModal';
import { formatRelativeTime } from '../../../../../utils/utils';
import PostCardCommentInput from '../PostCardCommentInput/PostCardCommentInput';
import PostCardLikeButton from '../PostCardActions/PostCardLikeButton';
import useScrollEnd from '../../../hooks/useScrollEnd';
import './PostCardCommentItem.css';

function PostCardCommentItem({
  comment,
  currentUserId,
  onCommentError,
  onUpdateComment,
  onDeleteComment,
  onLikeComment,
  replyState,
  onLoadReplies,
  onCreateReply,
  avatarUrl,
  currentUserName,
  isReply = false,
}) {
  const { t, i18n } = useTranslation();
  const [isOptionsModalOpen, setIsOptionsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isLiked, setIsLiked] = useState(Boolean(comment.likedByMe));
  const [likeCount, setLikeCount] = useState(comment.likeCount || 0);
  const [isLikePending, setIsLikePending] = useState(false);
  const [isReplyFormOpen, setIsReplyFormOpen] = useState(false);
  const [areRepliesOpen, setAreRepliesOpen] = useState(false);
  const {
    scrollContainerRef: repliesListRef,
    isAtEnd: isAtRepliesEnd,
    handleScroll: handleRepliesScroll,
  } = useScrollEnd(!isReply && areRepliesOpen);
  const authorName = comment.author?.fullName || t('community:postCard.anonymousUser');
  const locale = i18n.resolvedLanguage || 'vi';
  const isOwnComment =
    currentUserId != null &&
    comment.author?.id != null &&
    String(currentUserId) === String(comment.author.id);
  const commentOptions = [
    {
      id: 'edit-comment',
      label: t('community:comments.edit'),
      icon: 'edit',
      disabled: !onUpdateComment,
      onSelect: () => setIsEditing(true),
    },
    {
      id: 'delete-comment',
      label: t('community:comments.delete'),
      icon: 'delete',
      variant: 'danger',
      disabled: !onDeleteComment,
      onSelect: () => setIsDeleteConfirmOpen(true),
    },
  ];

  const handleConfirmDelete = async () => {
    if (!onDeleteComment) return;
    setIsDeleting(true);
    try {
      await onDeleteComment(comment.id);
      setIsDeleteConfirmOpen(false);
    } finally {
      setIsDeleting(false);
    }
  };

  const handleLike = async () => {
    if (!onLikeComment || isLikePending) return;

    const previousLiked = isLiked;
    const previousLikeCount = likeCount;
    const nextLiked = !previousLiked;
    setIsLiked(nextLiked);
    setLikeCount(Math.max(0, previousLikeCount + (nextLiked ? 1 : -1)));
    setIsLikePending(true);

    try {
      const succeeded = await onLikeComment(comment.id, nextLiked);
      if (succeeded === false) {
        setIsLiked(previousLiked);
        setLikeCount(previousLikeCount);
      }
    } catch {
      setIsLiked(previousLiked);
      setLikeCount(previousLikeCount);
    } finally {
      setIsLikePending(false);
    }
  };

  const handleToggleReplies = () => {
    const shouldOpen = !areRepliesOpen;
    setAreRepliesOpen(shouldOpen);
    if (shouldOpen && !replyState?.isLoaded && !replyState?.isLoading) {
      onLoadReplies?.(comment.id);
    }
  };

  const handleCreateReply = async (content, imageFile) => {
    const succeeded = await onCreateReply?.(comment.id, content, imageFile);
    if (succeeded) {
      setAreRepliesOpen(true);
      setIsReplyFormOpen(false);
    }
    return succeeded;
  };

  const handleLoadMoreReplies = () => {
    onLoadReplies?.(comment.id, undefined, true);
  };

  const handleToggleReplyForm = () => {
    const shouldOpen = !isReplyFormOpen;
    setIsReplyFormOpen(shouldOpen);
    if (
      shouldOpen &&
      (comment.replyCount || 0) > 0 &&
      !replyState?.isLoaded &&
      !replyState?.isLoading
    ) {
      onLoadReplies?.(comment.id);
    }
  };

  return (
    <article className={`post-card-comment-item${isReply ? ' post-card-comment-item--reply' : ''}`}>
      <img
        src={
          comment.author?.avatarUrl ||
          `https://ui-avatars.com/api/?name=${encodeURIComponent(authorName)}`
        }
        alt=""
        className="post-card-comment-item__avatar"
      />
      <div className="post-card-comment-item__body">
        <header className="post-card-comment-item__header">
          <span className="post-card-comment-item__author">{authorName}</span>
          <time className="post-card-comment-item__time" dateTime={comment.createdAt}>
            {formatRelativeTime(comment.createdAt, locale)}
          </time>
          {isOwnComment && (
            <div className="post-card-comment-item__options">
              <OptionsMenu onClick={() => setIsOptionsModalOpen(true)} />
              <OptionsModal
                isOpen={isOptionsModalOpen}
                title={t('community:comments.optionsTitle')}
                options={commentOptions}
                onClose={() => setIsOptionsModalOpen(false)}
              />
              <ConfirmModal
                isOpen={isDeleteConfirmOpen}
                title={t('community:comments.deleteConfirmTitle')}
                message={t('community:comments.deleteConfirmMessage')}
                confirmText={t('common:delete')}
                cancelText={t('common:cancel')}
                onConfirm={handleConfirmDelete}
                onCancel={() => setIsDeleteConfirmOpen(false)}
                isDanger
                isLoading={isDeleting}
              />
            </div>
          )}
        </header>
        {isEditing ? (
          <PostCardCommentInput
            mode="edit"
            userName={authorName}
            onCommentError={onCommentError}
            initialContent={comment.content}
            initialImageUrl={comment.imageUrl || ''}
            onCancel={() => setIsEditing(false)}
            onSubmit={(content, imageFile, imageUrl) =>
              onUpdateComment(comment.id, { content, imageFile, imageUrl }).then((succeeded) => {
                if (succeeded) setIsEditing(false);
                return succeeded;
              })
            }
          />
        ) : (
          <p className="post-card-comment-item__content">{comment.content}</p>
        )}
        {!isEditing && comment.imageUrl && (
          <img
            src={comment.imageUrl}
            alt={t('community:comments.attachedImage', { name: authorName })}
            className="post-card-comment-item__image"
          />
        )}
        {!isEditing && (
          <div className="post-card-comment-item__actions">
            <PostCardLikeButton
              isLiked={isLiked}
              onLike={handleLike}
              isPending={isLikePending || !onLikeComment}
              likeCount={likeCount}
            />
            {!isReply && onCreateReply && (
              <>
                <button
                  type="button"
                  className="post-card-comment-item__text-action"
                  onClick={handleToggleReplyForm}
                >
                  <span className="material-symbols-outlined">reply</span>
                  <span>{t('community:comments.reply')}</span>
                </button>
                {(comment.replyCount || 0) > 0 && (
                  <button
                    type="button"
                    className="post-card-comment-item__text-action"
                    onClick={handleToggleReplies}
                    aria-expanded={areRepliesOpen}
                  >
                    {areRepliesOpen
                      ? t('community:comments.hideReplies')
                      : t('community:comments.viewReplies', { count: comment.replyCount })}
                  </button>
                )}
              </>
            )}
          </div>
        )}
        {!isReply && isReplyFormOpen && (
          <PostCardCommentInput
            mode="reply"
            avatarUrl={avatarUrl}
            userName={currentUserName}
            onCommentError={onCommentError}
            onSubmit={handleCreateReply}
            placeholder={t('community:comments.replyPlaceholder', { name: authorName })}
          />
        )}
        {!isReply && areRepliesOpen && (
          <div className="post-card-comment-item__replies">
            {replyState?.isLoading && !replyState.items?.length ? (
              <div className="post-card-comment-item__replies-state">
                <span className="app-loading-spinner" />
                <span>{t('community:comments.loading')}</span>
              </div>
            ) : replyState?.error ? (
              <div className="post-card-comment-item__replies-state post-card-comment-item__replies-state--error">
                <span>{replyState.error}</span>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => onLoadReplies?.(comment.id)}
                  disabled={replyState.isLoading}
                >
                  {t('community:comments.retry')}
                </button>
              </div>
            ) : (
              <>
                <div
                  ref={repliesListRef}
                  className="post-card-comment-item__replies-list"
                  onScroll={handleRepliesScroll}
                >
                  {replyState?.items?.map((reply) => (
                    <PostCardCommentItem
                      key={reply.id}
                      comment={reply}
                      currentUserId={currentUserId}
                      currentUserName={currentUserName}
                      avatarUrl={avatarUrl}
                      onCommentError={onCommentError}
                      onUpdateComment={onUpdateComment}
                      onDeleteComment={onDeleteComment}
                      onLikeComment={onLikeComment}
                      isReply
                    />
                  ))}
                </div>
                {replyState?.isLoading && (
                  <div className="post-card-comment-item__replies-state">
                    <span className="app-loading-spinner" />
                    <span>{t('community:comments.loading')}</span>
                  </div>
                )}
                {replyState?.hasMore && isAtRepliesEnd && (
                  <button
                    type="button"
                    className="btn-secondary post-card-comment-item__load-replies"
                    onClick={handleLoadMoreReplies}
                    disabled={replyState.isLoading}
                  >
                    {t('community:comments.loadMoreReplies')}
                  </button>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </article>
  );
}

export default PostCardCommentItem;
