import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import PostCard from '../../components/PostCard/PostCard';
import PostFormModal from '../../components/PostFormModal/PostFormModal';
import FilterSidebar from '../../components/FilterSidebar/FilterSidebar';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import Toast from '../../../../components/Toast/Toast';
import { getPostGroupsApi, getPublishedPostsApi } from '../../communityApi';
import useCommunityPostActions from '../../hooks/useCommunityPostActions';
import useCommunityPostDelete from '../../hooks/useCommunityPostDelete';
import useCommunityPostEditor from '../../hooks/useCommunityPostEditor';
import useInfiniteScroll from '../../hooks/useInfiniteScroll';
import { mapPost } from '../../utils/mapPost';
import './CommunityFeedPage.css';

function CommunityFeedPage({ onNavigate, initialSelectedGroupId = 'ALL' }) {
  const { t } = useTranslation();

  const [posts, setPosts] = useState([]);
  const [groups, setGroups] = useState([]);
  const [selectedGroupId, setSelectedGroupId] = useState(initialSelectedGroupId);
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

  const fetchPosts = useCallback(async (groupId, nextCursor) => {
    const response = await getPublishedPostsApi({
      cursor: nextCursor,
      limit: 20,
      groupId: groupId === 'ALL' ? undefined : groupId,
    });
    return response?.data || {};
  }, []);

  const fetchNextPage = useCallback(
    (nextCursor) => fetchPosts(selectedGroupId, nextCursor),
    [fetchPosts, selectedGroupId]
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
      setPosts((previousPosts) => previousPosts.filter((post) => post.id !== updatedPost.id));
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
    shouldAddCreatedPostToPosts: false,
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
    fetchPage: fetchNextPage,
    hasMore,
    isLoadingFirstPage,
    onPageLoaded: handlePageLoaded,
    requestGenerationRef,
  });

  const loadInitialPosts = (groupId) => {
    const requestGeneration = ++requestGenerationRef.current;
    resetLoadingMore();
    setIsLoadingFirstPage(true);
    setError(null);
    setPosts([]);
    setCursor(null);
    setHasMore(false);
    window.scrollTo({ top: 0 });

    fetchPosts(groupId, null)
      .then((data) => {
        if (requestGeneration !== requestGenerationRef.current) return;
        setPosts(data.items || []);
        setCursor(data.nextCursor || null);
        setHasMore(Boolean(data.hasMore));
      })
      .catch(() => {
        if (requestGeneration === requestGenerationRef.current) {
          setError(t('common:loadError'));
        }
      })
      .finally(() => {
        if (requestGeneration === requestGenerationRef.current) {
          setIsLoadingFirstPage(false);
        }
      });
  };

  useEffect(() => {
    loadInitialPosts(selectedGroupId);
  }, [selectedGroupId]);

  const groupOptions = groups.map((g) => ({ value: g.id.toString(), label: g.groupName }));

  return (
    <div className="community-feed-page">
      <div className="community-feed-page__content-layout">
        <aside className="community-feed-page__sidebar-col">
          <button
            type="button"
            className="btn-primary community-feed-page__create-post-btn"
            onClick={handleOpenCreatePostForm}
          >
            <span className="material-symbols-outlined">add</span>
            <span>{t('community:createPost')}</span>
          </button>
          <FilterSidebar
            filterType="group"
            selectedValue={selectedGroupId}
            onSelect={setSelectedGroupId}
            groups={groups}
          />
        </aside>

        <main className="community-feed-page__list">
          {error ? (
            <EmptyState
              icon="error"
              description={error}
              isError={true}
              actionLabel={t('common:retry')}
              onAction={() => loadInitialPosts(selectedGroupId)}
            />
          ) : isLoadingFirstPage ? (
            <div className="glass-card community-feed-page__loading-card">
              <div className="app-loading-spinner" />
            </div>
          ) : posts.length > 0 ? (
            <>
              {posts.map((post) => {
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
              <div ref={sentinelRef} className="community-feed-page__sentinel" />
              {isLoadingMore && (
                <div className="community-feed-page__loading-more">
                  <div className="app-loading-spinner" />
                </div>
              )}
            </>
          ) : (
            <EmptyState
              icon="inbox"
              title={t('community:feed.emptyTitle')}
              description={t('community:feed.emptyGroup')}
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

export default CommunityFeedPage;
