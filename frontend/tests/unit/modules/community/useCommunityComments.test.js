import { beforeEach, describe, expect, it, vi } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import useCommunityComments from '@/modules/community/hooks/useCommunityComments';
import {
  createCommentApi,
  deleteCommentApi,
  getCommentsByPostApi,
  getRepliesByCommentApi,
  likeCommentApi,
  unlikeCommentApi,
  updateCommentApi,
} from '@/modules/community/communityApi';
import { uploadFileForVerification } from '@/services/uploadFile';

vi.mock('@/modules/community/communityApi', () => ({
  createCommentApi: vi.fn(),
  deleteCommentApi: vi.fn(),
  getCommentsByPostApi: vi.fn(),
  getRepliesByCommentApi: vi.fn(),
  likeCommentApi: vi.fn(),
  unlikeCommentApi: vi.fn(),
  updateCommentApi: vi.fn(),
}));
vi.mock('@/services/uploadFile', () => ({ uploadFileForVerification: vi.fn() }));

const makePageResponse = (content, isLast = true) => ({
  success: true,
  data: { content, isLast },
});

const COMMENT_A = { id: 1, content: 'First', replyCount: 0 };
const COMMENT_B = { id: 2, content: 'Second', replyCount: 0 };
const REPLY_1 = { id: 101, content: 'Reply one' };

