import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { deletePostApi } from '../communityApi';

function useCommunityPostDelete({ onDeleted, onError }) {
  const { t } = useTranslation();

  const handleDeletePost = useCallback(
    async (postId) => {
      let response;
      try {
        response = await deletePostApi(postId);
      } catch (error) {
        onError?.(
          error?.response?.data?.message || t('community:postCard.deleteFailed')
        );
        return false;
      }

      if (response?.success === false) {
        onError?.(response.message || t('community:postCard.deleteFailed'));
        return false;
      }

      onDeleted?.(postId, response);
      return true;
    },
    [onDeleted, onError, t]
  );

  return { handleDeletePost };
}

export default useCommunityPostDelete;
