import { useCallback } from 'react';
import { createPost, likePost, unlikePost } from '../communityApi';

function useCommunityPostActions({
  onLikeError,
  onPostCreated,
  setPosts,
  shouldAddCreatedPostToPosts = true,
}) {
  const handleCreatePost = useCallback(
    async ({ group, title, content, uploadedMedia = [] }) => {
      const response = await createPost({
        groupId: Number(group),
        title,
        content,
        mediaUrls: uploadedMedia.map((media) => media.fileKey),
      });

      if (response?.data && shouldAddCreatedPostToPosts) {
        setPosts((previousPosts) => [response.data, ...previousPosts]);
      }

      onPostCreated();
    },
    [onPostCreated, setPosts, shouldAddCreatedPostToPosts]
  );

  const handleLikePost = useCallback(
    async (postId, nextLiked) => {
      try {
        if (nextLiked) {
          await likePost(postId);
        } else {
          await unlikePost(postId);
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

  return { handleCreatePost, handleLikePost };
}

export default useCommunityPostActions;
