import apiClient from '../../services/apiClient';

/**
 * @param {Object} params - { keyword, tagIds, cefrLevelIds, page, limit, sort, ownerType }
 */
export const getDecksApi = async (params = {}) => {
  const queryParams = {
    page: params.page,
    size: params.limit,
    sort: params.sort,
    ownerType: params.ownerType,
    tagIds: params.tagIds,
    cefrLevelIds: params.cefrLevelIds,
  };

  const keyword = params.keyword || params.q;
  if (keyword && keyword.trim()) {
    queryParams.q = keyword.trim();
  }

  const response = await apiClient.get('/decks', {
    params: queryParams,
    paramsSerializer: { indexes: null },
  });
  return response.data;
};

export const getDeckByIdApi = async (deckId) => {
  const response = await apiClient.get(`/decks/${deckId}`);
  return response.data;
};

export const getTagsApi = async () => {
  const response = await apiClient.get('/tags');
  return response.data;
};

export const getCefrLevelsApi = async () => {
  const response = await apiClient.get('/cefr-levels');
  return response.data;
};

/**
 * @param {Object} params - { keyword, status, tagId, cefrLevelId, page, limit }
 */
export const getUserDecksApi = async (params = {}) => {
  const response = await apiClient.get('/users/me/decks');
  let decks = response.data?.data || [];

  // Filter keyword theo title hoặc description
  const keyword = (params.keyword || params.q || '').trim().toLowerCase();
  if (keyword) {
    decks = decks.filter(
      (d) =>
        d.title?.toLowerCase().includes(keyword) || d.description?.toLowerCase().includes(keyword)
    );
  }

  // Filter theo status (ALL | PUBLISHED | DRAFT)
  if (params.status && params.status !== 'ALL') {
    decks = decks.filter((d) => d.status === params.status);
  }

  // Filter theo tag ID
  if (params.tagId) {
    decks = decks.filter((d) =>
      d.tags?.some((tag) => tag.id === params.tagId)
    );
  }

  // Filter theo CEFR ID
  if (params.cefrLevelId) {
    decks = decks.filter((d) =>
      d.cefrLevels?.some((cefr) => cefr.id === params.cefrLevelId)
    );
  }

  const page = params.page || 1;
  const limit = params.limit || 9;
  const totalElements = decks.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / limit));
  const content = decks.slice((page - 1) * limit, page * limit);

  return {
    ...response.data,
    data: {
      content,
      totalElements,
      totalPages,
      page,
      limit,
    },
  };
};

/**
 * @param {Object} data - { title, description }
 */
export const createUserDeckApi = async (data) => {
  const payload = {
    title: data.title?.trim(),
    description: data.description?.trim() || null,
  };
  const response = await apiClient.post('/decks', payload);
  return response.data;
};

/**
 * @param {Object} data - { title, description }
 */
export const updateUserDeckApi = async (deckId, data) => {
  const payload = {
    title: data.title?.trim(),
    description: data.description?.trim() || null,
  };
  const response = await apiClient.put(`/decks/${deckId}`, payload);
  return response.data;
};

/**
 * @param {boolean} published
 */
export const publishUserDeckApi = async (deckId, published) => {
  const response = await apiClient.put(`/decks/${deckId}/publication`, {
    published: Boolean(published),
  });
  return response.data;
};

export const deleteUserDeckApi = async (deckId) => {
  const response = await apiClient.delete(`/decks/${deckId}`);
  return response.data;
};

export const getTopicsApi = async (deckId) => {
  const response = await apiClient.get(`/decks/${deckId}/topics`);
  return response.data;
};

/**
 * @param {Object} data - { deckId, name }
 */
export const createTopicApi = async (data) => {
  const payload = {
    deckId: Number(data.deckId),
    name: data.name?.trim(),
  };
  const response = await apiClient.post('/topics', payload);
  return response.data;
};

/**
 * @param {Object} data - { name }
 */
export const updateTopicApi = async (topicId, data) => {
  const payload = {
    name: data.name?.trim(),
  };
  const response = await apiClient.put(`/topics/${topicId}`, payload);
  return response.data;
};

export const deleteTopicApi = async (topicId) => {
  const response = await apiClient.delete(`/topics/${topicId}`);
  return response.data;
};

/**
 * @param {number|string} topicId
 */
export const getCardsByTopicApi = async (topicId) => {
  const response = await apiClient.get(`/topics/${topicId}/cards`);
  return response.data;
};

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
  };
  const response = await apiClient.post('/cards', payload);
  return response.data;
};

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
  };
  const response = await apiClient.put(`/cards/${cardId}`, payload);
  return response.data;
};

export const deleteUserCardApi = async (cardId) => {
  const response = await apiClient.delete(`/cards/${cardId}`);
  return response.data;
};
