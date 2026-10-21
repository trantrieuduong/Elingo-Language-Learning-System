import apiClient from '../../../services/apiClient'

/**
 * Lấy danh sách topics của một deck công khai.
 * @param {string|number} deckId - ID của deck
 */
export const getTopicsByDeckIdApi = async (deckId) => {
  const response = await apiClient.get(`/decks/${deckId}/topics`)
  return response.data
}

/**
 * @param {string} topicId - ID của topic
 */
export const getUnlearnedCardsByTopicIdApi = async (topicId) => {
  const response = await apiClient.get(`/topics/${topicId}/cards`)
  return response.data
}

/**
 * @param {string|number} cardId
 * @param {number} grade - Điểm đánh giá SRS SM-2 (0, 1, 2, 3)
 */
export const submitSrsReviewApi = async (cardId, grade) => {
  const response = await apiClient.post(`/flashcards/${cardId}/srs`, { grade })
  return response.data
}

/**
 * Cập nhật trạng thái gắn sao cho thẻ
 * @param {string|number} cardId
 * @param {boolean} starred
 */
export const toggleStarApi = async (cardId, starred = true) => {
  const response = await apiClient.put('/flashcards/stars', { cardId, starred })
  return response.data
}

/**
 * Cập nhật trạng thái ẩn thẻ
 * @param {string|number} cardId
 * @param {boolean} hidden
 */
export const toggleHideApi = async (cardId, hidden = true) => {
  const response = await apiClient.put('/flashcards/hidden', { cardId, hidden })
  return response.data
}

/**
 * Lấy danh sách flashcard đến hạn ôn tập (Spaced Repetition System).
 * @param {number} [limit=100]
 */
export const getCardsForReviewApi = async (limit = 100) => {
  const response = await apiClient.get('/flashcards/due', { params: { limit } })
  return response.data
}

