import apiClient from '../../../services/apiClient';

/**
 * Chuẩn hóa các tham số tìm kiếm và lọc cho AdminDeckSearchRequest
 * @param {Object} params - { keyword, ownerType, status, isPremium, tagIds, cefrLevelIds, page, size, sort }
 */
const buildDeckSearchParams = (params = {}) => {
  const query = {};
  if (params.keyword && params.keyword.trim()) {
    query.keyword = params.keyword.trim();
  }
  if (params.ownerType) {
    query.ownerType = params.ownerType;
  }
  if (params.status) {
    query.status = params.status;
  }
  if (typeof params.isPremium === 'boolean') {
    query.isPremium = params.isPremium;
  }
  if (Array.isArray(params.tagIds) && params.tagIds.length > 0) {
    query.tagIds = params.tagIds;
  }
  if (Array.isArray(params.cefrLevelIds) && params.cefrLevelIds.length > 0) {
    query.cefrLevelIds = params.cefrLevelIds;
  }
  if (params.page !== undefined && params.page !== null) {
    query.page = params.page;
  }
  if (params.size !== undefined && params.size !== null) {
    query.size = params.size;
  }
  if (params.sort) {
    query.sort = params.sort;
  }
  return query;
};

/* ============================================================
   1. CEFR LEVELS API
   ============================================================ */

/**
 * Lấy danh mục 6 cấp độ CEFR
 */
export const fetchCefrLevels = async () => {
  const res = await apiClient.get('/cefr-levels');
  return res.data?.data || [];
};

/* ============================================================
   2. DECKS API (AdminDeckController)
   ============================================================ */

/**
 * Tìm kiếm và phân trang danh sách bộ thẻ
 */
export const fetchAdminDecks = async (params = {}) => {
  const res = await apiClient.get('/admin/decks', {
    params: buildDeckSearchParams(params),
  });
  return res.data?.data;
};

/**
 * Lấy chi tiết thông tin một bộ thẻ
 */
export const fetchAdminDeckDetail = async (deckId) => {
  const res = await apiClient.get(`/admin/decks/${deckId}`);
  return res.data?.data;
};

/**
 * Tạo mới bộ thẻ hệ thống
 */
export const createSystemDeck = async (payload) => {
  const res = await apiClient.post('/admin/decks', payload);
  return res.data?.data;
};

/**
 * Cập nhật thông tin bộ thẻ hệ thống
 */
export const updateSystemDeck = async (deckId, payload) => {
  const res = await apiClient.put(`/admin/decks/${deckId}`, payload);
  return res.data?.data;
};

/**
 * Đổi trạng thái bộ thẻ (DRAFT, PUBLISHED, ARCHIVED)
 */
export const updateDeckStatus = async (deckId, status) => {
  const res = await apiClient.patch(`/admin/decks/${deckId}/status`, { status });
  return res.data?.data;
};

/**
 * Xóa cứng bộ thẻ người dùng vi phạm quy định
 */
export const deleteUserDeck = async (deckId, reason = 'Policy violation') => {
  const res = await apiClient.delete(`/admin/decks/${deckId}`, {
    params: { reason },
  });
  return res.data;
};

/**
 * Lấy danh sách chủ đề thuộc bộ thẻ
 */
export const fetchDeckTopics = async (deckId) => {
  const res = await apiClient.get(`/admin/decks/${deckId}/topics`);
  return res.data?.data || [];
};

/**
 * Sắp xếp lại thứ tự chủ đề trong bộ thẻ hệ thống
 */
export const reorderDeckTopics = async (deckId, orderedIds) => {
  const res = await apiClient.patch(`/admin/decks/${deckId}/topics/orders`, {
    orderedIds,
  });
  return res.data;
};

/* ============================================================
   3. TOPICS API (AdminTopicController)
   ============================================================ */

/**
 * Tạo chủ đề mới trong bộ thẻ hệ thống
 */
export const createSystemTopic = async (payload) => {
  const res = await apiClient.post('/admin/topics', payload);
  return res.data?.data;
};

/**
 * Cập nhật tên chủ đề hệ thống
 */
