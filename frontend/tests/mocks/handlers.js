import { http, HttpResponse } from 'msw'

const API_URL = import.meta.env.VITE_API_URL

export const handlers = [
  http.post(`${API_URL}/auth/login`, () => {
    return HttpResponse.json({
      success: true,
      data: { accessToken: 'mock-access-token-12345' },
    })
  }),

  http.post(`${API_URL}/auth/refresh`, () => {
    return HttpResponse.json({
      success: true,
      data: { accessToken: 'mock-refreshed-access-token' },
    })
  }),

  http.get(`${API_URL}/users/me`, () => {
    return HttpResponse.json({
      success: true,
      data: { id: 1, username: 'testuser', email: 'test@example.com', role: 'USER' },
    })
  }),

  http.get(`${API_URL}/users/:userId/badges`, () => {
    return HttpResponse.json({ success: true, data: [] })
  }),

  http.get(`${API_URL}/users/:userId/levels`, () => {
    return HttpResponse.json({ success: true, data: null })
  }),

  http.get(`${API_URL}/users/:userId/streaks`, () => {
    return HttpResponse.json({
      success: true,
      data: { currentStreak: 0, longestStreak: 0, checkinDates: [] },
    })
  }),

  http.get(`${API_URL}/users/:userId/posts`, () => {
    return HttpResponse.json({ success: true, data: { items: [] } })
  }),

  http.get(`${API_URL}/post-groups`, () => {
    return HttpResponse.json({ success: true, data: [] })
  }),

  http.post(`${API_URL}/auth/signup`, () => {
    return HttpResponse.json({ success: true, message: 'Đăng ký thành công' })
  }),

  http.get(`${API_URL}/decks`, () => {
    return HttpResponse.json({ success: true, data: { content: [], totalPages: 1 } })
  }),

  http.get(`${API_URL}/decks/:deckId`, ({ params }) => {
    return HttpResponse.json({
      success: true,
      data: {
        id: params.deckId,
        title: 'Mock Deck',
        slug: 'mock-deck',
        cardCount: 0,
        topicCount: 0,
        status: 'DRAFT',
      },
    })
  }),

  http.get(`${API_URL}/users/me/decks`, () => {
    return HttpResponse.json({ success: true, data: [] })
  }),

  http.post(`${API_URL}/decks`, () => {
    return HttpResponse.json({
      success: true,
      data: { id: 'new-deck-id', title: 'New Deck', status: 'DRAFT' },
    })
  }),

  http.put(`${API_URL}/decks/:deckId`, ({ params }) => {
    return HttpResponse.json({
      success: true,
      data: { id: params.deckId, title: 'Updated Deck' },
    })
  }),

  http.put(`${API_URL}/decks/:deckId/publication`, () => {
    return HttpResponse.json({ success: true, data: {} })
  }),

  http.delete(`${API_URL}/decks/:deckId`, () => {
    return HttpResponse.json({ success: true })
  }),

  http.get(`${API_URL}/tags`, () => {
    return HttpResponse.json({ success: true, data: [{ id: 't1', label: 'Du lịch' }] })
  }),

  http.get(`${API_URL}/cefr-levels`, () => {
    return HttpResponse.json({ success: true, data: [{ id: 'a1', label: 'A1' }] })
  }),

  http.get(`${API_URL}/decks/:deckId/topics`, () => {
    return HttpResponse.json({ success: true, data: [] })
  }),

  http.post(`${API_URL}/topics`, () => {
    return HttpResponse.json({ success: true, data: { id: 1, name: 'New Topic' } })
  }),

  http.put(`${API_URL}/topics/:topicId`, () => {
    return HttpResponse.json({ success: true, data: { id: 1, name: 'Updated Topic' } })
  }),

  http.delete(`${API_URL}/topics/:topicId`, () => {
    return HttpResponse.json({ success: true })
  }),

  http.get(`${API_URL}/topics/:topicId/cards`, () => {
    return HttpResponse.json({ success: true, data: [] })
  }),

  http.post(`${API_URL}/cards`, () => {
    return HttpResponse.json({
      success: true,
      data: { id: 'card-1', term: 'hello', translation: 'xin chào' },
    })
  }),

  http.put(`${API_URL}/cards/:cardId`, () => {
    return HttpResponse.json({ success: true, data: {} })
  }),

  http.delete(`${API_URL}/cards/:cardId`, () => {
    return HttpResponse.json({ success: true })
  }),
]
