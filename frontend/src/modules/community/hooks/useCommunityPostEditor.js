import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { createPostApi, getPostByIdApi, updatePostApi } from '../communityApi';

function useCommunityPostEditor({
  onEditError,
  onPostCreated,
  onPostUpdated,
  setPosts,
  shouldAddCreatedPostToPosts = true,
}) {
  const { t } = useTranslation();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingPost, setEditingPost] = useState(null);

  const handleOpenCreatePostForm = useCallback(() => {
    setEditingPost(null);
    setIsModalOpen(true);
  }, []);

  const handleClosePostForm = useCallback(() => {
    setIsModalOpen(false);
    setEditingPost(null);
  }, []);

  const handleOpenEditPostForm = useCallback(
    async (postId) => {
      try {
        const response = await getPostByIdApi(postId);
        const post = response?.data;
        if (response?.success === false) {
          throw new Error(t('community:postForm.errors.loadPostFailed'));
        }

        setEditingPost(post);
        setIsModalOpen(true);
      } catch {
        onEditError?.(t('community:postForm.errors.loadPostFailed'));
      }
    },
    [onEditError, t]
  );

  const handleSubmitPostForm = useCallback(
    async ({ group, title, content, uploadedMedia = [] }) => {
      const response = editingPost
        ? await updatePostApi(editingPost.id, {
            title,
            content,
            mediaUrls: uploadedMedia.map((media) => media.fileKey),
          })
        : await createPostApi({
            groupId: Number(group),
            title,
            content,
            mediaUrls: uploadedMedia.map((media) => media.fileKey),
          });

      if (response?.success === false) return response;

      const post = response?.data;
      if (!post?.id) {
        return {
          success: false,
          message: t('community:postForm.errors.updateFailed'),
        };
      }

      if (editingPost) {
        onPostUpdated?.(post);
      } else {
        if (shouldAddCreatedPostToPosts) {
          setPosts?.((previousPosts) => [post, ...previousPosts]);
        }
        onPostCreated?.(post);
      }

      handleClosePostForm();
      return response;
    },
    [
      editingPost,
      handleClosePostForm,
      onPostCreated,
      onPostUpdated,
      setPosts,
      shouldAddCreatedPostToPosts,
      t,
    ]
  );

  return {
    editingPost,
    handleClosePostForm,
    handleOpenEditPostForm,
    handleOpenCreatePostForm,
    handleSubmitPostForm,
    isModalOpen,
  };
}

export default useCommunityPostEditor;
