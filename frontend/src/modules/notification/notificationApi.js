import apiClient from '../../services/apiClient';

/**
 * @param {Object} params - { page, size }
 */
export const getNotificationsApi = async (params = {}) => {
  const response = await apiClient.get('/notifications', { params });
  return response.data;
};

export const getUnreadNotificationCountApi = async () => {
  const response = await apiClient.get('/notifications/unread-count');
  return response.data;
};

export const markNotificationReadApi = async (id) => {
  const response = await apiClient.patch(`/notifications/${id}/read`);
  return response.data;
};

export const markAllNotificationsReadApi = async () => {
  const response = await apiClient.patch('/notifications/read-all');
  return response.data;
};
