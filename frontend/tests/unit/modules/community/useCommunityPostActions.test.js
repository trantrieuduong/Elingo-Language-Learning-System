import { beforeEach, describe, expect, it, vi } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import useCommunityPostActions from '@/modules/community/hooks/useCommunityPostActions';
import { likePostApi, unlikePostApi } from '@/modules/community/communityApi';

vi.mock('@/modules/community/communityApi', () => ({
  likePostApi: vi.fn(),
  unlikePostApi: vi.fn(),
}));

describe('useCommunityPostActions', () => {
  const makePost = (overrides = {}) => ({
    id: 'post-1',
    likedByMe: false,
    likeCount: 5,
    ...overrides,
  });

  let setPosts;
  let onLikeError;

  beforeEach(() => {
    vi.clearAllMocks();
    setPosts = vi.fn();
    onLikeError = vi.fn();
  });

  const renderActionsHook = () =>
    renderHook(() => useCommunityPostActions({ onLikeError, setPosts }));

  describe('handleLikePost', () => {
    describe('when liking (nextLiked = true)', () => {
      it('calls likePostApi and updates the post optimistically', async () => {
        likePostApi.mockResolvedValue({ success: true });

        const { result } = renderActionsHook();
        let returnValue;

        await act(async () => {
          returnValue = await result.current.handleLikePost('post-1', true);
        });

        expect(likePostApi).toHaveBeenCalledWith('post-1');
        expect(unlikePostApi).not.toHaveBeenCalled();
        expect(returnValue).toBe(true);

        // Verify the updater passed to setPosts produces the correct next state
        const updater = setPosts.mock.calls[0][0];
        const previousPosts = [makePost({ id: 'post-1', likedByMe: false, likeCount: 5 })];
        const nextPosts = updater(previousPosts);
        expect(nextPosts).toEqual([{ id: 'post-1', likedByMe: true, likeCount: 6 }]);
      });

      it('leaves unrelated posts unchanged', async () => {
        likePostApi.mockResolvedValue({ success: true });
        const { result } = renderActionsHook();

        await act(async () => {
          await result.current.handleLikePost('post-1', true);
        });

        const updater = setPosts.mock.calls[0][0];
        const other = makePost({ id: 'post-2', likedByMe: false, likeCount: 10 });
        const nextPosts = updater([makePost({ id: 'post-1' }), other]);
        expect(nextPosts[1]).toEqual(other);
      });
    });

    describe('when unliking (nextLiked = false)', () => {
      it('calls unlikePostApi and decrements likeCount', async () => {
        unlikePostApi.mockResolvedValue({ success: true });

        const { result } = renderActionsHook();
        let returnValue;

        await act(async () => {
          returnValue = await result.current.handleLikePost('post-1', false);
        });

        expect(unlikePostApi).toHaveBeenCalledWith('post-1');
        expect(likePostApi).not.toHaveBeenCalled();
        expect(returnValue).toBe(true);

        const updater = setPosts.mock.calls[0][0];
        const previousPosts = [makePost({ id: 'post-1', likedByMe: true, likeCount: 5 })];
        const nextPosts = updater(previousPosts);
        expect(nextPosts).toEqual([{ id: 'post-1', likedByMe: false, likeCount: 4 }]);
      });
    });

    describe('when API throws', () => {
      it('calls onLikeError with the thrown error and returns false', async () => {
        const networkError = new Error('Network error');
        likePostApi.mockRejectedValue(networkError);

        const { result } = renderActionsHook();
        let returnValue;

        await act(async () => {
          returnValue = await result.current.handleLikePost('post-1', true);
        });

        expect(onLikeError).toHaveBeenCalledWith(networkError);
        expect(returnValue).toBe(false);
        // setPosts must NOT be called on failure
        expect(setPosts).not.toHaveBeenCalled();
      });

      it('calls onLikeError and returns false when unlikePostApi throws', async () => {
        const networkError = new Error('Unlike failed');
        unlikePostApi.mockRejectedValue(networkError);

        const { result } = renderActionsHook();
        let returnValue;

        await act(async () => {
          returnValue = await result.current.handleLikePost('post-1', false);
        });

        expect(onLikeError).toHaveBeenCalledWith(networkError);
        expect(returnValue).toBe(false);
        expect(setPosts).not.toHaveBeenCalled();
      });
    });
  });
});
