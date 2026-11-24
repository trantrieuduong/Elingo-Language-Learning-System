import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  createCommentApi,
  getCommentsByPostApi,
  getPublishedPostsApi,
  getRepliesByCommentApi,
  getUserPostsApi,
  likeCommentApi,
  unlikeCommentApi,
  updateCommentApi,
} from '@/modules/community/communityApi';

vi.mock('@/services/apiClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

describe('communityApi', () => {
  beforeEach(() => vi.clearAllMocks());

  it('builds published and user post query parameters without empty filters', async () => {
    apiClient.get.mockResolvedValue({ data: { content: [] } });

    await getPublishedPostsApi({ cursor: 'next', limit: 5, groupId: 2 });
    await getPublishedPostsApi({ groupId: 'all' });
    await getUserPostsApi(7, { cursor: 'cursor', limit: 10 });

    expect(apiClient.get).toHaveBeenNthCalledWith(1, '/posts', {
      params: { limit: 5, cursor: 'next', groupId: 2 },
    });
    expect(apiClient.get).toHaveBeenNthCalledWith(2, '/posts', { params: { limit: 20 } });
    expect(apiClient.get).toHaveBeenNthCalledWith(3, '/users/7/posts', {
      params: { limit: 10, cursor: 'cursor' },
    });
  });

  it('creates comments with optional parent and image fields only when supplied', async () => {
    apiClient.post.mockResolvedValue({ data: { success: true } });
    await createCommentApi({ postId: 1, content: 'Root' });
    await createCommentApi({
      postId: 1,
      parentCommentId: 2,
      content: 'Reply',
      imageUrl: 'image-key',
    });

    expect(apiClient.post).toHaveBeenNthCalledWith(1, '/comments', { postId: 1, content: 'Root' });
    expect(apiClient.post).toHaveBeenNthCalledWith(2, '/comments', {
      postId: 1,
      parentCommentId: 2,
      content: 'Reply',
      imageUrl: 'image-key',
    });
  });

  it('uses comment endpoints with their sort order and mutation verbs', async () => {
    apiClient.get.mockResolvedValue({ data: [] });
    apiClient.put.mockResolvedValue({ data: { success: true } });
    apiClient.post.mockResolvedValue({ data: { success: true } });
    apiClient.delete.mockResolvedValue({ data: { success: true } });

    await getCommentsByPostApi(3, { page: 2, size: 5 });
    await getRepliesByCommentApi(4);
    await updateCommentApi(4, { content: 'Edited', imageUrl: 'new-key' });
    await likeCommentApi(4);
    await unlikeCommentApi(4);

    expect(apiClient.get).toHaveBeenNthCalledWith(1, '/posts/3/comments', {
      params: { page: 2, size: 5, sort: 'createdAt,desc' },
    });
    expect(apiClient.get).toHaveBeenNthCalledWith(2, '/comments/4/replies', {
      params: { page: 1, size: 20, sort: 'createdAt,asc' },
    });
    expect(apiClient.put).toHaveBeenCalledWith('/comments/4', {
      content: 'Edited',
      imageUrl: 'new-key',
    });
    expect(apiClient.post).toHaveBeenCalledWith('/comments/4/like');
    expect(apiClient.delete).toHaveBeenCalledWith('/comments/4/like');
  });
});
