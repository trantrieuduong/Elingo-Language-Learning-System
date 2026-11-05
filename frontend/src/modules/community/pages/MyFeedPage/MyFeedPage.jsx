import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../../context/AuthContext';
import PostCard from '../../components/PostCard/PostCard';
import PostFormModal from '../../components/PostFormModal/PostFormModal';
import FilterSidebar from '../../components/FilterSidebar/FilterSidebar';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import Toast from '../../../../components/Toast/Toast';
import { getPostGroupsApi, getUserPostsApi } from '../../communityApi';
import useCommunityPostActions from '../../hooks/useCommunityPostActions';
import useCommunityPostDelete from '../../hooks/useCommunityPostDelete';
import useCommunityPostEditor from '../../hooks/useCommunityPostEditor';
import useInfiniteScroll from '../../hooks/useInfiniteScroll';
import { mapPost } from '../../utils/mapPost';
import './MyFeedPage.css';

function MyFeedPage({ onNavigate }) {
  const { t } = useTranslation();
  const { user } = useAuth();
  const userId = user?.id;

  const [posts, setPosts] = useState([]);
  const [groups, setGroups] = useState([]);
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [cursor, setCursor] = useState(null);
  const [hasMore, setHasMore] = useState(false);
  const [isLoadingFirstPage, setIsLoadingFirstPage] = useState(false);

  const [error, setError] = useState(null);
  const [toast, setToast] = useState({ message: '', variant: 'success' });

  const requestGenerationRef = useRef(0);

  useEffect(() => {
    const fetchGroups = async () => {
      try {
        const data = await getPostGroupsApi();
        setGroups(data?.data || []);
      } catch {
        // groups stays empty
      }
    };
    fetchGroups();
  }, []);

  const fetchUserPosts = useCallback(
    async (nextCursor) => {
      if (!userId) return { items: [], hasMore: false, nextCursor: null };
      const response = await getUserPostsApi(userId, {
        cursor: nextCursor,
        limit: 20,
      });
      return response?.data || {};
    },
    [userId]
  );

  const handlePageLoaded = useCallback((pageData) => {
    setPosts((prev) => [...prev, ...(pageData.items || [])]);
    setCursor(pageData.nextCursor || null);
    setHasMore(Boolean(pageData.hasMore));
  }, []);

  const handlePostCreated = useCallback(() => {
    setToast({
      message: t('community:postForm.successAlert'),
      variant: 'success',
    });
  }, [t]);

  const handlePostUpdated = useCallback(
    (updatedPost) => {
      setPosts((previousPosts) =>
        previousPosts.map((post) => (post.id === updatedPost.id ? updatedPost : post))
      );
      setToast({ message: t('community:postForm.updateSuccess'), variant: 'success' });
    },
    [t]
  );

  const handlePostEditError = useCallback((message) => setToast({ message, variant: 'error' }), []);
  const handlePostDeleted = useCallback(
    (postId) => {
      setPosts((previousPosts) =>
        previousPosts.filter((post) => String(post.id) !== String(postId))
      );
      setToast({ message: t('community:postCard.deleteSuccess'), variant: 'success' });
    },
    [t]
  );
  const handlePostDeleteError = useCallback(
    (message) => setToast({ message, variant: 'error' }),
    []
  );
  const { handleDeletePost } = useCommunityPostDelete({
    onDeleted: handlePostDeleted,
    onError: handlePostDeleteError,
  });
  const handleCommentCreated = useCallback(
    (postId) => {
      setPosts((previousPosts) =>
        previousPosts.map((post) =>
          String(post.id) === String(postId)
            ? { ...post, commentCount: (post.commentCount || 0) + 1 }
            : post
        )
      );
      setToast({ message: t('community:commentSuccess'), variant: 'success' });
    },
    [t]
  );
  const handleCommentDeleted = useCallback(
    (postId, commentId, parentCommentId, removedCommentCount = 1) => {
      setPosts((previousPosts) =>
        previousPosts.map((post) =>
          String(post.id) === String(postId)
            ? {
                ...post,
                commentCount: Math.max(0, (post.commentCount || 0) - removedCommentCount),
              }
            : post
        )
      );
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
    handleOpenCreatePostForm,
    handleSubmitPostForm,
    isModalOpen,
  } = useCommunityPostEditor({
    onEditError: handlePostEditError,
    onPostCreated: handlePostCreated,
    onPostUpdated: handlePostUpdated,
    setPosts,
  });

  const handleLikeError = useCallback(
    () =>
      setToast({
        message: t('api:common.UNKNOWN_ERROR'),
        variant: 'error',
      }),
    [t]
  );
  const { handleLikePost } = useCommunityPostActions({
    onLikeError: handleLikeError,
    setPosts,
  });

  const { isLoadingMore, resetLoadingMore, sentinelRef } = useInfiniteScroll({
    cursor,
    fetchPage: fetchUserPosts,
    hasMore,
    isLoadingFirstPage,
    onPageLoaded: handlePageLoaded,
    requestGenerationRef,
  });

  const loadInitialPosts = async () => {
    if (!userId) return;
    const requestGeneration = ++requestGenerationRef.current;
    resetLoadingMore();
    setIsLoadingFirstPage(true);
    setError(null);
    setPosts([]);
    setCursor(null);
    setHasMore(false);
    window.scrollTo({ top: 0 });
    try {
      const pageData = await fetchUserPosts(null);
      if (requestGeneration !== requestGenerationRef.current) return;
      setPosts(pageData?.items || []);
      setCursor(pageData?.nextCursor || null);
      setHasMore(Boolean(pageData?.hasMore));
    } catch {
      if (requestGeneration === requestGenerationRef.current) {
        setError(t('common:loadError'));
      }
    } finally {
      if (requestGeneration === requestGenerationRef.current) {
        setIsLoadingFirstPage(false);
      }
    }
  };

  useEffect(() => {
    if (userId) {
      loadInitialPosts();
    }
  }, [userId]);

  // Lọc theo trạng thái bài viết.
  const filteredPosts = useMemo(() => {
    return posts.filter((post) => {
      const postStatus = (post.status || 'PUBLISHED').toUpperCase();
      const matchesStatus = selectedStatus === 'ALL' || postStatus === selectedStatus;
      return matchesStatus;
    });
  }, [posts, selectedStatus]);

  const groupOptions = groups.map((g) => ({ value: g.id.toString(), label: g.groupName }));

  const handleStatusChange = (status) => {
    if (status === selectedStatus) return;

    setSelectedStatus(status);
    window.scrollTo({ top: 0 });
  };

  return (
    <div className="my-feed-page">
      <div className="my-feed-page__content-layout">
        <aside className="my-feed-page__sidebar-col">
          <button
            type="button"
            className="btn-primary my-feed-page__create-post-btn"
            onClick={handleOpenCreatePostForm}
          >
            <span className="material-symbols-outlined">add</span>
            <span>{t('community:createPost')}</span>
          </button>
          <FilterSidebar
            filterType="status"
            selectedValue={selectedStatus}
            onSelect={handleStatusChange}
          />
        </aside>

        <main className="my-feed-page__list">
          {error ? (
            <EmptyState
              icon="error"
              description={error}
              isError={true}
              actionLabel={t('common:retry')}
              onAction={loadInitialPosts}
            />
          ) : isLoadingFirstPage ? (
            <div className="glass-card my-feed-page__loading-card">
              <div className="app-loading-spinner" />
            </div>
          ) : filteredPosts.length > 0 ? (
            <>
              {filteredPosts.map((post) => {
                const mapped = mapPost(post, groups);
                return (
                  <PostCard
                    key={post.id}
                    postId={post.id}
                    author={mapped.author}
                    group={mapped.groupName}
                    createdAt={post.createdAt}
                    status={post.status}
                    title={post.title}
                    content={post.content}
                    media={mapped.medias}
                    likeCount={post.likeCount}
                    commentCount={post.commentCount}
                    likedByMe={post.likedByMe}
                    onLike={(nextLiked) => handleLikePost(post.id, nextLiked)}
                    onCommentCreated={handleCommentCreated}
                    onCommentError={handleCommentError}
                    onCommentDeleted={handleCommentDeleted}
                    onCommentDeleteSuccess={handleCommentDeleteSuccess}
                    onOpenPost={() => {
                      if (onNavigate) onNavigate(`/community/posts/${post.id}`);
                    }}
                    onNavigate={onNavigate}
                    onEdit={handleOpenEditPostForm}
                    onDelete={handleDeletePost}
                  />
                );
              })}
              <div ref={sentinelRef} className="my-feed-page__sentinel" />
              {isLoadingMore && (
                <div className="my-feed-page__loading-more">
                  <div className="app-loading-spinner" />
                </div>
              )}
            </>
          ) : (
            <EmptyState
              icon="inbox"
              title={t('community:feed.emptyTitle')}
              description={t('community:feed.emptyMyFeed')}
              onAction={handleOpenCreatePostForm}
              actionVariant="primary"
            />
          )}
        </main>
      </div>

      <PostFormModal
        isOpen={isModalOpen}
        onClose={handleClosePostForm}
        onSubmit={handleSubmitPostForm}
        groups={groupOptions}
        initialPost={editingPost}
      />

      <Toast
        message={toast.message}
        variant={toast.variant}
        onDismiss={() => setToast((prev) => ({ ...prev, message: '' }))}
      />
    </div>
  );
}

export default MyFeedPage;
