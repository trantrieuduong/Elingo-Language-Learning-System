import apiClient from '../../services/apiClient'

/**
 * Lấy danh sách các decks published có phân trang và filter từ backend.
 * Backend endpoint: GET /decks
 * Query params: q, tags, cefr, premium, ownerType, page (1-indexed), size, sort
 *
 * @param {Object} params - { keyword, tagCode, cefrCode, page, limit, sort }
 */
export const getDecksApi = async (params = {}) => {
  const queryParams = {
    // Spring is configured with `one-indexed-parameters: true`, so the UI page
    // number must be sent unchanged (page 1 maps to Spring's internal index 0).
    page: params.page || 1,
    size: params.limit || params.size || 20,
    sort: params.sort,
    ownerType: params.ownerType,
  }

  const keyword = params.keyword || params.q
  if (keyword && keyword.trim()) {
    queryParams.q = keyword.trim()
  }

  if (params.tagCode) {
    queryParams.tags = [params.tagCode]
  } else if (Array.isArray(params.tags) && params.tags.length > 0) {
    queryParams.tags = params.tags
  }

  if (params.cefrCode) {
    const rawCode = String(params.cefrCode).trim()
    const match = rawCode.match(/(A1|A2|B1|B2|C1|C2)/i)
    if (match) {
      queryParams.cefr = [match[1].toUpperCase()]
    }
  } else if (Array.isArray(params.cefr) && params.cefr.length > 0) {
    const validCefr = params.cefr
      .map((c) => String(c).trim().match(/(A1|A2|B1|B2|C1|C2)/i)?.[1]?.toUpperCase())
      .filter(Boolean)
    if (validCefr.length > 0) {
      queryParams.cefr = validCefr
    }
  }

  const response = await apiClient.get('/decks', { params: queryParams })
  return response.data
}

export const getTagsApi = async () => {
  const response = await apiClient.get('/tags')
  return response.data
}

export const getCefrLevelsApi = async () => {
  const response = await apiClient.get('/cefr-levels')
  return response.data
}

// ─────────────────────────────────────────────────────────────
// User Deck CRUD APIs
// ─────────────────────────────────────────────────────────────

/**
 * Lấy danh sách bộ từ vựng do người dùng tự tạo (User Decks) từ backend.
 * Backend endpoint: GET /users/me/decks -> ApiResponse<List<DeckResponse>>
 *
 * @param {Object} params - { keyword, status, tagCode, cefrCode, page, limit }
 */
export const getUserDecksApi = async (params = {}) => {
  const response = await apiClient.get('/users/me/decks')
  let decks = response.data?.data || []

  // Filter keyword theo title hoặc description
  const keyword = (params.keyword || params.q || '').trim().toLowerCase()
  if (keyword) {
    decks = decks.filter(
      (d) =>
        d.title?.toLowerCase().includes(keyword) ||
        d.description?.toLowerCase().includes(keyword)
    )
  }

  // Filter theo status (ALL | PUBLISHED | DRAFT)
  if (params.status && params.status !== 'ALL') {
    decks = decks.filter((d) => d.status === params.status)
  }

  // Filter theo tagCode
  if (params.tagCode) {
    decks = decks.filter((d) =>
      d.tags?.some((t) => t.code === params.tagCode || t.label === params.tagCode)
    )
  }

  // Filter theo cefrCode
  if (params.cefrCode) {
    decks = decks.filter((d) =>
      d.cefrLevels?.some((c) => c.code === params.cefrCode || c.label === params.cefrCode)
    )
  }

  const page = params.page || 1
  const limit = params.limit || 9
  const totalElements = decks.length
  const totalPages = Math.max(1, Math.ceil(totalElements / limit))
  const content = decks.slice((page - 1) * limit, page * limit)

  return {
    ...response.data,
    data: {
      content,
      totalElements,
      totalPages,
      page,
      limit,
    },
  }
}

/**
 * Tạo mới bộ từ vựng của người dùng (UserDeckRequest).
 * Backend endpoint: POST /decks
 * Body: { title, description }
 *
 * @param {Object} data - { title, description }
 */
export const createUserDeckApi = async (data) => {
  const payload = {
    title: data.title?.trim(),
    description: data.description?.trim() || null,
  }
  const response = await apiClient.post('/decks', payload)
  return response.data
}

/**
 * Cập nhật bộ từ vựng của người dùng (UserDeckRequest).
 * Backend endpoint: PUT /decks/{deckId}
 * Body: { title, description }
 *
 * @param {number|string} deckId - ID của deck cần sửa
 * @param {Object} data - { title, description }
 */
export const updateUserDeckApi = async (deckId, data) => {
  const payload = {
    title: data.title?.trim(),
    description: data.description?.trim() || null,
  }
  const response = await apiClient.put(`/decks/${deckId}`, payload)
  return response.data
}

/**
 * Xuất bản hoặc hủy xuất bản bộ từ vựng.
 * Backend endpoint: PUT /decks/{deckId}/publication
 * Body: { published: boolean }
 *
 * @param {number|string} deckId
 * @param {boolean} published
 */
export const publishUserDeckApi = async (deckId, published) => {
  const response = await apiClient.put(`/decks/${deckId}/publication`, {
    published: Boolean(published),
  })
  return response.data
}

