import apiClient from '../../../services/apiClient';

/**
 * Format query params for DateRange filter
 * @param {Object} params - { from: 'YYYY-MM-DD', to: 'YYYY-MM-DD', granularity: 'DAY' | 'WEEK' | 'MONTH' }
 */
const buildParams = (params = {}) => {
  const query = {};
  if (params.from) query.from = params.from;
  if (params.to) query.to = params.to;
  if (params.granularity) query.granularity = params.granularity;
  return query;
};

/* ============================================================
   1. USER STATS (UserStatsController)
   ============================================================ */
export const fetchUserGrowth = async (params) => {
  const res = await apiClient.get('/admin/stats/users/growth', { params: buildParams(params) });
  return res.data?.data;
};

export const fetchUserActivity = async (params) => {
  const res = await apiClient.get('/admin/stats/users/activity', { params: buildParams(params) });
  return res.data?.data;
};

export const fetchUserBreakdown = async () => {
  const res = await apiClient.get('/admin/stats/users/breakdown');
  return res.data?.data;
};

/* ============================================================
   2. REVENUE STATS (RevenueStatsController)
   ============================================================ */
export const fetchRevenueTimeSeries = async (params) => {
  const res = await apiClient.get('/admin/stats/revenue/timeseries', {
    params: buildParams(params),
  });
  return res.data?.data;
};

export const fetchRevenueBreakdown = async (params) => {
  const res = await apiClient.get('/admin/stats/revenue/breakdown', {
    params: buildParams(params),
  });
  return res.data?.data;
};

export const fetchSubscriptionStats = async () => {
  const res = await apiClient.get('/admin/stats/revenue/subscriptions');
  return res.data?.data;
};

/* ============================================================
   3. LEARNING STATS (LearningStatsController)
   ============================================================ */
export const fetchLearningActivity = async (params) => {
  const res = await apiClient.get('/admin/stats/learning/activity', {
    params: buildParams(params),
  });
  return res.data?.data;
};

export const fetchDeckStats = async (limit = 10) => {
  const res = await apiClient.get('/admin/stats/learning/decks', { params: { limit } });
  return res.data?.data;
};

export const fetchLessonStats = async (limit = 10) => {
  const res = await apiClient.get('/admin/stats/learning/lessons', { params: { limit } });
  return res.data?.data;
};

/* ============================================================
   4. COMMUNITY STATS (CommunityStatsController)
   ============================================================ */
export const fetchSocialStats = async (params) => {
  const res = await apiClient.get('/admin/stats/community/social', { params: buildParams(params) });
  return res.data?.data;
};

export const fetchModerationStats = async (params) => {
  const res = await apiClient.get('/admin/stats/community/moderation', {
    params: buildParams(params),
  });
  return res.data?.data;
};

export const fetchReportStats = async (params) => {
  const res = await apiClient.get('/admin/stats/community/reports', {
    params: buildParams(params),
  });
  return res.data?.data;
};
