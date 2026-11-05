import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  createCommentApi,
  deleteCommentApi,
  getCommentsByPostApi,
  getRepliesByCommentApi,
  likeCommentApi,
  unlikeCommentApi,
  updateCommentApi,
} from '../communityApi';
import { uploadFileForVerification } from '../../../services/uploadFile';

const COMMENTS_PAGE_SIZE = 20;

function useCommunityComments({
  postId,
  onCommentCreated,
  onCommentError,
  onCommentUpdated,
  onCommentDeleted,
  onCommentDeleteSuccess,
  onCommentLikeError,
}) {
  const { t } = useTranslation();
  const [comments, setComments] = useState([]);
  const [isLoadingFirstPage, setIsLoadingFirstPage] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [hasMore, setHasMore] = useState(false);
  const [repliesByCommentId, setRepliesByCommentId] = useState({});
  const pageRef = useRef(1);
  const hasLoadedRef = useRef(false);
  const requestIdRef = useRef(0);
  const replyRequestIdsRef = useRef({});
  const replyPagesRef = useRef({});
  const loadingReplyIdsRef = useRef(new Set());

  useEffect(() => {
    return () => {
      requestIdRef.current += 1;
    };
  }, []);

  const loadComments = useCallback(
    async (page = 1, append = false) => {
      const requestId = ++requestIdRef.current;
      setLoadError('');
      if (append) setIsLoadingMore(true);
      else setIsLoadingFirstPage(true);

      try {
        const response = await getCommentsByPostApi(postId, {
          page,
          size: COMMENTS_PAGE_SIZE,
        });
        const pageData = response?.data;
        if (response?.success === false || !Array.isArray(pageData?.content)) {
          throw new Error(t('community:comments.loadFailed'));
        }

        if (requestId !== requestIdRef.current) return;
        setComments((previousComments) =>
          append ? [...previousComments, ...pageData.content] : pageData.content
        );
        pageRef.current = page;
        hasLoadedRef.current = true;
        setHasMore(!pageData.isLast);
      } catch {
        if (requestId === requestIdRef.current) {
          setLoadError(t('community:comments.loadFailed'));
        }
      } finally {
        if (requestId === requestIdRef.current) {
          setIsLoadingFirstPage(false);
          setIsLoadingMore(false);
        }
      }
    },
    [postId, t]
  );

  const handleLoadComments = useCallback(() => {
    if (!hasLoadedRef.current && !isLoadingFirstPage) loadComments();
  }, [isLoadingFirstPage, loadComments]);

  const handleLoadMoreComments = useCallback(() => {
    if (hasMore && !isLoadingFirstPage && !isLoadingMore) {
      loadComments(pageRef.current + 1, true);
    }
  }, [hasMore, isLoadingFirstPage, isLoadingMore, loadComments]);

  const handleLoadReplies = useCallback(
    async (commentId, page = 1, append = false) => {
      const replyId = String(commentId);
      if (loadingReplyIdsRef.current.has(replyId)) return false;

      const requestedPage = append ? (replyPagesRef.current[replyId] ?? 0) + 1 : page;
      const requestId = (replyRequestIdsRef.current[commentId] || 0) + 1;
      replyRequestIdsRef.current[commentId] = requestId;
      loadingReplyIdsRef.current.add(replyId);
      setRepliesByCommentId((previousReplies) => ({
        ...previousReplies,
        [commentId]: {
          ...previousReplies[commentId],
          isLoading: true,
          error: '',
        },
      }));

      try {
        const response = await getRepliesByCommentApi(commentId, {
          page: requestedPage,
          size: COMMENTS_PAGE_SIZE,
        });
        const pageData = response?.data;
        if (response?.success === false || !Array.isArray(pageData?.content)) {
          throw new Error(t('community:comments.repliesLoadFailed'));
        }

        if (replyRequestIdsRef.current[commentId] !== requestId) return false;
        replyPagesRef.current[replyId] = requestedPage;
        setRepliesByCommentId((previousReplies) => ({
          ...previousReplies,
          [commentId]: {
            items: append
              ? [
                  ...(previousReplies[commentId]?.items || []),
                  ...pageData.content.filter(
                    (reply) =>
                      !(previousReplies[commentId]?.items || []).some(
                        (existingReply) => String(existingReply.id) === String(reply.id)
                      )
                  ),
                ]
              : pageData.content,
            page: requestedPage,
            hasMore: !pageData.isLast,
            isLoading: false,
            isLoaded: true,
            error: '',
          },
        }));
        return true;
      } catch {
        if (replyRequestIdsRef.current[commentId] === requestId) {
          setRepliesByCommentId((previousReplies) => ({
            ...previousReplies,
            [commentId]: {
              ...previousReplies[commentId],
              isLoading: false,
              error: t('community:comments.repliesLoadFailed'),
            },
          }));
        }
        return false;
      } finally {
        loadingReplyIdsRef.current.delete(replyId);
      }
    },
    [t]
  );

  const handleCreateReply = useCallback(
    async (parentCommentId, content, imageFile) => {
      try {
        const repliesWereLoaded = Boolean(repliesByCommentId[parentCommentId]?.isLoaded);
        const imageUrl = imageFile
          ? await uploadFileForVerification(imageFile, imageFile.type)
          : undefined;
        const response = await createCommentApi({
          postId,
          parentCommentId,
          content,
          imageUrl,
        });
        if (response?.success === false) {
          onCommentError?.(response.message || t('community:comments.replyFailed'));
          return false;
        }

        const createdReply = response?.data;
        if (createdReply?.id) {
          const repliesAreLoaded = repliesWereLoaded || (await handleLoadReplies(parentCommentId));
          setRepliesByCommentId((previousReplies) => {
            const parentReplies = previousReplies[parentCommentId] || {
              items: [],
              page: 1,
              hasMore: false,
              isLoaded: true,
            };
            return {
              ...previousReplies,
              [parentCommentId]: {
                ...parentReplies,
                items: [
                  ...parentReplies.items.filter(
                    (reply) => String(reply.id) !== String(createdReply.id)
                  ),
                  createdReply,
                ],
                isLoaded: repliesAreLoaded,
                error: '',
              },
            };
          });
        } else {
          await handleLoadReplies(parentCommentId);
        }

        setComments((previousComments) =>
          previousComments.map((comment) =>
            String(comment.id) === String(parentCommentId)
              ? { ...comment, replyCount: (comment.replyCount || 0) + 1 }
              : comment
          )
        );
        onCommentCreated?.(postId);
        return true;
      } catch (error) {
        onCommentError?.(error?.response?.data?.message || t('community:comments.replyFailed'));
        return false;
      }
    },
    [handleLoadReplies, onCommentCreated, onCommentError, postId, repliesByCommentId, t]
  );

  const handleCreateComment = useCallback(
    async (content, imageFile) => {
      try {
        const imageUrl = imageFile
          ? await uploadFileForVerification(imageFile, imageFile.type)
          : undefined;
        const response = await createCommentApi({ postId, content, imageUrl });
        if (response?.success === false) {
          onCommentError?.(response.message);
          return false;
        }

        const createdComment = response?.data;
        if (createdComment?.id) {
          setComments((previousComments) => [
            createdComment,
            ...previousComments.filter(
              (comment) => String(comment.id) !== String(createdComment.id)
            ),
          ]);
          hasLoadedRef.current = true;
        } else {
          await loadComments();
        }

        onCommentCreated?.(postId);
        return true;
      } catch (error) {
        onCommentError?.(error?.response?.data?.message || error.message);
        return false;
      }
    },
    [loadComments, onCommentCreated, onCommentError, postId]
  );

  const handleUpdateComment = useCallback(
    async (commentId, { content, imageFile, imageUrl }) => {
      try {
        const updatedImageUrl = imageFile
          ? await uploadFileForVerification(imageFile, imageFile.type)
          : imageUrl;
        const response = await updateCommentApi(commentId, {
          content,
          imageUrl: updatedImageUrl,
        });
        if (response?.success === false) {
          onCommentError?.(response.message || t('community:comments.updateFailed'));
          return false;
        }

        const updatedComment =
          response?.data && typeof response.data === 'object' ? response.data : {};
        const applyUpdate = (comment) =>
          String(comment.id) === String(commentId)
            ? {
                ...comment,
                ...updatedComment,
                content,
                imageUrl: updatedComment.imageUrl ?? (imageFile ? updatedImageUrl : imageUrl),
              }
            : comment;
        setComments((previousComments) => previousComments.map(applyUpdate));
        setRepliesByCommentId((previousReplies) =>
          Object.fromEntries(
            Object.entries(previousReplies).map(([parentId, replyPage]) => [
              parentId,
              { ...replyPage, items: (replyPage.items || []).map(applyUpdate) },
            ])
          )
        );
        onCommentUpdated?.(updatedComment);
        return true;
      } catch (error) {
        onCommentError?.(
          error?.response?.data?.message || error?.message || t('community:comments.updateFailed')
        );
        return false;
      }
    },
    [onCommentError, onCommentUpdated, t]
  );

  const handleDeleteComment = useCallback(
    async (commentId) => {
      let response;
      try {
        response = await deleteCommentApi(commentId);
      } catch (error) {
        onCommentError?.(error?.response?.data?.message || t('community:comments.deleteFailed'));
        return false;
      }

      if (response?.success === false) {
        onCommentError?.(response.message || t('community:comments.deleteFailed'));
        return false;
      }

      const parentEntry = Object.entries(repliesByCommentId).find(([, replyPage]) =>
        replyPage.items?.some((reply) => String(reply.id) === String(commentId))
      );
      const parentCommentId = parentEntry?.[0];
      const rootComment = comments.find((comment) => String(comment.id) === String(commentId));
      const removedCommentCount = rootComment ? 1 + (rootComment.replyCount || 0) : 1;
      if (parentCommentId) {
        setRepliesByCommentId((previousReplies) => ({
          ...previousReplies,
          [parentCommentId]: {
            ...previousReplies[parentCommentId],
            items: previousReplies[parentCommentId].items.filter(
              (reply) => String(reply.id) !== String(commentId)
            ),
          },
        }));
        setComments((previousComments) =>
          previousComments.map((comment) =>
            String(comment.id) === parentCommentId
              ? { ...comment, replyCount: Math.max(0, (comment.replyCount || 0) - 1) }
              : comment
          )
        );
      } else {
        setComments((previousComments) =>
          previousComments.filter((comment) => String(comment.id) !== String(commentId))
        );
        setRepliesByCommentId((previousReplies) => {
          const nextReplies = { ...previousReplies };
          delete nextReplies[commentId];
          return nextReplies;
        });
        delete replyPagesRef.current[String(commentId)];
      }
      onCommentDeleted?.(postId, commentId, parentCommentId || null, removedCommentCount);
      onCommentDeleteSuccess?.();
      return true;
    },
    [
      comments,
      onCommentDeleteSuccess,
      onCommentDeleted,
      onCommentError,
      postId,
      repliesByCommentId,
      t,
    ]
  );

  const handleLikeComment = useCallback(
    async (commentId, nextLiked) => {
      try {
        const response = nextLiked
          ? await likeCommentApi(commentId)
          : await unlikeCommentApi(commentId);
        if (response?.success === false) {
          onCommentLikeError?.(response.message || t('community:comments.likeFailed'));
          return false;
        }

        return true;
      } catch (error) {
        onCommentLikeError?.(error?.response?.data?.message || t('community:comments.likeFailed'));
        return false;
      }
    },
    [onCommentLikeError, t]
  );

  return {
    comments,
    repliesByCommentId,
    hasMore,
    isLoadingFirstPage,
    isLoadingMore,
    loadError,
    loadComments,
    handleCreateComment,
    handleCreateReply,
    handleUpdateComment,
    handleDeleteComment,
    handleLoadReplies,
    handleLikeComment,
    handleLoadComments,
    handleLoadMoreComments,
  };
}

export default useCommunityComments;
