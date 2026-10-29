import apiClient from '../../../services/apiClient';

/**
 * Lấy danh sách bài học đã công khai (PUBLISHED) có phân trang và bộ lọc.
 * Endpoint: GET /lessons
 *
 * @param {Object} [params={}] - Tham số tìm kiếm và lọc
 * @param {string} [params.keyword] - Từ khóa tìm kiếm theo tiêu đề bài học
 * @param {number[]} [params.cefrLevelIds] - Danh sách id CEFR level
 * @param {number[]} [params.tagIds] - Danh sách id tag
 * @param {number} [params.page=1] - Số trang (1-based index)
 * @returns {Promise<Object>} Object chứa response.data ({ content, page, size, totalElements, totalPages, isLast })
 */
export const getLessonsApi = async (params = {}) => {
  const cleanParams = {};
  if (params.keyword !== undefined && params.keyword !== '') cleanParams.keyword = params.keyword;
  if (Array.isArray(params.cefrLevelIds) && params.cefrLevelIds.length > 0) {
    cleanParams.cefrLevelIds = params.cefrLevelIds;
  }
  if (Array.isArray(params.tagIds) && params.tagIds.length > 0) {
    cleanParams.tagIds = params.tagIds;
  }
  if (params.page !== undefined) cleanParams.page = params.page;

  const response = await apiClient.get('/lessons', { params: cleanParams });
  return response.data;
};

/**
 * Lấy thông tin chi tiết một bài học theo slug.
 * Endpoint: GET /lessons/slug/{slug}
 *
 * @param {string} slug - Slug định danh của bài học
 * @returns {Promise<Object>} Object chứa response.data ({ id, title, slug, description, ... })
 */
export const getLessonDetailApi = async (slug) => {
  const response = await apiClient.get(`/lessons/slug/${encodeURIComponent(slug)}`);
  return response.data;
};

/**
 * Lấy danh sách các phân đoạn (segments) kèm tiến độ học của bài học theo slug.
 * Endpoint: GET /lessons/slug/{slug}/segments
 *
 * @param {string} slug - Slug định danh của bài học
 * @returns {Promise<Object>} Object chứa response.data (mảng các Segment)
 */
export const getLessonSegmentsApi = async (slug) => {
  const response = await apiClient.get(`/lessons/slug/${encodeURIComponent(slug)}/segments`);
  return response.data;
};
