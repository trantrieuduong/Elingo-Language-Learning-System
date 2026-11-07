import apiClient from '../../services/apiClient';

export const getPremiumPlansApi = async () => {
  const response = await apiClient.get('/premium/plans');
  return response.data;
};
