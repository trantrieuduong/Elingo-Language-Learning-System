/**
 * Mock data & in-memory / localStorage storage cho CRUD User Deck.
 * Mô phỏng chính xác DTO: CreateDeckRequest, UpdateDeckRequest, DeckResponse, TagResponse, CefrLevelResponse.
 */

export const MOCK_TAGS = [
  { id: 1, code: 'ielts', label: 'IELTS' },
  { id: 2, code: 'toeic', label: 'TOEIC' },
  { id: 3, code: 'communication', label: 'Giao tiếp' },
  { id: 4, code: 'travel', label: 'Du lịch' },
  { id: 5, code: 'business', label: 'Kinh doanh' },
  { id: 6, code: 'academic', label: 'Học thuật' },
  { id: 7, code: 'daily', label: 'Đời sống' },
]

export const MOCK_CEFR_LEVELS = [
  { id: 1, code: 'A1', label: 'A1' },
  { id: 2, code: 'A2', label: 'A2' },
  { id: 3, code: 'B1', label: 'B1' },
  { id: 4, code: 'B2', label: 'B2' },
  { id: 5, code: 'C1', label: 'C1' },
  { id: 6, code: 'C2', label: 'C2' },
]

// Sample cover image presets để người dùng chọn nhanh khi tạo/sửa bộ từ
export const SAMPLE_COVER_PRESETS = [
  {
    code: 'academic',
    label: 'IELTS / Học thuật',
    url: 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=600&q=80',
  },
  {
    code: 'travel',
    label: 'Du lịch & Khám phá',
    url: 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?auto=format&fit=crop&w=600&q=80',
  },
  {
    code: 'business',
    label: 'Kinh doanh & Công sở',
    url: 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=600&q=80',
  },
  {
    code: 'lifestyle',
    label: 'Đời sống & Mua sắm',
    url: 'https://images.unsplash.com/photo-1555529669-e69e7aa0ba9a?auto=format&fit=crop&w=600&q=80',
  },
  {
    code: 'technology',
    label: 'Công nghệ & Kỹ thuật',
    url: 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=600&q=80',
  },
]

export const INITIAL_USER_DECKS = [
  {
    id: 101,
    title: 'IELTS Speaking Part 2 Collocations',
    slug: 'ielts-speaking-part-2-collocations',
    description: 'Tập hợp các cụm từ đắt giá giúp nâng band điểm từ vựng (Lexical Resource) trong phần thi nói IELTS Speaking.',
    isPremium: false,
    coverImageUrl: 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=600&q=80',
    status: 'PUBLISHED',
    ownerType: 'USER',
    topicCount: 2,
    cardCount: 3,
    learnedCardCount: 1,
    tags: [
      { id: 1, code: 'ielts', label: 'IELTS' },
      { id: 6, code: 'academic', label: 'Học thuật' },
    ],
    cefrLevels: [
      { id: 4, code: 'B2', label: 'B2' },
      { id: 5, code: 'C1', label: 'C1' },
    ],
  },
  {
    id: 102,
    title: 'Tiếng Anh Du lịch & Check-in Khách sạn',
    slug: 'tieng-anh-du-lich-va-check-in-khach-san',
    description: 'Mẫu câu và từ vựng thông dụng nhất khi làm thủ tục tại sân bay, nhận phòng khách sạn và hỏi đường.',
    isPremium: false,
    coverImageUrl: 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?auto=format&fit=crop&w=600&q=80',
    status: 'PUBLISHED',
    ownerType: 'USER',
    topicCount: 2,
    cardCount: 1,
    learnedCardCount: 0,
    tags: [
      { id: 4, code: 'travel', label: 'Du lịch' },
      { id: 3, code: 'communication', label: 'Giao tiếp' },
    ],
    cefrLevels: [
      { id: 2, code: 'A2', label: 'A2' },
      { id: 3, code: 'B1', label: 'B1' },
    ],
  },
  {
    id: 103,
    title: 'Email & Đàm phán Doanh nghiệp',
    slug: 'email-va-dam-phan-doanh-nghiep',
    description: 'Bản nháp đang tổng hợp các thành ngữ thương mại và cấu trúc viết thư trang trọng cho đối tác nước ngoài.',
    isPremium: false,
    coverImageUrl: 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=600&q=80',
    status: 'DRAFT',
    ownerType: 'USER',
    topicCount: 1,
    cardCount: 0,
    learnedCardCount: 0,
    tags: [{ id: 5, code: 'business', label: 'Kinh doanh' }],
    cefrLevels: [
      { id: 3, code: 'B1', label: 'B1' },
      { id: 4, code: 'B2', label: 'B2' },
    ],
  },
  {
    id: 104,
    title: 'Từ vựng Đời sống & Mua sắm',
    slug: 'tu-vung-doi-song-va-mua-sam',
    description: 'Danh sách từ vựng các món đồ gia dụng, thực phẩm siêu thị và cách mặc cả khi mua sắm.',
    isPremium: false,
    coverImageUrl: 'https://images.unsplash.com/photo-1555529669-e69e7aa0ba9a?auto=format&fit=crop&w=600&q=80',
    status: 'DRAFT',
    ownerType: 'USER',
    topicCount: 1,
    cardCount: 0,
    learnedCardCount: 0,
    tags: [{ id: 7, code: 'daily', label: 'Đời sống' }],
    cefrLevels: [{ id: 1, code: 'A1', label: 'A1' }],
  },
]

