import apiClient from '../../../services/apiClient';
import { fetchPagedAdminResource } from '../utils/adminApiUtils';

// =========================================================================
// 1. QUẢN LÝ GÓI THUÊ BAO (AdminPremiumPlanController)
// =========================================================================

/**
 * Lấy toàn bộ danh sách gói thuê bao (bao gồm cả gói đang tắt, không phân trang)
 * @returns {Promise<Array<Object>>} List<AdminPremiumPlanResponse>
 */
export const getAllPlans = async () => {
  const res = await apiClient.get('/admin/premium/plans');
  return res.data?.data || [];
};

/**
 * Lấy chi tiết gói thuê bao theo ID
 * @param {number|string} planId
 * @returns {Promise<Object>} AdminPremiumPlanResponse
 */
export const getPlanById = async (planId) => {
  const res = await apiClient.get(`/admin/premium/plans/${planId}`);
  return res.data?.data;
};

/**
 * Tạo mới gói thuê bao (mặc định isActive = false)
 * @param {Object} payload - { name, price, currency, durationDays }
 * @returns {Promise<Object>} AdminPremiumPlanResponse
 */
export const createPlan = async (payload) => {
  const res = await apiClient.post('/admin/premium/plans', {
    name: payload.name ? payload.name.trim() : '',
    price: Number(payload.price),
    currency: payload.currency ? payload.currency.trim().toUpperCase() : 'VND',
    durationDays: Number(payload.durationDays),
  });
  return res.data?.data;
};

/**
 * Cập nhật thông tin gói thuê bao (chỉ cho phép sửa name và price)
 * @param {number|string} planId
 * @param {Object} payload - { name, price }
 * @returns {Promise<Object>} AdminPremiumPlanResponse
 */
export const updatePlan = async (planId, payload) => {
  const res = await apiClient.put(`/admin/premium/plans/${planId}`, {
    name: payload.name ? payload.name.trim() : '',
    price: Number(payload.price),
  });
  return res.data?.data;
};

/**
 * Vô hiệu hóa gói thuê bao
 * @param {number|string} planId
 * @returns {Promise<Object>} AdminPremiumPlanResponse
 */
export const disablePlan = async (planId) => {
  const res = await apiClient.put(`/admin/premium/plans/${planId}/disable`);
  return res.data?.data;
};

/**
 * Kích hoạt lại gói thuê bao
 * @param {number|string} planId
 * @returns {Promise<Object>} AdminPremiumPlanResponse
 */
export const enablePlan = async (planId) => {
  const res = await apiClient.put(`/admin/premium/plans/${planId}/enable`);
  return res.data?.data;
};

// =========================================================================
// 2. LỊCH SỬ GIAO DỊCH THANH TOÁN (AdminPremiumTransactionController)
// =========================================================================

/**
 * Tìm kiếm và lọc giao dịch thanh toán với phân trang 1-indexed
 * @param {Object} params - { keyword, status, planId, userId, createdFrom, createdTo, page, size, sort }
 * @returns {Promise<Object>} PageResponse<AdminTransactionResponse>
 */
export const searchTransactions = (params = {}) =>
  fetchPagedAdminResource('/admin/premium/transactions', params, {
    keyword: 'string',
    status: 'string',
    planId: 'number',
    userId: 'number',
    createdFrom: 'truthy',
    createdTo: 'truthy',
  });

/**
 * Lấy chi tiết đầy đủ giao dịch thanh toán theo ID
 * @param {number|string} transactionId
 * @returns {Promise<Object>} AdminTransactionDetailResponse
 */
export const getTransactionById = async (transactionId) => {
  const res = await apiClient.get(`/admin/premium/transactions/${transactionId}`);
  return res.data?.data;
};

/**
 * Xác nhận thủ công giao dịch PENDING thành SUCCESS kèm lý do
 * @param {number|string} transactionId
 * @param {string} reason - Lý do xác nhận (tối đa 500 ký tự)
 * @returns {Promise<Object>} AdminTransactionDetailResponse
 */
export const confirmTransaction = async (transactionId, reason) => {
  const res = await apiClient.put(`/admin/premium/transactions/${transactionId}/confirm`, {
    reason: reason ? reason.trim() : '',
  });
  return res.data?.data;
};

/**
 * Hoàn tiền giao dịch SUCCESS thành REFUNDED kèm lý do
 * @param {number|string} transactionId
 * @param {string} reason - Lý do hoàn tiền (tối đa 500 ký tự)
 * @returns {Promise<Object>} AdminTransactionDetailResponse
 */
export const refundTransaction = async (transactionId, reason) => {
  const res = await apiClient.put(`/admin/premium/transactions/${transactionId}/refund`, {
    reason: reason ? reason.trim() : '',
  });
  return res.data?.data;
};

// =========================================================================
// 3. NHẬT KÝ WEBHOOK THANH TOÁN (AdminPremiumWebhookLogController)
// =========================================================================

/**
 * Tìm kiếm và lọc nhật ký webhook thanh toán với phân trang 1-indexed
 * @param {Object} params - { providerId, transactionId, status, signatureValid, receivedFrom, receivedTo, page, size, sort }
 * @returns {Promise<Object>} PageResponse<AdminWebhookLogResponse>
 */
export const searchWebhookLogs = (params = {}) =>
  fetchPagedAdminResource('/admin/premium/webhook-logs', params, {
    providerId: 'number',
    transactionId: 'number',
    status: 'string',
    signatureValid: 'boolean',
    receivedFrom: 'truthy',
    receivedTo: 'truthy',
  });

/**
 * Lấy chi tiết nhật ký webhook bao gồm raw JSON payload
 * @param {number|string} logId
 * @returns {Promise<Object>} AdminWebhookLogDetailResponse
 */
export const getWebhookLogById = async (logId) => {
  const res = await apiClient.get(`/admin/premium/webhook-logs/${logId}`);
  return res.data?.data;
};
