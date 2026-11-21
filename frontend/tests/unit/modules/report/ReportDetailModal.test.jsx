import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ReportDetailModal from '../../../../src/modules/report/components/ReportDetailModal/ReportDetailModal';
import { getMyReportApi } from '../../../../src/modules/report/reportApi';
import { LANGS, tr, useLang } from '../../../utils/i18n';

vi.mock('../../../../src/modules/report/reportApi', () => ({
  getMyReportApi: vi.fn(),
}));

describe('ReportDetailModal component', () => {
  const mockOnClose = vi.fn();

  const mockResolvedReport = {
    id: 12,
    targetType: 'POST',
    description: 'Bài viết chứa nội dung xúc phạm người khác',
    status: 'RESOLVED',
    createdAt: '2026-11-20T10:00:00Z',
    resolvedAt: '2026-11-20T11:00:00Z',
    resolutionNote: 'Đã xóa bài viết vi phạm',
    imageUrls: ['https://example.com/proof1.png', 'https://example.com/proof2.png'],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when isOpen is false', () => {
    const { container } = render(
      <ReportDetailModal
        isOpen={false}
        onClose={mockOnClose}
        reportId={12}
        initialReport={mockResolvedReport}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  describe.each(LANGS)('Rendering with initialReport and fresh fetch [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders initialReport immediately and fetches fresh details from getMyReportApi', async () => {
      getMyReportApi.mockResolvedValueOnce({
        success: true,
        data: {
          ...mockResolvedReport,
          description: 'Mô tả được cập nhật từ backend',
        },
      });

      render(
        <ReportDetailModal
          isOpen={true}
          onClose={mockOnClose}
          reportId={12}
          initialReport={mockResolvedReport}
        />
      );

      // Header and labels
      expect(screen.getByText(tr('report:detailModalTitle'))).toBeInTheDocument();
      expect(screen.getByText(tr('report:fields.targetType'))).toBeInTheDocument();
      expect(screen.getByText(tr('report:fields.status'))).toBeInTheDocument();
      expect(screen.getByText(tr('report:fields.description'))).toBeInTheDocument();
      expect(screen.getByText(tr('report:fields.image'))).toBeInTheDocument();
      expect(screen.getByText(tr('report:fields.resolutionNote'))).toBeInTheDocument();

      // Check status badge
      expect(screen.getByText(tr('report:status.RESOLVED'))).toBeInTheDocument();

      // Check images
      const images = screen.getAllByRole('img');
      expect(images).toHaveLength(2);
      expect(images[0]).toHaveAttribute('src', 'https://example.com/proof1.png');

      // Wait for fresh data from getMyReportApi
      await waitFor(() => {
        expect(getMyReportApi).toHaveBeenCalledWith(12);
        expect(screen.getByText('Mô tả được cập nhật từ backend')).toBeInTheDocument();
      });
    });

    it('renders fallback placeholders when report has no images and no resolution note', async () => {
      const pendingReport = {
        id: 13,
        targetType: 'COMMENT',
        description: 'Spam comment',
        status: 'PENDING',
        createdAt: '2026-11-20T10:00:00Z',
        resolvedAt: null,
        resolutionNote: null,
        imageUrls: [],
      };

      getMyReportApi.mockResolvedValueOnce({
        success: true,
        data: pendingReport,
      });

      render(
        <ReportDetailModal
          isOpen={true}
          onClose={mockOnClose}
          reportId={13}
          initialReport={pendingReport}
        />
      );

      expect(screen.getByText(tr('report:status.PENDING'))).toBeInTheDocument();
      expect(screen.getByText(tr('report:noImages'))).toBeInTheDocument();
      expect(screen.getByText(tr('report:noResolutionNote'))).toBeInTheDocument();
    });
  });

  describe('User interactions and modal closing', () => {
    it('calls onClose when close button in header or footer is clicked', async () => {
      const user = userEvent.setup();
      getMyReportApi.mockResolvedValueOnce({
        success: true,
        data: mockResolvedReport,
      });

      render(
        <ReportDetailModal
          isOpen={true}
          onClose={mockOnClose}
          reportId={12}
          initialReport={mockResolvedReport}
        />
      );

      // Close button in header (icon text "close")
      const headerCloseBtn = screen.getByRole('button', { name: 'close' });
      await user.click(headerCloseBtn);
      expect(mockOnClose).toHaveBeenCalledTimes(1);

      // Close button in footer
      const footerCloseBtn = screen.getByRole('button', { name: tr('common:close') });
      await user.click(footerCloseBtn);
      expect(mockOnClose).toHaveBeenCalledTimes(2);
    });

    it('calls onClose when pressing Escape key', async () => {
      const user = userEvent.setup();
      getMyReportApi.mockResolvedValueOnce({
        success: true,
        data: mockResolvedReport,
      });

      render(
        <ReportDetailModal
          isOpen={true}
          onClose={mockOnClose}
          reportId={12}
          initialReport={mockResolvedReport}
        />
      );

      await user.keyboard('{Escape}');
      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });

    it('renders REJECTED status with error badge', () => {
      const rejectedReport = {
        ...mockResolvedReport,
        status: 'REJECTED',
      };
      getMyReportApi.mockResolvedValueOnce({
        success: true,
        data: rejectedReport,
      });

      render(
        <ReportDetailModal
          isOpen={true}
          onClose={mockOnClose}
          reportId={12}
          initialReport={rejectedReport}
        />
      );

      expect(screen.getByText(tr('report:status.REJECTED'))).toHaveClass('badge--error');
    });

    it('renders RESOLVED status with mint badge', () => {
      const resolvedReport = {
        ...mockResolvedReport,
        status: 'RESOLVED',
      };
      getMyReportApi.mockResolvedValueOnce({
        success: true,
        data: resolvedReport,
      });

      render(
        <ReportDetailModal
          isOpen={true}
          onClose={mockOnClose}
          reportId={12}
          initialReport={resolvedReport}
        />
      );

      expect(screen.getByText(tr('report:status.RESOLVED'))).toHaveClass('badge--mint');
    });

    it('handles getMyReportApi error by retaining initialReport', async () => {
      getMyReportApi.mockRejectedValueOnce(new Error('Network error'));

      render(
        <ReportDetailModal
          isOpen={true}
          onClose={mockOnClose}
          reportId={12}
          initialReport={mockResolvedReport}
        />
      );

      await waitFor(() => {
        expect(getMyReportApi).toHaveBeenCalledWith(12);
      });

      expect(screen.getByText('Bài viết chứa nội dung xúc phạm người khác')).toBeInTheDocument();
    });
  });
});
