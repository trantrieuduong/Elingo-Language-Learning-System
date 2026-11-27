import apiClient from '../../../services/apiClient';
import { fetchPagedAdminResource } from '../utils/adminApiUtils';

/**
 * Tìm kiếm và lọc danh sách người dùng với phân trang 1-indexed
 * @param {Object} params - { keyword, role, isVerified, isActive, createdFrom, createdTo, page, size, sort }
 * @returns {Promise<Object>} PageResponse<AdminUserResponse>
 */
export const searchUsers = (params = {}) =>
  fetchPagedAdminResource('/admin/users', params, {
    keyword: 'string',
    role: 'string',
    isVerified: 'boolean',
    isActive: 'boolean',
    createdFrom: 'truthy',
    createdTo: 'truthy',
  });

/**
 * Lấy chi tiết thông tin hồ sơ của một người dùng theo ID
 * @param {number|string} userId
 * @returns {Promise<Object>} AdminUserResponse
 */
export const getUserById = async (userId) => {
  const res = await apiClient.get(`/admin/users/${userId}`);
  return res.data?.data;
};

/**
 * Khóa tài khoản người dùng kèm lý do bắt buộc
 * @param {number|string} userId
 * @param {string} reason - Lý do khóa tài khoản (tối đa 500 ký tự)
 * @returns {Promise<Object>} AdminUserResponse
 */
export const lockUser = async (userId, reason) => {
  const res = await apiClient.put(`/admin/users/${userId}/lock`, {
    reason: reason ? reason.trim() : '',
  });
  return res.data?.data;
};

/**
 * Mở khóa và kích hoạt lại tài khoản người dùng
 * @param {number|string} userId
 * @returns {Promise<Object>} AdminUserResponse
 */
export const unlockUser = async (userId) => {
  const res = await apiClient.put(`/admin/users/${userId}/unlock`);
  return res.data?.data;
};
