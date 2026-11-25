import apiClient from '../../services/apiClient';

export const getMyBadgesApi = async () => {
  const response = await apiClient.get('/users/me/badges');
  return response.data;
};

export const getUserBadgesApi = async (userId) => {
  const response = await apiClient.get(`/users/${userId}/badges`);
  return response.data;
};

export const getMyLevelProgressApi = async () => {
  const response = await apiClient.get('/users/me/levels');
  return response.data;
};

/**
 * Lấy thứ hạng cá nhân và XP cần để vượt hạng tiếp theo.
 * @param {'WEEKLY'|'MONTHLY'} [periodType='WEEKLY']
 * @returns {Promise<{success: boolean, data: Object}>}
 */
export const getMyRankApi = async (periodType = 'WEEKLY') => {
  const response = await apiClient.get('/users/me/rank', {
    params: { periodType },
  });
  return response.data;
};

export const getUserLevelProgressApi = async (userId) => {
  const response = await apiClient.get(`/users/${userId}/levels`);
  return response.data;
};

/**
 * Lấy streak hiện tại, streak dài nhất, lịch check-in và trạng thái hôm nay.
 * @param {Object} [params] - { year, month }
 * @returns {Promise<{success: boolean, data: Object}>}
 */
export const getMyStreakApi = async (params = {}) => {
  const response = await apiClient.get('/users/me/streaks', { params });
  return response.data;
};

export const getUserStreakApi = async (userId, params = {}) => {
  const response = await apiClient.get(`/users/${userId}/streaks`, { params });
  return response.data;
};

/**
 * Lấy danh sách tất cả các level roadmap của hệ thống.
 * @returns {Promise<{success: boolean, data: Array}>}
 */
export const getAllLevelsApi = async () => {
  const response = await apiClient.get('/gamification/levels');
  return response.data;
};

/**
 * Lấy bảng xếp hạng realtime (tuần hoặc tháng), có phân trang.
 * @param {Object} [params] - { periodType, page, size }
 * @returns {Promise<{success: boolean, data: Object}>}
 */
export const getRealtimeLeaderboardApi = async ({
  periodType = 'WEEKLY',
  page = 1,
  size = 20,
} = {}) => {
  const response = await apiClient.get('/gamification/leaderboards', {
    params: {
      periodType,
      page,
      size,
    },
  });
  return response.data;
};

/**
 * Lấy bảng xếp hạng lịch sử theo periodKey (ví dụ '2026-W45' hoặc '2026-10').
 * @param {Object} params - { periodType, periodKey, page, size }
 * @returns {Promise<{success: boolean, data: Object}>}
 */
export const getHistoricalLeaderboardApi = async ({
  periodType,
  periodKey,
  page = 1,
  size = 20,
}) => {
  const response = await apiClient.get('/gamification/leaderboards/history', {
    params: {
      periodType,
      periodKey,
      page,
      size,
    },
  });
  return response.data;
};
