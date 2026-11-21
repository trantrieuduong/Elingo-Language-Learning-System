import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ReportPage from '../../../../src/modules/report/pages/ReportPage';
import { getMyReportsApi, getMyReportApi } from '../../../../src/modules/report/reportApi';
import { LANGS, tr, trRx, useLang } from '../../../utils/i18n';

vi.mock('../../../../src/modules/report/reportApi', () => ({
  getMyReportsApi: vi.fn(),
  getMyReportApi: vi.fn(),
}));

describe('ReportPage page component', () => {
  const mockNavigate = vi.fn();

  const mockReportList = [
    {
      id: 1,
      targetType: 'POST',
      description: 'Nội dung bài viết vi phạm tiêu chuẩn cộng đồng',
      status: 'PENDING',
      createdAt: '2026-11-20T10:00:00Z',
      resolvedAt: null,
      resolutionNote: null,
      imageUrls: [],
    },
    {
      id: 2,
      targetType: 'USER',
      description: 'Người dùng có hành vi xúc phạm và quấy rối',
      status: 'RESOLVED',
      createdAt: '2026-11-19T08:30:00Z',
      resolvedAt: '2026-11-19T14:00:00Z',
      resolutionNote: 'Đã tạm khóa tài khoản người vi phạm 7 ngày',
      imageUrls: ['https://example.com/proof1.png'],
    },
    {
      id: 3,
      targetType: 'COMMENT',
      description: 'Bình luận spam quảng cáo',
      status: 'REJECTED',
      createdAt: '2026-11-18T09:15:00Z',
      resolvedAt: '2026-11-18T10:00:00Z',
      resolutionNote: 'Không đủ bằng chứng vi phạm',
      imageUrls: [],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('Rendering and i18n [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders report list table with columns, target types and status badges', async () => {
      getMyReportsApi.mockResolvedValueOnce({
        success: true,
        data: {
          content: mockReportList,
          totalPages: 1,
        },
      });

      render(<ReportPage onNavigate={mockNavigate} />);

      // Columns
      await waitFor(() => {
        expect(screen.getByText(tr('report:table.targetType'))).toBeInTheDocument();
        expect(screen.getByText(tr('report:table.description'))).toBeInTheDocument();
        expect(screen.getByText(tr('report:table.status'))).toBeInTheDocument();
        expect(screen.getByText(tr('report:table.createdAt'))).toBeInTheDocument();
        expect(screen.getByText(tr('report:table.actions'))).toBeInTheDocument();
      });

      // Target badges
      expect(screen.getByText(tr('report:targetTypes.POST'))).toBeInTheDocument();
      expect(screen.getByText(tr('report:targetTypes.USER'))).toBeInTheDocument();
      expect(screen.getByText(tr('report:targetTypes.COMMENT'))).toBeInTheDocument();

      // Status badges
      expect(screen.getByText(tr('report:status.PENDING'))).toBeInTheDocument();
      expect(screen.getByText(tr('report:status.RESOLVED'))).toBeInTheDocument();
      expect(screen.getByText(tr('report:status.REJECTED'))).toBeInTheDocument();
    });

    it('renders empty state when there are no reports', async () => {
      getMyReportsApi.mockResolvedValueOnce({
        success: true,
        data: {
          content: [],
          totalPages: 0,
        },
      });

      render(<ReportPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText(tr('report:emptyTitle'))).toBeInTheDocument();
        expect(screen.getByText(tr('report:emptyDescription'))).toBeInTheDocument();
      });
    });

    it('renders error state and retries on action click', async () => {
      const user = userEvent.setup();
      getMyReportsApi
        .mockRejectedValueOnce({
          response: { data: { message: 'Network error occurred' } },
        })
        .mockResolvedValueOnce({
          success: true,
          data: {
            content: mockReportList,
            totalPages: 1,
          },
        });

      render(<ReportPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Network error occurred')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: trRx('common:retry') })).toBeInTheDocument();
      });

      // Click retry
      await user.click(screen.getByRole('button', { name: trRx('common:retry') }));

      await waitFor(() => {
        expect(getMyReportsApi).toHaveBeenCalledTimes(2);
        expect(screen.getByText(tr('report:table.targetType'))).toBeInTheDocument();
      });
    });
  });

  describe('Pagination & modal interaction', () => {
    it('handles pagination page changes', async () => {
      const user = userEvent.setup();
      getMyReportsApi.mockResolvedValue({
        success: true,
        data: {
          content: mockReportList,
          totalPages: 3,
        },
      });

      render(<ReportPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText(tr('report:table.targetType'))).toBeInTheDocument();
      });

      // Check pagination element: click on page number 2 button
      const page2Btn = screen.getByRole('button', { name: '2' });
      await user.click(page2Btn);

      await waitFor(() => {
        expect(getMyReportsApi).toHaveBeenCalledWith({ page: 2, size: 10 });
      });
    });

    it('opens detail modal when clicking view details button and closes it', async () => {
      const user = userEvent.setup();
      getMyReportsApi.mockResolvedValueOnce({
        success: true,
        data: {
          content: [mockReportList[1]],
          totalPages: 1,
        },
      });
      getMyReportApi.mockResolvedValueOnce({
        success: true,
        data: mockReportList[1],
      });

      render(<ReportPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'visibility' })).toBeInTheDocument();
      });

      const detailBtn = screen.getByRole('button', { name: 'visibility' });
      await user.click(detailBtn);

      // Modal opens
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByText(tr('report:detailModalTitle'))).toBeInTheDocument();
      });

      // Click footer close button in modal
      const footerCloseBtn = screen.getByRole('button', { name: tr('common:close') });
      await user.click(footerCloseBtn);

      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });

    it('handles response with success=false', async () => {
      getMyReportsApi.mockResolvedValueOnce({
        success: false,
        message: 'Custom error from backend',
      });

      render(<ReportPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Custom error from backend')).toBeInTheDocument();
      });
    });
  });
});
