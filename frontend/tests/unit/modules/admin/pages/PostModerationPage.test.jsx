import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostModerationPage from '@/modules/admin/pages/PostModerationPage/PostModerationPage';
import * as adminCommunityApi from '@/modules/admin/services/adminCommunityApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/services/apiClient', () => ({
  default: { get: vi.fn(), put: vi.fn() },
}));

vi.mock('@/modules/admin/services/adminCommunityApi', () => ({
  searchModerationPosts: vi.fn(),
  getModerationPostDetail: vi.fn(),
  searchModerationLogs: vi.fn(),
  approveModerationPost: vi.fn(),
  rejectModerationPost: vi.fn(),
  getPostGroups: vi.fn(),
}));

describe('PostModerationPage component', () => {
  const samplePosts = {
    content: [
      {
        id: 105,
        groupId: 1,
        groupName: 'English Grammar',
        author: { id: 15, username: 'john_doe', fullName: 'John Doe', avatarUrl: null },
        title: 'Tips for IELTS Speaking',
        content: 'Practice daily with partner',
        status: 'NEEDS_REVIEW',
        likeCount: 0,
        commentCount: 0,
        medias: [],
        latestModeration: {
          id: 201,
          postId: 105,
          actorType: 'AI',
          action: 'FLAG_FOR_REVIEW',
          aiLabel: 'SPAM',
          aiScore: 0.72,
          note: 'Borderline score',
          createdAt: '2026-03-15T09:30:00',
        },
        createdAt: '2026-03-15T09:00:00',
        updatedAt: '2026-03-15T09:30:00',
      },
    ],
    page: 1,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    isLast: true,
  };

  const sampleLogs = {
    content: [
      {
        id: 201,
        postId: 105,
        actorType: 'AI',
        actorId: null,
        actorUsername: null,
        action: 'FLAG_FOR_REVIEW',
        aiLabel: 'SPAM',
        aiScore: 0.72,
        note: 'Borderline score',
        createdAt: '2026-03-15T09:30:00',
      },
    ],
    page: 1,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    isLast: true,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    adminCommunityApi.getPostGroups.mockResolvedValue([{ id: 1, groupName: 'English Grammar' }]);
    adminCommunityApi.searchModerationPosts.mockResolvedValue(samplePosts);
    adminCommunityApi.searchModerationLogs.mockResolvedValue(sampleLogs);
    adminCommunityApi.getModerationPostDetail.mockResolvedValue({
      ...samplePosts.content[0],
      moderationLogs: sampleLogs.content,
    });
    adminCommunityApi.approveModerationPost.mockResolvedValue({
      ...samplePosts.content[0],
      status: 'PUBLISHED',
    });
  });

  describe.each(LANGS)('PostModerationPage rendering and actions [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('loads NEEDS_REVIEW posts by default and renders table row', async () => {
      render(<PostModerationPage />);

      await waitFor(() => {
        expect(adminCommunityApi.searchModerationPosts).toHaveBeenCalledWith(
          expect.objectContaining({ status: 'NEEDS_REVIEW', page: 1, size: 20 })
        );
      });

      expect(await screen.findByText('Tips for IELTS Speaking')).toBeInTheDocument();
      expect(screen.getByText('john_doe')).toBeInTheDocument();
    });

    it('switches to moderation logs tab and loads logs', async () => {
      const user = userEvent.setup();
      render(<PostModerationPage />);

      await screen.findByText('Tips for IELTS Speaking');
      const logsTab = screen.getByRole('tab', {
        name: lng === 'vi' ? 'Nhật ký kiểm duyệt' : 'Moderation Logs',
      });
      await user.click(logsTab);

      await waitFor(() => {
        expect(adminCommunityApi.searchModerationLogs).toHaveBeenCalledWith(
          expect.objectContaining({ page: 1, size: 20 })
        );
      });
      expect(await screen.findByText('SPAM')).toBeInTheDocument();
    });

    it('opens approve modal and submits manual approval', async () => {
      const user = userEvent.setup();
      render(<PostModerationPage />);

      await screen.findByText('Tips for IELTS Speaking');
      const approveBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Duyệt bài' : 'Approve',
      });
      await user.click(approveBtn);

      const submitBtn = screen.getAllByRole('button', {
        name: lng === 'vi' ? 'Duyệt bài' : 'Approve',
      }).at(-1);
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminCommunityApi.approveModerationPost).toHaveBeenCalledWith(105, '');
      });
    });
  });
});
