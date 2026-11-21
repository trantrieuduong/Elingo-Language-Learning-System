import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ReportModal from '../../../../src/modules/report/components/ReportModal/ReportModal';
import { createReportApi } from '../../../../src/modules/report/reportApi';
import { uploadFileForVerification } from '../../../../src/services/uploadFile';
import { LANGS, tr, trRx, useLang } from '../../../utils/i18n';

vi.mock('../../../../src/modules/report/reportApi', () => ({
  createReportApi: vi.fn(),
}));

vi.mock('../../../../src/services/uploadFile', () => ({
  uploadFileForVerification: vi.fn(),
}));

describe('ReportModal component', () => {
  const mockOnClose = vi.fn();
  const mockOnSuccess = vi.fn();

  let mockUrlCounter = 0;

  beforeEach(() => {
    vi.clearAllMocks();
    mockUrlCounter = 0;
    URL.createObjectURL = vi.fn(() => `blob:mock-url-${++mockUrlCounter}`);
    URL.revokeObjectURL = vi.fn();
  });

  it('renders nothing when isOpen is false', () => {
    const { container } = render(
      <ReportModal isOpen={false} onClose={mockOnClose} targetType="POST" targetId={100} />
    );
    expect(container.firstChild).toBeNull();
    // container: Phần tử DOM mà React Testing Library tạo ra để chứa component được render
  });

  describe.each(LANGS)('Rendering and i18n [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders modal with target badge, label, description input and action buttons', () => {
      render(
        <ReportModal
          isOpen={true}
          onClose={mockOnClose}
          targetType="LESSON"
          targetId={55}
          targetLabel="Bài học tiếng Anh sơ sài"
        />
      );

      expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent(tr('report:modalTitle'));
      expect(screen.getByText(tr('report:targetTypes.LESSON'))).toBeInTheDocument();
      expect(screen.getByText('Bài học tiếng Anh sơ sài')).toBeInTheDocument();
      expect(screen.getByLabelText(trRx('report:descriptionLabel'))).toBeInTheDocument();
      expect(screen.getByRole('button', { name: tr('common:cancel') })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: tr('report:submit') })).toBeInTheDocument();

      // Submit button is disabled initially because description is under 10 chars
      expect(screen.getByRole('button', { name: tr('report:submit') })).toBeDisabled();
    });
  });

  describe('Form validation and submission', () => {
    it('enables submit button only when description length >= 10 chars', async () => {
      const user = userEvent.setup();
      render(<ReportModal isOpen={true} onClose={mockOnClose} targetType="POST" targetId={10} />);

      const textarea = screen.getByLabelText(trRx('report:descriptionLabel'));
      const submitBtn = screen.getByRole('button', { name: tr('report:submit') });

      expect(submitBtn).toBeDisabled();

      await user.type(textarea, 'Short');
      expect(submitBtn).toBeDisabled();

      await user.type(textarea, ' text more than 10');
      expect(submitBtn).toBeEnabled();
    });

    it('submits successfully without images and triggers onSuccess and onClose', async () => {
      const user = userEvent.setup();
      createReportApi.mockResolvedValueOnce({ success: true });

      render(
        <ReportModal
          isOpen={true}
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
          targetType="POST"
          targetId={123}
          targetLabel="Spam post"
        />
      );

      const textarea = screen.getByLabelText(trRx('report:descriptionLabel'));
      await user.type(textarea, 'Bài viết chứa nội dung quấy rối và spam');

      const submitBtn = screen.getByRole('button', { name: tr('report:submit') });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(createReportApi).toHaveBeenCalledWith({
          targetType: 'POST',
          targetId: 123,
          description: 'Bài viết chứa nội dung quấy rối và spam',
          imageUrls: [],
        });
        expect(mockOnSuccess).toHaveBeenCalledTimes(1);
        expect(mockOnClose).toHaveBeenCalledTimes(1);
      });
    });

    it('uploads images before creating report', async () => {
      uploadFileForVerification
        .mockResolvedValueOnce('verified-key-1')
        .mockResolvedValueOnce('verified-key-2');
      createReportApi.mockResolvedValueOnce({ success: true });

      const { baseElement } = render(
        <ReportModal
          isOpen={true}
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
          targetType="USER"
          targetId={88}
        />
      );

      const fileInput = baseElement.querySelector('input[type="file"]');
      const file1 = new File(['dummy1'], 'proof1.png', { type: 'image/png' });
      const file2 = new File(['dummy2'], 'proof2.jpeg', { type: 'image/jpeg' });

      fireEvent.change(fileInput, { target: { files: [file1, file2] } });

      // Check previews rendered
      expect(screen.getAllByRole('img')).toHaveLength(2);

      const user = userEvent.setup();
      const textarea = screen.getByLabelText(trRx('report:descriptionLabel'));
      await user.type(textarea, 'Người dùng gian lận trong đấu trường từ vựng');

      const submitBtn = screen.getByRole('button', { name: tr('report:submit') });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(uploadFileForVerification).toHaveBeenCalledTimes(2);
        expect(createReportApi).toHaveBeenCalledWith({
          targetType: 'USER',
          targetId: 88,
          description: 'Người dùng gian lận trong đấu trường từ vựng',
          imageUrls: ['verified-key-1', 'verified-key-2'],
        });
        expect(mockOnSuccess).toHaveBeenCalledTimes(1);
        expect(mockOnClose).toHaveBeenCalledTimes(1);
      });
    });

    it('removes selected image when clicking remove button', async () => {
      const user = userEvent.setup();
      const { baseElement } = render(
        <ReportModal isOpen={true} onClose={mockOnClose} targetType="POST" targetId={10} />
      );

      const fileInput = baseElement.querySelector('input[type="file"]');
      const file = new File(['dummy'], 'proof.png', { type: 'image/png' });
      fireEvent.change(fileInput, { target: { files: [file] } });

      expect(screen.getAllByRole('img')).toHaveLength(1);

      // Click remove button on preview
      const removeBtn = baseElement.querySelector('.report-modal__media-remove');
      expect(removeBtn).toBeInTheDocument();
      await user.click(removeBtn);

      expect(screen.queryAllByRole('img')).toHaveLength(0);
      expect(URL.revokeObjectURL).toHaveBeenCalled();
    });

    it('shows error message if API call fails', async () => {
      const user = userEvent.setup();
      createReportApi.mockRejectedValueOnce({
        response: { data: { message: 'Báo cáo thất bại do tài khoản bị hạn chế' } },
      });

      render(
        <ReportModal isOpen={true} onClose={mockOnClose} targetType="COMMENT" targetId={999} />
      );

      const textarea = screen.getByLabelText(trRx('report:descriptionLabel'));
      await user.type(textarea, 'Bình luận chứa nội dung phản cảm');

      const submitBtn = screen.getByRole('button', { name: tr('report:submit') });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText('Báo cáo thất bại do tài khoản bị hạn chế')).toBeInTheDocument();
      });
      expect(mockOnClose).not.toHaveBeenCalled();
    });

    it('shows error message if uploading file fails', async () => {
      uploadFileForVerification.mockRejectedValueOnce(new Error('Tải ảnh lên thất bại'));

      const { baseElement } = render(
        <ReportModal isOpen={true} onClose={mockOnClose} targetType="DECK" targetId={77} />
      );

      const fileInput = baseElement.querySelector('input[type="file"]');
      const file = new File(['dummy'], 'proof.png', { type: 'image/png' });
      fireEvent.change(fileInput, { target: { files: [file] } });

      const user = userEvent.setup();
      const textarea = screen.getByLabelText(trRx('report:descriptionLabel'));
      await user.type(textarea, 'Bộ thẻ chứa nội dung sai lệch và bạo lực');

      const submitBtn = screen.getByRole('button', { name: tr('report:submit') });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText('Tải ảnh lên thất bại')).toBeInTheDocument();
      });
      expect(createReportApi).not.toHaveBeenCalled();
    });

    it('closes modal when cancel button or header close icon is clicked', async () => {
      const user = userEvent.setup();
      const { baseElement } = render(
        <ReportModal isOpen={true} onClose={mockOnClose} targetType="POST" targetId={10} />
      );

      const cancelBtn = screen.getByRole('button', { name: tr('common:cancel') });
      // getByRole tự động fail nếu không tìm thấy phần tử
      await user.click(cancelBtn);
      expect(mockOnClose).toHaveBeenCalledTimes(1);

      const headerCloseBtn = screen.getByRole('button', { name: 'close' });
      await user.click(headerCloseBtn);
      expect(mockOnClose).toHaveBeenCalledTimes(2);
    });

    it('closes modal when pressing Escape', async () => {
      const user = userEvent.setup();
      render(<ReportModal isOpen={true} onClose={mockOnClose} targetType="POST" targetId={10} />);

      await user.keyboard('{Escape}');
      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });

    it('warns when selecting more than maximum allowed images (5 slots)', () => {
      const { baseElement } = render(
        <ReportModal isOpen={true} onClose={mockOnClose} targetType="POST" targetId={10} />
      );

      const fileInput = baseElement.querySelector('input[type="file"]');
      const files = Array.from(
        { length: 6 },
        (_, i) => new File(['dummy'], `proof${i}.png`, { type: 'image/png' })
      );

      fireEvent.change(fileInput, { target: { files } });

      // Only 5 files accepted
      expect(
        screen.getByText(tr('common:firstFilesSelected', { slots: 5 }))
      ).toBeInTheDocument();

      expect(screen.getAllByRole('img')).toHaveLength(5);
    });
  });
});
