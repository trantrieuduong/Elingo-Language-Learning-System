import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../../context/AuthContext';
import PostCardHeader from './PostCardHeader/PostCardHeader';
import PostCardContent from './PostCardContent/PostCardContent';
import PostCardMedia from './PostCardMedia/PostCardMedia';
import PostCardStats from './PostCardStats/PostCardStats';
import PostCardActions from './PostCardActions/PostCardActions';
import PostCardCommentInput from './PostCardCommentInput/PostCardCommentInput';
import PostComments from './PostComments/PostComments';
import useCommunityComments from '../../hooks/useCommunityComments';
import OptionsModal from '../../../../components/OptionsModal/OptionsModal';
import ConfirmModal from '../../../../components/ConfirmModal/ConfirmModal';
import Toast from '../../../../components/Toast/Toast';
import './PostCard.css';

/**
 * @param {Object} author - Thông tin tác giả ({ id, fullName, username, avatarUrl })
 * @param {Array} media - Danh sách media (ảnh, video)
 */
function PostCard({
  postId,
  author,
  group,
  createdAt,
  status,
  title,
  content,
  media = [],
  likeCount = 0,
  commentCount = 0,
  likedByMe = false,
  onLike,
  onCommentCreated,
  onCommentError,
  onCommentDeleted,
  onCommentDeleteSuccess,
  onEdit,
  onDelete,
  onOpenPost,
  onNavigate,
}) {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [isLiked, setIsLiked] = useState(likedByMe);
  const [isCommentOpen, setIsCommentOpen] = useState(false);
  const [isLikePending, setIsLikePending] = useState(false);
  const [isOptionsModalOpen, setIsOptionsModalOpen] = useState(false);
  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [optionsToast, setOptionsToast] = useState({ message: '', variant: 'success' });
  const handleCommentUpdated = useCallback(
    () =>
      setOptionsToast({
        message: t('community:comments.updateSuccess'),
        variant: 'success',
      }),
    [t]
  );
  const {
    comments,
    hasMore: hasMoreComments,
    isLoadingFirstPage: isLoadingFirstPageComments,
    isLoadingMore: isLoadingMoreComments,
    loadError: commentsError,
    loadComments,
    repliesByCommentId,
    handleCreateComment,
    handleCreateReply,
    handleUpdateComment,
    handleDeleteComment,
    handleLikeComment,
    handleLoadReplies,
    handleLoadComments,
    handleLoadMoreComments,
  } = useCommunityComments({
    postId,
    onCommentCreated,
    onCommentError,
    onCommentLikeError: onCommentError,
    onCommentUpdated: handleCommentUpdated,
    onCommentDeleted,
    onCommentDeleteSuccess,
  });

  const isPublished = status === 'PUBLISHED';
  const canEdit = Boolean(user?.id && author?.id) && String(user.id) === String(author.id);

  const displayedLikeCount = likeCount + (isLiked ? 1 : 0) - (likedByMe ? 1 : 0);

  const handleLike = async () => {
    if (!isPublished || isLikePending || !onLike) return;

    const previousLiked = isLiked;
    const nextLiked = !previousLiked;
    setIsLiked(nextLiked);

    setIsLikePending(true);
    try {
      const succeeded = await onLike(nextLiked);
      if (succeeded === false) setIsLiked(previousLiked);
    } catch {
      setIsLiked(previousLiked);
    } finally {
      setIsLikePending(false);
    }
  };

  const handleCommentToggle = () => {
    if (!isPublished) return;
    if (isCommentOpen) {
      setIsCommentOpen(false);
      return;
    }

    setIsCommentOpen(true);
    handleLoadComments();
  };

  const handleCommentSubmit = async (newComment, imageFile) => {
    return handleCreateComment(newComment, imageFile);
  };

  const handleConfirmDelete = async () => {
    if (!onDelete) return;
    setIsDeleting(true);
    try {
      await onDelete(postId);
      setIsDeleteConfirmOpen(false);
    } finally {
      setIsDeleting(false);
    }
  };

  const handleCopyPostLink = async () => {
    try {
      const postUrl = new URL(
        `/community/posts/${encodeURIComponent(postId)}`,
        window.location.origin
      ).toString();
      await navigator.clipboard.writeText(postUrl);
      setOptionsToast({ message: t('community:postCard.postLinkCopied'), variant: 'success' });
    } catch {
      setOptionsToast({ message: t('community:postCard.copyPostLinkFailed'), variant: 'error' });
    }
  };

  const postOptions = [
    ...(canEdit && onEdit
      ? [
          {
            id: 'edit-post',
            label: t('community:postCard.editPost'),
            icon: 'edit',
            onSelect: () => onEdit(postId),
          },
        ]
      : []),
    ...(canEdit && onDelete
      ? [
          {
            id: 'delete-post',
            label: t('community:postCard.deletePost'),
            icon: 'delete',
            variant: 'danger',
            onSelect: () => setIsDeleteConfirmOpen(true),
          },
        ]
      : []),
    {
      id: 'copy-post-link',
      label: t('community:postCard.copyPostLink'),
      icon: 'content_copy',
      onSelect: handleCopyPostLink,
    },
  ];

  return (
    <article className="post-card glass-card">
      <div className="post-card__body">
        <PostCardHeader
          author={author}
          group={group}
          createdAt={createdAt}
          status={status}
          onAuthorClick={() => {
            const authorId = author?.id;
            if (authorId && onNavigate) {
              onNavigate(`/profile/${authorId}`);
            }
          }}
          onGroupClick={() => {
            if (onNavigate) {
              onNavigate('/community/groups');
            }
          }}
          onOptionsClick={() => setIsOptionsModalOpen(true)}
        />

        <PostCardContent title={title} content={content} onOpenPost={onOpenPost} />
      </div>

      <PostCardMedia media={media} />

      <div className="post-card__footer">
        <PostCardStats
          likeCount={displayedLikeCount}
          commentCount={commentCount}
          onCommentClick={handleCommentToggle}
        />

        <PostCardActions
          isLiked={isLiked}
          onLike={handleLike}
          isCommentOpen={isCommentOpen}
          onCommentToggle={handleCommentToggle}
          isPostUnpublished={!isPublished}
          isLikePending={isLikePending}
        />

        {isCommentOpen && (
          <div className="post-card__comments">
            <PostComments
              comments={comments}
              hasMore={hasMoreComments}
              currentUserId={user?.id}
              onCommentError={onCommentError}
              isLoadingFirstPage={isLoadingFirstPageComments}
              isLoadingMore={isLoadingMoreComments}
              error={commentsError}
              onLoadMore={handleLoadMoreComments}
              onRetry={comments.length > 0 ? handleLoadMoreComments : () => loadComments()}
              onUpdateComment={handleUpdateComment}
              onDeleteComment={handleDeleteComment}
              onLikeComment={handleLikeComment}
              onLoadReplies={handleLoadReplies}
              onCreateReply={handleCreateReply}
              repliesByCommentId={repliesByCommentId}
              avatarUrl={user?.avatarUrl}
              userName={user?.fullName}
            />
            <PostCardCommentInput
              avatarUrl={user?.avatarUrl}
              userName={user?.fullName}
              onCommentError={onCommentError}
              onSubmit={handleCommentSubmit}
            />
          </div>
        )}
      </div>
      {isOptionsModalOpen && (
        <OptionsModal
          isOpen={isOptionsModalOpen}
          title={t('community:postCard.postOptionsTitle')}
          options={postOptions}
          onClose={() => setIsOptionsModalOpen(false)}
        />
      )}
      <ConfirmModal
        isOpen={isDeleteConfirmOpen}
        title={t('community:postCard.deleteConfirmTitle')}
        message={t('community:postCard.deleteConfirmMessage')}
        confirmText={t('common:delete')}
        cancelText={t('common:cancel')}
        onConfirm={handleConfirmDelete}
        onCancel={() => setIsDeleteConfirmOpen(false)}
        isDanger
        isLoading={isDeleting}
      />
      <Toast
        message={optionsToast.message}
        variant={optionsToast.variant}
        onDismiss={() => setOptionsToast((previousToast) => ({ ...previousToast, message: '' }))}
      />
    </article>
  );
}

export default PostCard;