/**
 * Xóa bộ từ vựng của người dùng.
 * Backend endpoint: DELETE /decks/{deckId}
 *
 * @param {number|string} deckId
 */
export const deleteUserDeckApi = async (deckId) => {
  const response = await apiClient.delete(`/decks/${deckId}`)
  return response.data
}

// ─────────────────────────────────────────────────────────────
// Topic CRUD APIs
// ─────────────────────────────────────────────────────────────

/**
 * Lấy danh sách chủ đề (topics) thuộc 1 deck từ backend.
 * Backend endpoint: GET /decks/{deckId}/topics -> ApiResponse<List<TopicResponse>>
 *
 * @param {Object} params - { deckId, keyword, page, limit }
 */
export const getTopicsApi = async ({ deckId, keyword = '', page = 1, limit = 9 } = {}) => {
  const response = await apiClient.get(`/decks/${deckId}/topics`)
  const topics = response.data?.data || []

  const normalizedKeyword = keyword.trim().toLowerCase()
  const filteredTopics = normalizedKeyword
    ? topics.filter(
        (topic) =>
          topic.name?.toLowerCase().includes(normalizedKeyword) ||
          topic.slug?.toLowerCase().includes(normalizedKeyword)
      )
    : topics

  const totalElements = filteredTopics.length
  const totalPages = Math.max(1, Math.ceil(totalElements / limit))
  const content = filteredTopics.slice((page - 1) * limit, page * limit)

  return {
    ...response.data,
    data: {
      content,
      totalElements,
      totalPages,
      number: page - 1,
      size: limit,
      first: page === 1,
      last: page >= totalPages,
    },
  }
}

/**
 * Tạo mới Topic của người dùng (CreateUserTopicRequest).
 * Backend endpoint: POST /topics
 * Body: { deckId, name }
 *
 * @param {Object} data - { deckId, name }
 */
export const createTopicApi = async (data) => {
  const payload = {
    deckId: Number(data.deckId),
    name: data.name?.trim(),
  }
  const response = await apiClient.post('/topics', payload)
  return response.data
}

/**
 * Cập nhật Topic của người dùng (UpdateUserTopicRequest).
 * Backend endpoint: PUT /topics/{topicId}
 * Body: { name }
 *
 * @param {number|string} topicId - ID của topic cần sửa
 * @param {Object} data - { name }
 */
export const updateTopicApi = async (topicId, data) => {
  const payload = {
    name: data.name?.trim(),
  }
  const response = await apiClient.put(`/topics/${topicId}`, payload)
  return response.data
}

/**
 * Xóa Topic của người dùng.
 * Backend endpoint: DELETE /topics/{topicId}
 *
 * @param {number|string} topicId
 */
export const deleteTopicApi = async (topicId) => {
  const response = await apiClient.delete(`/topics/${topicId}`)
  return response.data
}

// ─────────────────────────────────────────────────────────────
// Card CRUD APIs
// ─────────────────────────────────────────────────────────────

/**
 * Lấy danh sách flashcards thuộc 1 topic từ backend.
 * Backend endpoint: GET /topics/{topicId}/cards -> ApiResponse<List<CardResponse>>
 *
 * @param {number|string} topicId
 */
export const getCardsByTopicApi = async (topicId) => {
  const response = await apiClient.get(`/topics/${topicId}/cards`)
  return response.data
}

/**
 * Tạo mới card trong topic của người dùng (CreateUserCardRequest).
 * Backend endpoint: POST /cards
 * Body: { topicId, term, pos, translation, explanationVi, explanationEn, examplesVi, examplesEn }
 *
 * @param {Object} data
 */
export const createUserCardApi = async (data) => {
  const payload = {
    topicId: Number(data.topicId),
    term: data.term?.trim(),
    pos: data.pos?.trim() || null,
    translation: data.translation?.trim(),
    explanationVi: data.explanationVi?.trim() || null,
    explanationEn: data.explanationEn?.trim() || null,
    examplesVi: data.examplesVi?.trim() || null,
    examplesEn: data.examplesEn?.trim() || null,
  }
  const response = await apiClient.post('/cards', payload)
  return response.data
}

/**
 * Cập nhật thông tin card của người dùng (UpdateUserCardRequest).
 * Backend endpoint: PUT /cards/{cardId}
 * Body: { term, pos, translation, explanationVi, explanationEn, examplesVi, examplesEn }
 *
 * @param {number|string} cardId
 * @param {Object} data
 */
export const updateUserCardApi = async (cardId, data) => {
  const payload = {
    term: data.term?.trim(),
    pos: data.pos?.trim() || null,
    translation: data.translation?.trim(),
    explanationVi: data.explanationVi?.trim() || null,
    explanationEn: data.explanationEn?.trim() || null,
    examplesVi: data.examplesVi?.trim() || null,
    examplesEn: data.examplesEn?.trim() || null,
  }
  const response = await apiClient.put(`/cards/${cardId}`, payload)
  return response.data
}

/**
 * Xóa card của người dùng.
 * Backend endpoint: DELETE /cards/{cardId}
 *
 * @param {number|string} cardId
 */
export const deleteUserCardApi = async (cardId) => {
  const response = await apiClient.delete(`/cards/${cardId}`)
  return response.data
}
