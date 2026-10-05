import apiClient from '../../../services/apiClient'
import { MOCK_REVIEW_CARDS } from './mockReviewCards'

/**
 * Lấy danh sách topics của một deck công khai.
 * @param {string} deckId - ID của deck
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
 * @param {string} cardId
 * @param {number} grade - Điểm đánh giá (0, 1, 2, 3)
 */
export const submitSrsReviewApi = async (cardId, grade) => {
  const response = await apiClient.patch(`/flashcards/${cardId}/srs`, { grade })
  return response.data
}

/**
 * @param {string} cardId
 */
export const toggleStarApi = async (cardId) => {
  const response = await apiClient.patch(`/flashcards/${cardId}/stars`)
  return response.data
}

/**
 * @param {string} cardId
 */
export const toggleHideApi = async (cardId) => {
  const response = await apiClient.patch(`/flashcards/${cardId}/hidden`)
  return response.data
}

/**
 * Lấy danh sách flashcard đến hạn ôn tập (Spaced Repetition System) - Mock API call.
 * @param {Object} [params] - { limit }
 */
export const getCardsForReviewApi = async (_params = {}) => {
  // Giả lập độ trễ mạng khi gọi API
  await new Promise((resolve) => setTimeout(resolve, 350))

  return {
    success: true,
    data: MOCK_REVIEW_CARDS,
    message: 'Tải danh sách flashcard đến hạn ôn tập thành công'
  }
}
