import apiClient from '../../../services/apiClient';
import { fetchPagedAdminResource } from '../utils/adminApiUtils';

/**
 * Tìm kiếm và lọc danh sách bài viết trong hàng đợi kiểm duyệt (phân trang 1-indexed)
 */
export const searchModerationPosts = (params = {}) =>
  fetchPagedAdminResource('/admin/community/moderation/posts', params, {
    status: 'string',
    groupId: 'value',
    userId: 'value',
    keyword: 'string',
    createdFrom: 'truthy',
    createdTo: 'truthy',
  });

/**
 * Lấy chi tiết bài viết kèm lịch sử kiểm duyệt AI và Admin
 */
export const getModerationPostDetail = async (postId) => {
  const res = await apiClient.get(`/admin/community/moderation/posts/${postId}`);
  return res.data?.data;
};

/**
 * Tìm kiếm và lọc nhật ký kiểm duyệt cộng đồng (phân trang 1-indexed)
 */
export const searchModerationLogs = (params = {}) =>
  fetchPagedAdminResource('/admin/community/moderation/logs', params, {
    postId: 'value',
    actorType: 'string',
    action: 'string',
    aiLabel: 'string',
    createdFrom: 'truthy',
    createdTo: 'truthy',
  });

/**
 * Duyệt tay bài viết (chuyển trạng thái sang PUBLISHED)
 */
export const approveModerationPost = async (postId, note) => {
  const payload = note && note.trim() ? { note: note.trim() } : {};
  const res = await apiClient.put(`/admin/community/moderation/posts/${postId}/approve`, payload);
  return res.data?.data;
};

/**
 * Từ chối bài viết (chuyển trạng thái sang REJECTED) kèm lý do bắt buộc
 */
export const rejectModerationPost = async (postId, note) => {
  const res = await apiClient.put(`/admin/community/moderation/posts/${postId}/reject`, {
    note: note ? note.trim() : '',
  });
  return res.data?.data;
};

/**
 * Tìm kiếm và lọc danh sách báo cáo vi phạm (phân trang 1-indexed)
 */
export const searchReports = (params = {}) =>
  fetchPagedAdminResource('/admin/community/reports', params, {
    status: 'string',
    targetType: 'string',
    reporterId: 'value',
    targetId: 'value',
    createdFrom: 'truthy',
    createdTo: 'truthy',
  });

/**
 * Lấy chi tiết báo cáo vi phạm theo ID
 */
export const getReportById = async (reportId) => {
  const res = await apiClient.get(`/admin/community/reports/${reportId}`);
  return res.data?.data;
};

/**
 * Xử lý báo cáo vi phạm (RESOLVED)
 */
export const resolveReport = async (reportId, resolutionNote) => {
  const payload =
    resolutionNote && resolutionNote.trim() ? { resolutionNote: resolutionNote.trim() } : {};
  const res = await apiClient.put(`/admin/community/reports/${reportId}/resolve`, payload);
  return res.data?.data;
};

/**
 * Từ chối báo cáo vi phạm (REJECTED) kèm lý do bắt buộc
 */
export const rejectReport = async (reportId, resolutionNote) => {
  const res = await apiClient.put(`/admin/community/reports/${reportId}/reject`, {
    resolutionNote: resolutionNote ? resolutionNote.trim() : '',
  });
  return res.data?.data;
};

/**
 * Lấy danh sách nhóm bài viết để hiển thị trong bộ lọc
 */
export const getPostGroups = async () => {
  const res = await apiClient.get('/post-groups');
  return res.data?.data || [];
};

/**
 * Tìm kiếm và lọc danh sách nhóm học tập phía admin (phân trang 1-indexed)
 */
export const searchPostGroups = (params = {}) =>
  fetchPagedAdminResource('/admin/community/groups', params, {
    keyword: 'string',
  });

/**
 * Lấy chi tiết nhóm học tập theo ID
 */
export const getPostGroupById = async (groupId) => {
  const res = await apiClient.get(`/admin/community/groups/${groupId}`);
  return res.data?.data;
};

/**
 * Tạo nhóm học tập mới
 */
export const createPostGroup = async (payload) => {
  const res = await apiClient.post('/admin/community/groups', {
    groupName: payload.groupName ? payload.groupName.trim() : '',
    description:
      payload.description && payload.description.trim() ? payload.description.trim() : null,
  });
  return res.data?.data;
};

/**
 * Cập nhật toàn bộ thông tin nhóm học tập (PUT ghi đè)
 */
export const updatePostGroup = async (groupId, payload) => {
  const res = await apiClient.put(`/admin/community/groups/${groupId}`, {
    groupName: payload.groupName ? payload.groupName.trim() : '',
    description:
      payload.description && payload.description.trim() ? payload.description.trim() : null,
  });
  return res.data?.data;
};

/**
 * Xóa nhóm học tập rỗng
 */
export const deletePostGroup = async (groupId) => {
  const res = await apiClient.delete(`/admin/community/groups/${groupId}`);
  return res.data?.data;
};
