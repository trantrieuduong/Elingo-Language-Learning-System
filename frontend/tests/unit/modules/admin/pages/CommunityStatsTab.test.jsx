import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CommunityStatsTab from '@/modules/admin/pages/DashboardPage/tabs/CommunityStatsTab';
import {
  fetchSocialStats,
  fetchModerationStats,
  fetchReportStats,
} from '@/modules/admin/services/adminStatsApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminStatsApi', () => ({
  fetchSocialStats: vi.fn(),
  fetchModerationStats: vi.fn(),
  fetchReportStats: vi.fn(),
}));

const mockSocialData = {
  totalPosts: 1250,
  totalLikes: 8400,
  totalComments: 3100,
  engagementSeries: [
    {
      date: '2026-10-01',
      likeCount: 120,
      commentCount: 45,
      totalInteractions: 165,
    },
    {
      date: '2026-10-02',
      likeCount: 200,
      commentCount: 80,
      totalInteractions: 280,
    },
  ],
};

const mockModerationData = {
  actions: {
    totalActions: 450,
    autoApproveCount: 380,
    manualApproveCount: 70,
    automationRate: 84.4,
  },
  avgManualReviewSeconds: 42,
  backlog: {
    totalBacklog: 8,
    needsReviewCount: 5,
    pendingCount: 3,
  },
  aiLabelDistribution: [
    { category: 'CLEAN', count: 380, percentage: 84.4 },
    { category: 'SPAM', count: 50, percentage: 11.1 },
    { category: 'TOXIC', count: 20, percentage: 4.5 },
  ],
};

const mockReportData = {
  totalReports: 25,
  pendingReports: 3,
  byTargetType: [
    { category: 'POST', count: 18, percentage: 72 },
    { category: 'COMMENT', count: 7, percentage: 28 },
  ],
};

const sampleFilter = {
  from: '2026-10-01',
  to: '2026-10-31',
  granularity: 'DAY',
};

describe('CommunityStatsTab component', () => {
  const mockNavigate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n and data rendering [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders loading skeletons initially before API settles', async () => {
      fetchSocialStats.mockImplementation(() => new Promise(() => {}));
      fetchModerationStats.mockImplementation(() => new Promise(() => {}));
      fetchReportStats.mockImplementation(() => new Promise(() => {}));

      const { container } = render(
        <CommunityStatsTab filter={sampleFilter} reloadKey={0} onNavigate={mockNavigate} />
      );

      // Skeletons are rendered
      expect(container.querySelectorAll('._skeletonCard_097561, [class*="skeletonCard"]').length).toBeGreaterThan(0);
    });

    it('renders error state when any API call rejects', async () => {
      fetchSocialStats.mockRejectedValueOnce(new Error('Connection failure'));
      fetchModerationStats.mockResolvedValueOnce(mockModerationData);
      fetchReportStats.mockResolvedValueOnce(mockReportData);

      render(<CommunityStatsTab filter={sampleFilter} reloadKey={0} onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Connection failure')).toBeInTheDocument();
      });
    });

    it('renders 5 KPI cards with correct data and badges', async () => {
      fetchSocialStats.mockResolvedValueOnce(mockSocialData);
      fetchModerationStats.mockResolvedValueOnce(mockModerationData);
      fetchReportStats.mockResolvedValueOnce(mockReportData);

      render(<CommunityStatsTab filter={sampleFilter} reloadKey={0} onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(
          screen.getByText(lng === 'vi' ? 'Tổng bài viết thảo luận' : 'Total Discussion Posts')
        ).toBeInTheDocument();
      });

      // Automation rate card
      expect(screen.getByText('84.4%')).toBeInTheDocument();

      // Avg review time card
      expect(screen.getByText('42s')).toBeInTheDocument();

      // Moderation backlog KPI
      expect(
        screen.getByText(lng === 'vi' ? 'Bài viết chờ kiểm duyệt' : 'Pending Moderation Posts')
      ).toBeInTheDocument();
      expect(screen.getByText('8')).toBeInTheDocument();

      // Reports KPI
      expect(
        screen.getByText(lng === 'vi' ? 'Báo cáo chờ xử lý' : 'Pending Violation Reports')
      ).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
    });

    it('navigates to /admin/posts when clicking moderation backlog card', async () => {
      const user = userEvent.setup();
      fetchSocialStats.mockResolvedValueOnce(mockSocialData);
      fetchModerationStats.mockResolvedValueOnce(mockModerationData);
      fetchReportStats.mockResolvedValueOnce(mockReportData);

      render(<CommunityStatsTab filter={sampleFilter} reloadKey={0} onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('8')).toBeInTheDocument();
      });

      const moderationTitle = lng === 'vi' ? 'Bài viết chờ kiểm duyệt' : 'Pending Moderation Posts';
      const moderationCard = screen.getByText(moderationTitle).closest('div');
      await user.click(moderationCard);

      expect(mockNavigate).toHaveBeenCalledWith('/admin/posts');
    });

    it('navigates to /admin/reports when clicking pending reports card', async () => {
      const user = userEvent.setup();
      fetchSocialStats.mockResolvedValueOnce(mockSocialData);
      fetchModerationStats.mockResolvedValueOnce(mockModerationData);
      fetchReportStats.mockResolvedValueOnce(mockReportData);

      render(<CommunityStatsTab filter={sampleFilter} reloadKey={0} onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('3')).toBeInTheDocument();
      });

      const reportTitle =
        lng === 'vi' ? 'Báo cáo chờ xử lý' : 'Pending Violation Reports';
      const reportCard = screen.getByText(reportTitle).closest('div');
      await user.click(reportCard);

      expect(mockNavigate).toHaveBeenCalledWith('/admin/reports');
    });

    it('re-fetches data when reloadKey changes', async () => {
      fetchSocialStats.mockResolvedValue(mockSocialData);
      fetchModerationStats.mockResolvedValue(mockModerationData);
      fetchReportStats.mockResolvedValue(mockReportData);

      const { rerender } = render(
        <CommunityStatsTab filter={sampleFilter} reloadKey={0} onNavigate={mockNavigate} />
      );

      await waitFor(() => {
        expect(fetchSocialStats).toHaveBeenCalledTimes(1);
      });

      rerender(<CommunityStatsTab filter={sampleFilter} reloadKey={1} onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(fetchSocialStats).toHaveBeenCalledTimes(2);
      });
    });
  });
});
