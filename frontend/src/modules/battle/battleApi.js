import apiClient from '../../services/apiClient';

export const getBattleHistoryApi = async (params = {}) => {
  const query = { page: params.page ?? 1, size: params.size ?? 10 };
  if (params.result) query.result = params.result;
  if (params.mode) query.mode = params.mode;
  if (params.matchType) query.matchType = params.matchType;
  const response = await apiClient.get('/battles/history', { params: query });
  return response.data;
};

export const getBattleDetailApi = async (matchId) => {
  const response = await apiClient.get(`/battles/${matchId}`);
  return response.data;
};

export const getBattleStatsApi = async () => {
  const response = await apiClient.get('/battles/stats');
  return response.data;
};
