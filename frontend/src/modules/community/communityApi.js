import apiClient from '../../services/apiClient';

export const getPostGroupsApi = async () => {
  const response = await apiClient.get('/post-groups');
  return response.data;
};

export const createPostApi = async ({ groupId, title, content, mediaUrls = [] }) => {
  const response = await apiClient.post('/posts', { groupId, title, content, mediaUrls });
  return response.data;
};

export const getPublishedPostsApi = async ({ cursor, limit = 20, groupId } = {}) => {
  const params = { limit };
  if (cursor) params.cursor = cursor;
  if (groupId && groupId !== 'all') params.groupId = groupId;
  const response = await apiClient.get('/posts', { params });
  return response.data;
};

export const getUserPostsApi = async (userId, { cursor, limit = 20 } = {}) => {
  const params = { limit };
  if (cursor) params.cursor = cursor;
  const response = await apiClient.get(`/users/${userId}/posts`, { params });
  return response.data;
};

export const likePostApi = async (postId) => {
  const response = await apiClient.post(`/posts/${postId}/like`);
  return response.data;
};

export const unlikePostApi = async (postId) => {
  const response = await apiClient.delete(`/posts/${postId}/like`);
  return response.data;
};

export const getPostByIdApi = async (postId) => {
  const response = await apiClient.get(`/posts/${postId}`);
  return response.data;
};

export const updatePostApi = async (postId, { title, content, mediaUrls = [] }) => {
  const response = await apiClient.put(`/posts/${postId}`, { title, content, mediaUrls });
  return response.data;
};