const STORAGE_KEY = 'elingo_user_decks_mock'

/**
 * Tải danh sách user decks từ localStorage hoặc fallback về initial.
 */
export const loadStoredUserDecks = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed
      }
    }
  } catch (e) {
    console.warn('Could not read user decks from localStorage:', e)
  }
  return [...INITIAL_USER_DECKS]
}

/**
 * Lưu danh sách user decks vào localStorage.
 */
export const saveStoredUserDecks = (decks) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(decks))
  } catch (e) {
    console.warn('Could not save user decks to localStorage:', e)
  }
}

/**
 * Chuyển đổi tiêu đề có dấu tiếng Việt thành slug hợp lệ theo quy tắc backend:
 * Pattern: ^[a-z0-9]+(?:-[a-z0-9]+)*$
 * Test cases S1, S2, S3
 */
export const generateSlug = (title) => {
  if (!title) return ''

  let str = title.trim().toLowerCase()

  // Bỏ dấu tiếng Việt
  str = str.replace(/[àáạảãâầấậẩẫăằắặẳẵ]/g, 'a')
  str = str.replace(/[èéẹẻẽêềếệểễ]/g, 'e')
  str = str.replace(/[ìíịỉĩ]/g, 'i')
  str = str.replace(/[òóọỏõôồốộổỗơờớợởỡ]/g, 'o')
  str = str.replace(/[ùúụủũưừứựửữ]/g, 'u')
  str = str.replace(/[ỳýỵỷỹ]/g, 'y')
  str = str.replace(/đ/g, 'd')

  // Bỏ ký tự đặc biệt, chỉ giữ chữ thường, số, dấu cách, dấu gạch ngang
  str = str.replace(/[^a-z0-9\s-]/g, '')

  // Thay khoảng trắng, gạch dưới bằng gạch ngang
  str = str.replace(/[\s_]+/g, '-')

  // Rút gọn nhiều dấu gạch ngang liền nhau
  str = str.replace(/-+/g, '-')

  // Bỏ dấu gạch ngang ở đầu và cuối
  str = str.replace(/^-+|-+$/g, '')

  return str
}

/**
 * Kiểm tra tính hợp lệ của slug theo pattern của backend (@ValidSlug).
 */
export const isValidSlug = (slug) => {
  if (!slug || typeof slug !== 'string') return false
  const SLUG_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/
  return SLUG_PATTERN.test(slug.trim())
}

/**
 * Mock API: Lấy danh sách user decks có filter, search, pagination.
 */
