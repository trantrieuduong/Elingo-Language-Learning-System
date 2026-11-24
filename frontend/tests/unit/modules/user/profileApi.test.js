import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  getMyProfileApi,
  getUserByIdApi,
  updateProfileApi,
  sendOtpUpdateEmailApi,
  updateEmailApi,
  setPasswordApi,
  changePasswordApi,
} from '@/modules/user/profileApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
  },
}));

describe('profileApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getMyProfileApi', () => {
    it('calls GET /users/me and returns response.data', async () => {
      const mockResponse = { data: { success: true, data: { id: 1, username: 'testuser' } } };
      apiClient.get.mockResolvedValue(mockResponse);

      const result = await getMyProfileApi();

      expect(apiClient.get).toHaveBeenCalledWith('/users/me');
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('getUserByIdApi', () => {
    it('calls GET /users/:userId and returns response.data', async () => {
      const mockResponse = { data: { success: true, data: { id: 2, username: 'otheruser' } } };
      apiClient.get.mockResolvedValue(mockResponse);

      const result = await getUserByIdApi(2);

      expect(apiClient.get).toHaveBeenCalledWith('/users/2');
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('updateProfileApi', () => {
    it('calls PUT /users/me with payload and returns response.data', async () => {
      const mockResponse = { data: { success: true, data: { fullName: 'New Name' } } };
      apiClient.put.mockResolvedValue(mockResponse);

      const payload = { fullName: 'New Name', username: 'newusername' };
      const result = await updateProfileApi(payload);

      expect(apiClient.put).toHaveBeenCalledWith('/users/me', payload);
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('sendOtpUpdateEmailApi', () => {
    it('calls POST /users/me/email/otp with newEmail and returns response.data', async () => {
      const mockResponse = { data: { success: true, message: 'OTP sent' } };
      apiClient.post.mockResolvedValue(mockResponse);

      const result = await sendOtpUpdateEmailApi('new@example.com');

      expect(apiClient.post).toHaveBeenCalledWith('/users/me/email/otp', { newEmail: 'new@example.com' });
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('updateEmailApi', () => {
    it('calls PATCH /users/me/email with correct payload and returns response.data', async () => {
      const mockResponse = { data: { success: true, message: 'Email updated' } };
      apiClient.patch.mockResolvedValue(mockResponse);

      const payload = {
        newEmail: 'new@example.com',
        newEmailOtp: '123456',
        password: 'Password123@',
      };
      const result = await updateEmailApi(payload);

      expect(apiClient.patch).toHaveBeenCalledWith('/users/me/email', payload);
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('setPasswordApi', () => {
    it('calls POST /users/me/password with newPassword and returns response.data', async () => {
      const mockResponse = { data: { success: true, message: 'Password set' } };
      apiClient.post.mockResolvedValue(mockResponse);

      const result = await setPasswordApi('StrongPassword1!');

      expect(apiClient.post).toHaveBeenCalledWith('/users/me/password', { newPassword: 'StrongPassword1!' });
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('changePasswordApi', () => {
    it('calls PATCH /users/me/password with old and new passwords and returns response.data', async () => {
      const mockResponse = { data: { success: true, message: 'Password changed' } };
      apiClient.patch.mockResolvedValue(mockResponse);

      const payload = {
        oldPassword: 'OldPassword1!',
        newPassword: 'NewPassword2!',
      };
      const result = await changePasswordApi(payload);

      expect(apiClient.patch).toHaveBeenCalledWith('/users/me/password', payload);
      expect(result).toEqual(mockResponse.data);
    });
  });
});
