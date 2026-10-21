/**
 * Mock data & in-memory / localStorage storage cho CRUD Topic.
 * Mô phỏng chính xác DTO: CreateTopicRequest, UpdateTopicRequest, TopicResponse.
 *
 * TopicResponse: { id, deckId, name, slug, order, cardCount, unlearnedCardCount }
 * CreateTopicRequest: { deckId, name (required), slug }
 * UpdateTopicRequest: { name (required), slug, order }
 */

import { generateSlug, isValidSlug } from './userDeckMockData'

const STORAGE_KEY = 'elingo_topics_mock'

export const INITIAL_TOPICS = [
  {
    id: 1,
    deckId: 101,
    name: 'Daily routines',
    slug: 'daily-routines',
    order: 1,
    cardCount: 2,
    unlearnedCardCount: 1,
  },
  {
    id: 2,
    deckId: 101,
    name: 'Work and study',
    slug: 'work-and-study',
    order: 2,
    cardCount: 1,
    unlearnedCardCount: 1,
  },
  {
    id: 3,
    deckId: 102,
    name: 'Travel essentials',
    slug: 'travel-essentials',
    order: 1,
    cardCount: 1,
    unlearnedCardCount: 1,
  },
  {
    id: 4,
    deckId: 102,
    name: 'Getting around',
    slug: 'getting-around',
    order: 2,
    cardCount: 0,
    unlearnedCardCount: 0,
  },
  {
    id: 5,
    deckId: 103,
    name: 'Business communication',
    slug: 'business-communication',
    order: 1,
    cardCount: 0,
    unlearnedCardCount: 0,
  },
  {
    id: 6,
    deckId: 104,
    name: 'Shopping vocabulary',
    slug: 'shopping-vocabulary',
    order: 1,
    cardCount: 0,
    unlearnedCardCount: 0,
  },
]

/**
 * Tải danh sách topics từ localStorage hoặc fallback về initial.
 */
export const loadStoredTopics = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed.map((topic) => ({
          ...topic,
          deckId:
            topic.deckId ??
            INITIAL_TOPICS.find((initialTopic) => initialTopic.id === topic.id)?.deckId,
        }))
      }
    }
  } catch (e) {
    console.warn('Could not read topics from localStorage:', e)
  }
  return [...INITIAL_TOPICS]
}

/**
 * Lưu danh sách topics vào localStorage.
 */
export const saveStoredTopics = (topics) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(topics))
  } catch (e) {
    console.warn('Could not save topics to localStorage:', e)
  }
}

/**
 * Mock API: Lấy danh sách topics có tìm kiếm và phân trang.
 *
 * @param {Object} params - { deckId, keyword, page, limit }
 */
