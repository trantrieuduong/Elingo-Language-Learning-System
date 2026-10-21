import apiClient from '../../services/apiClient'
import {
  getUserDecksMock,
  createUserDeckMock,
  updateUserDeckMock,
  deleteUserDeckMock,
} from './userDeckMockData'
import {
  getTopicsMock,
  createTopicMock,
  updateTopicMock,
  deleteTopicMock,
} from './topicMockData'

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

/**
 * Lấy danh sách bộ từ vựng do người dùng tự tạo (User Decks).
 * Tự động dùng mock data để phục vụ kiểm thử và thiết kế UI.
 *
 * @param {Object} params - { keyword, status, tagCode, cefrCode, page, limit }
 */
export const getUserDecksApi = async (params = {}) => {
  try {
    // Nếu backend có endpoint riêng, gọi endpoint; fallback về mock
    const response = await apiClient.get('/decks/my-decks', { params })
    return response.data
  } catch {
    return getUserDecksMock(params)
  }
}

/**
 * Tạo mới bộ từ vựng của người dùng (CreateDeckRequest).
 *
 * @param {Object} data - { title, slug, description, coverImageUrl, tagIds, cefrLevelIds }
 */
export const createUserDeckApi = async (data) => {
  try {
    const response = await apiClient.post('/decks', data)
    return response.data
  } catch {
    return createUserDeckMock(data)
  }
}

/**
 * Cập nhật bộ từ vựng của người dùng (UpdateDeckRequest).
 *
 * @param {number|string} deckId - ID của deck cần sửa
 * @param {Object} data - { title, slug, description, coverImageUrl, status, tagIds, cefrLevelIds }
 */
export const updateUserDeckApi = async (deckId, data) => {
  try {
    const response = await apiClient.put(`/decks/${deckId}`, data)
    return response.data
  } catch {
    return updateUserDeckMock(deckId, data)
  }
}

/**
 * Xóa bộ từ vựng (Soft-delete nếu đã publish, Hard-delete nếu là draft).
 *
 * @param {number|string} deckId - ID của deck cần xóa
 */
export const deleteUserDeckApi = async (deckId) => {
  try {
    const response = await apiClient.delete(`/decks/${deckId}`)
    return response.data
  } catch {
    return deleteUserDeckMock(deckId)
  }
}

// ─────────────────────────────────────────────────────────────
// Topic API
// ─────────────────────────────────────────────────────────────

/**
 * Lấy danh sách chủ đề (topics) có phân trang và tìm kiếm.
 *
 * @param {Object} params - { deckId, keyword, page, limit }
 */
export const getTopicsApi = async ({ deckId, keyword = '', page = 1, limit = 9 } = {}) => {
  try {
    const response = await apiClient.get(`/decks/${deckId}/topics`)
    const topics = response.data?.data

    if (!Array.isArray(topics)) return response.data

    const normalizedKeyword = keyword.trim().toLowerCase()
    const filteredTopics = normalizedKeyword
      ? topics.filter(
          (topic) =>
            topic.name?.toLowerCase().includes(normalizedKeyword) ||
            topic.slug?.toLowerCase().includes(normalizedKeyword)
        )
      : topics
    const totalElements = filteredTopics.length
    const totalPages = Math.max(1, Math.ceil(totalElements / limit))
    const content = filteredTopics.slice((page - 1) * limit, page * limit)

    return {
      ...response.data,
      data: {
        content,
        totalElements,
        totalPages,
        number: page - 1,
        size: limit,
        first: page === 1,
        last: page >= totalPages,
      },
    }
  } catch {
    return getTopicsMock({ deckId, keyword, page, limit })
  }
}

/**
 * Tạo mới Topic dựa trên CreateTopicRequest.
 *
 * @param {Object} data - { deckId, name, slug }
 */
export const createTopicApi = async (data) => {
  try {
    const response = await apiClient.post('/topics', data)
    return response.data
  } catch {
    return createTopicMock(data)
  }
}

/**
 * Cập nhật Topic dựa trên UpdateTopicRequest.
 *
 * @param {number|string} topicId - ID của topic cần sửa
 * @param {Object} data - { name, slug, order }
 */
export const updateTopicApi = async (topicId, data) => {
  try {
    const response = await apiClient.put(`/topics/${topicId}`, data)
    return response.data
  } catch {
    return updateTopicMock(topicId, data)
  }
}

/**
 * Xóa Topic.
 *
 * @param {number|string} topicId - ID của topic cần xóa
 */
export const deleteTopicApi = async (topicId) => {
  try {
    const response = await apiClient.delete(`/topics/${topicId}`)
    return response.data
  } catch {
    return deleteTopicMock(topicId)
  }
}
