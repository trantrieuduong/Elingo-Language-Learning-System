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
]
