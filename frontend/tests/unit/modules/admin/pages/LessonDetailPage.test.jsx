import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LessonDetailPage from '@/modules/admin/pages/LessonManagementPage/LessonDetailPage';
import * as adminContentApi from '@/modules/admin/services/adminContentApi';
import { useLang, LANGS, tr } from '../../../../utils/i18n';

// Spy dùng chung cho stub YouTubePlayer (phải khai báo bằng vi.hoisted vì vi.mock được hoist lên đầu file)
const { mockSeekTo } = vi.hoisted(() => ({ mockSeekTo: vi.fn() }));

vi.mock('@/modules/admin/services/adminContentApi', () => ({
  fetchAdminLessonDetail: vi.fn(),
  fetchLessonSegments: vi.fn(),
  createLessonSegment: vi.fn(),
  updateLessonSegment: vi.fn(),
  deleteLessonSegments: vi.fn(),
  createAdminLesson: vi.fn(),
  updateAdminLesson: vi.fn(),
  publishAdminLesson: vi.fn(),
  unpublishAdminLesson: vi.fn(),
  archiveAdminLesson: vi.fn(),
  fetchAdminTags: vi.fn(),
  fetchCefrLevels: vi.fn(),
}));

vi.mock('@/services/uploadFile', () => ({
  uploadFileForVerification: vi.fn(),
}));

// YouTubePlayer thật sẽ nạp iframe API của YouTube (jsdom không chạy được) nên dùng stub.
// Stub phơi ra các hàm điều khiển qua ref giống player thật để kiểm tra việc seek khi chọn đoạn.
vi.mock(
  '@/modules/admin/pages/LessonManagementPage/components/YouTubePlayer',
  async () => {
    const React = (await import('react')).default;
    const MockYouTubePlayer = React.forwardRef(function MockYouTubePlayer({ videoId }, ref) {
      React.useImperativeHandle(ref, () => ({
        seekTo: mockSeekTo,
        getCurrentTime: () => 0,
        play: () => {},
        pause: () => {},
        getDurationMs: () => 0,
      }));
      return React.createElement('div', {
        'data-testid': 'youtube-player',
        'data-video-id': videoId,
      });
    });
    return { default: MockYouTubePlayer };
  }
);

// Helpers: lấy chuỗi i18n của namespace admin
const L = (key, options) => tr(`admin:content.lessons.${key}`, options);
const A = (key, options) => tr(`admin:${key}`, options);

// YouTube video id hợp lệ phải đúng 11 ký tự [A-Za-z0-9_-]
const VALID_URL = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ';

