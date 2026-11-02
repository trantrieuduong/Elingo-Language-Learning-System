import apiClient from '../../services/apiClient';

export const getPostGroups = async () => {
  const response = await apiClient.get('/post-groups');
  return response.data;
};

export const createPost = async ({ groupId, title, content, mediaUrls = [] }) => {
  const response = await apiClient.post('/posts', { groupId, title, content, mediaUrls });
  return response.data;
};

export const getPublishedPosts = async ({ cursor, limit = 20, groupId } = {}) => {
  const params = { limit };
  if (cursor) params.cursor = cursor;
  if (groupId && groupId !== 'all') params.groupId = groupId;
  const response = await apiClient.get('/posts', { params });
  return response.data;
};

export const getUserPosts = async (userId, { cursor, limit = 20 } = {}) => {
  const params = { limit };
  if (cursor) params.cursor = cursor;
  const response = await apiClient.get(`/users/${userId}/posts`, { params });
  return response.data;
};

export const likePost = async (postId) => {
  const response = await apiClient.post(`/posts/${postId}/like`);
  return response.data;
};

export const unlikePost = async (postId) => {
  const response = await apiClient.delete(`/posts/${postId}/like`);
  return response.data;
};
