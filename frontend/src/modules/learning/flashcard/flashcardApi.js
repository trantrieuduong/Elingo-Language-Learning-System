import apiClient from '../../../services/apiClient'

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