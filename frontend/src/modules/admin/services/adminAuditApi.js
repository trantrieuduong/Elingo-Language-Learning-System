import apiClient from '../../../services/apiClient';

/**
 * Lấy danh sách nhật ký hoạt động của admin có phân trang và bộ lọc
 * @param {Object} params - { adminId, action, targetType, targetId, from, to, page, size, sort }
 * @returns {Promise<Object>} PageResponse<AdminAuditLogResponse>
 */
export const fetchAuditLogs = async (params = {}) => {
  const query = {};

  if (params.adminId) {
    query.adminId = params.adminId;
  }
  if (params.action && params.action.trim()) {
    query.action = params.action.trim();
  }
  if (params.targetType && params.targetType.trim()) {
    query.targetType = params.targetType.trim();
  }
  if (params.targetId && params.targetId.trim()) {
    query.targetId = params.targetId.trim();
  }
  if (params.from) {
    query.from = params.from;
  }
  if (params.to) {
    query.to = params.to;
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

  const res = await apiClient.get('/admin/audit-logs', { params: query });
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
 * Lấy danh mục tất cả hành động AdminAuditAction
 * @returns {Promise<Array<string>>} Danh sách tên hành động
 */
export const fetchAuditActions = async () => {
  const res = await apiClient.get('/admin/audit-logs/actions');
  return res.data?.data || [];
};

/**
 * Lấy danh mục tất cả loại đối tượng AdminAuditTargetType
 * @returns {Promise<Array<string>>} Danh sách loại đối tượng
 */
export const fetchAuditTargetTypes = async () => {
  const res = await apiClient.get('/admin/audit-logs/target-types');
  return res.data?.data || [];
};
