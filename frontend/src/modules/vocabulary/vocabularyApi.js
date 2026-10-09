import apiClient from '../../services/apiClient'

/**
 * Lấy danh sách các decks published có phân trang và filter.
 * 
 * @param {Object} params - { keyword, tagCode, cefrCode, page }
 */
export const getDecksApi = async (params = {}) => {
  const response = await apiClient.get('/decks', { params })
  return response.data
}
export const getTagsApi = async () => {
  const response = await apiClient.get('/tags')
  return response.data
}

export const getCefrLevelsApi = async () => {
  const response = await apiClient.get('/cefr-levels')
  return response.data
}
