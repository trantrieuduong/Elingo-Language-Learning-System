import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LessonFormModal from '@/modules/admin/pages/LessonManagementPage/components/LessonFormModal';
import * as adminContentApi from '@/modules/admin/services/adminContentApi';
import * as uploadFile from '@/services/uploadFile';
import { useLang, LANGS, tr, trRx } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminContentApi', () => ({
  createAdminLesson: vi.fn(),
  updateAdminLesson: vi.fn(),
}));

vi.mock('@/services/uploadFile', () => ({
  uploadFileForVerification: vi.fn(),
}));

// YouTubePlayer thật sẽ nạp iframe API của YouTube (jsdom không chạy được),
// nên thay bằng stub có 3 nút để test chủ động giả lập: player sẵn sàng / video chặn embed / không lấy được thời lượng.
vi.mock(
  '@/modules/admin/pages/LessonManagementPage/components/YouTubePlayer',
  async () => {
    const React = (await import('react')).default;
    const MockYouTubePlayer = React.forwardRef(function MockYouTubePlayer(
      { videoId, onReady, onError },
      _ref
    ) {
      return React.createElement(
        'div',
        { 'data-testid': 'youtube-player', 'data-video-id': videoId },
        React.createElement(
          'button',
          { type: 'button', onClick: () => onReady?.({ durationMs: 120000 }) },
          'mock-player-ready'
        ),
        React.createElement(
          'button',
          { type: 'button', onClick: () => onError?.(150) },
          'mock-player-embed-blocked'
        ),
        React.createElement(
          'button',
          { type: 'button', onClick: () => onError?.('DURATION_UNAVAILABLE') },
          'mock-player-duration-unavailable'
        )
      );
    });
    return { default: MockYouTubePlayer };
  }
);

// Helper: lấy chuỗi i18n của namespace admin -> content.lessons.*
const L = (key, options) => tr(`admin:content.lessons.${key}`, options);

// YouTube video id hợp lệ phải đúng 11 ký tự [A-Za-z0-9_-]
const VALID_URL = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ';

