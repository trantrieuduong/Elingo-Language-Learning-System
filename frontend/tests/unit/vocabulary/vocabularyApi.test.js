import { describe, it, expect, vi, beforeEach } from 'vitest'
import apiClient from '@/services/apiClient'
import {
  getDecksApi,
  getDeckByIdApi,
  getTagsApi,
  getCefrLevelsApi,
  getUserDecksApi,
  createUserDeckApi,
  updateUserDeckApi,
  publishUserDeckApi,
  deleteUserDeckApi,
  getTopicsApi,
  createTopicApi,
  updateTopicApi,
  deleteTopicApi,
  getCardsByTopicApi,
  createUserCardApi,
  updateUserCardApi,
  deleteUserCardApi,
} from '@/modules/vocabulary/vocabularyApi'

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('vocabularyApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getDecksApi', () => {
    it('sends expected params to apiClient.get /decks and returns response.data', async () => {
      const mockRes = { success: true, data: { content: [], totalPages: 1 } }
      apiClient.get.mockResolvedValue({ data: mockRes })

      const res = await getDecksApi({
        keyword: '  ielts  ',
        tagIds: ['t1'],
        cefrLevelIds: ['a1'],
        page: 2,
        limit: 12,
        sort: 'newest',
        ownerType: 'SYSTEM',
      })

      expect(apiClient.get).toHaveBeenCalledWith('/decks', {
        params: {
          page: 2,
          size: 12,
          sort: 'newest',
          ownerType: 'SYSTEM',
          tagIds: ['t1'],
          cefrLevelIds: ['a1'],
          q: 'ielts',
        },
        paramsSerializer: { indexes: null },
      })
      expect(res).toEqual(mockRes)
    })

    it('omits empty or whitespace-only keyword from params', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true } })

      await getDecksApi({ keyword: '   ' })

      expect(apiClient.get).toHaveBeenCalledWith('/decks', {
        params: {
          page: 1,
          size: undefined,
          sort: undefined,
          ownerType: undefined,
          tagIds: undefined,
          cefrLevelIds: undefined,
        },
        paramsSerializer: { indexes: null },
      })
    })
  })

  describe('getDeckByIdApi', () => {
    it('calls GET /decks/:deckId and returns response.data', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true, data: {} } })
      const res = await getDeckByIdApi('deck-1')
      expect(apiClient.get).toHaveBeenCalledTimes(1)
      expect(apiClient.get).toHaveBeenCalledWith('/decks/deck-1')
      expect(res).toEqual({ success: true, data: {} })
    })
  })

  describe('getTagsApi', () => {
    it('calls GET /tags and returns response.data', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true, data: [] } })
      const res = await getTagsApi()
      expect(apiClient.get).toHaveBeenCalledTimes(1)
      expect(apiClient.get).toHaveBeenCalledWith('/tags')
      expect(res).toEqual({ success: true, data: [] })
    })
  })

  describe('getCefrLevelsApi', () => {
    it('calls GET /cefr-levels and returns response.data', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true, data: [] } })
      const res = await getCefrLevelsApi()
      expect(apiClient.get).toHaveBeenCalledTimes(1)
      expect(apiClient.get).toHaveBeenCalledWith('/cefr-levels')
      expect(res).toEqual({ success: true, data: [] })
    })
  })

  describe('getUserDecksApi', () => {
    const sampleDecks = [
      {
        id: '1',
        title: 'Travel Vocabulary',
        description: 'Words for trips',
        status: 'PUBLISHED',
        tags: [{ id: 't1' }],
        cefrLevels: [{ id: 'a1' }],
      },
      {
        id: '2',
        title: 'Business English',
        description: 'Office terms',
        status: 'DRAFT',
        tags: [{ id: 't2' }],
        cefrLevels: [{ id: 'b1' }],
      },
      {
        id: '3',
        title: 'Daily Conversation',
        description: 'Everyday travel phrases',
        status: 'PUBLISHED',
        tags: [{ id: 't1' }],
        cefrLevels: [{ id: 'a2' }],
      },
    ]

    beforeEach(() => {
      apiClient.get.mockResolvedValue({
        data: { success: true, data: sampleDecks },
      })
    })

    it('fetches /users/me/decks and returns all decks when no filters are provided', async () => {
      const res = await getUserDecksApi()
      expect(apiClient.get).toHaveBeenCalledWith('/users/me/decks')
      expect(res.data.content).toHaveLength(3)
      expect(res.data.totalElements).toBe(3)
      expect(res.data.totalPages).toBe(1)
    })

    it('filters by keyword matching title or description', async () => {
      const res = await getUserDecksApi({ keyword: 'travel' })
      expect(res.data.content.map((d) => d.id)).toEqual(['1', '3'])
    })

    it('filters by status PUBLISHED and DRAFT, and skips filtering when status is ALL', async () => {
      const published = await getUserDecksApi({ status: 'PUBLISHED' })
      expect(published.data.content.map((d) => d.id)).toEqual(['1', '3'])

      const draft = await getUserDecksApi({ status: 'DRAFT' })
      expect(draft.data.content.map((d) => d.id)).toEqual(['2'])

      const all = await getUserDecksApi({ status: 'ALL' })
      expect(all.data.content).toHaveLength(3)
    })

    it('filters by tagId and cefrLevelId', async () => {
      const byTag = await getUserDecksApi({ tagId: 't2' })
      expect(byTag.data.content.map((d) => d.id)).toEqual(['2'])

      const byCefr = await getUserDecksApi({ cefrLevelId: 'a2' })
      expect(byCefr.data.content.map((d) => d.id)).toEqual(['3'])
    })

    it('paginates results and returns correct content, totalPages, and totalElements for page 2', async () => {
      const res = await getUserDecksApi({ page: 2, limit: 2 })
      expect(res.data.content.map((d) => d.id)).toEqual(['3'])
      expect(res.data.totalElements).toBe(3)
      expect(res.data.totalPages).toBe(2)
      expect(res.data.page).toBe(2)
      expect(res.data.limit).toBe(2)
    })
  })

  describe('User Deck mutations', () => {
    it('createUserDeckApi trims title and description and sends null for empty description', async () => {
      apiClient.post.mockResolvedValue({ data: { success: true } })

      await createUserDeckApi({ title: '  My Deck  ', description: '   ' })
      expect(apiClient.post).toHaveBeenCalledWith('/decks', {
        title: 'My Deck',
        description: null,
      })

      await createUserDeckApi({ title: 'Deck 2', description: '  Desc  ' })
      expect(apiClient.post).toHaveBeenCalledWith('/decks', {
        title: 'Deck 2',
        description: 'Desc',
      })
    })

    it('updateUserDeckApi calls PUT /decks/:deckId with trimmed payload', async () => {
      apiClient.put.mockResolvedValue({ data: { success: true } })

      await updateUserDeckApi('deck-1', { title: ' New ', description: '' })
      expect(apiClient.put).toHaveBeenCalledWith('/decks/deck-1', {
        title: 'New',
        description: null,
      })
    })

    it('publishUserDeckApi coerces published to boolean and calls PUT /decks/:deckId/publication', async () => {
      apiClient.put.mockResolvedValue({ data: { success: true } })

      await publishUserDeckApi('deck-1', 1)
      expect(apiClient.put).toHaveBeenCalledWith('/decks/deck-1/publication', { published: true })

      await publishUserDeckApi('deck-1', false)
      expect(apiClient.put).toHaveBeenCalledWith('/decks/deck-1/publication', { published: false })
    })

    it('deleteUserDeckApi calls DELETE /decks/:deckId', async () => {
      apiClient.delete.mockResolvedValue({ data: { success: true } })
      const res = await deleteUserDeckApi('deck-1')
      expect(apiClient.delete).toHaveBeenCalledWith('/decks/deck-1')
      expect(res).toEqual({ success: true })
    })
  })

  describe('Topic APIs', () => {
    it('getTopicsApi calls GET /decks/:deckId/topics', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true, data: [] } })
      await getTopicsApi('10')
      expect(apiClient.get).toHaveBeenCalledWith('/decks/10/topics')
    })

    it('createTopicApi calls POST /topics with numeric deckId and trimmed name', async () => {
      apiClient.post.mockResolvedValue({ data: { success: true } })
      await createTopicApi({ deckId: '10', name: '  Animals  ' })
      expect(apiClient.post).toHaveBeenCalledWith('/topics', {
        deckId: 10,
        name: 'Animals',
      })
    })

    it('updateTopicApi calls PUT /topics/:topicId with trimmed name', async () => {
      apiClient.put.mockResolvedValue({ data: { success: true } })
      await updateTopicApi(5, { name: '  Nature  ' })
      expect(apiClient.put).toHaveBeenCalledWith('/topics/5', { name: 'Nature' })
    })

    it('deleteTopicApi calls DELETE /topics/:topicId', async () => {
      apiClient.delete.mockResolvedValue({ data: { success: true } })
      await deleteTopicApi(5)
      expect(apiClient.delete).toHaveBeenCalledWith('/topics/5')
    })
  })

  describe('Card APIs', () => {
    it('getCardsByTopicApi calls GET /topics/:topicId/cards', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true, data: [] } })
      await getCardsByTopicApi(3)
      expect(apiClient.get).toHaveBeenCalledWith('/topics/3/cards')
    })

    it('createUserCardApi calls POST /cards with numeric topicId and null for empty optional fields', async () => {
      apiClient.post.mockResolvedValue({ data: { success: true } })
      await createUserCardApi({
        topicId: '3',
        term: '  hello  ',
        pos: '  ',
        translation: '  xin chào  ',
        explanationVi: '',
        explanationEn: '  greeting  ',
        examplesVi: '',
        examplesEn: '   ',
      })

      expect(apiClient.post).toHaveBeenCalledWith('/cards', {
        topicId: 3,
        term: 'hello',
        pos: null,
        translation: 'xin chào',
        explanationVi: null,
        explanationEn: 'greeting',
        examplesVi: null,
        examplesEn: null,
      })
    })

    it('updateUserCardApi calls PUT /cards/:cardId with trimmed payload', async () => {
      apiClient.put.mockResolvedValue({ data: { success: true } })
      await updateUserCardApi('card-1', {
        term: '  book ',
        pos: 'noun',
        translation: ' sách ',
        explanationVi: '',
        explanationEn: '',
        examplesVi: '',
        examplesEn: '',
      })

      expect(apiClient.put).toHaveBeenCalledWith('/cards/card-1', {
        term: 'book',
        pos: 'noun',
        translation: 'sách',
        explanationVi: null,
        explanationEn: null,
        examplesVi: null,
        examplesEn: null,
      })
    })

    it('deleteUserCardApi calls DELETE /cards/:cardId', async () => {
      apiClient.delete.mockResolvedValue({ data: { success: true } })
      await deleteUserCardApi('card-1')
      expect(apiClient.delete).toHaveBeenCalledWith('/cards/card-1')
    })
  })
})
