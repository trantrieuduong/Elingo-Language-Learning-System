import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ReportManagementPage from '@/modules/admin/pages/ReportManagementPage/ReportManagementPage';
import * as adminCommunityApi from '@/modules/admin/services/adminCommunityApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/services/apiClient', () => ({
  default: { get: vi.fn(), put: vi.fn() },
}));

vi.mock('@/modules/admin/services/adminCommunityApi', () => ({
  searchReports: vi.fn(),
  getReportById: vi.fn(),
  resolveReport: vi.fn(),
  rejectReport: vi.fn(),
}));

describe('ReportManagementPage component', () => {
  const sampleReports = {
    content: [
      {
        id: 42,
        targetType: 'POST',
        targetId: 105,
        description: 'Spam links in post content',
        status: 'PENDING',
        reporterId: 12,
        reporterUsername: 'jane_doe',
        imageUrls: ['https://cdn.elingo.com/r1.png'],
        resolvedById: null,
        resolvedByUsername: null,
        resolvedAt: null,
        resolutionNote: null,
        createdAt: '2026-03-14T09:00:00',
        updatedAt: '2026-03-14T09:00:00',
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
    adminCommunityApi.searchReports.mockResolvedValue(sampleReports);
    adminCommunityApi.getReportById.mockResolvedValue(sampleReports.content[0]);
    adminCommunityApi.resolveReport.mockResolvedValue({
      ...sampleReports.content[0],
      status: 'RESOLVED',
    });
    adminCommunityApi.rejectReport.mockResolvedValue({
      ...sampleReports.content[0],
      status: 'REJECTED',
    });
  });

  describe.each(LANGS)('ReportManagementPage rendering and actions [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('loads PENDING reports by default and renders table row', async () => {
      render(<ReportManagementPage />);

      await waitFor(() => {
        expect(adminCommunityApi.searchReports).toHaveBeenCalledWith(
          expect.objectContaining({ status: 'PENDING', page: 1, size: 20 })
        );
      });

      expect(await screen.findByText('Spam links in post content')).toBeInTheDocument();
      expect(screen.getByText('jane_doe')).toBeInTheDocument();
    });

    it('validates mandatory reason when rejecting a report', async () => {
      const user = userEvent.setup();
      render(<ReportManagementPage />);

      await screen.findByText('Spam links in post content');
      const rejectBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Từ chối' : 'Reject',
      });
      await user.click(rejectBtn);

      const submitBtn = screen.getAllByRole('button', {
        name: lng === 'vi' ? 'Từ chối' : 'Reject',
      }).at(-1);
      await user.click(submitBtn);

      expect(
        await screen.findByText(
          lng === 'vi' ? 'Vui lòng nhập lý do từ chối báo cáo' : 'Please enter a rejection reason'
        )
      ).toBeInTheDocument();
      expect(adminCommunityApi.rejectReport).not.toHaveBeenCalled();

      const textarea = screen.getByRole('textbox');
      await user.type(textarea, 'No violation');
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminCommunityApi.rejectReport).toHaveBeenCalledWith(42, 'No violation');
      });
    });

    it('renders external target link opening in a new tab', async () => {
      render(<ReportManagementPage />);

      await screen.findByText('Spam links in post content');
      const targetLink = screen.getByRole('link', { name: '#105' });
      expect(targetLink).toHaveAttribute('href', '/community/posts/105');
      expect(targetLink).toHaveAttribute('target', '_blank');
      expect(targetLink).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('resolves a pending report', async () => {
      const user = userEvent.setup();
      render(<ReportManagementPage />);

      await screen.findByText('Spam links in post content');
      const resolveBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xử lý' : 'Resolve',
      });
      await user.click(resolveBtn);

      const submitBtn = screen.getAllByRole('button', {
        name: lng === 'vi' ? 'Xử lý' : 'Resolve',
      }).at(-1);
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminCommunityApi.resolveReport).toHaveBeenCalledWith(42, '');
      });
    });
  });
});
