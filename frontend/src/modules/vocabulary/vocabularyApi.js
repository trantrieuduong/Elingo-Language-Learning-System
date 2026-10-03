import apiClient from '../../services/apiClient'

/**
 * Lấy danh sách các decks published có phân trang và filter.
 * 
 * @param {Object} params - { q, tagId, cefrLevelId, page, limit }
 * @returns {Promise<Object>} Backend response: { success, data, message, code }
 */
export const getDecksApi = async (params = {}) => {
  const response = await apiClient.get('/v1/decks', { params })//thừa v1
  return response.data
}
