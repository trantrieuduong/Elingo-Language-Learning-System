import apiClient from '../../../services/apiClient';

/**
 * Lấy danh sách cấu hình hệ thống (có thể lọc theo module)
 * @param {string} [module] - Tên phân hệ (PREMIUM, COMMUNITY, GAMIFICATION...)
 * @returns {Promise<Array>} Danh sách SystemConfigResponse
 */
export const fetchSystemConfigs = async (module) => {
  const params = {};
  if (module && module.trim()) {
    params.module = module.trim();
  }
  const res = await apiClient.get('/admin/system-configs', { params });
  return res.data?.data || [];
};

/**
 * Lấy danh sách tên tất cả các phân hệ cấu hình
 * @returns {Promise<Array<string>>} Danh sách tên module
 */
export const fetchConfigModules = async () => {
  const res = await apiClient.get('/admin/system-configs/config-module');
  return res.data?.data || [];
};

/**
 * Cập nhật cấu hình hệ thống theo key
 * @param {string} configKey - Mã cấu hình
 * @param {Object} payload - { value: string, description?: string }
 * @returns {Promise<Object>} SystemConfigResponse sau khi cập nhật
 */
export const updateSystemConfig = async (configKey, payload) => {
  const res = await apiClient.put(
    `/admin/system-configs/${encodeURIComponent(configKey)}`,
    payload
  );
  return res.data?.data;
};
