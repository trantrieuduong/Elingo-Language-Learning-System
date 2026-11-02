import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import PostCard from '../../components/PostCard/PostCard';
import PostFormModal from '../../components/PostFormModal/PostFormModal';
import FilterSidebar from '../../components/FilterSidebar/FilterSidebar';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import Toast from '../../../../components/Toast/Toast';
import { getPostGroups, getPublishedPosts } from '../../communityApi';
import useCommunityPostActions from '../../hooks/useCommunityPostActions';
import useInfiniteScroll from '../../hooks/useInfiniteScroll';
import { mapPost } from '../../utils/mapPost';
import './CommunityFeedPage.css';

function CommunityFeedPage({ onNavigate, initialSelectedGroupId = 'ALL' }) {
  const { t } = useTranslation();

  const [posts, setPosts] = useState([]);
  const [groups, setGroups] = useState([]);
  const [selectedGroupId, setSelectedGroupId] = useState(initialSelectedGroupId);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const [cursor, setCursor] = useState(null);
  const [hasMore, setHasMore] = useState(false);
  const [isLoadingInitial, setIsLoadingInitial] = useState(false);

  const [error, setError] = useState(null);
  const [toast, setToast] = useState({ message: '', variant: 'success' });

  const requestGenerationRef = useRef(0);

  useEffect(() => {
    const fetchGroups = async () => {
      try {
        const data = await getPostGroups();
        setGroups(data?.data || []);
      } catch {
        // groups stays empty
      }
    };
    fetchGroups();
  }, []);

  const fetchPosts = useCallback(async (groupId, nextCursor) => {
    const response = await getPublishedPosts({
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
    setIsModalOpen(false);
    setToast({
      message: t('community:postForm.successAlert'),
      variant: 'success',
    });
  }, [t]);

  const handleLikeError = useCallback(
    () =>
      setToast({
        message: t('api:common.UNKNOWN_ERROR'),
        variant: 'error',
      }),
    [t]
  );
  const { handleCreatePost, handleLikePost } = useCommunityPostActions({
    onLikeError: handleLikeError,
    onPostCreated: handlePostCreated,
    setPosts,
    shouldAddCreatedPostToPosts: false,
  });

  const { isLoadingMore, resetLoadingMore, sentinelRef } = useInfiniteScroll({
    cursor,
    fetchPage: fetchNextPage,
    hasMore,
    isLoadingInitial,
    onPageLoaded: handlePageLoaded,
    requestGenerationRef,
  });

  const loadInitialPosts = (groupId) => {
    const requestGeneration = ++requestGenerationRef.current;
    resetLoadingMore();
    setIsLoadingInitial(true);
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
          setIsLoadingInitial(false);
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
            onClick={() => setIsModalOpen(true)}
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
          ) : isLoadingInitial ? (
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
                    onOpenPost={() => {
                      if (onNavigate) onNavigate(`/community/post/${post.id}`);
                    }}
                    onNavigate={onNavigate}
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
              onAction={() => setIsModalOpen(true)}
              actionVariant="primary"
            />
          )}
        </main>
      </div>

      <PostFormModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleCreatePost}
        groups={groupOptions}
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
