import { beforeEach, describe, expect, it, vi } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import useCommunityComments from '@/modules/community/hooks/useCommunityComments';
import {
  createCommentApi,
  getCommentsByPostApi,
  likeCommentApi,
  unlikeCommentApi,
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

describe('useCommunityComments', () => {
  const onCommentCreated = vi.fn();
  const onCommentError = vi.fn();
  const onCommentLikeError = vi.fn();
  const renderCommentsHook = () =>
    renderHook(() =>
      useCommunityComments({ postId: 10, onCommentCreated, onCommentError, onCommentLikeError })
    );

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads the first comment page and exposes pagination state', async () => {
    getCommentsByPostApi.mockResolvedValue({
      success: true,
      data: { content: [{ id: 1, content: 'First' }], isLast: false },
    });
    const { result } = renderCommentsHook();

    await act(async () => {
      await result.current.loadComments();
    });

    expect(getCommentsByPostApi).toHaveBeenCalledWith(10, { page: 1, size: 20 });
    expect(result.current.comments).toEqual([{ id: 1, content: 'First' }]);
    expect(result.current.hasMore).toBe(true);
  });

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

  it('reports create and like failures and selects the correct like endpoint', async () => {
    createCommentApi.mockResolvedValue({ success: false, message: 'Create failed' });
    likeCommentApi.mockResolvedValue({ success: false, message: 'Like failed' });
    unlikeCommentApi.mockResolvedValue({ success: true });
    const { result } = renderCommentsHook();

    await act(async () => {
      expect(await result.current.handleCreateComment('New')).toBe(false);
    });
    await act(async () => {
      expect(await result.current.handleLikeComment(4, true)).toBe(false);
    });
    await act(async () => {
      expect(await result.current.handleLikeComment(4, false)).toBe(true);
    });

    expect(onCommentError).toHaveBeenCalledWith('Create failed');
    expect(onCommentLikeError).toHaveBeenCalledWith('Like failed');
    expect(likeCommentApi).toHaveBeenCalledWith(4);
    expect(unlikeCommentApi).toHaveBeenCalledWith(4);
  });
});
