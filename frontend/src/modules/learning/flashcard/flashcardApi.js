import apiClient from '../../../services/apiClient'

/**
 * Lấy danh sách topics của một deck.
 * @param {string} deckId - ID của deck
 */
export const getTopicsByDeckIdApi = async (deckId) => {
  const response = await apiClient.get(`/decks/${deckId}/topics`)
  return response.data
}