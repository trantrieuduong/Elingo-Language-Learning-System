import { useCallback } from 'react';
import { likePostApi, unlikePostApi } from '../communityApi';

function useCommunityPostActions({ onLikeError, setPosts }) {
  const handleLikePost = useCallback(
    async (postId, nextLiked) => {
      try {
        if (nextLiked) {
          await likePostApi(postId);
        } else {
          await unlikePostApi(postId);
        }

        setPosts((previousPosts) =>
          previousPosts.map((post) =>
            post.id === postId
              ? {
                  ...post,
                  likedByMe: nextLiked,
                  likeCount: post.likeCount + (nextLiked ? 1 : -1),
                }
              : post
          )
        );
        return true;
      } catch (error) {
        onLikeError(error);
        return false;
      }
    },
    [onLikeError, setPosts]
  );

  return { handleLikePost };
}

export default useCommunityPostActions;
