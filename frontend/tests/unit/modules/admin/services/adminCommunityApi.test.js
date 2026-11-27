import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  searchModerationPosts,
  getModerationPostDetail,
  searchModerationLogs,
  approveModerationPost,
  rejectModerationPost,
  searchReports,
  getReportById,
  resolveReport,
  rejectReport,
  getPostGroups,
  searchPostGroups,
  getPostGroupById,
  createPostGroup,
  updatePostGroup,
  deletePostGroup,
} from '@/modules/admin/services/adminCommunityApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('adminCommunityApi service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('searchModerationPosts sends normalized query params', async () => {
    const mockPage = { content: [], page: 1, size: 20, totalElements: 0, totalPages: 0, isLast: true };
    apiClient.get.mockResolvedValueOnce({ data: { data: mockPage } });

    const res = await searchModerationPosts({
      status: 'NEEDS_REVIEW',
      groupId: 2,
      userId: 5,
      keyword: '  ielts  ',
      createdFrom: '2026-03-01',
      createdTo: '2026-03-10',
      page: 1,
      size: 20,
      sort: 'createdAt,desc',
    });

    expect(apiClient.get).toHaveBeenCalledWith('/admin/community/moderation/posts', {
      params: {
        status: 'NEEDS_REVIEW',
        groupId: 2,
        userId: 5,
        keyword: 'ielts',
        createdFrom: '2026-03-01',
        createdTo: '2026-03-10',
        page: 1,
        size: 20,
        sort: 'createdAt,desc',
      },
    });
    expect(res).toEqual(mockPage);
  });

  it('getModerationPostDetail fetches post detail by id', async () => {
    const detail = { id: 105, title: 'Sample' };
    apiClient.get.mockResolvedValueOnce({ data: { data: detail } });

    const res = await getModerationPostDetail(105);

    expect(apiClient.get).toHaveBeenCalledWith('/admin/community/moderation/posts/105');
    expect(res).toEqual(detail);
  });

  it('searchModerationLogs sends normalized log filter params', async () => {
    const mockPage = { content: [], page: 1, size: 20, totalElements: 0, totalPages: 0, isLast: true };
    apiClient.get.mockResolvedValueOnce({ data: { data: mockPage } });

    await searchModerationLogs({
      postId: 105,
      actorType: 'AI',
      action: 'FLAG_FOR_REVIEW',
      aiLabel: '  SPAM  ',
      page: 1,
      size: 20,
    });

    expect(apiClient.get).toHaveBeenCalledWith('/admin/community/moderation/logs', {
      params: {
        postId: 105,
        actorType: 'AI',
        action: 'FLAG_FOR_REVIEW',
        aiLabel: 'SPAM',
        page: 1,
        size: 20,
      },
    });
  });

  it('approveModerationPost and rejectModerationPost call PUT endpoints', async () => {
    apiClient.put.mockResolvedValue({ data: { data: { id: 105 } } });

    await approveModerationPost(105, '  Safe  ');
    expect(apiClient.put).toHaveBeenCalledWith('/admin/community/moderation/posts/105/approve', {
      note: 'Safe',
    });

    await rejectModerationPost(105, '  Spam links  ');
    expect(apiClient.put).toHaveBeenCalledWith('/admin/community/moderation/posts/105/reject', {
      note: 'Spam links',
    });
  });

  it('searchReports and getReportById call report endpoints', async () => {
    const mockPage = { content: [], page: 1, size: 20, totalElements: 0, totalPages: 0, isLast: true };
    apiClient.get.mockResolvedValueOnce({ data: { data: mockPage } });

    await searchReports({ status: 'PENDING', targetType: 'POST', page: 1, size: 20 });
    expect(apiClient.get).toHaveBeenCalledWith('/admin/community/reports', {
      params: { status: 'PENDING', targetType: 'POST', page: 1, size: 20 },
    });

    apiClient.get.mockResolvedValueOnce({ data: { data: { id: 42 } } });
    const detail = await getReportById(42);
    expect(apiClient.get).toHaveBeenCalledWith('/admin/community/reports/42');
    expect(detail).toEqual({ id: 42 });
  });

  it('resolveReport and rejectReport call PUT endpoints', async () => {
    apiClient.put.mockResolvedValue({ data: { data: { id: 42 } } });

    await resolveReport(42, '  Done  ');
    expect(apiClient.put).toHaveBeenCalledWith('/admin/community/reports/42/resolve', {
      resolutionNote: 'Done',
    });

    await rejectReport(42, '  Not valid  ');
    expect(apiClient.put).toHaveBeenCalledWith('/admin/community/reports/42/reject', {
      resolutionNote: 'Not valid',
    });
  });

  it('getPostGroups fetches /post-groups', async () => {
    apiClient.get.mockResolvedValueOnce({ data: { data: [{ id: 1, groupName: 'Grammar' }] } });
    const groups = await getPostGroups();
    expect(apiClient.get).toHaveBeenCalledWith('/post-groups');
    expect(groups).toEqual([{ id: 1, groupName: 'Grammar' }]);
  });

  it('searchPostGroups and getPostGroupById call admin group endpoints', async () => {
    const mockPage = { content: [], page: 1, size: 20, totalElements: 0, totalPages: 0, isLast: true };
    apiClient.get.mockResolvedValueOnce({ data: { data: mockPage } });

    await searchPostGroups({ keyword: '  Grammar  ', page: 1, size: 20, sort: 'createdAt,desc' });
    expect(apiClient.get).toHaveBeenCalledWith('/admin/community/groups', {
      params: { keyword: 'Grammar', page: 1, size: 20, sort: 'createdAt,desc' },
    });

    apiClient.get.mockResolvedValueOnce({ data: { data: { id: 1, groupName: 'Grammar' } } });
    const detail = await getPostGroupById(1);
    expect(apiClient.get).toHaveBeenCalledWith('/admin/community/groups/1');
    expect(detail).toEqual({ id: 1, groupName: 'Grammar' });
  });

  it('createPostGroup, updatePostGroup, and deletePostGroup call write endpoints', async () => {
    apiClient.post.mockResolvedValueOnce({ data: { data: { id: 1 } } });
    await createPostGroup({ groupName: '  Grammar  ', description: '   ' });
    expect(apiClient.post).toHaveBeenCalledWith('/admin/community/groups', {
      groupName: 'Grammar',
      description: null,
    });

    apiClient.put.mockResolvedValueOnce({ data: { data: { id: 1 } } });
    await updatePostGroup(1, { groupName: '  Speaking  ', description: '  Practice  ' });
    expect(apiClient.put).toHaveBeenCalledWith('/admin/community/groups/1', {
      groupName: 'Speaking',
      description: 'Practice',
    });

    apiClient.delete.mockResolvedValueOnce({ data: { success: true } });
    await deletePostGroup(1);
    expect(apiClient.delete).toHaveBeenCalledWith('/admin/community/groups/1');
  });
});
