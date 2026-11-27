import apiClient from '../../../services/apiClient';
import { fetchPagedAdminResource } from '../utils/adminApiUtils';

/**
 * Lấy danh sách nhật ký hoạt động của admin có phân trang và bộ lọc
 * @param {Object} params - { adminId, action, targetType, targetId, from, to, page, size, sort }
 * @returns {Promise<Object>} PageResponse<AdminAuditLogResponse>
 */
export const fetchAuditLogs = (params = {}) =>
  fetchPagedAdminResource('/admin/audit-logs', params, {
    adminId: 'truthy',
    action: 'string',
    targetType: 'string',
    targetId: 'string',
    from: 'truthy',
    to: 'truthy',
  });

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
