import { useState } from 'react';
import { useAuth } from '../../../../context/AuthContext';
import PostCardHeader from './PostCardHeader/PostCardHeader';
import PostCardContent from './PostCardContent/PostCardContent';
import PostCardMedia from './PostCardMedia/PostCardMedia';
import PostCardStats from './PostCardStats/PostCardStats';
import PostCardActions from './PostCardActions/PostCardActions';
import PostCardCommentInput from './PostCardCommentInput/PostCardCommentInput';
import PostOptionsModal from '../PostOptionsModal/PostOptionsModal';
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
  onComment,
  onEdit,
  onOpenPost,
  onNavigate,
}) {
  const { user } = useAuth();
  const [isLiked, setIsLiked] = useState(likedByMe);
  const [isCommentOpen, setIsCommentOpen] = useState(false);
  const [isLikePending, setIsLikePending] = useState(false);
  const [isOptionsModalOpen, setIsOptionsModalOpen] = useState(false);

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
    setIsCommentOpen((prev) => !prev);
    if (onComment) onComment();
  };

  const handleCommentSubmit = (newComment) => {
    if (onComment) onComment(newComment);
    setIsCommentOpen(false);
  };

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
          <PostCardCommentInput
            avatarUrl={user?.avatarUrl}
            userName={user?.fullName}
            onSubmit={handleCommentSubmit}
          />
        )}
      </div>
      {isOptionsModalOpen && (
        <PostOptionsModal
          postId={postId}
          isOpen={isOptionsModalOpen}
          onClose={() => setIsOptionsModalOpen(false)}
          onEdit={onEdit}
          canEdit={canEdit}
        />
      )}
    </article>
  );
}

export default PostCard;
