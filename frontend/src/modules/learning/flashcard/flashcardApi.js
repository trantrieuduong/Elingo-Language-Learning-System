import apiClient from '../../../services/apiClient';

export const getTopicsByDeckSlugApi = async (deckSlug) => {
  const response = await apiClient.get(`/decks/slug/${encodeURIComponent(deckSlug)}/topics`);
  return response.data;
};

export const getUnlearnedCardsByTopicIdApi = async (topicId) => {
  const response = await apiClient.get(`/topics/${topicId}/cards`);
  return response.data;
};

/**
 * @param {number} grade - Điểm đánh giá SRS SM-2 (0, 1, 2, 3)
 */
export const submitSrsReviewApi = async (cardId, grade) => {
  const response = await apiClient.post(`/flashcards/${cardId}/srs`, { grade });
  return response.data;
};

/**
 * @param {boolean} starred
 */
export const toggleStarApi = async (cardId, starred = true) => {
  const response = await apiClient.put('/flashcards/stars', { cardId, starred });
  return response.data;
};

/**
 * @param {boolean} hidden
 */
export const toggleHideApi = async (cardId, hidden = true) => {
  const response = await apiClient.put('/flashcards/hidden', { cardId, hidden });
  return response.data;
};

/**
 * @param {number} [limit=100]
 */
export const getCardsForReviewApi = async (limit = 100) => {
  const response = await apiClient.get('/flashcards/due', { params: { limit } });
  return response.data;
};
