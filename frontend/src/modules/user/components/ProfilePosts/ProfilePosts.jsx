import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PostCard from '../../../community/components/PostCard/PostCard';
import { getPostGroupsApi, getUserPostsApi } from '../../../community/communityApi';
import useCommunityPostActions from '../../../community/hooks/useCommunityPostActions';
import { mapPost } from '../../../community/utils/mapPost';
import './ProfilePosts.css';

function ProfilePosts({ userId, onNavigate }) {
  const { t } = useTranslation(['community', 'common', 'user']);
  const [posts, setPosts] = useState([]);
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadPosts = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    setError('');
    try {
      const [postsResponse, groupsResponse] = await Promise.all([
        getUserPostsApi(userId, { limit: 20 }),
        getPostGroupsApi().catch(() => null),
      ]);
      if (!postsResponse?.success) throw new Error(postsResponse?.message);
      setPosts(postsResponse.data?.items || []);
      setGroups(groupsResponse?.data || []);
    } catch (requestError) {
      setError(requestError?.message || t('common:loadError'));
    } finally {
      setLoading(false);
    }
  }, [t, userId]);

  useEffect(() => {
    loadPosts();
  }, [loadPosts]);

  const { handleLikePost } = useCommunityPostActions({
    setPosts,
    onLikeError: () => setError(t('common:loadError')),
  });

  return (
    <section className="profile-posts" aria-labelledby="profile-posts-title">
      <div className="profile-posts__heading">
        <div className="profile-posts__heading-icon glass-thumb">
          <span className="material-symbols-outlined">article</span>
        </div>
        <h2 id="profile-posts-title" className="text-headline-sm">
          {t('user:posts')}
        </h2>
      </div>

      {loading ? (
        <div className="glass-card profile-posts__loading">
          <div className="app-loading-spinner" />
        </div>
      ) : error ? (
        <EmptyState
          icon="error"
          isError
          description={error}
          actionLabel={t('common:retry')}
          onAction={loadPosts}
        />
      ) : posts.length === 0 ? (
        <EmptyState variant="compact" icon="inbox" title={t('community:feed.emptyTitle')} />
      ) : (
        <div className="profile-posts__list">
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
                onOpenPost={() => onNavigate?.(`/community/posts/${post.id}`)}
                onNavigate={onNavigate}
              />
            );
          })}
        </div>
      )}
    </section>
  );
}

export default ProfilePosts;
