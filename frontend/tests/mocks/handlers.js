import { http, HttpResponse } from 'msw'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'

export const handlers = [
  http.post(`${API_URL}/auth/login`, () => {
    return HttpResponse.json({
      success: true,
      data: { accessToken: 'mock-access-token-12345' },
    })
  }),

  http.get(`${API_URL}/users/me`, () => {
    return HttpResponse.json({
      success: true,
      data: { id: 1, username: 'testuser', email: 'test@example.com', role: 'USER' },
    })
  }),

  http.post(`${API_URL}/auth/signup`, () => {
    return HttpResponse.json({ success: true, message: 'Đăng ký thành công' })
  }),
]