describe('useCommunityComments', () => {
  const onCommentCreated = vi.fn();
  const onCommentError = vi.fn();
  const onCommentUpdated = vi.fn();
  const onCommentDeleted = vi.fn();
  const onCommentDeleteSuccess = vi.fn();
  const onCommentLikeError = vi.fn();

  const renderCommentsHook = () =>
    renderHook(() =>
      useCommunityComments({
        postId: 10,
        onCommentCreated,
        onCommentError,
        onCommentUpdated,
        onCommentDeleted,
        onCommentDeleteSuccess,
        onCommentLikeError,
      })
    );

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('loadComments', () => {
    it('loads the first comment page and exposes pagination state', async () => {
      getCommentsByPostApi.mockResolvedValue(
        makePageResponse([COMMENT_A], false)
      );
      const { result } = renderCommentsHook();

      await act(async () => {
        await result.current.loadComments();
      });

      expect(getCommentsByPostApi).toHaveBeenCalledWith(10, { page: 1, size: 20 });
      expect(result.current.comments).toEqual([COMMENT_A]);
      expect(result.current.hasMore).toBe(true);
      expect(result.current.isLoadingFirstPage).toBe(false);
    });

    it('appends comments when append=true', async () => {
      getCommentsByPostApi
        .mockResolvedValueOnce(makePageResponse([COMMENT_A], false))
        .mockResolvedValueOnce(makePageResponse([COMMENT_B], true));

      const { result } = renderCommentsHook();

      await act(async () => {
        await result.current.loadComments(1);
      });
      await act(async () => {
        await result.current.loadComments(2, true);
      });

      expect(result.current.comments).toEqual([COMMENT_A, COMMENT_B]);
      expect(result.current.hasMore).toBe(false);
    });

    it('sets loadError when API returns success:false', async () => {
      getCommentsByPostApi.mockResolvedValue({ success: false, data: null });
      const { result } = renderCommentsHook();

      await act(async () => {
        await result.current.loadComments();
      });

      expect(result.current.loadError).not.toBe('');
      expect(result.current.comments).toEqual([]);
    });

    it('sets loadError when API throws', async () => {
      getCommentsByPostApi.mockRejectedValue(new Error('Network'));
      const { result } = renderCommentsHook();

      await act(async () => {
        await result.current.loadComments();
      });

      expect(result.current.loadError).not.toBe('');
    });
  });

  describe('handleLoadComments', () => {
    it('triggers loadComments only on first call (hasLoaded guard)', async () => {
      getCommentsByPostApi.mockResolvedValue(makePageResponse([COMMENT_A]));
      const { result } = renderCommentsHook();

      await act(async () => { result.current.handleLoadComments(); });
      await waitFor(() => expect(result.current.comments).toHaveLength(1));

      // second call should not trigger a second fetch
      await act(async () => { result.current.handleLoadComments(); });
      expect(getCommentsByPostApi).toHaveBeenCalledTimes(1);
    });
  });

  describe('handleLoadMoreComments', () => {
    it('fetches the next page when hasMore is true', async () => {
      getCommentsByPostApi
        .mockResolvedValueOnce(makePageResponse([COMMENT_A], false))
        .mockResolvedValueOnce(makePageResponse([COMMENT_B], true));

      const { result } = renderCommentsHook();

      await act(async () => { await result.current.loadComments(1); });
      await act(async () => { result.current.handleLoadMoreComments(); });
      await waitFor(() => expect(result.current.comments).toHaveLength(2));

      expect(getCommentsByPostApi).toHaveBeenCalledTimes(2);
      expect(result.current.hasMore).toBe(false);
    });

    it('does nothing when hasMore is false', async () => {
      getCommentsByPostApi.mockResolvedValueOnce(makePageResponse([COMMENT_A], true));
      const { result } = renderCommentsHook();

      await act(async () => { await result.current.loadComments(); });
      await act(async () => { result.current.handleLoadMoreComments(); });

      // Only the initial loadComments call
      expect(getCommentsByPostApi).toHaveBeenCalledTimes(1);
    });
  });

  describe('handleCreateComment', () => {
    it('uploads an image and prepends a created comment', async () => {
      uploadFileForVerification.mockResolvedValue('uploaded-key');
      createCommentApi.mockResolvedValue({ success: true, data: { id: 2, content: 'New' } });
      const { result } = renderCommentsHook();
      const image = new File(['x'], 'image.png', { type: 'image/png' });

      await act(async () => {
        expect(await result.current.handleCreateComment('New', image)).toBe(true);
      });

      expect(uploadFileForVerification).toHaveBeenCalledWith(image, 'image/png');
      expect(createCommentApi).toHaveBeenCalledWith({
        postId: 10,
        content: 'New',
        imageUrl: 'uploaded-key',
      });
      expect(result.current.comments).toEqual([{ id: 2, content: 'New' }]);
      expect(onCommentCreated).toHaveBeenCalledWith(10);
    });

    it('creates a comment without calling upload when imageFile is omitted', async () => {
      createCommentApi.mockResolvedValue({ success: true, data: { id: 3, content: 'Text' } });
      const { result } = renderCommentsHook();

      await act(async () => {
        expect(await result.current.handleCreateComment('Text')).toBe(true);
      });

      expect(uploadFileForVerification).not.toHaveBeenCalled();
      expect(createCommentApi).toHaveBeenCalledWith({
        postId: 10,
        content: 'Text',
        imageUrl: undefined,
      });
      expect(result.current.comments).toEqual([{ id: 3, content: 'Text' }]);
      expect(onCommentCreated).toHaveBeenCalledWith(10);
    });

    it('calls onCommentError and returns false when API returns success:false', async () => {
      createCommentApi.mockResolvedValue({ success: false, message: 'Create failed' });
      const { result } = renderCommentsHook();

      await act(async () => {
        expect(await result.current.handleCreateComment('Text')).toBe(false);
      });

      expect(onCommentError).toHaveBeenCalledWith('Create failed');
      expect(onCommentCreated).not.toHaveBeenCalled();
    });

    it('calls onCommentError and returns false when API throws', async () => {
      createCommentApi.mockRejectedValue({ response: { data: { message: 'Server error' } } });
      const { result } = renderCommentsHook();

      await act(async () => {
        expect(await result.current.handleCreateComment('Text')).toBe(false);
      });

      expect(onCommentError).toHaveBeenCalledWith('Server error');
    });

    it('reloads all comments when created comment has no id', async () => {
      createCommentApi.mockResolvedValue({ success: true, data: null });
      getCommentsByPostApi.mockResolvedValue(makePageResponse([COMMENT_A]));
      const { result } = renderCommentsHook();

      await act(async () => {
        await result.current.handleCreateComment('Text');
      });

      expect(getCommentsByPostApi).toHaveBeenCalledTimes(1);
    });
  });

  describe('handleLoadReplies', () => {
    it('loads replies for a comment and stores them', async () => {
      getRepliesByCommentApi.mockResolvedValue(makePageResponse([REPLY_1]));
      const { result } = renderCommentsHook();

      let ok;
      await act(async () => {
        ok = await result.current.handleLoadReplies(1);
      });

      expect(ok).toBe(true);
      expect(getRepliesByCommentApi).toHaveBeenCalledWith(1, { page: 1, size: 20 });
      expect(result.current.repliesByCommentId[1].items).toEqual([REPLY_1]);
      expect(result.current.repliesByCommentId[1].isLoaded).toBe(true);
      expect(result.current.repliesByCommentId[1].hasMore).toBe(false);
    });

    it('returns false and stores error on API failure', async () => {
      getRepliesByCommentApi.mockRejectedValue(new Error('Network'));
      const { result } = renderCommentsHook();

      let ok;
      await act(async () => {
        ok = await result.current.handleLoadReplies(1);
      });

      expect(ok).toBe(false);
      expect(result.current.repliesByCommentId[1].error).not.toBe('');
    });

    it('returns false and stores error when API returns success:false', async () => {
      getRepliesByCommentApi.mockResolvedValue({ success: false, data: null });
      const { result } = renderCommentsHook();

      let ok;
      await act(async () => {
        ok = await result.current.handleLoadReplies(99);
      });

      expect(ok).toBe(false);
      expect(result.current.repliesByCommentId[99].isLoading).toBe(false);
    });

    it('deduplicates replies when appending', async () => {
      getRepliesByCommentApi
        .mockResolvedValueOnce(makePageResponse([REPLY_1], false))
        .mockResolvedValueOnce(makePageResponse([REPLY_1, { id: 102, content: 'New' }], true));

      const { result } = renderCommentsHook();

      await act(async () => { await result.current.handleLoadReplies(1); });
      await act(async () => { await result.current.handleLoadReplies(1, 1, true); });

      const items = result.current.repliesByCommentId[1].items;
      const ids = items.map((r) => r.id);
      // REPLY_1 must appear only once
      expect(ids.filter((id) => id === 101)).toHaveLength(1);
      expect(ids).toContain(102);
    });
  });

  describe('handleCreateReply', () => {
    it('creates a reply and increments the parent comment replyCount', async () => {
      createCommentApi.mockResolvedValue({ success: true, data: { id: 201, content: 'Reply' } });
      getRepliesByCommentApi.mockResolvedValue(makePageResponse([{ id: 201, content: 'Reply' }]));

      const { result } = renderCommentsHook();

      // Seed a parent comment
      await act(async () => {
        getCommentsByPostApi.mockResolvedValueOnce(
          makePageResponse([{ id: 1, content: 'Parent', replyCount: 0 }])
        );
        await result.current.loadComments();
      });

      let ok;
      await act(async () => {
        ok = await result.current.handleCreateReply(1, 'Reply text', null);
      });

      expect(ok).toBe(true);
      expect(createCommentApi).toHaveBeenCalledWith(
        expect.objectContaining({ postId: 10, parentCommentId: 1, content: 'Reply text' })
      );
      const parentAfter = result.current.comments.find((c) => c.id === 1);
      expect(parentAfter.replyCount).toBe(1);
      expect(onCommentCreated).toHaveBeenCalledWith(10);
    });

    it('returns false and calls onCommentError on success:false', async () => {
      createCommentApi.mockResolvedValue({ success: false, message: 'Reply failed' });
      const { result } = renderCommentsHook();

      let ok;
      await act(async () => {
        ok = await result.current.handleCreateReply(1, 'Text', null);
      });

      expect(ok).toBe(false);
      expect(onCommentError).toHaveBeenCalledWith('Reply failed');
    });

    it('uploads image and passes imageUrl to createCommentApi', async () => {
      uploadFileForVerification.mockResolvedValue('reply-img-key');
      createCommentApi.mockResolvedValue({ success: true, data: { id: 202 } });
      getRepliesByCommentApi.mockResolvedValue(makePageResponse([{ id: 202 }]));
      const { result } = renderCommentsHook();
      const file = new File(['img'], 'r.jpg', { type: 'image/jpeg' });

      await act(async () => {
        await result.current.handleCreateReply(1, 'With image', file);
      });

      expect(uploadFileForVerification).toHaveBeenCalledWith(file, 'image/jpeg');
      expect(createCommentApi).toHaveBeenCalledWith(
        expect.objectContaining({ imageUrl: 'reply-img-key' })
      );
    });
  });

  describe('handleUpdateComment', () => {
    it('updates a root comment in state', async () => {
      getCommentsByPostApi.mockResolvedValueOnce(
        makePageResponse([{ id: 1, content: 'Old', imageUrl: null }])
      );
      updateCommentApi.mockResolvedValue({
        success: true,
        data: { id: 1, content: 'Updated', imageUrl: null },
      });
      const { result } = renderCommentsHook();

      await act(async () => { await result.current.loadComments(); });
      let ok;
      await act(async () => {
        ok = await result.current.handleUpdateComment(1, { content: 'Updated', imageUrl: null });
      });

      expect(ok).toBe(true);
      expect(updateCommentApi).toHaveBeenCalledWith(1, { content: 'Updated', imageUrl: null });
      const updated = result.current.comments.find((c) => c.id === 1);
      expect(updated.content).toBe('Updated');
      expect(onCommentUpdated).toHaveBeenCalled();
    });

    it('uploads new imageFile before calling updateCommentApi', async () => {
      uploadFileForVerification.mockResolvedValue('new-img-key');
      updateCommentApi.mockResolvedValue({ success: true, data: {} });
      const { result } = renderCommentsHook();
      const file = new File(['img'], 'update.jpg', { type: 'image/jpeg' });

      await act(async () => {
        await result.current.handleUpdateComment(1, { content: 'x', imageFile: file, imageUrl: 'old-key' });
      });

      expect(uploadFileForVerification).toHaveBeenCalledWith(file, 'image/jpeg');
      expect(updateCommentApi).toHaveBeenCalledWith(1, { content: 'x', imageUrl: 'new-img-key' });
    });

    it('returns false and calls onCommentError on success:false', async () => {
      updateCommentApi.mockResolvedValue({ success: false, message: 'Update failed' });
      const { result } = renderCommentsHook();

      let ok;
      await act(async () => {
        ok = await result.current.handleUpdateComment(1, { content: 'x' });
      });

      expect(ok).toBe(false);
      expect(onCommentError).toHaveBeenCalledWith('Update failed');
    });

    it('returns false and calls onCommentError when API throws', async () => {
      updateCommentApi.mockRejectedValue(new Error('Network'));
      const { result } = renderCommentsHook();

      let ok;
      await act(async () => {
        ok = await result.current.handleUpdateComment(1, { content: 'x' });
      });

      expect(ok).toBe(false);
      expect(onCommentError).toHaveBeenCalled();
    });
  });

  describe('handleDeleteComment', () => {
    it('removes a root comment and its replies from state', async () => {
      getCommentsByPostApi.mockResolvedValueOnce(
        makePageResponse([{ id: 1, content: 'Root', replyCount: 2 }, COMMENT_B])
      );
      deleteCommentApi.mockResolvedValue({ success: true });
      const { result } = renderCommentsHook();

      await act(async () => { await result.current.loadComments(); });
      let ok;
      await act(async () => {
        ok = await result.current.handleDeleteComment(1);
      });

      expect(ok).toBe(true);
      expect(deleteCommentApi).toHaveBeenCalledWith(1);
      expect(result.current.comments.find((c) => c.id === 1)).toBeUndefined();
      expect(onCommentDeleted).toHaveBeenCalledWith(10, 1, null, 3); // 1 + 2 replies
      expect(onCommentDeleteSuccess).toHaveBeenCalled();
    });

    it('removes a reply from its parent and decrements replyCount', async () => {
      getCommentsByPostApi.mockResolvedValueOnce(
        makePageResponse([{ id: 1, content: 'Parent', replyCount: 1 }])
      );
      getRepliesByCommentApi.mockResolvedValue(makePageResponse([REPLY_1]));
      deleteCommentApi.mockResolvedValue({ success: true });
      const { result } = renderCommentsHook();

      await act(async () => { await result.current.loadComments(); });
      await act(async () => { await result.current.handleLoadReplies(1); });

      let ok;
      await act(async () => {
        ok = await result.current.handleDeleteComment(101);
      });

      expect(ok).toBe(true);
      expect(result.current.repliesByCommentId[1]?.items?.find((r) => r.id === 101)).toBeUndefined();
      const parent = result.current.comments.find((c) => c.id === 1);
      expect(parent.replyCount).toBe(0);
      expect(onCommentDeleted).toHaveBeenCalledWith(10, 101, '1', 1);
    });

    it('returns false and calls onCommentError when deleteCommentApi throws', async () => {
      deleteCommentApi.mockRejectedValue({ response: { data: { message: 'Delete error' } } });
      const { result } = renderCommentsHook();

      let ok;
      await act(async () => {
        ok = await result.current.handleDeleteComment(1);
      });

      expect(ok).toBe(false);
      expect(onCommentError).toHaveBeenCalledWith('Delete error');
    });

    it('returns false and calls onCommentError when API returns success:false', async () => {
      deleteCommentApi.mockResolvedValue({ success: false, message: 'Forbidden' });
      const { result } = renderCommentsHook();

      let ok;
      await act(async () => {
        ok = await result.current.handleDeleteComment(1);
      });

      expect(ok).toBe(false);
      expect(onCommentError).toHaveBeenCalledWith('Forbidden');
    });
  });

  describe('handleLikeComment', () => {
    it('calls likeCommentApi when nextLiked=true and returns true', async () => {
      likeCommentApi.mockResolvedValue({ success: true });
      const { result } = renderCommentsHook();

      let ok;
      await act(async () => {
        ok = await result.current.handleLikeComment(4, true);
      });

      expect(likeCommentApi).toHaveBeenCalledWith(4);
      expect(unlikeCommentApi).not.toHaveBeenCalled();
      expect(ok).toBe(true);
    });

    it('calls unlikeCommentApi when nextLiked=false and returns true', async () => {
      unlikeCommentApi.mockResolvedValue({ success: true });
      const { result } = renderCommentsHook();

      let ok;
      await act(async () => {
        ok = await result.current.handleLikeComment(4, false);
      });

      expect(unlikeCommentApi).toHaveBeenCalledWith(4);
      expect(likeCommentApi).not.toHaveBeenCalled();
      expect(ok).toBe(true);
    });

    it('calls onCommentLikeError and returns false on success:false', async () => {
      likeCommentApi.mockResolvedValue({ success: false, message: 'Like failed' });
      const { result } = renderCommentsHook();

      await act(async () => {
        expect(await result.current.handleLikeComment(4, true)).toBe(false);
      });

      expect(onCommentLikeError).toHaveBeenCalledWith('Like failed');
    });

    it('calls onCommentLikeError and returns false when API throws', async () => {
      likeCommentApi.mockRejectedValue({ response: { data: { message: 'Error' } } });
      const { result } = renderCommentsHook();

      await act(async () => {
        expect(await result.current.handleLikeComment(4, true)).toBe(false);
      });

      expect(onCommentLikeError).toHaveBeenCalledWith('Error');
    });
  });
});