export const getUserDecksMock = async ({
  keyword = '',
  status = 'ALL',
  tagCode = null,
  cefrCode = null,
  page = 1,
  limit = 9,
} = {}) => {
  await new Promise((r) => setTimeout(r, 200)) // Fake latency nhẹ
  const allDecks = loadStoredUserDecks()

  let filtered = allDecks.filter((d) => d.ownerType === 'USER' && d.status !== 'ARCHIVED')

  if (status && status !== 'ALL') {
    filtered = filtered.filter((d) => d.status === status)
  }

  if (keyword) {
    const q = keyword.trim().toLowerCase()
    filtered = filtered.filter(
      (d) =>
        d.title?.toLowerCase().includes(q) ||
        d.description?.toLowerCase().includes(q) ||
        d.slug?.toLowerCase().includes(q)
    )
  }

  if (tagCode) {
    filtered = filtered.filter((d) => d.tags?.some((t) => t.code === tagCode))
  }

  if (cefrCode) {
    filtered = filtered.filter((d) => d.cefrLevels?.some((c) => c.code === cefrCode))
  }

  const totalElements = filtered.length
  const totalPages = Math.max(1, Math.ceil(totalElements / limit))
  const start = (page - 1) * limit
  const pagedContent = filtered.slice(start, start + limit)

  return {
    success: true,
    data: {
      content: pagedContent,
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
 * Mock API: Tạo mới User Deck dựa trên CreateDeckRequest.
 */
export const createUserDeckMock = async (request) => {
  await new Promise((r) => setTimeout(r, 300))
  const { title, slug, description, coverImageUrl, tagIds = [], cefrLevelIds = [] } = request

  if (!title || !title.trim()) {
    return { success: false, message: 'Tên bộ từ không được để trống (INVALID_REQUEST)' }
  }

  if (!isValidSlug(slug)) {
    return { success: false, message: 'Định dạng slug không hợp lệ (INVALID_REQUEST)' }
  }

  const currentDecks = loadStoredUserDecks()
  if (currentDecks.some((d) => d.slug === slug.trim())) {
    return { success: false, message: 'Slug này đã tồn tại trong hệ thống (DECK_SLUG_EXISTED)' }
  }

  const resolvedTags = MOCK_TAGS.filter((t) => tagIds.includes(t.id))
  const resolvedCefr = MOCK_CEFR_LEVELS.filter((c) => cefrLevelIds.includes(c.id))

  const newDeck = {
    id: Date.now(),
    title: title.trim(),
    slug: slug.trim(),
    description: description ? description.trim() : '',
    coverImageUrl: coverImageUrl ? coverImageUrl.trim() : '',
    isPremium: false, // User deck luôn là false
    status: 'DRAFT', // Mặc định khi tạo mới
    ownerType: 'USER',
    topicCount: 0,
    cardCount: 0,
    learnedCardCount: 0,
    publishedAt: null,
    tags: resolvedTags,
    cefrLevels: resolvedCefr,
  }

  const updatedList = [newDeck, ...currentDecks]
  saveStoredUserDecks(updatedList)

  return {
    success: true,
    data: newDeck,
    message: 'Tạo bộ từ vựng mới thành công',
  }
}

/**
 * Mock API: Cập nhật User Deck dựa trên UpdateDeckRequest.
 */
export const updateUserDeckMock = async (deckId, request) => {
  await new Promise((r) => setTimeout(r, 300))
  const { title, slug, description, coverImageUrl, status, tagIds, cefrLevelIds } = request

  if (!title || !title.trim()) {
    return { success: false, message: 'Tên bộ từ không được để trống (INVALID_REQUEST)' }
  }

  if (!isValidSlug(slug)) {
    return { success: false, message: 'Định dạng slug không hợp lệ (INVALID_REQUEST)' }
  }

  const currentDecks = loadStoredUserDecks()
  const index = currentDecks.findIndex((d) => d.id === Number(deckId))

  if (index === -1) {
    return { success: false, message: 'Không tìm thấy bộ từ vựng (DECK_NOT_FOUND)' }
  }

  // Check trùng slug với deck khác
  if (currentDecks.some((d) => d.id !== Number(deckId) && d.slug === slug.trim())) {
    return { success: false, message: 'Slug đã được sử dụng bởi bộ từ khác (DECK_SLUG_EXISTED)' }
  }

  const existing = currentDecks[index]

  const updatedDeck = {
    ...existing,
    title: title.trim(),
    slug: slug.trim(),
    description: description !== undefined ? description?.trim() : existing.description,
    coverImageUrl: coverImageUrl !== undefined ? coverImageUrl?.trim() : existing.coverImageUrl,
    status: status || existing.status,
    publishedAt:
      status === 'PUBLISHED' && !existing.publishedAt ? new Date().toISOString() : existing.publishedAt,
    tags:
      tagIds !== undefined
        ? MOCK_TAGS.filter((t) => tagIds.includes(t.id))
        : existing.tags,
    cefrLevels:
      cefrLevelIds !== undefined
        ? MOCK_CEFR_LEVELS.filter((c) => cefrLevelIds.includes(c.id))
        : existing.cefrLevels,
  }

  currentDecks[index] = updatedDeck
  saveStoredUserDecks(currentDecks)

  return {
    success: true,
    data: updatedDeck,
    message: 'Cập nhật bộ từ vựng thành công',
  }
}

/**
 * Mock API: Xóa User Deck (Soft delete nếu đã publish, Hard delete nếu là draft).
 */
export const deleteUserDeckMock = async (deckId) => {
  await new Promise((r) => setTimeout(r, 250))
  const currentDecks = loadStoredUserDecks()
  const target = currentDecks.find((d) => d.id === Number(deckId))

  if (!target) {
    return { success: false, message: 'Không tìm thấy bộ từ vựng (DECK_NOT_FOUND)' }
  }

  let updatedList
  if (target.publishedAt || target.status === 'PUBLISHED') {
    // Soft delete -> chuyển status thành ARCHIVED
    updatedList = currentDecks.map((d) =>
      d.id === Number(deckId) ? { ...d, status: 'ARCHIVED', topicCount: 0, cardCount: 0 } : d
    )
  } else {
    // Hard delete
    updatedList = currentDecks.filter((d) => d.id !== Number(deckId))
  }

  saveStoredUserDecks(updatedList)

  return {
    success: true,
    message: 'Xóa bộ từ vựng thành công',
  }
}

/**
 * Đặt lại dữ liệu về ban đầu.
 */
export const resetUserDecksMock = () => {
  saveStoredUserDecks(INITIAL_USER_DECKS)
  return INITIAL_USER_DECKS
}
