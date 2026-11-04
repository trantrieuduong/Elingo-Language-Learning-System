import { useTranslation } from 'react-i18next';
import EmptyState from '../../../../../components/EmptyState/EmptyState';
import useScrollEnd from '../../../hooks/useScrollEnd';
import PostCardCommentItem from '../PostCardCommentItem/PostCardCommentItem';
import './PostComments.css';

function PostComments({
  comments = [],
  hasMore = false,
  currentUserId,
  onCommentError,
  isLoadingFirstPage = false,
  isLoadingMore = false,
  error = '',
  onLoadMore,
  onRetry,
  onUpdateComment,
  onDeleteComment,
  onLikeComment,
  onLoadReplies,
  onCreateReply,
  repliesByCommentId = {},
  avatarUrl,
  userName,
}) {
  const { t } = useTranslation();
  const {
    scrollContainerRef: commentsListRef,
    isAtEnd: isAtCommentsEnd,
    handleScroll: handleCommentsScroll,
  } = useScrollEnd();

  return (
    <section className="post-comments glass-well">
      {isLoadingFirstPage && comments.length === 0 ? (
        <div className="post-comments__state">
          <span className="app-loading-spinner" />
          <span>{t('community:comments.loading')}</span>
        </div>
      ) : error && comments.length === 0 ? (
        <EmptyState
          variant="compact"
          icon="error"
          description={error}
          isError
          actionLabel={t('community:comments.retry')}
          onAction={onRetry}
        />
      ) : comments.length === 0 ? (
        <EmptyState
          variant="compact"
          icon="chat_bubble_outline"
          description={t('community:comments.empty')}
        />
      ) : (
        <>
          <ul ref={commentsListRef} className="post-comments__list" onScroll={handleCommentsScroll}>
            {comments.map((comment) => (
              <li key={comment.id}>
                <PostCardCommentItem
                  comment={comment}
                  currentUserId={currentUserId}
                  onCommentError={onCommentError}
                  onUpdateComment={onUpdateComment}
                  onDeleteComment={onDeleteComment}
                  onLikeComment={onLikeComment}
                  replyState={repliesByCommentId[comment.id]}
                  onLoadReplies={onLoadReplies}
                  onCreateReply={onCreateReply}
                  avatarUrl={avatarUrl}
                  currentUserName={userName}
                />
              </li>
            ))}
          </ul>
          {error && (
            <div className="post-comments__load-error">
              <span>{error}</span>
              <button type="button" className="btn-secondary" onClick={onRetry}>
                {t('community:comments.retry')}
              </button>
            </div>
          )}
          {hasMore && !error && isAtCommentsEnd && (
            <button
              type="button"
              className="btn-secondary post-comments__load-more"
              onClick={onLoadMore}
              disabled={isLoadingMore}
            >
              {isLoadingMore ? (
                <>
                  <span className="app-loading-spinner" />
                  <span>{t('community:comments.loading')}</span>
                </>
              ) : (
                t('community:comments.loadMore')
              )}
            </button>
          )}
        </>
      )}
    </section>
  );
}

export default PostComments;
