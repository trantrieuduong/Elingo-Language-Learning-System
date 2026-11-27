import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LessonStatusModal from '@/modules/admin/pages/LessonManagementPage/components/LessonStatusModal';
import * as adminContentApi from '@/modules/admin/services/adminContentApi';
import { useLang, LANGS, tr } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminContentApi', () => ({
  publishAdminLesson: vi.fn(),
  unpublishAdminLesson: vi.fn(),
  archiveAdminLesson: vi.fn(),
}));

// Helper: lấy chuỗi i18n của namespace admin -> content.lessons.*
const L = (key, options) => tr(`admin:content.lessons.${key}`, options);

describe('LessonStatusModal component', () => {
  const mockOnClose = vi.fn();
  const mockOnSuccess = vi.fn();

  const sampleLesson = {
    id: 1,
    title: 'English Grammar Basics',
    status: 'DRAFT',
  };

  const defaultProps = {
    isOpen: true,
    lesson: sampleLesson,
    onClose: mockOnClose,
    onSuccess: mockOnSuccess,
  };

  const getStatusSelect = () => screen.getByLabelText(L('statusFilter'));
  const getSaveBtn = () => screen.getByRole('button', { name: L('saveBtn') });

  beforeEach(() => {
    vi.clearAllMocks();
    adminContentApi.publishAdminLesson.mockReset();
    adminContentApi.unpublishAdminLesson.mockReset();
    adminContentApi.archiveAdminLesson.mockReset();
  });

  describe.each(LANGS)('i18n rendering and status change operations [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders modal with lesson title', () => {
      render(<LessonStatusModal {...defaultProps} />);

      expect(
        screen.getByRole('heading', { name: L('changeStatusBtn') })
      ).toBeInTheDocument();
      expect(screen.getByText('English Grammar Basics')).toBeInTheDocument();
    });

    it('shows current status selected by default', () => {
      render(<LessonStatusModal {...defaultProps} />);

      expect(getStatusSelect()).toHaveValue('DRAFT');
      expect(getStatusSelect()).toHaveDisplayValue(L('statusDraft'));
    });

    it('publishes lesson when changing status to PUBLISHED', async () => {
      const updated = { ...sampleLesson, status: 'PUBLISHED' };
      adminContentApi.publishAdminLesson.mockResolvedValueOnce(updated);

      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      await user.selectOptions(getStatusSelect(), 'PUBLISHED');
      await user.click(getSaveBtn());

      await waitFor(() => {
        expect(adminContentApi.publishAdminLesson).toHaveBeenCalledWith(1);
        expect(mockOnSuccess).toHaveBeenCalledWith(updated);
      });
    });

    it('unpublishes lesson when changing status to DRAFT', async () => {
      const publishedLesson = { ...sampleLesson, status: 'PUBLISHED' };
      const updated = { ...publishedLesson, status: 'DRAFT' };
      adminContentApi.unpublishAdminLesson.mockResolvedValueOnce(updated);

      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} lesson={publishedLesson} />);

      await user.selectOptions(getStatusSelect(), 'DRAFT');
      await user.click(getSaveBtn());

      await waitFor(() => {
        expect(adminContentApi.unpublishAdminLesson).toHaveBeenCalledWith(1);
        expect(mockOnSuccess).toHaveBeenCalledWith(updated);
      });
    });

    it('archives lesson when changing status to ARCHIVED', async () => {
      const updated = { ...sampleLesson, status: 'ARCHIVED' };
      adminContentApi.archiveAdminLesson.mockResolvedValueOnce(updated);

      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      await user.selectOptions(getStatusSelect(), 'ARCHIVED');
      await user.click(getSaveBtn());

      await waitFor(() => {
        expect(adminContentApi.archiveAdminLesson).toHaveBeenCalledWith(1);
        expect(mockOnSuccess).toHaveBeenCalledWith(updated);
      });
    });

    it('disables save button when status is unchanged', () => {
      render(<LessonStatusModal {...defaultProps} />);

      expect(getSaveBtn()).toBeDisabled();
    });

    it('enables save button when status changes', async () => {
      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      await user.selectOptions(getStatusSelect(), 'PUBLISHED');

      expect(getSaveBtn()).toBeEnabled();
    });

    it('displays hint text for PUBLISHED status', async () => {
      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      expect(screen.queryByText(L('statusPublishedHint'))).not.toBeInTheDocument();

      await user.selectOptions(getStatusSelect(), 'PUBLISHED');

      expect(screen.getByText(L('statusPublishedHint'))).toBeInTheDocument();
    });

    it('displays hint text for ARCHIVED status', async () => {
      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      expect(screen.queryByText(L('statusArchivedHint'))).not.toBeInTheDocument();

      await user.selectOptions(getStatusSelect(), 'ARCHIVED');

      expect(screen.getByText(L('statusArchivedHint'))).toBeInTheDocument();
    });

    it('closes modal when close (X) button is clicked', async () => {
      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      // Nút X dùng aria-label (không có text) nên getByLabelText chỉ trả về đúng nút này
      await user.click(screen.getByLabelText(L('cancelBtn')));

      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });

    it('closes modal when cancel button is clicked', async () => {
      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      // getByRole('button', { name: 'Cancel' }) sẽ khớp cả nút X (aria-label) lẫn nút Hủy,
      // nên tìm theo text hiển thị để chỉ lấy nút Hủy ở footer.
      await user.click(screen.getByText(L('cancelBtn')));

      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });

    it('closes modal when backdrop is clicked', async () => {
      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      const backdrop = screen.getByRole('dialog').parentElement;
      await user.click(backdrop);

      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });

    it('does not close modal when dialog content is clicked', async () => {
      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      await user.click(screen.getByRole('dialog'));

      expect(mockOnClose).not.toHaveBeenCalled();
    });

    it('displays error message when status update fails', async () => {
      adminContentApi.publishAdminLesson.mockRejectedValueOnce(
        new Error('Cannot publish lesson without segments')
      );

      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      await user.selectOptions(getStatusSelect(), 'PUBLISHED');
      await user.click(getSaveBtn());

      expect(
        await screen.findByText('Cannot publish lesson without segments')
      ).toBeInTheDocument();
      expect(mockOnSuccess).not.toHaveBeenCalled();
    });

    it('prefers server error message from response', async () => {
      adminContentApi.publishAdminLesson.mockRejectedValueOnce({
        response: { data: { message: 'Server says no' } },
      });

      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      await user.selectOptions(getStatusSelect(), 'PUBLISHED');
      await user.click(getSaveBtn());

      expect(await screen.findByText('Server says no')).toBeInTheDocument();
    });

    it('disables controls while submitting', async () => {
      let resolvePublish;
      adminContentApi.publishAdminLesson.mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolvePublish = resolve;
          })
      );

      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      await user.selectOptions(getStatusSelect(), 'PUBLISHED');
      await user.click(getSaveBtn());

      await waitFor(() => {
        expect(getSaveBtn()).toBeDisabled();
        expect(getStatusSelect()).toBeDisabled();
      });

      // Kết thúc request để không để promise treo sau test
      resolvePublish({ ...sampleLesson, status: 'PUBLISHED' });
      await waitFor(() => expect(mockOnSuccess).toHaveBeenCalled());
    });

    it('returns null when modal is closed', () => {
      const { container } = render(<LessonStatusModal {...defaultProps} isOpen={false} />);

      expect(container.firstChild).toBeNull();
    });

    it('returns null when lesson is not provided', () => {
      const { container } = render(<LessonStatusModal {...defaultProps} lesson={null} />);

      expect(container.firstChild).toBeNull();
    });

    it('clears error when status changes after error', async () => {
      adminContentApi.publishAdminLesson.mockRejectedValueOnce(new Error('Network error'));

      const user = userEvent.setup();
      render(<LessonStatusModal {...defaultProps} />);

      await user.selectOptions(getStatusSelect(), 'PUBLISHED');
      await user.click(getSaveBtn());

      expect(await screen.findByText('Network error')).toBeInTheDocument();

      await user.selectOptions(getStatusSelect(), 'ARCHIVED');

      expect(screen.queryByText('Network error')).not.toBeInTheDocument();
    });

    it('renders all three status options', () => {
      render(<LessonStatusModal {...defaultProps} />);

      [L('statusDraft'), L('statusPublished'), L('statusArchived')].forEach((label) => {
        expect(screen.getByRole('option', { name: label })).toBeInTheDocument();
      });
    });

    it('handles already archived lesson', () => {
      const archivedLesson = { ...sampleLesson, status: 'ARCHIVED' };
      render(<LessonStatusModal {...defaultProps} lesson={archivedLesson} />);

      expect(getStatusSelect()).toHaveValue('ARCHIVED');
      expect(getSaveBtn()).toBeDisabled();
    });

    it('resets form state when modal reopens with another lesson', () => {
      const { rerender } = render(<LessonStatusModal {...defaultProps} />);

      rerender(<LessonStatusModal {...defaultProps} isOpen={false} />);

      const updatedLesson = { ...sampleLesson, status: 'PUBLISHED' };
      rerender(<LessonStatusModal {...defaultProps} lesson={updatedLesson} isOpen={true} />);

      expect(getStatusSelect()).toHaveValue('PUBLISHED');
    });
  });
});
