import { describe, it, expect, vi, beforeEach } from 'vitest'
import apiClient from '@/services/apiClient'
import {
  getTopicsByDeckSlugApi,
  getUnlearnedCardsByTopicIdApi,
  submitSrsReviewApi,
  toggleStarApi,
  toggleHideApi,
  getCardsForReviewApi,
} from '@/modules/learning/flashcard/flashcardApi'

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

describe('flashcardApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getTopicsByDeckSlugApi', () => {
    it('calls GET /decks/slug/:deckSlug/topics with encoded slug and returns response.data', async () => {
      const mockRes = { success: true, data: [{ id: 1, name: 'Basics' }] }
      apiClient.get.mockResolvedValue({ data: mockRes })

      const res = await getTopicsByDeckSlugApi('ielts core/vocab')

      expect(apiClient.get).toHaveBeenCalledTimes(1)
      expect(apiClient.get).toHaveBeenCalledWith('/decks/slug/ielts%20core%2Fvocab/topics')
      expect(res).toEqual(mockRes)
    })
  })

  describe('getUnlearnedCardsByTopicIdApi', () => {
    it('calls GET /topics/:topicId/cards and returns response.data', async () => {
      const mockRes = { success: true, data: [{ id: 10, term: 'hello' }] }
      apiClient.get.mockResolvedValue({ data: mockRes })

      const res = await getUnlearnedCardsByTopicIdApi(42)

      expect(apiClient.get).toHaveBeenCalledTimes(1)
      expect(apiClient.get).toHaveBeenCalledWith('/topics/42/cards')
      expect(res).toEqual(mockRes)
    })
  })

  describe('submitSrsReviewApi', () => {
    it('calls POST /flashcards/:cardId/srs with grade payload and returns response.data', async () => {
      const mockRes = { success: true, data: {} }
      apiClient.post.mockResolvedValue({ data: mockRes })

      const res = await submitSrsReviewApi(99, 2)

      expect(apiClient.post).toHaveBeenCalledTimes(1)
      expect(apiClient.post).toHaveBeenCalledWith('/flashcards/99/srs', { grade: 2 })
      expect(res).toEqual(mockRes)
    })
  })

  describe('toggleStarApi', () => {
    it('calls PUT /flashcards/stars with default starred=true', async () => {
      const mockRes = { success: true }
      apiClient.put.mockResolvedValue({ data: mockRes })

      const res = await toggleStarApi(15)

      expect(apiClient.put).toHaveBeenCalledTimes(1)
      expect(apiClient.put).toHaveBeenCalledWith('/flashcards/stars', {
        cardId: 15,
        starred: true,
      })
      expect(res).toEqual(mockRes)
    })

    it('calls PUT /flashcards/stars with explicit starred=false', async () => {
      apiClient.put.mockResolvedValue({ data: { success: true } })

      await toggleStarApi(15, false)

      expect(apiClient.put).toHaveBeenCalledWith('/flashcards/stars', {
        cardId: 15,
        starred: false,
      })
    })
  })

  describe('toggleHideApi', () => {
    it('calls PUT /flashcards/hidden with default hidden=true', async () => {
      const mockRes = { success: true }
      apiClient.put.mockResolvedValue({ data: mockRes })

      const res = await toggleHideApi(21)

      expect(apiClient.put).toHaveBeenCalledTimes(1)
      expect(apiClient.put).toHaveBeenCalledWith('/flashcards/hidden', {
        cardId: 21,
        hidden: true,
      })
      expect(res).toEqual(mockRes)
    })

    it('calls PUT /flashcards/hidden with explicit hidden=false', async () => {
      apiClient.put.mockResolvedValue({ data: { success: true } })

      await toggleHideApi(21, false)

      expect(apiClient.put).toHaveBeenCalledWith('/flashcards/hidden', {
        cardId: 21,
        hidden: false,
      })
    })
  })

  describe('getCardsForReviewApi', () => {
    it('calls GET /flashcards/due with default limit=100', async () => {
      const mockRes = { success: true, data: [] }
      apiClient.get.mockResolvedValue({ data: mockRes })

      const res = await getCardsForReviewApi()

      expect(apiClient.get).toHaveBeenCalledTimes(1)
      expect(apiClient.get).toHaveBeenCalledWith('/flashcards/due', { params: { limit: 100 } })
      expect(res).toEqual(mockRes)
    })

    it('calls GET /flashcards/due with custom limit', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true, data: [] } })

      await getCardsForReviewApi(25)

      expect(apiClient.get).toHaveBeenCalledWith('/flashcards/due', { params: { limit: 25 } })
    })
  })
})
