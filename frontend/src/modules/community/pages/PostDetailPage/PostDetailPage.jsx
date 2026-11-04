import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import BackButton from '../../../../components/BackButton/BackButton';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import Toast from '../../../../components/Toast/Toast';
import PostCard from '../../components/PostCard/PostCard';
import PostFormModal from '../../components/PostFormModal/PostFormModal';
import { getPostByIdApi, likePostApi, unlikePostApi } from '../../communityApi';
import useCommunityPostDelete from '../../hooks/useCommunityPostDelete';
import useCommunityPostEditor from '../../hooks/useCommunityPostEditor';
import { mapPost } from '../../utils/mapPost';
import './PostDetailPage.css';

function PostDetailPage({ postId, canGoBack, onNavigate }) {
  const { t } = useTranslation(['community', 'api', 'common']);
  const [post, setPost] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);
  const [toast, setToast] = useState({ message: '', variant: 'error' });

  const fetchPost = useCallback(async () => {
    try {
      const response = await getPostByIdApi(postId);
      if (response?.success === false) {
        return { post: null, error: response.message || t('common:loadError') };
      }

      const postData = response?.data ?? response;
      if (!postData?.id) {
        return { post: null, error: t('postDetail.notFound') };
      }

      return { post: postData, error: null };
    } catch (requestError) {
      return {
        post: null,
        error:
          requestError?.response?.status === 404 ? t('postDetail.notFound') : t('common:loadError'),
      };
    }
  }, [postId, t]);

  useEffect(() => {
    let isSubscribed = true;
    window.scrollTo({ top: 0, behavior: 'instant' });
    fetchPost().then(({ post: loadedPost, error: loadError }) => {
      if (!isSubscribed) return;
      setPost(loadedPost);
      setError(loadError);
      setIsLoading(false);
    });

    return () => {
      isSubscribed = false;
    };
  }, [fetchPost, retryCount]);

  const handleRetry = () => {
    setError(null);
    setIsLoading(true);
    setRetryCount((previousCount) => previousCount + 1);
  };

  const handleBack = () => {
    if (onNavigate) onNavigate(canGoBack ? -1 : '/community');
  };

  const handlePostUpdated = useCallback(
    (updatedPost) => {
      setPost(updatedPost);
      setToast({ message: t('postForm.updateSuccess'), variant: 'success' });
    },
    [t]
  );
  const handlePostEditError = useCallback((message) => setToast({ message, variant: 'error' }), []);
  const handleCommentCreated = useCallback(() => {
    setPost((previousPost) => ({
      ...previousPost,
      commentCount: (previousPost.commentCount || 0) + 1,
    }));
    setToast({ message: t('community:commentSuccess'), variant: 'success' });
  }, [t]);
  const handleCommentDeleted = useCallback(
    (postId, commentId, parentCommentId, removedCommentCount = 1) => {
      setPost((previousPost) => ({
        ...previousPost,
        commentCount: Math.max(0, (previousPost.commentCount || 0) - removedCommentCount),
      }));
    },
    []
  );
  const handleCommentDeleteSuccess = useCallback(() => {
    setToast({ message: t('community:comments.deleteSuccess'), variant: 'success' });
  }, [t]);
  const handleCommentError = useCallback(
    (message) => setToast({ message: message || t('api:common.UNKNOWN_ERROR'), variant: 'error' }),
    [t]
  );
  const {
    editingPost,
    handleClosePostForm,
    handleOpenEditPostForm,
    handleSubmitPostForm,
    isModalOpen,
  } = useCommunityPostEditor({
    onEditError: handlePostEditError,
    onPostUpdated: handlePostUpdated,
  });

  const handleLike = async (nextLiked) => {
    try {
      const response = nextLiked ? await likePostApi(postId) : await unlikePostApi(postId);
      if (response?.success === false) {
        setToast({ message: response.message || t('api:common.UNKNOWN_ERROR'), variant: 'error' });
        return false;
      }

      setPost((previousPost) => ({
        ...previousPost,
        likedByMe: nextLiked,
        likeCount: previousPost.likeCount + (nextLiked ? 1 : -1),
      }));
      return true;
    } catch {
      setToast({ message: t('api:common.UNKNOWN_ERROR'), variant: 'error' });
      return false;
    }
  };

  const handlePostDeleted = useCallback(() => {
    if (onNavigate) onNavigate('/community');
  }, [onNavigate]);
  const handlePostDeleteError = useCallback(
    (message) => setToast({ message, variant: 'error' }),
    []
  );
  const { handleDeletePost } = useCommunityPostDelete({
    onDeleted: handlePostDeleted,
    onError: handlePostDeleteError,
  });

  const mappedPost = post ? mapPost(post) : null;

  return (
    <main className="post-detail-page">
      <BackButton
        label={t('common:back')}
        onClick={handleBack}
        className="post-detail-page__back"
      />

      <div className="post-detail-page__content">
        {isLoading ? (
          <div className="glass-card post-detail-page__loading">
            <div className="app-loading-spinner" />
          </div>
        ) : error ? (
          <EmptyState
            icon="error"
            description={error}
            isError
            actionLabel={t('common:retry')}
            onAction={handleRetry}
            className="post-detail-page__empty-state"
          />
        ) : (
          <PostCard
            postId={post.id}
            author={mappedPost.author}
            group={mappedPost.groupName}
            createdAt={post.createdAt}
            status={post.status}
            title={post.title}
            content={post.content}
            media={mappedPost.medias}
            likeCount={post.likeCount}
            commentCount={post.commentCount}
            likedByMe={post.likedByMe}
            onLike={handleLike}
            onCommentCreated={handleCommentCreated}
            onCommentError={handleCommentError}
            onCommentDeleted={handleCommentDeleted}
            onCommentDeleteSuccess={handleCommentDeleteSuccess}
            onEdit={handleOpenEditPostForm}
            onDelete={handleDeletePost}
            onNavigate={onNavigate}
          />
        )}
      </div>

      <Toast
        message={toast.message}
        variant={toast.variant}
        onDismiss={() => setToast((previousToast) => ({ ...previousToast, message: '' }))}
      />
      <PostFormModal
        isOpen={isModalOpen}
        onClose={handleClosePostForm}
        onSubmit={handleSubmitPostForm}
        groups={[]}
        initialPost={editingPost}
      />
    </main>
  );
}

export default PostDetailPage;
