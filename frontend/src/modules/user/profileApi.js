import apiClient from '../../services/apiClient';

export const getMyProfileApi = async () => {
  const response = await apiClient.get('/users/me');
  return response.data;
};

export const getUserByIdApi = async (userId) => {
  const response = await apiClient.get(`/users/${userId}`);
  return response.data;
};

export const updateProfileApi = async (payload) => {
  const response = await apiClient.put('/users/me', payload);
  return response.data;
};

export const sendOtpUpdateEmailApi = async (newEmail) => {
  const response = await apiClient.post('/users/me/email/otp', { newEmail });
  return response.data;
};

export const updateEmailApi = async ({ newEmail, newEmailOtp, password }) => {
  const response = await apiClient.patch('/users/me/email', {
    newEmail,
    newEmailOtp,
    password,
  });
  return response.data;
};

export const setPasswordApi = async (newPassword) => {
  const response = await apiClient.post('/users/me/password', { newPassword });
  return response.data;
};

export const changePasswordApi = async ({ oldPassword, newPassword }) => {
  const response = await apiClient.patch('/users/me/password', { oldPassword, newPassword });
  return response.data;
};
