import apiClient from '../../services/apiClient';

/**
 * @param {Object} data
 * @param {'POST'|'COMMENT'|'USER'|'DECK'|'LESSON'} data.targetType
 * @param {number|string} data.targetId
 * @param {string} data.description - 10 đến 1000 ký tự
 * @param {string[]} [data.imageUrls] - Tối đa 5 ảnh đã verify
 */
export async function createReportApi(data) {
  const response = await apiClient.post('/reports', {
    targetType: data.targetType,
    targetId: Number(data.targetId),
    description: data.description.trim(),
    imageUrls: data.imageUrls,
  });
  return response.data;
}

/**
 * @param {Object} [params] - Phân trang pageable - page, size
 */
export async function getMyReportsApi(params = {}) {
  const response = await apiClient.get('/reports', { params });
  return response.data;
}

export async function getMyReportApi(reportId) {
  const response = await apiClient.get(`/reports/${reportId}`);
  return response.data;
}