describe('LessonFormModal component', () => {
  const mockOnClose = vi.fn();
  const mockOnSuccess = vi.fn();

  const sampleTags = [
    { id: 1, label: 'Grammar' },
    { id: 2, label: 'Vocabulary' },
  ];

  const sampleCefrLevels = [
    { id: 1, label: 'A1' },
    { id: 2, label: 'B1' },
    { id: 3, label: 'B2' },
  ];

  const sampleLesson = {
    id: 1,
    title: 'English Grammar Basics',
    description: 'Learn fundamental grammar rules',
    sourceUrl: VALID_URL,
    durationMs: 600000,
    thumbnailUrl: 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg',
    isPremium: false,
    tags: [{ id: 1, label: 'Grammar' }],
    cefrLevels: [{ id: 2, label: 'B1' }],
  };

  const defaultProps = {
    isOpen: true,
    mode: 'create',
    initialData: null,
    tags: sampleTags,
    cefrLevels: sampleCefrLevels,
    onClose: mockOnClose,
    onSuccess: mockOnSuccess,
  };

  const getTitleInput = () => screen.getByLabelText(`${L('lessonTitleLabel')} *`);
  const getDescInput = () => screen.getByLabelText(L('lessonDescriptionLabel'));
  const getUrlInput = () => screen.getByLabelText(`${L('sourceUrlLabel')} *`);
  const getSubmitBtn = (mode = 'create') =>
    screen.getByRole('button', { name: mode === 'create' ? L('createBtn') : L('saveBtn') });
  const getFileInput = (container) => container.querySelector('input[type="file"]');

  // Điền form tạo mới hợp lệ + giả lập player load xong
  const fillValidCreateForm = async (user, title = 'New Lesson') => {
    await user.type(getTitleInput(), title);
    await user.type(getUrlInput(), VALID_URL);
    await user.click(screen.getByText('mock-player-ready'));
  };

  beforeEach(() => {
    vi.clearAllMocks();
    adminContentApi.createAdminLesson.mockReset();
    adminContentApi.updateAdminLesson.mockReset();
    uploadFile.uploadFileForVerification.mockReset();
    global.URL.createObjectURL = vi.fn(() => 'blob:mock-url');
  });

  describe.each(LANGS)('i18n rendering and form operations [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders create modal with empty form', () => {
      render(<LessonFormModal {...defaultProps} />);

      expect(screen.getByRole('heading', { name: L('createModalTitle') })).toBeInTheDocument();
      expect(getTitleInput()).toHaveValue('');
      expect(getDescInput()).toHaveValue('');
      expect(getUrlInput()).toHaveValue('');
      expect(getSubmitBtn()).toBeDisabled();
    });

    it('renders edit modal with existing data', () => {
      render(<LessonFormModal {...defaultProps} mode="edit" initialData={sampleLesson} />);

      expect(screen.getByRole('heading', { name: L('editModalTitle') })).toBeInTheDocument();
      expect(getTitleInput()).toHaveValue('English Grammar Basics');
      expect(getDescInput()).toHaveValue('Learn fundamental grammar rules');
      expect(getUrlInput()).toHaveValue(VALID_URL);
      // Edit mode đã có durationMs -> video coi như sẵn sàng, nút Lưu bật
      expect(getSubmitBtn('edit')).toBeEnabled();
    });

    it('shows error for invalid YouTube URL', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await user.type(getUrlInput(), 'invalid-url');

      expect(await screen.findByText(L('sourceUrlInvalid'))).toBeInTheDocument();
      expect(screen.queryByTestId('youtube-player')).not.toBeInTheDocument();
    });

    it('shows video preview and duration for a valid YouTube URL', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await user.type(getUrlInput(), VALID_URL);

      expect(screen.getByTestId('youtube-player')).toHaveAttribute('data-video-id', 'dQw4w9WgXcQ');
      expect(screen.queryByText(L('sourceUrlInvalid'))).not.toBeInTheDocument();
      // Nhãn "Thời lượng:" và trạng thái nằm chung 1 span nên match theo regex
      expect(screen.getByText(trRx('admin:content.lessons.durationLoading'))).toBeInTheDocument();

      await user.click(screen.getByText('mock-player-ready'));

      // 120000ms -> 02:00
      expect(screen.getByText(/02:00/)).toBeInTheDocument();
      expect(
        screen.queryByText(trRx('admin:content.lessons.durationLoading'))
      ).not.toBeInTheDocument();
    });

    it('disables submit when title is empty', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await user.type(getUrlInput(), VALID_URL);
      await user.click(screen.getByText('mock-player-ready'));

      expect(getSubmitBtn()).toBeDisabled();
    });

    it('disables submit until the video duration is loaded', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await user.type(getTitleInput(), 'New Lesson');
      await user.type(getUrlInput(), VALID_URL);

      expect(getSubmitBtn()).toBeDisabled();

      await user.click(screen.getByText('mock-player-ready'));

      expect(getSubmitBtn()).toBeEnabled();
    });

    it('shows error and keeps submit disabled when video blocks embedding', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await user.type(getTitleInput(), 'New Lesson');
      await user.type(getUrlInput(), VALID_URL);
      await user.click(screen.getByText('mock-player-embed-blocked'));

      expect(screen.getByText(L('embedBlocked'))).toBeInTheDocument();
      expect(getSubmitBtn()).toBeDisabled();
    });

    it('shows error when video duration is unavailable', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await user.type(getUrlInput(), VALID_URL);
      await user.click(screen.getByText('mock-player-duration-unavailable'));

      expect(screen.getByText(L('videoUnavailable'))).toBeInTheDocument();
    });

    it('creates a new lesson successfully', async () => {
      const saved = { id: 10, title: 'New Lesson', status: 'DRAFT' };
      adminContentApi.createAdminLesson.mockResolvedValueOnce(saved);

      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await fillValidCreateForm(user);
      await user.type(getDescInput(), 'Some description');
      await user.click(screen.getByLabelText('#Grammar'));
      await user.click(screen.getByLabelText('B1'));
      await user.click(screen.getByLabelText(L('isPremiumLabel')));

      await user.click(getSubmitBtn());

      await waitFor(() => {
        expect(adminContentApi.createAdminLesson).toHaveBeenCalledTimes(1);
      });
      expect(adminContentApi.createAdminLesson).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'New Lesson',
          description: 'Some description',
          sourceUrl: VALID_URL,
          durationMs: 120000,
          isPremium: true,
          tagIds: [1],
          cefrLevelIds: [2],
        })
      );
      await waitFor(() => expect(mockOnSuccess).toHaveBeenCalledWith(saved));
    });

    it('trims title before submitting', async () => {
      adminContentApi.createAdminLesson.mockResolvedValueOnce({ id: 11 });

      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await fillValidCreateForm(user, '   Padded Title   ');
      await user.click(getSubmitBtn());

      await waitFor(() => {
        expect(adminContentApi.createAdminLesson).toHaveBeenCalledWith(
          expect.objectContaining({ title: 'Padded Title' })
        );
      });
    });

    it('updates an existing lesson successfully', async () => {
      const saved = { ...sampleLesson, title: 'Updated Title' };
      adminContentApi.updateAdminLesson.mockResolvedValueOnce(saved);

      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} mode="edit" initialData={sampleLesson} />);

      const titleInput = getTitleInput();
      await user.clear(titleInput);
      await user.type(titleInput, 'Updated Title');
      await user.click(getSubmitBtn('edit'));

      await waitFor(() => {
        expect(adminContentApi.updateAdminLesson).toHaveBeenCalledWith(
          1,
          expect.objectContaining({
            title: 'Updated Title',
            sourceUrl: VALID_URL,
            durationMs: 600000,
            tagIds: [1],
            cefrLevelIds: [2],
          })
        );
      });
      await waitFor(() => expect(mockOnSuccess).toHaveBeenCalledWith(saved));
      expect(adminContentApi.createAdminLesson).not.toHaveBeenCalled();
    });

    it('displays error message when create fails', async () => {
      adminContentApi.createAdminLesson.mockRejectedValueOnce(new Error('Network error'));

      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await fillValidCreateForm(user);
      await user.click(getSubmitBtn());

      expect(await screen.findByText('Network error')).toBeInTheDocument();
      expect(mockOnSuccess).not.toHaveBeenCalled();
      // Sau lỗi form vẫn giữ dữ liệu để người dùng thử lại
      expect(getTitleInput()).toHaveValue('New Lesson');
    });

    it('prefers server error message from response', async () => {
      adminContentApi.createAdminLesson.mockRejectedValueOnce({
        response: { data: { message: 'Duplicate lesson' } },
      });

      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await fillValidCreateForm(user);
      await user.click(getSubmitBtn());

      expect(await screen.findByText('Duplicate lesson')).toBeInTheDocument();
    });

    it('toggles tags selection', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      const grammarCheckbox = screen.getByLabelText('#Grammar');
      expect(grammarCheckbox).not.toBeChecked();

      await user.click(grammarCheckbox);
      expect(grammarCheckbox).toBeChecked();

      await user.click(grammarCheckbox);
      expect(grammarCheckbox).not.toBeChecked();
    });

    it('toggles CEFR levels selection', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      const a1Checkbox = screen.getByLabelText('A1');
      expect(a1Checkbox).not.toBeChecked();

      await user.click(a1Checkbox);
      expect(a1Checkbox).toBeChecked();

      await user.click(a1Checkbox);
      expect(a1Checkbox).not.toBeChecked();
    });

    it('toggles isPremium checkbox', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      const premiumCheckbox = screen.getByLabelText(L('isPremiumLabel'));
      expect(premiumCheckbox).not.toBeChecked();

      await user.click(premiumCheckbox);
      expect(premiumCheckbox).toBeChecked();
    });

    it('handles thumbnail upload', async () => {
      uploadFile.uploadFileForVerification.mockResolvedValueOnce('uploaded-key-123');

      const user = userEvent.setup();
      const { container } = render(<LessonFormModal {...defaultProps} />);

      const file = new File(['thumbnail'], 'thumbnail.jpg', { type: 'image/jpeg' });
      await user.upload(getFileInput(container), file);

      await waitFor(() => {
        expect(uploadFile.uploadFileForVerification).toHaveBeenCalledWith(file, 'image/jpeg');
      });
      expect(await screen.findByAltText('Thumbnail preview')).toHaveAttribute(
        'src',
        'blob:mock-url'
      );
    });

    it('sends uploaded thumbnail key when submitting', async () => {
      uploadFile.uploadFileForVerification.mockResolvedValueOnce('uploaded-key-123');
      adminContentApi.createAdminLesson.mockResolvedValueOnce({ id: 12 });

      const user = userEvent.setup();
      const { container } = render(<LessonFormModal {...defaultProps} />);

      await fillValidCreateForm(user);
      await user.upload(
        getFileInput(container),
        new File(['thumbnail'], 'thumbnail.png', { type: 'image/png' })
      );
      await screen.findByAltText('Thumbnail preview');
      await waitFor(() => expect(getSubmitBtn()).toBeEnabled());

      await user.click(getSubmitBtn());

      await waitFor(() => {
        expect(adminContentApi.createAdminLesson).toHaveBeenCalledWith(
          expect.objectContaining({ thumbnailUrl: 'uploaded-key-123' })
        );
      });
    });

    it('validates thumbnail file type', async () => {
      // user-event mặc định lọc file theo thuộc tính accept của input,
      // nên phải tắt applyAccept để file PDF thật sự được đưa vào onChange.
      const user = userEvent.setup({ applyAccept: false });
      const { container } = render(<LessonFormModal {...defaultProps} />);

      const file = new File(['document'], 'document.pdf', { type: 'application/pdf' });
      await user.upload(getFileInput(container), file);

      expect(await screen.findByText(L('thumbnailTypeError'))).toBeInTheDocument();
      expect(uploadFile.uploadFileForVerification).not.toHaveBeenCalled();
    });

    it('validates thumbnail file size', async () => {
      const user = userEvent.setup();
      const { container } = render(<LessonFormModal {...defaultProps} />);

      const largeFile = new File(['x'], 'large.jpg', { type: 'image/jpeg' });
      Object.defineProperty(largeFile, 'size', { value: 6 * 1024 * 1024 });
      await user.upload(getFileInput(container), largeFile);

      expect(await screen.findByText(L('thumbnailSizeError'))).toBeInTheDocument();
      expect(uploadFile.uploadFileForVerification).not.toHaveBeenCalled();
    });

    it('shows error when thumbnail upload fails', async () => {
      uploadFile.uploadFileForVerification.mockRejectedValueOnce(new Error('Upload exploded'));

      const user = userEvent.setup();
      const { container } = render(<LessonFormModal {...defaultProps} />);

      await user.upload(
        getFileInput(container),
        new File(['thumbnail'], 'thumbnail.jpg', { type: 'image/jpeg' })
      );

      expect(await screen.findByText('Upload exploded')).toBeInTheDocument();
    });

    it('removes uploaded thumbnail', async () => {
      uploadFile.uploadFileForVerification.mockResolvedValueOnce('uploaded-key-123');

      const user = userEvent.setup();
      const { container } = render(<LessonFormModal {...defaultProps} />);

      await user.upload(
        getFileInput(container),
        new File(['thumbnail'], 'thumbnail.jpg', { type: 'image/jpeg' })
      );

      const removeBtn = await screen.findByRole('button', { name: L('removeImage') });
      await user.click(removeBtn);

      expect(screen.queryByRole('button', { name: L('removeImage') })).not.toBeInTheDocument();
      expect(screen.queryByAltText('Thumbnail preview')).not.toBeInTheDocument();
    });

    it('closes modal when close (X) button is clicked', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      // Nút X chỉ có aria-label nên getByLabelText trả đúng 1 phần tử
      await user.click(screen.getByLabelText(L('cancelBtn')));

      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });

    it('closes modal when cancel button is clicked', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      // Tìm theo text để không khớp nhầm nút X (cùng accessible name)
      await user.click(screen.getByText(L('cancelBtn')));

      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });

    it('closes modal when backdrop is clicked', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await user.click(screen.getByRole('dialog').parentElement);

      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });

    it('does not close modal when dialog content is clicked', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      await user.click(screen.getByRole('dialog'));

      expect(mockOnClose).not.toHaveBeenCalled();
    });

    it('limits title length to 100 characters', async () => {
      const user = userEvent.setup();
      render(<LessonFormModal {...defaultProps} />);

      const titleInput = getTitleInput();
      await user.type(titleInput, 'a'.repeat(101));

      expect(titleInput).toHaveAttribute('maxlength', '100');
      expect(titleInput.value).toHaveLength(100);
    });

    it('limits description length to 1000 characters', () => {
      render(<LessonFormModal {...defaultProps} />);

      expect(getDescInput()).toHaveAttribute('maxlength', '1000');
    });

    it('renders with no tags', () => {
      render(<LessonFormModal {...defaultProps} tags={[]} />);

      expect(screen.getByRole('heading', { name: L('createModalTitle') })).toBeInTheDocument();
      expect(screen.queryByText(L('selectTags'))).not.toBeInTheDocument();
    });

    it('renders with no CEFR levels', () => {
      render(<LessonFormModal {...defaultProps} cefrLevels={[]} />);

      expect(screen.getByRole('heading', { name: L('createModalTitle') })).toBeInTheDocument();
      expect(screen.queryByText(L('selectCefr'))).not.toBeInTheDocument();
    });

    it('returns null when modal is closed', () => {
      const { container } = render(<LessonFormModal {...defaultProps} isOpen={false} />);

      expect(container.firstChild).toBeNull();
    });

    it('pre-selects tags and CEFR levels in edit mode', () => {
      render(<LessonFormModal {...defaultProps} mode="edit" initialData={sampleLesson} />);

      expect(screen.getByLabelText('#Grammar')).toBeChecked();
      expect(screen.getByLabelText('#Vocabulary')).not.toBeChecked();
      expect(screen.getByLabelText('B1')).toBeChecked();
      expect(screen.getByLabelText('A1')).not.toBeChecked();
    });

    it('shows premium checkbox as checked in edit mode when lesson is premium', () => {
      const premiumLesson = { ...sampleLesson, isPremium: true };
      render(<LessonFormModal {...defaultProps} mode="edit" initialData={premiumLesson} />);

      expect(screen.getByLabelText(L('isPremiumLabel'))).toBeChecked();
    });
  });
});