export const getTopicsMock = async ({ deckId, keyword = '', page = 1, limit = 9 } = {}) => {
  await new Promise((r) => setTimeout(r, 180))
  const all = loadStoredTopics()

  let filtered = [...all]

  if (deckId !== undefined && deckId !== null) {
    filtered = filtered.filter((topic) => String(topic.deckId) === String(deckId))
  }

  if (keyword) {
    const q = keyword.trim().toLowerCase()
    filtered = filtered.filter(
      (t) => t.name?.toLowerCase().includes(q) || t.slug?.toLowerCase().includes(q)
    )
  }

  // Sắp xếp theo order tăng dần
  filtered.sort((a, b) => (a.order ?? 999) - (b.order ?? 999))

  const totalElements = filtered.length
  const totalPages = Math.max(1, Math.ceil(totalElements / limit))
  const start = (page - 1) * limit
  const content = filtered.slice(start, start + limit)

  return {
    success: true,
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
 * Mock API: Tạo mới Topic dựa trên CreateTopicRequest.
 *
 * @param {Object} request - { deckId, name, slug }
 */
export const createTopicMock = async (request) => {
  await new Promise((r) => setTimeout(r, 280))
  const { deckId, name, slug } = request

  if (!deckId) {
    return { success: false, message: 'Bộ từ không hợp lệ (INVALID_REQUEST)' }
  }
  if (!name || !name.trim()) {
    return { success: false, message: 'Tên chủ đề không được để trống (INVALID_REQUEST)' }
  }

  const resolvedSlug = slug?.trim() || generateSlug(name)

  if (!isValidSlug(resolvedSlug)) {
    return { success: false, message: 'Định dạng slug không hợp lệ (INVALID_REQUEST)' }
  }

  const current = loadStoredTopics()

  if (current.some((t) => String(t.deckId) === String(deckId) && t.slug === resolvedSlug)) {
    return { success: false, message: 'Slug này đã tồn tại (TOPIC_SLUG_EXISTED)' }
  }

  const maxOrder = current
    .filter((topic) => String(topic.deckId) === String(deckId))
    .reduce((max, topic) => Math.max(max, topic.order ?? 0), 0)

  const newTopic = {
    id: Date.now(),
    deckId,
    name: name.trim(),
    slug: resolvedSlug,
    order: maxOrder + 1,
    cardCount: 0,
    unlearnedCardCount: 0,
  }

  const updated = [...current, newTopic]
  saveStoredTopics(updated)

  return { success: true, data: newTopic, message: 'Tạo chủ đề mới thành công' }
}

/**
 * Mock API: Cập nhật Topic dựa trên UpdateTopicRequest.
 *
 * @param {number|string} topicId - ID của topic cần sửa
 * @param {Object} request - { name, slug, order }
 */
export const updateTopicMock = async (topicId, request) => {
  await new Promise((r) => setTimeout(r, 280))
  const { name, slug, order } = request

  if (!name || !name.trim()) {
    return { success: false, message: 'Tên chủ đề không được để trống (INVALID_REQUEST)' }
  }

  const resolvedSlug = slug?.trim() || generateSlug(name)

  if (!isValidSlug(resolvedSlug)) {
    return { success: false, message: 'Định dạng slug không hợp lệ (INVALID_REQUEST)' }
  }

  const current = loadStoredTopics()
  const index = current.findIndex((t) => t.id === Number(topicId))

  if (index === -1) {
    return { success: false, message: 'Không tìm thấy chủ đề (TOPIC_NOT_FOUND)' }
  }

  if (
    current.some(
      (t) =>
        t.id !== Number(topicId) &&
        String(t.deckId) === String(current[index].deckId) &&
        t.slug === resolvedSlug
    )
  ) {
    return {
      success: false,
      message: 'Slug đã được sử dụng bởi chủ đề khác (TOPIC_SLUG_EXISTED)',
    }
  }

  const existing = current[index]
  const updated = {
    ...existing,
    name: name.trim(),
    slug: resolvedSlug,
    order: order !== undefined && order !== null ? Number(order) : existing.order,
  }

  current[index] = updated
  saveStoredTopics(current)

  return { success: true, data: updated, message: 'Cập nhật chủ đề thành công' }
}

/**
 * Mock API: Xóa Topic.
 *
 * @param {number|string} topicId - ID của topic cần xóa
 */
export const deleteTopicMock = async (topicId) => {
  await new Promise((r) => setTimeout(r, 230))
  const current = loadStoredTopics()
  const target = current.find((t) => t.id === Number(topicId))

  if (!target) {
    return { success: false, message: 'Không tìm thấy chủ đề (TOPIC_NOT_FOUND)' }
  }

  const updated = current.filter((t) => t.id !== Number(topicId))
  saveStoredTopics(updated)

  return { success: true, message: 'Đã xóa chủ đề thành công' }
}

/**
 * Đặt lại dữ liệu topics về ban đầu.
 */
export const resetTopicsMock = () => {
  saveStoredTopics(INITIAL_TOPICS)
  return INITIAL_TOPICS
}