export const updateSystemTopic = async (topicId, payload) => {
  const res = await apiClient.put(`/admin/topics/${topicId}`, payload);
  return res.data?.data;
};

/**
 * Xóa chủ đề hệ thống
 */
export const deleteSystemTopic = async (topicId) => {
  const res = await apiClient.delete(`/admin/topics/${topicId}`);
  return res.data;
};

/**
 * Lấy danh sách thẻ từ vựng trong chủ đề
 */
export const fetchTopicCards = async (topicId) => {
  const res = await apiClient.get(`/admin/topics/${topicId}/cards`);
  return res.data?.data || [];
};

/**
 * Sắp xếp lại thứ tự thẻ từ vựng trong chủ đề hệ thống
 */
export const reorderTopicCards = async (topicId, orderedIds) => {
  const res = await apiClient.patch(`/admin/topics/${topicId}/cards/orders`, {
    orderedIds,
  });
  return res.data;
};

/* ============================================================
   4. CARDS API (AdminCardController)
   ============================================================ */

/**
 * Lấy chi tiết thẻ từ vựng
 */
export const fetchCardDetail = async (cardId) => {
  const res = await apiClient.get(`/admin/cards/${cardId}`);
  return res.data?.data;
};

/**
 * Tạo mới thẻ từ vựng hệ thống
 */
export const createSystemCard = async (payload) => {
  const res = await apiClient.post('/admin/cards', payload);
  return res.data?.data;
};

/**
 * Cập nhật thẻ từ vựng hệ thống
 */
export const updateSystemCard = async (cardId, payload) => {
  const res = await apiClient.put(`/admin/cards/${cardId}`, payload);
  return res.data?.data;
};

/**
 * Xóa thẻ từ vựng hệ thống
 */
export const deleteSystemCard = async (cardId) => {
  const res = await apiClient.delete(`/admin/cards/${cardId}`);
  return res.data;
};

/* ============================================================
   5. TAGS API (AdminTagController)
   ============================================================ */

/**
 * Lấy toàn bộ danh sách thẻ chủ đề (Tag)
 */
export const fetchAdminTags = async () => {
  const res = await apiClient.get('/admin/tags');
  return res.data?.data || [];
};

/**
 * Lấy chi tiết một thẻ chủ đề
 */
export const fetchAdminTagDetail = async (tagId) => {
  const res = await apiClient.get(`/admin/tags/${tagId}`);
  return res.data?.data;
};

/**
 * Tạo mới thẻ chủ đề
 */
export const createAdminTag = async (payload) => {
  const res = await apiClient.post('/admin/tags', payload);
  return res.data?.data;
};

/**
 * Cập nhật nhãn thẻ chủ đề
 */
export const updateAdminTag = async (tagId, payload) => {
  const res = await apiClient.put(`/admin/tags/${tagId}`, payload);
  return res.data?.data;
};

/**
 * Xóa thẻ chủ đề
 */
export const deleteAdminTag = async (tagId) => {
  const res = await apiClient.delete(`/admin/tags/${tagId}`);
  return res.data;
};

/* ============================================================
   6. ORPHANED CARDS API (AdminOrphanedCardController)
   ============================================================ */

/**
 * Lấy thống kê tổng quan các thẻ từ vựng mồ côi
 */
export const fetchOrphanedCardStats = async () => {
  const res = await apiClient.get('/admin/orphaned-cards/stats');
  return res.data?.data;
};

/**
 * Lấy danh sách phân trang các thẻ từ vựng mồ côi (1-indexed)
 */
export const fetchOrphanedCards = async (params = {}) => {
  const query = {};
  if (params.page !== undefined && params.page !== null) {
    query.page = params.page;
  }
  if (params.size !== undefined && params.size !== null) {
    query.size = params.size;
  }
  if (params.sort) {
    query.sort = params.sort;
  }
  const res = await apiClient.get('/admin/orphaned-cards', {
    params: query,
  });
  return res.data?.data;
};

/**
 * Kích hoạt dọn dẹp các thẻ mồ côi an toàn (chưa có lịch sử học) và media trên R2
 */
export const cleanupOrphanedCards = async () => {
  const res = await apiClient.post('/admin/orphaned-cards/cleanup');
  return res.data?.data;
};