describe('LessonDetailPage component', () => {
  const mockNavigate = vi.fn();

  const sampleLesson = {
    id: 1,
    title: 'English Grammar Basics',
    slug: 'english-grammar-basics',
    description: 'Learn fundamental grammar rules',
    sourceUrl: VALID_URL,
    durationMs: 600000,
    thumbnailUrl: 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg',
    isPremium: false,
    status: 'PUBLISHED',
    tags: [{ id: 1, label: 'Grammar' }],
    cefrLevels: [{ id: 2, label: 'B1' }],
  };

  // Lưu ý: field của đoạn phụ đề là transcriptOriginal (không phải text)
  const sampleSegments = [
    {
      id: 10,
      startMs: 0,
      endMs: 60000,
      transcriptOriginal: 'Introduction to grammar',
      translation: 'Giới thiệu về ngữ pháp',
    },
    {
      id: 11,
      startMs: 60000,
      endMs: 120000,
      transcriptOriginal: 'Parts of speech overview',
      translation: 'Tổng quan về các loại từ',
    },
  ];

  const getDialog = () => within(screen.getByRole('dialog'));
  const renderPage = () => render(<LessonDetailPage lessonId="1" onNavigate={mockNavigate} />);
  const waitForPage = () =>
    screen.findByRole('heading', { level: 1, name: 'English Grammar Basics' });
  const transcriptLabel = () => `${L('transcriptLabel')} *`;
  const translationLabel = () => `${L('translationLabel')} *`;

  const selectSegment = async (user, text = 'Introduction to grammar') => {
    await user.click(screen.getByText(text));
  };

  beforeEach(() => {
    vi.clearAllMocks();
    Object.values(adminContentApi).forEach((fn) => fn.mockReset());

    adminContentApi.fetchAdminLessonDetail.mockResolvedValue(sampleLesson);
    adminContentApi.fetchLessonSegments.mockResolvedValue(sampleSegments);
    adminContentApi.fetchAdminTags.mockResolvedValue([
      { id: 1, label: 'Grammar' },
      { id: 2, label: 'Vocabulary' },
    ]);
    adminContentApi.fetchCefrLevels.mockResolvedValue([
      { id: 1, label: 'A1' },
      { id: 2, label: 'B1' },
      { id: 3, label: 'B2' },
    ]);
  });

  describe.each(LANGS)('i18n rendering and Lesson detail operations [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    // ───────────────────────── Hiển thị ─────────────────────────

    it('renders lesson detail with segments', async () => {
      renderPage();

      expect(await waitForPage()).toBeInTheDocument();
      expect(screen.getByText('Introduction to grammar')).toBeInTheDocument();
      expect(screen.getByText('Giới thiệu về ngữ pháp')).toBeInTheDocument();
      expect(screen.getByText('Parts of speech overview')).toBeInTheDocument();
      expect(screen.getByText('Learn fundamental grammar rules')).toBeInTheDocument();
      expect(screen.getByTestId('youtube-player')).toHaveAttribute(
        'data-video-id',
        'dQw4w9WgXcQ'
      );
    });

    it('does not render lesson content while loading', () => {
      adminContentApi.fetchAdminLessonDetail.mockImplementationOnce(() => new Promise(() => {}));

      renderPage();

      expect(screen.queryByRole('heading', { level: 1 })).not.toBeInTheDocument();
    });

    it('fetches lesson and segments by lessonId', async () => {
      renderPage();
      await waitForPage();

      expect(adminContentApi.fetchAdminLessonDetail).toHaveBeenCalledWith('1');
      expect(adminContentApi.fetchLessonSegments).toHaveBeenCalledWith('1');
    });

    it('displays lesson meta information', async () => {
      renderPage();
      await waitForPage();

      expect(screen.getByText(L('statusPublished'))).toBeInTheDocument();
      expect(screen.getByText(L('freeTier'))).toBeInTheDocument();
      expect(screen.getByText('10:00')).toBeInTheDocument();
      expect(screen.getByText(L('segmentsCount', { count: 2 }))).toBeInTheDocument();
      expect(screen.getByText('B1')).toBeInTheDocument();
      expect(screen.getByText('#Grammar')).toBeInTheDocument();
    });

    it('displays premium badge when lesson is premium', async () => {
      adminContentApi.fetchAdminLessonDetail.mockResolvedValue({
        ...sampleLesson,
        isPremium: true,
      });

      renderPage();
      await waitForPage();

      expect(screen.getByText(L('premiumTier'))).toBeInTheDocument();
      expect(screen.queryByText(L('freeTier'))).not.toBeInTheDocument();
    });

    it('displays archived status badge', async () => {
      adminContentApi.fetchAdminLessonDetail.mockResolvedValue({
        ...sampleLesson,
        status: 'ARCHIVED',
      });

      renderPage();
      await waitForPage();

      expect(screen.getByText(L('statusArchived'))).toBeInTheDocument();
    });

    it('opens external source link in a new tab', async () => {
      renderPage();
      await waitForPage();

      const sourceLink = screen.getByRole('link', { name: L('sourceLink') });
      expect(sourceLink).toHaveAttribute('href', VALID_URL);
      expect(sourceLink).toHaveAttribute('target', '_blank');
      expect(sourceLink).toHaveAttribute('rel', expect.stringContaining('noopener'));
    });

    it('shows video unavailable message when source URL is not a YouTube link', async () => {
      adminContentApi.fetchAdminLessonDetail.mockResolvedValue({
        ...sampleLesson,
        sourceUrl: 'https://example.com/not-youtube',
      });

      renderPage();
      await waitForPage();

      expect(screen.getByText(L('videoUnavailable'))).toBeInTheDocument();
      expect(screen.queryByTestId('youtube-player')).not.toBeInTheDocument();
    });

    it('shows empty state when lesson has no segments', async () => {
      adminContentApi.fetchLessonSegments.mockResolvedValue([]);

      renderPage();
      await waitForPage();

      expect(screen.getByText(L('noSegmentsTitle'))).toBeInTheDocument();
      expect(screen.getByText(L('noSegmentsDesc'))).toBeInTheDocument();
      // Không có đoạn nào thì không hiện nút "Chọn nhiều"
      expect(screen.queryByRole('button', { name: L('selectModeBtn') })).not.toBeInTheDocument();
    });

    it('shows select hint when no segment is selected', async () => {
      renderPage();
      await waitForPage();

      expect(screen.getByText(L('selectHint'))).toBeInTheDocument();
    });

    // ───────────────────────── Điều hướng & modal thông tin bài học ─────────────────────────

    it('navigates back to lesson list when back button is clicked', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await user.click(screen.getByRole('button', { name: L('backToLessons') }));

      expect(mockNavigate).toHaveBeenCalledWith('/admin/lessons');
    });

    it('opens edit lesson modal and updates lesson info', async () => {
      adminContentApi.updateAdminLesson.mockResolvedValueOnce({
        ...sampleLesson,
        title: 'Renamed Lesson',
      });

      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await user.click(screen.getByRole('button', { name: L('editLessonBtn') }));

      const dialog = getDialog();
      expect(dialog.getByRole('heading', { name: L('editModalTitle') })).toBeInTheDocument();

      const titleInput = dialog.getByLabelText(`${L('lessonTitleLabel')} *`);
      expect(titleInput).toHaveValue('English Grammar Basics');

      await user.clear(titleInput);
      await user.type(titleInput, 'Renamed Lesson');
      await user.click(dialog.getByRole('button', { name: L('saveBtn') }));

      await waitFor(() => {
        expect(adminContentApi.updateAdminLesson).toHaveBeenCalledWith(
          1,
          expect.objectContaining({ title: 'Renamed Lesson' })
        );
      });
      expect(await screen.findByText(L('lessonUpdateSuccess'))).toBeInTheDocument();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      // Sau khi sửa phải tải lại dữ liệu bài học
      await waitFor(() => {
        expect(adminContentApi.fetchAdminLessonDetail.mock.calls.length).toBeGreaterThanOrEqual(2);
      });
    });

    it('opens change status modal and archives the lesson', async () => {
      adminContentApi.archiveAdminLesson.mockResolvedValueOnce({
        ...sampleLesson,
        status: 'ARCHIVED',
      });

      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await user.click(screen.getByRole('button', { name: L('changeStatusBtn') }));

      // Dialog có cùng chữ "Đổi trạng thái" với nút mở nên phải tìm trong dialog
      const dialog = getDialog();
      expect(dialog.getByRole('heading', { name: L('changeStatusBtn') })).toBeInTheDocument();

      await user.selectOptions(dialog.getByLabelText(L('statusFilter')), 'ARCHIVED');
      await user.click(dialog.getByRole('button', { name: L('saveBtn') }));

      await waitFor(() => {
        expect(adminContentApi.archiveAdminLesson).toHaveBeenCalledWith(1);
      });
      expect(await screen.findByText(L('statusUpdateSuccess'))).toBeInTheDocument();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    // ───────────────────────── Chọn / tạo / sửa đoạn ─────────────────────────

    it('selects a segment from the list and seeks the player', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user, 'Parts of speech overview');

      expect(screen.getByRole('heading', { name: L('editSegmentTitle') })).toBeInTheDocument();
      expect(screen.getByLabelText(transcriptLabel())).toHaveValue('Parts of speech overview');
      expect(screen.getByLabelText(translationLabel())).toHaveValue('Tổng quan về các loại từ');
      expect(screen.getByLabelText(L('startTimeLabel'))).toHaveValue('01:00');
      expect(screen.getByLabelText(L('endTimeLabel'))).toHaveValue('02:00');
      // startMs = 60000 -> seek tới giây thứ 60
      expect(mockSeekTo).toHaveBeenCalledWith(60);
    });

    it('creates a new segment', async () => {
      const created = {
        id: 12,
        startMs: 120000,
        endMs: 125000,
        transcriptOriginal: 'New segment',
        translation: 'Đoạn mới',
      };
      adminContentApi.createLessonSegment.mockResolvedValueOnce(created);

      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await user.click(screen.getByRole('button', { name: L('addSegment') }));

      expect(screen.getByRole('heading', { name: L('newSegmentTitle') })).toBeInTheDocument();
      // Mặc định bắt đầu tại thời điểm kết thúc đoạn cuối, dài 5 giây
      expect(screen.getByLabelText(L('startTimeLabel'))).toHaveValue('02:00');
      expect(screen.getByLabelText(L('endTimeLabel'))).toHaveValue('02:05');

      const saveBtn = screen.getByRole('button', { name: L('saveSegmentBtn') });
      expect(saveBtn).toBeDisabled();

      await user.type(screen.getByLabelText(transcriptLabel()), 'New segment');
      await user.type(screen.getByLabelText(translationLabel()), 'Đoạn mới');
      expect(saveBtn).toBeEnabled();

      // Sau khi lưu, trang tải lại danh sách và phải có đoạn mới
      adminContentApi.fetchLessonSegments.mockResolvedValue([...sampleSegments, created]);
      await user.click(saveBtn);

      await waitFor(() => {
        expect(adminContentApi.createLessonSegment).toHaveBeenCalledWith({
          lessonId: 1,
          startMs: 120000,
          endMs: 125000,
          transcriptOriginal: 'New segment',
          translation: 'Đoạn mới',
        });
      });
      expect(await screen.findByText(L('segmentSaveSuccess'))).toBeInTheDocument();
      // Dùng role button thay vì getByText: sau khi lưu, đoạn mới được chọn nên nội dung
      // vừa nằm trong danh sách vừa nằm trong textarea của form -> getByText sẽ khớp nhiều phần tử.
      expect(await screen.findByRole('button', { name: /New segment/ })).toBeInTheDocument();
    });

    it('shows validation error when end time is not after start time', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await user.click(screen.getByRole('button', { name: L('addSegment') }));
      await user.type(screen.getByLabelText(transcriptLabel()), 'New segment');
      await user.type(screen.getByLabelText(translationLabel()), 'Đoạn mới');

      const endInput = screen.getByLabelText(L('endTimeLabel'));
      await user.clear(endInput);
      await user.type(endInput, '0001');

      expect(endInput).toHaveValue('00:01');
      expect(screen.getByText(L('timeOrderError'))).toBeInTheDocument();
      expect(screen.getByRole('button', { name: L('saveSegmentBtn') })).toBeDisabled();
    });

    it('shows validation error when segment overlaps another segment', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await user.click(screen.getByRole('button', { name: L('addSegment') }));

      const startInput = screen.getByLabelText(L('startTimeLabel'));
      const endInput = screen.getByLabelText(L('endTimeLabel'));
      await user.clear(startInput);
      await user.type(startInput, '0030');
      await user.clear(endInput);
      await user.type(endInput, '0050');

      expect(screen.getByText(L('timeOverlapError'))).toBeInTheDocument();
      expect(screen.getByRole('button', { name: L('saveSegmentBtn') })).toBeDisabled();
    });

    it('updates an existing segment', async () => {
      adminContentApi.updateLessonSegment.mockResolvedValueOnce({
        ...sampleSegments[0],
        transcriptOriginal: 'Updated introduction',
      });

      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user);

      const saveBtn = screen.getByRole('button', { name: L('saveSegmentBtn') });
      // Chưa sửa gì thì chưa được lưu
      expect(saveBtn).toBeDisabled();

      const transcriptInput = screen.getByLabelText(transcriptLabel());
      await user.clear(transcriptInput);
      await user.type(transcriptInput, 'Updated introduction');
      await user.click(saveBtn);

      await waitFor(() => {
        expect(adminContentApi.updateLessonSegment).toHaveBeenCalledWith(10, {
          startMs: 0,
          endMs: 60000,
          transcriptOriginal: 'Updated introduction',
          translation: 'Giới thiệu về ngữ pháp',
        });
      });
      expect(await screen.findByText(L('segmentSaveSuccess'))).toBeInTheDocument();
    });

    it('shows server error when saving a segment fails', async () => {
      adminContentApi.updateLessonSegment.mockRejectedValueOnce(new Error('Save failed'));

      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user);
      const transcriptInput = screen.getByLabelText(transcriptLabel());
      await user.clear(transcriptInput);
      await user.type(transcriptInput, 'Something else');
      await user.click(screen.getByRole('button', { name: L('saveSegmentBtn') }));

      expect(await screen.findByText('Save failed')).toBeInTheDocument();
    });

    it('cancels segment form without saving', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user);
      expect(screen.getByLabelText(transcriptLabel())).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: L('cancelBtn') }));

      expect(screen.queryByLabelText(transcriptLabel())).not.toBeInTheDocument();
      expect(screen.getByText(L('selectHint'))).toBeInTheDocument();
      expect(adminContentApi.updateLessonSegment).not.toHaveBeenCalled();
    });

    // ───────────────────────── Xóa 1 đoạn ─────────────────────────

    it('deletes a segment after confirmation', async () => {
      adminContentApi.deleteLessonSegments.mockResolvedValueOnce({});

      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user);
      await user.click(screen.getByRole('button', { name: L('deleteSegmentBtn') }));

      // Dialog có nút xác nhận trùng tên với nút xóa ở form nên tìm trong dialog
      const dialog = getDialog();
      expect(dialog.getByRole('heading', { name: L('deleteSegmentTitle') })).toBeInTheDocument();
      expect(adminContentApi.deleteLessonSegments).not.toHaveBeenCalled();

      await user.click(dialog.getByRole('button', { name: L('deleteSegmentBtn') }));

      await waitFor(() => {
        expect(adminContentApi.deleteLessonSegments).toHaveBeenCalledWith(1, [10]);
      });
      expect(await screen.findByText(L('segmentDeleteSuccess'))).toBeInTheDocument();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('shows error inside delete modal when deletion fails', async () => {
      adminContentApi.deleteLessonSegments.mockRejectedValueOnce(new Error('Delete failed'));

      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user);
      await user.click(screen.getByRole('button', { name: L('deleteSegmentBtn') }));
      await user.click(getDialog().getByRole('button', { name: L('deleteSegmentBtn') }));

      expect(await getDialog().findByText('Delete failed')).toBeInTheDocument();
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('cancels delete segment modal', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user);
      await user.click(screen.getByRole('button', { name: L('deleteSegmentBtn') }));

      // Tìm theo text để không khớp nhầm nút X (cùng accessible name "Hủy"/"Cancel")
      await user.click(getDialog().getByText(L('cancelBtn')));

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(adminContentApi.deleteLessonSegments).not.toHaveBeenCalled();
    });

    it('closes delete modal when Escape is pressed', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user);
      await user.click(screen.getByRole('button', { name: L('deleteSegmentBtn') }));
      expect(screen.getByRole('dialog')).toBeInTheDocument();

      await user.keyboard('{Escape}');

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    // ───────────────────────── Chọn nhiều & xóa hàng loạt ─────────────────────────

    it('enables multi-select mode for bulk delete', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await user.click(screen.getByRole('button', { name: L('selectModeBtn') }));

      // 1 checkbox "Chọn tất cả" + 1 checkbox cho mỗi đoạn
      expect(screen.getAllByRole('checkbox')).toHaveLength(1 + sampleSegments.length);
      expect(screen.getByRole('button', { name: L('exitSelectMode') })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: L('addSegment') })).toBeDisabled();
      expect(screen.getByRole('button', { name: L('bulkDeleteBtn') })).toBeDisabled();
    });

    it('exits multi-select mode', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await user.click(screen.getByRole('button', { name: L('selectModeBtn') }));
      await user.click(screen.getByRole('button', { name: L('exitSelectMode') }));

      expect(screen.queryAllByRole('checkbox')).toHaveLength(0);
      expect(screen.getByRole('button', { name: L('selectModeBtn') })).toBeInTheDocument();
    });

    it('toggles all checkboxes in select mode', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await user.click(screen.getByRole('button', { name: L('selectModeBtn') }));

      const selectAll = screen.getByLabelText(L('selectAll'));
      await user.click(selectAll);

      screen.getAllByRole('checkbox').forEach((checkbox) => {
        expect(checkbox).toBeChecked();
      });
      expect(screen.getByText(L('selectedCount', { count: 2 }))).toBeInTheDocument();

      await user.click(selectAll);

      screen.getAllByRole('checkbox').forEach((checkbox) => {
        expect(checkbox).not.toBeChecked();
      });
    });

    it('performs bulk delete of all selected segments', async () => {
      adminContentApi.deleteLessonSegments.mockResolvedValueOnce({});

      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await user.click(screen.getByRole('button', { name: L('selectModeBtn') }));
      await user.click(screen.getByLabelText(L('selectAll')));
      await user.click(screen.getByRole('button', { name: L('bulkDeleteBtn') }));

      const dialog = getDialog();
      expect(
        dialog.getByRole('heading', { name: L('bulkDeleteTitle', { count: 2 }) })
      ).toBeInTheDocument();

      await user.click(dialog.getByRole('button', { name: L('bulkDeleteBtn') }));

      await waitFor(() => {
        expect(adminContentApi.deleteLessonSegments).toHaveBeenCalledWith(1, [10, 11]);
      });
      expect(
        await screen.findByText(L('bulkDeleteSuccess', { count: 2 }))
      ).toBeInTheDocument();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('bulk deletes only the segments that were checked', async () => {
      adminContentApi.deleteLessonSegments.mockResolvedValueOnce({});

      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await user.click(screen.getByRole('button', { name: L('selectModeBtn') }));
      // Ở select mode, bấm vào đoạn sẽ tick/bỏ tick đoạn đó
      await user.click(screen.getByText('Parts of speech overview'));

      expect(screen.getByText(L('selectedCount', { count: 1 }))).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: L('bulkDeleteBtn') }));
      await user.click(getDialog().getByRole('button', { name: L('bulkDeleteBtn') }));

      await waitFor(() => {
        expect(adminContentApi.deleteLessonSegments).toHaveBeenCalledWith(1, [11]);
      });
    });

    // ───────────────────────── Thay đổi chưa lưu ─────────────────────────

    it('shows discard confirmation when switching segment with unsaved changes', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user);
      await user.type(screen.getByLabelText(transcriptLabel()), ' - modified');

      await user.click(screen.getByRole('button', { name: L('addSegment') }));

      const dialog = getDialog();
      expect(dialog.getByRole('heading', { name: L('discardTitle') })).toBeInTheDocument();
      expect(dialog.getByText(L('discardDesc'))).toBeInTheDocument();
    });

    it('keeps editing when "keep editing" is chosen in discard modal', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user);
      await user.type(screen.getByLabelText(transcriptLabel()), ' - modified');
      await user.click(screen.getByRole('button', { name: L('addSegment') }));

      await user.click(getDialog().getByRole('button', { name: L('keepEditingBtn') }));

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(screen.getByLabelText(transcriptLabel())).toHaveValue(
        'Introduction to grammar - modified'
      );
      expect(screen.getByRole('heading', { name: L('editSegmentTitle') })).toBeInTheDocument();
    });

    it('discards changes and opens the new segment form', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user);
      await user.type(screen.getByLabelText(transcriptLabel()), ' - modified');
      await user.click(screen.getByRole('button', { name: L('addSegment') }));

      await user.click(getDialog().getByRole('button', { name: L('discardBtn') }));

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(screen.getByRole('heading', { name: L('newSegmentTitle') })).toBeInTheDocument();
      expect(screen.getByLabelText(transcriptLabel())).toHaveValue('');
    });

    it('asks to discard unsaved changes before entering select mode', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForPage();

      await selectSegment(user);
      await user.type(screen.getByLabelText(transcriptLabel()), ' - modified');
      await user.click(screen.getByRole('button', { name: L('selectModeBtn') }));

      const dialog = getDialog();
      expect(dialog.getByRole('heading', { name: L('discardTitle') })).toBeInTheDocument();

      await user.click(dialog.getByRole('button', { name: L('discardBtn') }));

      expect(screen.getByRole('button', { name: L('exitSelectMode') })).toBeInTheDocument();
    });

    // ───────────────────────── Lỗi ─────────────────────────

    it('renders error state and retries', async () => {
      adminContentApi.fetchAdminLessonDetail.mockRejectedValueOnce(new Error('Lesson not found'));

      const user = userEvent.setup();
      renderPage();

      expect(await screen.findByText('Lesson not found')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: L('backToLessons') })).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: A('feedback.retry') }));

      expect(await waitForPage()).toBeInTheDocument();
      expect(screen.queryByText('Lesson not found')).not.toBeInTheDocument();
    });

    it('navigates back from the error state', async () => {
      adminContentApi.fetchAdminLessonDetail.mockRejectedValueOnce(new Error('Lesson not found'));

      const user = userEvent.setup();
      renderPage();
      await screen.findByText('Lesson not found');

      await user.click(screen.getByRole('button', { name: L('backToLessons') }));

      expect(mockNavigate).toHaveBeenCalledWith('/admin/lessons');
    });
  });
});
