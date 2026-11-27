import apiClient from '../../../services/apiClient';

export const EMPTY_PAGE_RESPONSE = {
  content: [],
  page: 1,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  isLast: true,
};

const PARSERS = {
  string: (val) => (val !== undefined && val !== null && String(val).trim() ? String(val).trim() : undefined),
  number: (val) => (val !== undefined && val !== null && val !== '' ? Number(val) : undefined),
  boolean: (val) => (val !== undefined && val !== null && val !== '' ? val === true || val === 'true' : undefined),
  strictBoolean: (val) => (typeof val === 'boolean' ? val : undefined),
  array: (val) => (Array.isArray(val) && val.length > 0 ? val : undefined),
  value: (val) => (val !== undefined && val !== null && val !== '' ? val : undefined),
  truthy: (val) => (val ? val : undefined),
};

/**
 * Chuẩn hóa query params cho các API tìm kiếm/phân trang phía Admin.
 * @param {Object} params
 * @param {Object<string, string>} fieldTypes
 * @returns {Object}
 */
export function buildAdminQuery(params = {}, fieldTypes = {}) {
  const query = {};

  for (const [key, type] of Object.entries(fieldTypes)) {
    const parser = PARSERS[type];
    if (!parser) continue;
    const parsed = parser(params[key]);
    if (parsed !== undefined) {
      query[key] = parsed;
    }
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
}

/**
 * Gọi API GET danh sách có phân trang và trả về PageResponse chuẩn.
 * @param {string} url
 * @param {Object} params
 * @param {Object<string, string>} fieldTypes
 * @returns {Promise<Object>}
 */
export async function fetchPagedAdminResource(url, params = {}, fieldTypes = {}) {
  const query = buildAdminQuery(params, fieldTypes);
  const res = await apiClient.get(url, { params: query });
  return res.data?.data || EMPTY_PAGE_RESPONSE;
}
