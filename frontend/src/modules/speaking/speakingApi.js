import apiClient from '../../services/apiClient';

// params: { page, size, keyword?, maxParticipants? }
export const getSpeakingRoomsApi = async (params = {}) => {
  const query = { page: params.page ?? 1, size: params.size ?? 10 };
  const keyword = (params.keyword ?? '').trim();
  if (keyword) query.keyword = keyword;
  if (params.maxParticipants) query.maxParticipants = params.maxParticipants;
  const response = await apiClient.get('/speaking-rooms', { params: query });
  return response.data;
};

export const getSpeakingRoomApi = async (roomCode) => {
  const response = await apiClient.get(`/speaking-rooms/${roomCode}`);
  return response.data;
};

// lấy config ICE server để tạo peer connection
export const getIceConfigApi = async () => {
  const response = await apiClient.get('/speaking-rooms/ice-config');
  return response.data;
};

// lấy danh sách lịch sử phòng nói chuyện của user hiện tại
export const getSpeakingHistoryApi = async (params = {}) => {
  const query = { page: params.page ?? 1, size: params.size ?? 10 };
  const keyword = (params.keyword ?? '').trim();
  if (keyword) query.keyword = keyword;
  if (params.maxParticipants) query.maxParticipants = params.maxParticipants;
  const response = await apiClient.get('/speaking-rooms/history', { params: query });
  return response.data;
};

// lấy tổng kết lịch sử phòng nói chuyện: { totalSeconds, roomCount, sessionCount }
export const getSpeakingHistorySummaryApi = async () => {
  const response = await apiClient.get('/speaking-rooms/history/summary');
  return response.data;
};

// lấy danh sách thành viên bị cấm (chỉ dành cho host)
export const getBansApi = async (roomCode) => {
  const response = await apiClient.get(`/speaking-rooms/${roomCode}/bans`);
  return response.data;
};

