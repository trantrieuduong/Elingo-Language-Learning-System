import apiClient from '../../../services/apiClient';

/**
 * Tìm kiếm và lọc danh sách người dùng với phân trang 1-indexed
 * @param {Object} params - { keyword, role, isVerified, isActive, createdFrom, createdTo, page, size, sort }
 * @returns {Promise<Object>} PageResponse<AdminUserResponse>
 */
export const searchUsers = async (params = {}) => {
  const query = {};

  if (params.keyword && params.keyword.trim()) {
    query.keyword = params.keyword.trim();
  }
  if (params.role && params.role.trim()) {
    query.role = params.role.trim();
  }
  if (params.isVerified !== undefined && params.isVerified !== null && params.isVerified !== '') {
    query.isVerified = params.isVerified === true || params.isVerified === 'true';
  }
  if (params.isActive !== undefined && params.isActive !== null && params.isActive !== '') {
    query.isActive = params.isActive === true || params.isActive === 'true';
  }
  if (params.createdFrom) {
    query.createdFrom = params.createdFrom;
  }
  if (params.createdTo) {
    query.createdTo = params.createdTo;
  }
  if (params.page !== undefined && params.page !== null) {
    query.page = params.page;
  }
  if (params.size !== undefined && params.size !== null) {
    query.size = params.size;
  }
  if (params.sort) {
    query.sort = params.sort;
  }

  const res = await apiClient.get('/admin/users', { params: query });
  return res.data?.data || {
    content: [],
    page: 1,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    isLast: true,
  };
};

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
