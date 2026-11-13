import apiClient from '../../services/apiClient';

export const getPremiumPlansApi = async () => {
  const response = await apiClient.get('/premium/plans');
  return response.data;
};

export const createPremiumCheckoutApi = async (planId) => {
  const response = await apiClient.post('/premium/checkout', { planId });
  return response.data;
};

export const getPremiumTransactionStatusApi = async (transactionId) => {
  const response = await apiClient.get(`/premium/transactions/${transactionId}`);
  return response.data;
};
