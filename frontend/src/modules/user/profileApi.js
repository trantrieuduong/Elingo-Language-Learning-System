import apiClient from '../../services/apiClient';

export const getMyProfileApi = async () => {
  const response = await apiClient.get('/users/me');
  return response.data;
};

export const getUserByIdApi = async (userId) => {
  const response = await apiClient.get(`/users/${userId}`);
  return response.data;
};
